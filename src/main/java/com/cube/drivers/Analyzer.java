/*
 *
 *    Copyright Cube I O
 *
 */

package com.cube.drivers;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import static com.cube.core.Comparator.MatchType.ExactMatch;

import com.cube.cache.RequestComparatorCache;
import com.cube.cache.ResponseComparatorCache;
import com.cube.cache.TemplateKey;
import com.cube.core.Comparator;
import com.cube.core.*;
import com.cube.dao.*;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-03-08
 * @author Prasad M D
 */
public class Analyzer {

    private static final Logger LOGGER = LogManager.getLogger(Analyzer.class);

    private Analyzer(String replayid, int reqcnt, ObjectMapper jsonmapper,
                     RequestComparatorCache requestComparatorCache, ResponseComparatorCache responseComparatorCache,
                     Optional<String> templateVersion) {
        analysis = new Analysis(replayid, reqcnt, templateVersion);
        this.jsonmapper = jsonmapper;

        //comparator = new TemplatedResponseComparator(TemplatedRRComparator.EQUALITYTEMPLATE, jsonmapper);
        //TemplatedRRComparator.EQUALITYTEMPLATE;
        this.responseComparatorCache = responseComparatorCache;
        this.requestComparatorCache = requestComparatorCache;
        this.templateVersion = templateVersion;
    }


    private Analysis analysis;
    //private ResponseComparator comparator = ResponseComparator.EQUALITYCOMPARATOR;
    private final ObjectMapper jsonmapper;
    // Template cache being passed from the config
    private final RequestComparatorCache requestComparatorCache;
    private final ResponseComparatorCache responseComparatorCache;
    private final Optional<String> templateVersion;




    /**
     * @param rrstore
     * @param reqs
     */
    private void analyze(ReqRespStore rrstore, Stream<List<Request>> reqs, Replay replay) {

        // using seed generated from replayid so that same requests get picked in replay and analyze
        long seed = replay.replayid.hashCode();
        Random random = new Random(seed);

        reqs.forEach(requestList -> {
            // for each batch of requests, expand on trace id for the given list of intermediate services
            // (a property of replay)
            List<Request> filteredList = requestList.stream().filter(request ->
                    replay.samplerate.map(sr -> random.nextDouble() <= sr).orElse(true))
                    .collect(Collectors.toList());

            Stream<Request> enhancedRequests = rrstore.expandOnTraceId(filteredList, replay.intermediateServices
                    ,replay.collection);

            enhancedRequests.forEach(r -> {


                // find matching request in replay
                // most fields are same as request except
                // RRType should be Replay
                // collection to set to replayid, since collection in replays are set to replayids
                Request rq = new Request(r.path, r.reqid, r.qparams, r.fparams, r.meta,
                        r.hdrs, r.method, r.body, Optional.ofNullable(analysis.replayid), r.timestamp,
                        Optional.of(RRBase.RR.Replay), r.customerid, r.app);

                List<Request> matches = new ArrayList<>();

                TemplateKey key = new TemplateKey(templateVersion, r.customerid.get(), r.app.get(),
                    r.getService().get(), r.path
                        , TemplateKey.Type.Request);
                RequestComparator comparator = requestComparatorCache.getRequestComparator(key, false);
                matches = rrstore.getRequests(rq, comparator, Optional.of(10))
                        .collect(Collectors.toList());
                // TODO: add toString override for the Request object to debug log
                if (!matches.isEmpty()) {
                    Map<String, Response> replayResponseMap = rrstore.getResponses(matches);
                    Optional<Response> recordedResponse = r.reqid.flatMap(rrstore::getResponse);
                    if (matches.size() > 1) {
                        analysis.reqmultiplematch++;
                    } else {
                        analysis.reqsinglematch++;
                    }

                    // fetch response of recording and replay
                    // TODO change it back to RecReqNoMatch
                    Analysis.RespMatchWithReq bestmatch = new Analysis.RespMatchWithReq(r, Optional.empty(),
                            Comparator.Match.DEFAULT, Optional.empty(), Optional.empty());
                    Comparator.MatchType bestreqmt = Comparator.MatchType.NoMatch;

                    // matches is ordered in decreasing order of request match score. so exact matches
                    // of requests, if any should be at the beginning
                    // If request matches exactly, consider that as the best match
                    // else find the best match based on response matching
                    for (Request replayreq : matches) {
                        Comparator.MatchType reqmt = comparator.compare(rq, replayreq);
                        Analysis.RespMatchWithReq match = checkRespMatch(r, replayreq, recordedResponse, replayResponseMap);
                        if (isReqRespMatchBetter(reqmt, match.getmt(), bestreqmt, bestmatch.getmt())) {
                            bestmatch = match;
                            bestreqmt = reqmt;
                            if (bestmatch.getmt() == ExactMatch) {
                                break;
                            }
                        }
                    }
                    // compare & write out result
                    if (bestreqmt == ExactMatch) {
                        analysis.reqmatched++;
                    } else {
                        analysis.reqpartiallymatched++;
                    }
                    switch (bestmatch.getmt()) {
                        case ExactMatch:
                            analysis.respmatched++;
                            break;
                        case FuzzyMatch:
                            analysis.resppartiallymatched++;
                            break;
                        default:
                            analysis.respnotmatched++;
                            break;
                    }

                    LOGGER.debug(bestmatch.getmt() + " OCCURRED FOR RESPONSE :: " + r.reqid.orElse("-1"));
                    LOGGER.debug("REQUEST 1 " + bestmatch.getRecordReq(jsonmapper).orElse(" N/A"));
                    LOGGER.debug("REQUEST 2 " + bestmatch.getReplayReq(jsonmapper).orElse("N/A"));
                    LOGGER.debug("DOC 1 " + bestmatch.getRecordedResponseBody().orElse(" N/A"));
                    LOGGER.debug("DOC 2 " + bestmatch.getReplayResponseBody().orElse(" N/A"));
                    bestmatch.getDiffs().forEach(
                            diff -> {
                                try {
                                    LOGGER.debug("DIFF :: " + jsonmapper.writeValueAsString(diff));
                                } catch (JsonProcessingException e) {
                                    // DO NOTHING
                                }
                            });

                    Analysis.ReqRespMatchResult res = new Analysis.ReqRespMatchResult(bestmatch, bestreqmt, matches.size(),
                            analysis.replayid, jsonmapper);
                    rrstore.saveResult(res);

                } else {
                    // TODO change it back to RecReqNoMatch
                    Analysis.ReqRespMatchResult res = new Analysis.ReqRespMatchResult(new Analysis.RespMatchWithReq(r,
                        Optional.empty(), Comparator.Match.NOMATCH, Optional.empty() , Optional.empty()),
                        Comparator.MatchType.NoMatch, matches.size(), analysis.replayid, jsonmapper);
                    rrstore.saveResult(res);
                    analysis.reqnotmatched++;
                }


                analysis.reqanalyzed++;
                if (analysis.reqanalyzed % UPDBATCHSIZE == 0) {
                    LOGGER.info(String.format("Analysis of replay %s completed %d requests", analysis.replayid, analysis.reqanalyzed));
                    rrstore.saveAnalysis(analysis);
                }

            });
        });
        analysis.status = Analysis.Status.Completed;

    }

    private Analysis.RespMatchWithReq checkRespMatch(Request recordreq, Request replayreq, Optional<Response> recordedResponse ,
                                                     Map<String, Response> replayResponseMap) {
        return recordreq.reqid.flatMap(recordreqid -> replayreq.reqid.flatMap(replayreqid -> {
            // fetch response of recording and replay
            // if enough information is not available to retrieve a template for matching , return a no match
            if (recordreq.app.isEmpty() || recordreq.customerid.isEmpty() || recordreq.path.isEmpty() ||
                recordreq.getService().isEmpty()) {
                LOGGER.error("Not enough information to construct a template cache key for recorded req :: "
                        + recordreq.reqid.get());
                return Optional.empty();
            }

            try {
                // get appropriate template from solr
                TemplateKey key = new TemplateKey(templateVersion, recordreq.customerid.get(),
                        recordreq.app.get(), recordreq.getService().get(), recordreq.path , TemplateKey.Type.Response);
                ResponseComparator comparator = responseComparatorCache.getResponseComparator(key);
                Optional<Response> replayresp = Optional.ofNullable(replayResponseMap.get(replayreqid));
                //question ? what happens when these optionals don't contain any value ...
                // what gets returned
                return recordedResponse.flatMap(recordedr -> replayresp.flatMap(replayr -> {
                    Comparator.Match rm = comparator.compare(recordedr, replayr);
                    return Optional.of(new Analysis.RespMatchWithReq(recordreq, Optional.of(replayreq) , rm ,
                            Optional.of(recordedr) , Optional.of(replayr)));
                }));
            } catch(RuntimeException e) {
                // if analysis retrieval caused an error, log the error and return NO MATCH
                String stackTraceError =  UtilException.extractFirstStackTraceLocation(e.getStackTrace());
                LOGGER.error("Exception while analyzing response :: " +
                        recordreq.reqid.orElse(" -1") + " " +  e.getMessage() + " " + stackTraceError);
                return Optional.empty();
            }

        })).orElse(new Analysis.RespMatchWithReq(recordreq, Optional.of(replayreq), Comparator.Match.NOMATCH
                , Optional.empty() , Optional.empty()));
    }

    private static boolean isReqRespMatchBetter(Comparator.MatchType reqm1, Comparator.MatchType respm1,
                                                Comparator.MatchType reqm2, Comparator.MatchType respm2) {
        // request match has to be better. Only if it is better, check response match
        if (reqm1.isBetterOrEqual(reqm2)) {
            return respm1.isBetter(respm2);
        }
        return false;
    }

    private static int UPDBATCHSIZE = 10;

    /**
     * @param replayid
     * @param rrstore
     * @return
     */
    public static Optional<Analysis> getStatus(String replayid, ReqRespStore rrstore) {
        return rrstore.getAnalysis(replayid);
    }




    /**
     * @param replayid
     * @param tracefield
     * @param templateVersion
     * @return
     */
    public static Optional<Analysis> analyze(String replayid, String tracefield,
                                             ReqRespStore rrstore, ObjectMapper jsonmapper,
                                             RequestComparatorCache requestComparatorCache,
                                             ResponseComparatorCache responseComparatorCache,
                                             Optional<String> templateVersion) {
        // String collection = Replay.getCollectionFromReplayId(replayid);

        Optional<Replay> replay = rrstore.getReplay(replayid);

        // optional matching on traceid //and requestid
        // this is not being used
        /*ReqMatchSpec rmspec = (ReqMatchSpec) ReqMatchSpec.builder()
            .withMpath(ComparisonType.Equal)
            .withMqparams(ComparisonType.Equal)
            .withMfparams(ComparisonType.Equal)
            .withMhdrs(ComparisonType.EqualOptional)
            .withHdrfields(Collections.singletonList(tracefield))
            .withMrrtype(ComparisonType.Equal)
            .withMcustomerid(ComparisonType.Equal)
            .withMapp(ComparisonType.Equal)
            .withMcollection(ComparisonType.Equal)
            .withMmeta(ComparisonType.Equal)
            .withMetafields(Collections.singletonList(RRBase.SERVICEFIELD))
            .build();*/
        //.withMreqid(MatchType.SCORE).build();

        /*
        CompareTemplate reqTemplate = new CompareTemplate();
        reqTemplate.addRule(new TemplateEntry(PATHPATH, DataType.Str, PresenceType.Optional, ComparisonType.Equal));
        reqTemplate.addRule(new TemplateEntry(QPARAMPATH, DataType.Obj, PresenceType.Optional, ComparisonType.Equal));
        reqTemplate.addRule(new TemplateEntry(FPARAMPATH, DataType.Obj, PresenceType.Optional, ComparisonType.Equal));
        reqTemplate.addRule(new TemplateEntry(HDRPATH+"/"+tracefield, DataType.Str, PresenceType.Optional, ComparisonType.EqualOptional));
        reqTemplate.addRule(new TemplateEntry(RRTYPEPATH, DataType.Str, PresenceType.Optional, ComparisonType.Equal));
        reqTemplate.addRule(new TemplateEntry(CUSTOMERIDPATH, DataType.Str, PresenceType.Optional, ComparisonType.Equal));
        reqTemplate.addRule(new TemplateEntry(APPPATH, DataType.Str, PresenceType.Optional, ComparisonType.Equal));
        reqTemplate.addRule(new TemplateEntry(COLLECTIONPATH, DataType.Str, PresenceType.Optional, ComparisonType.Equal));
        reqTemplate.addRule(new TemplateEntry(METAPATH + "/" + SERVICEFIELD, DataType.Str, PresenceType.Optional, ComparisonType.Equal));

        RequestComparator mspec = new TemplatedRequestComparator(reqTemplate, jsonmapper);
        */
        //RequestComparator mspec = rmspec;

        return replay.flatMap(r -> {
            // get request in batches ... for batching of corresponding trace id queries
            // TODO need to get the batch size from some config
            Pair<Stream<List<Request>> , Long> result = r.getRequestBatches(20 , rrstore);

            // if version is passed in analyze request, use it. Else, use the version associated
            // with the Replay
            Optional<String> templateVersionToUse = templateVersion.or(() -> r.templateVersion);

            //Result<Request> reqs = r.getRequests(rrstore, true);
            Analyzer analyzer = new Analyzer(replayid, result.getRight().intValue()
                   , jsonmapper , requestComparatorCache, responseComparatorCache, templateVersionToUse);
            if (!rrstore.saveAnalysis(analyzer.analysis)) {
                return Optional.empty();
            }

            analyzer.analyze(rrstore, result.getLeft(), r);

            // update the stored analysis
            rrstore.saveAnalysis(analyzer.analysis);

            return Optional.of(analyzer.analysis);
        });
    }


}

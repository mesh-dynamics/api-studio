/*
 *
 *    Copyright Cube I O
 *
 */

package com.cube.drivers;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.cube.agent.UtilException;
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
import com.cube.core.Comparator.MatchType;
import com.cube.dao.*;
import com.cube.dao.Analysis.RespMatchWithReqEvent;
import com.cube.ws.Config;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-03-08
 * @author Prasad M D
 */
public class Analyzer {

    private static final Logger LOGGER = LogManager.getLogger(Analyzer.class);

    private Analyzer(String replayid, int reqcnt, String templateVersion, Config config) {
        this.config = config;
        analysis = new Analysis(replayid, reqcnt, templateVersion);
        this.jsonMapper = config.jsonMapper;

        //comparator = new TemplatedResponseComparator(TemplatedRRComparator.EQUALITYTEMPLATE, jsonMapper);
        //TemplatedRRComparator.EQUALITYTEMPLATE;
        this.responseComparatorCache = config.responseComparatorCache;
        this.requestComparatorCache = config.requestComparatorCache;
        this.templateVersion = templateVersion;
    }


    private Analysis analysis;
    //private ResponseComparator comparator = ResponseComparator.EQUALITYCOMPARATOR;
    private final ObjectMapper jsonMapper;
    private final Config config;
    // Template cache being passed from the config
    private final RequestComparatorCache requestComparatorCache;
    private final ResponseComparatorCache responseComparatorCache;
    private final String templateVersion;




    /**
     * @param rrstore
     * @param reqs
     */
    // TODO: Event redesign cleanup: This can be removed
    private void analyze(ReqRespStore rrstore, Stream<List<Request>> reqs, Replay replay) {

        // using seed generated from replayid so that same requests get picked in replay and analyze
        long seed = replay.replayId.hashCode();
        Random random = new Random(seed);

        reqs.forEach(requestList -> {
            // for each batch of requests, expand on trace id for the given list of intermediate services
            // (a property of replay)
            List<Request> filteredList = requestList.stream().filter(request ->
                    replay.sampleRate.map(sr -> random.nextDouble() <= sr).orElse(true))
                    .collect(Collectors.toList());

            Stream<Request> enhancedRequests = rrstore.expandOnTraceId(filteredList, replay.intermediateServices
                    ,replay.collection);

            enhancedRequests.forEach(r -> {


                // find matching request in replay
                // most fields are same as request except
                // RRType should be Replay
                // collection to set to replayid, since collection in replays are set to replayids
                Request rq = new Request(r.apiPath, r.reqId, r.queryParams, r.formParams, r.meta,
                        r.hdrs, r.method, r.body, Optional.ofNullable(analysis.replayId), r.timestamp,
                        Optional.of(Event.RunType.Replay), r.customerId, r.app);

                List<Request> matches = new ArrayList<>();

                TemplateKey key = new TemplateKey(Optional.of(templateVersion), r.customerId.get(), r.app.get(),
                    r.getService().get(), r.apiPath, TemplateKey.Type.Request);
                RequestComparator comparator = requestComparatorCache.getRequestComparator(key, false);
                matches = rrstore.getRequests(rq, comparator, Optional.of(10))
                        .collect(Collectors.toList());
                // TODO: add toString override for the Request object to debug log
                if (!matches.isEmpty()) {
                    Map<String, Response> replayResponseMap = rrstore.getResponses(matches);
                    Optional<Response> recordedResponse = r.reqId.flatMap(rrstore::getResponse);
                    if (matches.size() > 1) {
                        analysis.reqmultiplematch++;
                    } else {
                        analysis.reqsinglematch++;
                    }

                    // fetch response of recording and replay
                    // TODO change it back to RecReqNoMatch
                    Analysis.RespMatchWithReq bestmatch = new Analysis.RespMatchWithReq(r, Optional.empty(),
                            Comparator.Match.DEFAULT, Optional.empty(), Optional.empty());
                    MatchType bestreqmt = MatchType.NoMatch;

                    // matches is ordered in decreasing order of request match score. so exact matches
                    // of requests, if any should be at the beginning
                    // If request matches exactly, consider that as the best match
                    // else find the best match based on response matching
                    for (Request replayreq : matches) {
                        MatchType reqmt = comparator.compare(rq, replayreq);
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

                    LOGGER.debug(bestmatch.getmt() + " OCCURRED FOR RESPONSE :: " + r.reqId.orElse("-1"));
                    LOGGER.debug("REQUEST 1 " + bestmatch.getRecordReq(jsonMapper).orElse(" N/A"));
                    LOGGER.debug("REQUEST 2 " + bestmatch.getReplayReq(jsonMapper).orElse("N/A"));
                    LOGGER.debug("DOC 1 " + bestmatch.getRecordedResponseBody().orElse(" N/A"));
                    LOGGER.debug("DOC 2 " + bestmatch.getReplayResponseBody().orElse(" N/A"));
                    bestmatch.getDiffs().forEach(
                            diff -> {
                                try {
                                    LOGGER.debug("DIFF :: " + jsonMapper.writeValueAsString(diff));
                                } catch (JsonProcessingException e) {
                                    // DO NOTHING
                                }
                            });

                    Analysis.ReqRespMatchResult res = new Analysis.ReqRespMatchResult(bestmatch, bestreqmt, matches.size(),
                            analysis.replayId, jsonMapper);
                    rrstore.saveResult(res);

                } else {
                    // TODO change it back to RecReqNoMatch
                    Analysis.ReqRespMatchResult res = new Analysis.ReqRespMatchResult(new Analysis.RespMatchWithReq(r,
                        Optional.empty(), Comparator.Match.NOMATCH, Optional.empty() , Optional.empty()),
                        MatchType.NoMatch, matches.size(), analysis.replayId, jsonMapper);
                    rrstore.saveResult(res);
                    analysis.reqnotmatched++;
                }


                analysis.reqanalyzed++;
                if (analysis.reqanalyzed % UPDBATCHSIZE == 0) {
                    LOGGER.info(String.format("Analysis of replay %s completed %d requests", analysis.replayId, analysis.reqanalyzed));
                    rrstore.saveAnalysis(analysis);
                }

            });
        });
        analysis.status = Analysis.Status.Completed;

    }

    /**
     * @param rrstore
     * @param reqs
     */
    private void analyzeWithEvent(ReqRespStore rrstore, Stream<List<Event>> reqs, Replay replay) {

        // using seed generated from replayid so that same requests get picked in replay and analyze
        long seed = replay.replayId.hashCode();
        Random random = new Random(seed);

        reqs.forEach(requestList -> {
            // for each batch of requests, expand on trace id for the given list of intermediate services
            // (a property of replay)
            List<Event> filteredList = requestList.stream().filter(request ->
                replay.sampleRate.map(sr -> random.nextDouble() <= sr).orElse(true))
                .collect(Collectors.toList());

            Stream<Event> enhancedRequests = expandOnTraceId(filteredList, replay.customerId,
                replay.app, replay.collection, rrstore);

            enhancedRequests.forEach(r -> {


                // find matching request in replay
                EventQuery eventQuery = reqEventToEventQuery(r, analysis.replayId, 10);

                TemplateKey key = new TemplateKey(Optional.of(templateVersion), replay.customerId, replay.app,
                    r.service, r.apiPath, TemplateKey.Type.Request);
                RequestComparator comparator = requestComparatorCache.getRequestComparator(key, false);


                Result<Event> matches = rrstore.getEvents(eventQuery);
                // TODO: add toString override for the Request object to debug log
                if (matches.numResults > 0) {

                    List<Event> matchedReqs = matches.getObjects().collect(Collectors.toList());
                    Map<String, Event> replayResponseMap = getReqToResponseMap(matchedReqs, replay.customerId,
                        replay.app, replay.replayId, r.service, r.eventType, r.traceId, rrstore);



                    Optional<Event> recordedResponse = getResponseFromRequestEvent(r, rrstore);
                    if (matches.numResults > 1) {
                        analysis.reqmultiplematch++;
                    } else {
                        analysis.reqsinglematch++;
                    }

                    // fetch response of recording and replay
                    // TODO change it back to RecReqNoMatch
                    RespMatchWithReqEvent bestmatch = new RespMatchWithReqEvent(r, Optional.empty(),
                        Comparator.Match.DEFAULT, Optional.empty(), Optional.empty());
                    MatchType bestreqmt = MatchType.NoMatch;

                    // matches is ordered in decreasing order of request match score. so exact matches
                    // of requests, if any should be at the beginning
                    // If request matches exactly, consider that as the best match
                    // else find the best match based on response matching
                    for (Event replayreq : matchedReqs) {
                        // we have removed EqualOptional in request match. So any match has to be ExactMatch
                        MatchType reqmt = ExactMatch;
                        RespMatchWithReqEvent match = checkRespEventMatch(r, replayreq, recordedResponse,
                            replayResponseMap);
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

                    LOGGER.debug(bestmatch.getmt() + " OCCURRED FOR RESPONSE :: " + r.reqId);
                    LOGGER.debug("REQUEST 1 " + bestmatch.getRecordReq(config).orElse(" N/A"));
                    LOGGER.debug("REQUEST 2 " + bestmatch.getReplayReq(config).orElse("N/A"));
                    LOGGER.debug("DOC 1 " + bestmatch.getRecordedResponseBody(config).orElse(" N/A"));
                    LOGGER.debug("DOC 2 " + bestmatch.getReplayResponseBody(config).orElse(" N/A"));
                    bestmatch.getDiffs().forEach(
                        diff -> {
                            try {
                                LOGGER.debug("DIFF :: " + jsonMapper.writeValueAsString(diff));
                            } catch (JsonProcessingException e) {
                                // DO NOTHING
                            }
                        });

                    Analysis.ReqRespMatchResult res = new Analysis.ReqRespMatchResult(bestmatch, bestreqmt,
                        (int) matches.numResults, analysis.replayId, jsonMapper);
                    rrstore.saveResult(res);

                } else {
                    // TODO change it back to RecReqNoMatch
                    Analysis.ReqRespMatchResult res = new Analysis.ReqRespMatchResult(new RespMatchWithReqEvent(r,
                        Optional.empty(), Comparator.Match.NOMATCH, Optional.empty() , Optional.empty()),
                        MatchType.NoMatch, (int)matches.numResults, analysis.replayId, jsonMapper);
                    rrstore.saveResult(res);
                    analysis.reqnotmatched++;
                }

                analysis.reqanalyzed++;
                if (analysis.reqanalyzed % UPDBATCHSIZE == 0) {
                    LOGGER.info(String.format("Analysis of replay %s completed %d requests", analysis.replayId, analysis.reqanalyzed));
                    rrstore.saveAnalysis(analysis);
                }

            });
        });
        analysis.status = Analysis.Status.Completed;

    }

    // TODO: Event redesign cleanup: This can be removed
    private Analysis.RespMatchWithReq checkRespMatch(Request recordreq, Request replayreq, Optional<Response> recordedResponse ,
                                                     Map<String, Response> replayResponseMap) {
        return recordreq.reqId.flatMap(recordreqid -> replayreq.reqId.flatMap(replayreqid -> {
            // fetch response of recording and replay
            // if enough information is not available to retrieve a template for matching , return a no match
            if (recordreq.app.isEmpty() || recordreq.customerId.isEmpty() || recordreq.apiPath.isEmpty() ||
                recordreq.getService().isEmpty()) {
                LOGGER.error("Not enough information to construct a template cache key for recorded req :: "
                        + recordreq.reqId.get());
                return Optional.empty();
            }

            try {
                // get appropriate template from solr
                TemplateKey key = new TemplateKey(Optional.of(templateVersion), recordreq.customerId.get(),
                        recordreq.app.get(), recordreq.getService().get(), recordreq.apiPath , TemplateKey.Type.Response);
                ResponseComparator comparator = responseComparatorCache.getResponseComparator(key);
                Optional<Response> replayresp = Optional.ofNullable(replayResponseMap.get(replayreqid));
                //question ? what happens when these optionals don't contain any value ...
                // what gets returned
                return recordedResponse.flatMap(recordedr -> replayresp.flatMap(replayr -> {
                    Comparator.Match rm = Comparator.Match.NOMATCH;
                    if (recordedr.status != replayr.status) {
                        rm = Comparator.Match.STATUSNOMATCH;
                    } else {
                        if (recordedr.status == javax.ws.rs.core.Response.Status.OK.getStatusCode()) {
                            rm = comparator.compare(recordedr, replayr);
                        } else {
                            // If the status of response was not OK, the body may not be a json, so it will lead to
                            // exception when applying json template rules. To avoid this, consider body as simple
                            // string whenever there is an error return status
                            rm = responseComparatorCache.getDefaultResponseComparator().compare(recordedr, replayr);
                        }
                    }
                    return Optional.of(new Analysis.RespMatchWithReq(recordreq, Optional.of(replayreq) , rm ,
                            Optional.of(recordedr) , Optional.of(replayr)));
                }));
            } catch(RuntimeException e) {
                // if analysis retrieval caused an error, log the error and return NO MATCH
                String stackTraceError =  UtilException.extractFirstStackTraceLocation(e.getStackTrace());
                LOGGER.error("Exception while analyzing response :: " +
                        recordreq.reqId.orElse(" -1") + " " +  e.getMessage() + " " + stackTraceError);
                return Optional.empty();
            }

        })).orElse(new Analysis.RespMatchWithReq(recordreq, Optional.of(replayreq), Comparator.Match.NOMATCH
                , Optional.empty() , Optional.empty()));
    }


    private RespMatchWithReqEvent checkRespEventMatch(Event recordreq, Event replayreq,
                                                      Optional<Event> recordedResponse ,
                                                     Map<String, Event> replayResponseMap) {

        try {
            // get appropriate template from solr
            TemplateKey key = new TemplateKey(Optional.of(templateVersion), recordreq.customerId,
                recordreq.app, recordreq.service, recordreq.apiPath , TemplateKey.Type.Response);
            ResponseComparator comparator = responseComparatorCache.getResponseComparator(key);
            Optional<Event> replayresp = Optional.ofNullable(replayResponseMap.get(replayreq.reqId));
            return recordedResponse.flatMap(recordedr -> replayresp.flatMap(replayr -> {
                Comparator.Match rm = comparator.compare(recordedr.getPayload(config), replayr.getPayload(config));
                return Optional.of(new RespMatchWithReqEvent(recordreq, Optional.of(replayreq) , rm ,
                    Optional.of(recordedr) , Optional.of(replayr)));
            })).orElseGet(() -> new RespMatchWithReqEvent(recordreq, Optional.of(replayreq),
                Comparator.Match.NOMATCH, Optional.empty() , Optional.empty()));
        } catch(RuntimeException e) {
            // if analysis retrieval caused an error, log the error and return NO MATCH
            String stackTraceError =  UtilException.extractFirstStackTraceLocation(e.getStackTrace());
            LOGGER.error("Exception while analyzing response :: " +
                recordreq.reqId + " " +  e.getMessage() + " " + stackTraceError);
            return new RespMatchWithReqEvent(recordreq, Optional.of(replayreq),
                Comparator.Match.NOMATCH, Optional.empty() , Optional.empty());
        }
    }

    private static boolean isReqRespMatchBetter(MatchType reqm1, MatchType respm1,
                                                MatchType reqm2, MatchType respm2) {
        // request match has to be better. Only if it is better, check response match
        if (reqm1.isBetterOrEqual(reqm2)) {
            return respm1.isBetter(respm2);
        }
        return false;
    }

    Stream<Event> expandOnTraceId(List<Event> requestList, String customerId,
                                  String app, String collectionId, ReqRespStore rrstore) {
        List<String> traceIds =
            requestList.stream().map(request -> request.traceId).collect(Collectors.toList());
        if (traceIds.isEmpty()) {
            return requestList.stream();
        }
        EventQuery.Builder builder = new EventQuery.Builder(customerId, app, Event.getRequestEventTypes());
        EventQuery eventQuery = builder
            .withCollection(collectionId)
            .withTraceIds(traceIds)
            .build();

        return rrstore.getEvents(eventQuery).getObjects();
    }

    EventQuery reqEventToEventQuery(Event reqEvent, String replayId, int limit) {

        EventQuery.Builder builder = new EventQuery.Builder(reqEvent.customerId, reqEvent.app, reqEvent.eventType);

        return builder.withPath(reqEvent.apiPath)
            .withCollection(replayId)
            .withRunType(Event.RunType.Replay)
            .withTraceId(reqEvent.traceId)
            .withPayloadKey(reqEvent.payloadKey)
            .withLimit(limit)
            .build();
    }

    public static EventQuery reqEventToResponseEventQuery(List<String> reqIds, String customerId, String app,
                                                          String collection, String service, Event.EventType eventType,
                                                          String traceId) {
        // eventually we will clean up code and make customerid and app non-optional in Request
        EventQuery.Builder builder = new EventQuery.Builder(customerId, app,
            Event.EventType.getResponseType(eventType));
        return builder.withCollection(collection)
            .withService(service)
            .withTraceId(traceId)
            .withReqIds(reqIds)
            .withLimit(reqIds.size())
            .build();
    }

    Map<String, Event> getReqToResponseMap(List<Event> reqEvents, String customerId, String app, String collection,
                                           String service, Event.EventType eventType, String traceId,
                                           ReqRespStore rrstore) {
        List<String> reqIds = reqEvents.stream().map(req -> req.reqId).collect(Collectors.toList());
        EventQuery eventQuery = reqEventToResponseEventQuery(reqIds, customerId, app, collection, service,
            eventType, traceId);
        Result<Event> responses = rrstore.getEvents(eventQuery);
        return responses.getObjects().collect(Collectors.toMap(Event::getReqId, Function.identity()));
    }

    static Optional<Event> getResponseFromRequestEvent(Event reqEvent, ReqRespStore rrstore) {
        EventQuery eventQuery = reqEventToResponseEventQuery(List.of(reqEvent.reqId), reqEvent.customerId, reqEvent.app,
            reqEvent.getCollection(), reqEvent.service, reqEvent.eventType, reqEvent.traceId);
        return rrstore.getEvents(eventQuery).getObjects().findFirst();

    }



    private static int UPDBATCHSIZE = 10;

    /**
     * @param replayId
     * @param rrstore
     * @return
     */
    public static Optional<Analysis> getStatus(String replayId, ReqRespStore rrstore) {
        return rrstore.getAnalysis(replayId);
    }


    private static int TRACEBATCHSIZE = 20; // for batching of TRACEID queries


    /**
     * @param replayId
     * @param tracefield
     * @return
     */
    public static Optional<Analysis> analyze(String replayId, String tracefield,
                                             Config config) {
        // String collection = Replay.getCollectionFromReplayId(replayid);

        ReqRespStore rrstore = config.rrstore;

        Optional<Replay> replay = rrstore.getReplay(replayId);

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

        RequestComparator mspec = new TemplatedRequestComparator(reqTemplate, jsonMapper);
        */
        //RequestComparator mspec = rmspec;

        return replay.flatMap(r -> {
            // get request in batches ... for batching of corresponding trace id queries
            // TODO need to get the batch size from some config
            Pair<Stream<List<Event>> , Long> result = r.getRequestEventBatches(TRACEBATCHSIZE , rrstore);

            String templateVersionToUse = r.templateVersion;

            //Result<Request> reqs = r.getRequests(rrstore, true);
            Analyzer analyzer = new Analyzer(replayId, result.getRight().intValue(), templateVersionToUse, config);
            if (!rrstore.saveAnalysis(analyzer.analysis)) {
                return Optional.empty();
            }

            analyzer.analyzeWithEvent(rrstore, result.getLeft().limit(1), r); // TODO: temporary limit of 1 fordebugging

            // update the stored analysis
            rrstore.saveAnalysis(analyzer.analysis);


            // Compute the aggregations here itself for all levels.
            Optional<String> service = Optional.empty();
            boolean bypath = true;

            Collection<MatchResultAggregate> resultAggregates = rrstore.computeResultAggregate(replayId, service, bypath);
            resultAggregates.forEach( resultAggregate -> {
                rrstore.saveMatchResultAggregate(resultAggregate);
            } );

            return Optional.of(analyzer.analysis);
        });
    }


}

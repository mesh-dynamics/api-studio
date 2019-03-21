/*
 *
 *    Copyright Cube I O
 *
 */

package com.cube.drivers;

import static com.cube.core.Comparator.MatchType.ExactMatch;
import static com.cube.dao.RRBase.*;
import static com.cube.dao.Request.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.cube.core.*;
import com.cube.core.CompareTemplate.ComparisonType;
import com.cube.core.CompareTemplate.DataType;
import com.cube.core.CompareTemplate.PresenceType;
import com.cube.dao.*;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-03-08
 * @author Prasad M D
 */
public class Analyzer {

    private static final Logger LOGGER = LogManager.getLogger(Analyzer.class);


    private Analyzer(String replayid, int reqcnt, ObjectMapper jsonmapper) {
        analysis = new Analysis(replayid, reqcnt);
        this.jsonmapper = jsonmapper;
        comparator = new TemplatedResponseComparator(TemplatedRRComparator.EQUALITYTEMPLATE, jsonmapper);
    }


    private Analysis analysis;
    private ResponseComparator comparator = ResponseComparator.EQUALITYCOMPARATOR;
    private final ObjectMapper jsonmapper;

    /**
     * @param rrstore
     * @param reqs
     * @param mspec
     */
    private void analyze(ReqRespStore rrstore, Stream<Request> reqs, RequestComparator mspec) {
        reqs.forEach(r -> {
            // find matching request in replay
            // most fields are same as request except
            // RRType should be Replay
            // collection to set to replayid, since collection in replays are set to replayids
            Request rq = new Request(r.path, r.reqid, r.qparams, r.fparams, r.meta,
                r.hdrs, r.method, r.body, Optional.ofNullable(analysis.replayid), r.timestamp,
                Optional.of(RRBase.RR.Replay), r.customerid, r.app);
            List<Request> matches = rrstore.getRequests(rq, mspec, Optional.of(10))
                .collect(Collectors.toList());


            // TODO: add toString override for the Request object to debug log
            if (!matches.isEmpty()) {
                if (matches.size() > 1) {
                    analysis.reqmultiplematch++;
                } else {
                    analysis.reqsinglematch++;
                }

                // fetch response of recording and replay

                Analysis.RespMatchWithReq bestmatch = new Analysis.RespMatchWithReq(r, null, Comparator.Match.NOMATCH);
                Comparator.MatchType bestreqmt = Comparator.MatchType.NoMatch;

                // matches is ordered in decreasing order of request match score. so exact matches
                // of requests, if any should be at the beginning
                // If request matches exactly, consider that as the best match
                // else find the best match based on response matching
                for (Request replayreq : matches) {
                    Comparator.MatchType reqmt = mspec.compare(rq, replayreq);
                    Analysis.RespMatchWithReq match = checkRespMatch(r, replayreq, rrstore);

                    if (isReqRespMatchBetter(reqmt, match.getmt(), bestreqmt, bestmatch.getmt())) {
                        bestmatch = match;
                        bestreqmt = reqmt;
                        if (bestreqmt == ExactMatch && bestmatch.getmt() == ExactMatch) {
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
                switch(bestmatch.getmt()) {
                    case ExactMatch: analysis.respmatched++; break;
                    case FuzzyMatch: analysis.resppartiallymatched++; break;
                    default: analysis.respnotmatched++; break;
                }
                Analysis.ReqRespMatchResult res = new Analysis.ReqRespMatchResult(bestmatch, bestreqmt, matches.size(),
                    analysis.replayid, jsonmapper);
                rrstore.saveResult(res);

            } else {
                analysis.reqnotmatched++;
            }


            analysis.reqanalyzed++;
            if (analysis.reqanalyzed % UPDBATCHSIZE == 0) {
                LOGGER.info(String.format("Analysis of replay %s completed %d requests", analysis.replayid, analysis.reqanalyzed));
                rrstore.saveAnalysis(analysis);
            }
        });
        analysis.status = Analysis.Status.Completed;

    }

    private Analysis.RespMatchWithReq checkRespMatch(Request recordreq, Request replayreq, ReqRespStore rrstore) {
        return recordreq.reqid.flatMap(recordreqid -> replayreq.reqid.flatMap(replayreqid -> {
            // fetch response of recording and replay
            Optional<Response> recordedresp = rrstore.getResponse(recordreqid);
            Optional<Response> replayresp = rrstore.getResponse(replayreqid);


            return recordedresp.flatMap(recordedr -> replayresp.flatMap(replayr -> {
                Comparator.Match rm = comparator.compare(recordedr, replayr);
                return Optional.of(new Analysis.RespMatchWithReq(recordreq, replayreq, rm));
            }));
        })).orElse(new Analysis.RespMatchWithReq(recordreq, replayreq, Comparator.Match.NOMATCH));
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
     * @return
     */
    public static Optional<Analysis> analyze(String replayid, String tracefield,
                                             ReqRespStore rrstore, ObjectMapper jsonmapper) {
        // String collection = Replay.getCollectionFromReplayId(replayid);

        Optional<Replay> replay = rrstore.getReplay(replayid);

        // optional matching on traceid //and requestid
        ReqMatchSpec rmspec = (ReqMatchSpec) ReqMatchSpec.builder()
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
            .build();
        //.withMreqid(MatchType.SCORE).build();

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
        //RequestComparator mspec = rmspec;

        return replay.flatMap(r -> {
            Result<Request> reqs = r.getRequests(rrstore);
            Analyzer analyzer = new Analyzer(replayid, (int) reqs.numResults(), jsonmapper);
            if (!rrstore.saveAnalysis(analyzer.analysis)) {
                return Optional.empty();
            }

            analyzer.analyze(rrstore, reqs.getObjects(), mspec);

            // update the stored analysis
            rrstore.saveAnalysis(analyzer.analysis);

            return Optional.of(analyzer.analysis);
        });
    }


}

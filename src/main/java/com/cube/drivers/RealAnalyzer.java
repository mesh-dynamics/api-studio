/*
 *
 *    Copyright Cube I O
 *
 */

package com.cube.drivers;


import static io.md.core.Comparator.MatchType.DontCare;
import static io.md.core.Comparator.MatchType.ExactMatch;
import static io.md.core.TemplateKey.Type;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.cube.agent.UtilException;
import io.md.core.Comparator;
import io.md.core.Comparator.Match;
import io.md.core.Comparator.MatchType;
import io.md.core.TemplateKey;
import io.md.dao.Analysis;
import io.md.dao.Analysis.ReqRespMatchWithEvent;
import io.md.dao.Event;
import io.md.dao.EventQuery;
import io.md.dao.Replay;
import io.md.dao.ReqRespMatchResult;
import io.md.services.Analyzer;
import io.md.utils.CubeObjectMapperProvider;

import com.cube.dao.MatchResultAggregate;
import com.cube.dao.ReplayUpdate;
import com.cube.dao.ReqRespStore;
import com.cube.dao.Result;
import com.cube.utils.Constants;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-03-08
 * @author Prasad M D
 */
public class RealAnalyzer implements Analyzer {

    private static final Logger LOGGER = LogManager.getLogger(RealAnalyzer.class);

    public RealAnalyzer(ReqRespStore rrstore) {
        this.rrstore = rrstore;
        this.jsonMapper = CubeObjectMapperProvider.getInstance();
    }


    private final ObjectMapper jsonMapper;
    private final ReqRespStore rrstore;

    /**
     * @param rrstore
     * @param reqs
     * @param templateVersion
     * @param analysis
     */
    private void analyzeWithEvent(ReqRespStore rrstore, Stream<List<Event>> reqs, Replay replay,
                                  String templateVersion, Analysis analysis) {

        // using seed generated from replayId so that same requests get picked in replay and analyze
        long seed = replay.replayId.hashCode();
        Random random = new Random(seed);

        reqs.forEach(UtilException.rethrowConsumer(requestList -> {
            // for each batch of requests, expand on trace id for the given list of intermediate services
            // (a property of replay)
            Map<String, Event> filteredList = requestList.stream().filter(request ->
                replay.sampleRate.map(sr -> random.nextDouble() <= sr).orElse(true))
                .collect(Collectors.toMap(event -> event.reqId, event->event));

            Stream<Event> enhancedRequests = expandOnTraceId(filteredList.values(), replay.customerId,
                replay.app, replay.collection, rrstore);

            enhancedRequests.forEach(UtilException.rethrowConsumer(r -> {
                analyzeRequestEvent(rrstore, replay, r, templateVersion, analysis, filteredList);
            }));
        }));
        analysis.status = Analysis.Status.MatchingCompleted;
    }

    private void analyzeRequestEvent(ReqRespStore rrstore, Replay replay, Event recordReq, String templateVersion,
                                     Analysis analysis, Map<String, Event> replayedReqs) {
        // find matching request in replay
        EventQuery eventQuery = reqEventToEventQuery(recordReq, analysis.replayId, 10, replayedReqs);

        Result<Event> matches = rrstore.getEvents(eventQuery);
        // TODO: add toString override for the Request object to debug log
        if (matches.numResults > 0) {

            List<Event> matchedReqs = matches.getObjects().collect(Collectors.toList());
            Map<String, Event> replayResponseMap = getReqToResponseMap(matchedReqs, replay.customerId,
                replay.app, replay.replayId, recordReq.service, recordReq.eventType, recordReq.getTraceId(), rrstore);



            Optional<Event> recordedResponse = getResponseFromRequestEvent(recordReq, rrstore);
            if (matches.numResults > 1) {
                analysis.reqMultipleMatch++;
            } else {
                analysis.reqSingleMatch++;
            }

            // fetch response of recording and replay
            // TODO change it back to RecReqNoMatch
            ReqRespMatchWithEvent bestmatch = new ReqRespMatchWithEvent(recordReq, Optional.empty(),
                Match.DEFAULT, Optional.empty(), Optional.empty(), Match.DEFAULT);
            MatchType bestReqMt = MatchType.NoMatch;

            // matches is ordered in decreasing order of request match score. so exact matches
            // of requests, if any should be at the beginning.
            // If request matches exactly, consider that as the best match
            // else find the best match first based on requestCompare matching and then responseCompare matching
            for (Event replayReq : matchedReqs) {
                // we have removed EqualOptional in request match. So any match has to be ExactMatch
                MatchType reqMt = ExactMatch;
                ReqRespMatchWithEvent match = checkReqRespEventMatch(recordReq, replayReq, recordedResponse,
                    replayResponseMap, templateVersion);
                if (isReqRespMatchBetter(reqMt, match.getReqCompareResType(), match.getRespCompareResType(), bestReqMt, bestmatch.getReqCompareResType(), bestmatch.getRespCompareResType())) {
                    bestmatch = match;
                    bestReqMt = reqMt;
                    // TODO : Should the break also based on bestmatch.getReqMt() == ExactMatch ?
                    if ((bestmatch.getReqCompareResType() == ExactMatch
                        || bestmatch.getReqCompareResType() == DontCare)
                        && bestmatch.getRespCompareResType() == ExactMatch) {
                        break;
                    }
                }
            }
            // compare & write out result
            if (bestReqMt == ExactMatch) {
                analysis.reqMatched++;
            } else {
                analysis.reqPartiallyMatched++;
            }

            switch (bestmatch.getReqCompareResType()) {
                case ExactMatch: case DontCare:
                    analysis.reqCompareMatched++;
                    break;
                case FuzzyMatch:
                    analysis.reqComparePartiallyMatched++;
                    break;
                default:
                    analysis.reqCompareNotMatched++;
                    break;
            }

            switch (bestmatch.getRespCompareResType()) {
                case ExactMatch:
                    analysis.respMatched++;
                    break;
                case FuzzyMatch:
                    analysis.respPartiallyMatched++;
                    break;
                default:
                    analysis.respNotMatched++;
                    break;
            }


            try {
                LOGGER.debug(new ObjectMessage(Map.of(
                    "recordedReqId",
                    Optional.ofNullable(recordReq.reqId).orElse(Constants.NOT_PRESENT),
                    "recordedReqPayload",
                    bestmatch.getRecordReq().orElse(Constants.NOT_PRESENT),
                    "replayReqPayload",
                    bestmatch.getReplayReq().orElse(Constants.NOT_PRESENT),
                    "reqCompareResType", bestmatch.getReqCompareResType().name(),
                    Constants.REQUEST_DIFF, jsonMapper.writeValueAsString(bestmatch.getReqDiffs()),
                    "recordedRespPayload",
                    bestmatch.getRecordedResponseBody().orElse(Constants.NOT_PRESENT),
                    "replayRespPayload",
                    bestmatch.getReplayResponseBody().orElse(Constants.NOT_PRESENT),
                    "respCompareResType", bestmatch.getRespCompareResType().name(),
                    Constants.RESPONSE_DIFF, jsonMapper.writeValueAsString(bestmatch.getRespDiffs())
                )));
            } catch (Exception e) {
                LOGGER.error(
                    new ObjectMessage(Map.of(Constants.MESSAGE, "Unable to log debug message")), e);
            }

            ReqRespMatchResult res = Analysis.createReqRespMatchResult(bestmatch, bestReqMt,
                (int) matches.numResults, analysis.replayId);
            rrstore.saveResult(res);

        } else {
            // TODO change it back to RecReqNoMatch
            ReqRespMatchResult res = Analysis.createReqRespMatchResult(new ReqRespMatchWithEvent(recordReq,
                Optional.empty(), Comparator.Match.NOMATCH, Optional.empty() , Optional.empty(), Comparator.Match.DONT_CARE),
                MatchType.NoMatch, (int)matches.numResults, analysis.replayId);
            rrstore.saveResult(res);
            analysis.reqNotMatched++;
        }

        analysis.reqAnalyzed++;
        if (analysis.reqAnalyzed % UPDBATCHSIZE == 0) {
            LOGGER.info(String.format("Analysis of replay %s completed %d requests", analysis.replayId, analysis.reqAnalyzed));
            rrstore.saveAnalysis(analysis);
        }
    }


    private ReqRespMatchWithEvent checkReqRespEventMatch(
        Event recordreq, Event replayreq,
        Optional<Event> recordedResponse,
        Map<String, Event> replayResponseMap, String templateVersion) {

        Comparator.Match reqCompareRes = Match.NOMATCH;
        Comparator.Match respCompareRes = Match.NOMATCH;
        Optional<Event> replayresp = Optional
            .ofNullable(replayResponseMap.get(replayreq.reqId));
        try {
            TemplateKey reqCompareKey = new TemplateKey(templateVersion, recordreq.customerId,
                recordreq.app, recordreq.service, recordreq.apiPath, Type.RequestCompare);
            Comparator reqComparator = rrstore.getComparator(reqCompareKey, recordreq.eventType);
            if (reqComparator.getCompareTemplate().getRules() != null &&
                ! reqComparator.getCompareTemplate().getRules().isEmpty()) {
                reqCompareRes = reqComparator
                    .compare(recordreq.payload, replayreq.payload);
            } else {
                reqCompareRes = new Comparator.Match(DontCare, "",
                    Collections.emptyList());
            }
            TemplateKey respCompareKey = new TemplateKey(templateVersion, recordreq.customerId,
                recordreq.app, recordreq.service, recordreq.apiPath, Type.ResponseCompare);

            if (recordedResponse.isPresent() && replayresp.isPresent()) {
                Event recordedr = recordedResponse.get();
                Event replayr = replayresp.get();
                Comparator respComparator = rrstore
                    .getComparator(respCompareKey, recordedr.eventType);
                respCompareRes = respComparator
                    .compare(recordedr.payload, replayr.payload);
            }
        } catch (Exception e) {
            LOGGER.error(new ObjectMessage(Map.of(
                Constants.MESSAGE, "Exception while analyzing request",
                Constants.REQ_ID_FIELD, Optional.ofNullable(recordreq.reqId)
                    .orElse(Constants.NOT_PRESENT))), e);
        }

        return new ReqRespMatchWithEvent(recordreq, Optional.of(replayreq),
            respCompareRes, recordedResponse, replayresp, reqCompareRes);
    }

    private static boolean isReqRespMatchBetter(MatchType reqm1, MatchType reqComparem1
        , MatchType respComparem1, MatchType reqm2, MatchType reqComparem2
        , MatchType respComparem2) {
        return isReqRespMatchBetter(List.of(reqm1, reqComparem1, respComparem1),
            List.of(reqm2, reqComparem2, respComparem2));
    }

    private static boolean isReqRespMatchBetter(List<MatchType> matchResultTuple1
        , List<MatchType> matchResultTuple2) {
        if (matchResultTuple1.size() != matchResultTuple2.size()) return false;
        Iterator<MatchType> iterator1 = matchResultTuple1.iterator();
        Iterator<MatchType> iterator2 = matchResultTuple2.iterator();
        while (iterator1.hasNext() && iterator2.hasNext()) {
            MatchType matchType1 = iterator1.next();
            MatchType matchType2 = iterator2.next();
            if (matchType1.isBetter(matchType2)) return true;
            if (matchType1 != matchType2) return false;
        }
        // note for the case when the entire lists are equal till the last element
        // , the control will reach here and we'll return false
        return false;
     }

    Stream<Event> expandOnTraceId(Collection<Event> requestList, String customerId,
                                  String app, String collectionId, ReqRespStore rrstore) {
        List<String> traceIds =
            requestList.stream().map(Event::getTraceId).collect(Collectors.toList());
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

    EventQuery reqEventToEventQuery(Event reqEvent, String replayId, int limit, Map<String, Event> replayedReqs) {

        EventQuery.Builder builder = new EventQuery.Builder(reqEvent.customerId, reqEvent.app, reqEvent.eventType);

        // For gateway replay requests, don't match apiPaths, since the dynamic injection could change the
        // path leading to mismatch
        if (!replayedReqs.containsKey(reqEvent.reqId)) {
            builder.withPath(reqEvent.apiPath);
        }

        return builder.withService(reqEvent.service)
            .withCollection(replayId)
            .withRunType(Event.RunType.Replay)
            .withTraceId(reqEvent.getTraceId())
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
            reqEvent.getCollection(), reqEvent.service, reqEvent.eventType, reqEvent.getTraceId());
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
     * @return
     */
    public Optional<Analysis> analyze(String replayId) {

        Optional<Replay> replay = rrstore.getReplay(replayId);

        return replay.flatMap(UtilException.rethrowFunction(r -> {
            // get request in batches ... for batching of corresponding trace id queries
            // TODO need to get the batch size from some config
            Pair<Stream<List<Event>> , Long> result = ReplayUpdate
                .getRequestBatchesUsingEvents(TRACEBATCHSIZE , rrstore, r);

            String templateVersionToUse = r.templateVersion;

            Analysis analysis = new Analysis(replayId, result.getRight().intValue(), templateVersionToUse);

            if (!rrstore.saveAnalysis(analysis)) {
                return Optional.empty();
            }

            analyzeWithEvent(rrstore, result.getLeft(), r, templateVersionToUse, analysis);

            // update the stored analysis
            rrstore.saveAnalysis(analysis);


            // Compute the aggregations here itself for all levels.
            Optional<String> service = Optional.empty();
            boolean bypath = true;

            Collection<MatchResultAggregate> resultAggregates = rrstore.computeResultAggregate(replayId, service, bypath);
            resultAggregates.forEach( resultAggregate -> {
                rrstore.saveMatchResultAggregate(resultAggregate);
            } );

            // Everything including aggregation completed
            analysis.status = Analysis.Status.Completed;
            if (rrstore.saveAnalysis(analysis)) {
                r.analysisCompleteTimestamp = Instant.now();
                rrstore.saveReplay(r);
            }

            return Optional.of(analysis);
        }));
    }

}

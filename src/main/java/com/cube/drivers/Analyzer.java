/*
 *
 *    Copyright Cube I O
 *
 */

package com.cube.drivers;

import static com.cube.core.Comparator.MatchType.ExactMatch;

import java.util.Collection;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.cube.agent.UtilException;

import com.cube.cache.ComparatorCache;
import com.cube.cache.ComparatorCache.TemplateNotFoundException;
import com.cube.cache.TemplateKey;
import com.cube.core.Comparator;
import com.cube.core.Comparator.MatchType;
import com.cube.dao.Analysis;
import com.cube.dao.Analysis.ReqRespMatchWithEvent;
import com.cube.dao.Event;
import com.cube.dao.EventQuery;
import com.cube.dao.MatchResultAggregate;
import com.cube.dao.Replay;
import com.cube.dao.ReqRespMatchResult;
import com.cube.dao.ReqRespStore;
import com.cube.dao.Result;
import com.cube.ws.Config;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-03-08
 * @author Prasad M D
 */
public class Analyzer {

    private static final Logger LOGGER = LogManager.getLogger(Analyzer.class);

    private Analyzer(String replayId, int reqcnt, String templateVersion, Config config) {
        this.config = config;
        analysis = new Analysis(replayId, reqcnt, templateVersion);
        this.jsonMapper = config.jsonMapper;

        this.comparatorCache = config.comparatorCache;
        this.templateVersion = templateVersion;
    }


    private Analysis analysis;
    //private ResponseComparator comparator = ResponseComparator.EQUALITYCOMPARATOR;
    private final ObjectMapper jsonMapper;
    private final Config config;
    // Template cache being passed from the config
    private final ComparatorCache comparatorCache;
    private final String templateVersion;



    /**
     * @param rrstore
     * @param reqs
     */
    private void analyzeWithEvent(ReqRespStore rrstore, Stream<List<Event>> reqs, Replay replay) throws TemplateNotFoundException {

        // using seed generated from replayId so that same requests get picked in replay and analyze
        long seed = replay.replayId.hashCode();
        Random random = new Random(seed);

        reqs.forEach(UtilException.rethrowConsumer(requestList -> {
            // for each batch of requests, expand on trace id for the given list of intermediate services
            // (a property of replay)
            List<Event> filteredList = requestList.stream().filter(request ->
                replay.sampleRate.map(sr -> random.nextDouble() <= sr).orElse(true))
                .collect(Collectors.toList());

            Stream<Event> enhancedRequests = expandOnTraceId(filteredList, replay.customerId,
                replay.app, replay.collection, rrstore);

            enhancedRequests.forEach(UtilException.rethrowConsumer(r -> {
                analyzeRequestEvent(rrstore, replay, r);
            }));
        }));
        analysis.status = Analysis.Status.MatchingCompleted;
    }

    private void analyzeRequestEvent(ReqRespStore rrstore, Replay replay, Event r) throws TemplateNotFoundException {
        // find matching request in replay
        EventQuery eventQuery = reqEventToEventQuery(r, analysis.replayId, 10);

        TemplateKey key = new TemplateKey(templateVersion, replay.customerId, replay.app,
            r.service, r.apiPath, TemplateKey.Type.Request);
        Comparator comparator = comparatorCache.getComparator(key, r.eventType);


        Result<Event> matches = rrstore.getEvents(eventQuery);
        // TODO: add toString override for the Request object to debug log
        if (matches.numResults > 0) {

            List<Event> matchedReqs = matches.getObjects().collect(Collectors.toList());
            Map<String, Event> replayResponseMap = getReqToResponseMap(matchedReqs, replay.customerId,
                replay.app, replay.replayId, r.service, r.eventType, r.getTraceId(), rrstore);



            Optional<Event> recordedResponse = getResponseFromRequestEvent(r, rrstore);
            if (matches.numResults > 1) {
                analysis.reqmultiplematch++;
            } else {
                analysis.reqsinglematch++;
            }

            // fetch response of recording and replay
            // TODO change it back to RecReqNoMatch
            ReqRespMatchWithEvent bestmatch = new ReqRespMatchWithEvent(r, Optional.empty(),
                Comparator.Match.DEFAULT, Optional.empty(), Optional.empty(), Optional.empty());
            MatchType bestreqmt = MatchType.NoMatch;

            // matches is ordered in decreasing order of request match score. so exact matches
            // of requests, if any should be at the beginning
            // If request matches exactly, consider that as the best match
            // else find the best match based on response matching
            for (Event replayreq : matchedReqs) {
                // we have removed EqualOptional in request match. So any match has to be ExactMatch
                MatchType reqmt = ExactMatch;
                ReqRespMatchWithEvent match = checkRespEventMatch(r, replayreq, recordedResponse,
                    replayResponseMap);
                if (isReqRespMatchBetter(reqmt, match.getRespMt(), bestreqmt, bestmatch.getRespMt())) {
                    bestmatch = match;
                    bestreqmt = reqmt;
                    if (bestmatch.getRespMt() == ExactMatch) {
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
            switch (bestmatch.getRespMt()) {
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

            LOGGER.debug(bestmatch.getRespMt() + " OCCURRED FOR RESPONSE :: " + r.reqId);
            LOGGER.debug("REQUEST 1 " + bestmatch.getRecordReq(config).orElse(" N/A"));
            LOGGER.debug("REQUEST 2 " + bestmatch.getReplayReq(config).orElse("N/A"));
            LOGGER.debug("DOC 1 " + bestmatch.getRecordedResponseBody(config).orElse(" N/A"));
            LOGGER.debug("DOC 2 " + bestmatch.getReplayResponseBody(config).orElse(" N/A"));
            bestmatch.getRespDiffs().forEach(
                diff -> {
                    try {
                        LOGGER.debug("DIFF :: " + jsonMapper.writeValueAsString(diff));
                    } catch (JsonProcessingException e) {
                        // DO NOTHING
                    }
                });

            ReqRespMatchResult res = new ReqRespMatchResult(bestmatch, bestreqmt,
                (int) matches.numResults, analysis.replayId);
            rrstore.saveResult(res);

        } else {
            // TODO change it back to RecReqNoMatch
            ReqRespMatchResult res = new ReqRespMatchResult(new ReqRespMatchWithEvent(r,
                Optional.empty(), Comparator.Match.NOMATCH, Optional.empty() , Optional.empty(), Optional.empty()),
                MatchType.NoMatch, (int)matches.numResults, analysis.replayId);
            rrstore.saveResult(res);
            analysis.reqnotmatched++;
        }

        analysis.reqanalyzed++;
        if (analysis.reqanalyzed % UPDBATCHSIZE == 0) {
            LOGGER.info(String.format("Analysis of replay %s completed %d requests", analysis.replayId, analysis.reqanalyzed));
            rrstore.saveAnalysis(analysis);
        }
    }


    private ReqRespMatchWithEvent checkRespEventMatch(Event recordreq, Event replayreq,
                                                      Optional<Event> recordedResponse ,
                                                     Map<String, Event> replayResponseMap) {

        try {
            // get appropriate template from solr
            TemplateKey key = new TemplateKey(templateVersion, recordreq.customerId,
                recordreq.app, recordreq.service, recordreq.apiPath , TemplateKey.Type.Response);
            Optional<Event> replayresp = Optional.ofNullable(replayResponseMap.get(replayreq.reqId));
            if (recordedResponse.isPresent() && replayresp.isPresent()) {
                Event recordedr = recordedResponse.get();
                Event replayr = replayresp.get();
                Comparator comparator = comparatorCache.getComparator(key, recordedr.eventType);
                Comparator.Match rm = comparator.compare(recordedr.getPayload(config), replayr.getPayload(config));
                return new ReqRespMatchWithEvent(recordreq, Optional.of(replayreq) , rm ,
                    Optional.of(recordedr) , Optional.of(replayr), Optional.empty());
            }
            return new ReqRespMatchWithEvent(recordreq, Optional.of(replayreq),
                Comparator.Match.NOMATCH, recordedResponse, replayresp , Optional.empty());
        } catch(Exception e) {
            // if analysis retrieval caused an error, log the error and return NO MATCH
            String stackTraceError =  UtilException.extractFirstStackTraceLocation(e.getStackTrace());
            LOGGER.error("Exception while analyzing response :: " +
                recordreq.reqId + " " +  e.getMessage() + " " + stackTraceError);
            return new ReqRespMatchWithEvent(recordreq, Optional.of(replayreq),
                Comparator.Match.NOMATCH, Optional.empty() , Optional.empty(), Optional.empty());
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

    EventQuery reqEventToEventQuery(Event reqEvent, String replayId, int limit) {

        EventQuery.Builder builder = new EventQuery.Builder(reqEvent.customerId, reqEvent.app, reqEvent.eventType);

        return builder.withPath(reqEvent.apiPath)
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
     * @param tracefield
     * @return
     */
    public static Optional<Analysis> analyze(String replayId, String tracefield,
                                             Config config) throws TemplateNotFoundException {

        ReqRespStore rrstore = config.rrstore;

        Optional<Replay> replay = rrstore.getReplay(replayId);

        return replay.flatMap(UtilException.rethrowFunction(r -> {
            // get request in batches ... for batching of corresponding trace id queries
            // TODO need to get the batch size from some config
            Pair<Stream<List<Event>> , Long> result = r.getRequestEventBatches(TRACEBATCHSIZE , rrstore);

            String templateVersionToUse = r.templateVersion;

            //Result<Request> reqs = r.getRequests(rrstore, true);
            Analyzer analyzer = new Analyzer(replayId, result.getRight().intValue(), templateVersionToUse, config);
            if (!rrstore.saveAnalysis(analyzer.analysis)) {
                return Optional.empty();
            }

            analyzer.analyzeWithEvent(rrstore, result.getLeft(), r);

            // update the stored analysis
            rrstore.saveAnalysis(analyzer.analysis);


            // Compute the aggregations here itself for all levels.
            Optional<String> service = Optional.empty();
            boolean bypath = true;

            Collection<MatchResultAggregate> resultAggregates = rrstore.computeResultAggregate(replayId, service, bypath);
            resultAggregates.forEach( resultAggregate -> {
                rrstore.saveMatchResultAggregate(resultAggregate);
            } );

            // Everything including aggregation completed
            analyzer.analysis.status = Analysis.Status.Completed;
            rrstore.saveAnalysis(analyzer.analysis);

            return Optional.of(analyzer.analysis);
        }));
    }


}

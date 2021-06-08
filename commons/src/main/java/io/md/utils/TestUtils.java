package io.md.utils;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.md.core.CollectionKey;
import io.md.core.CompareTemplate;
import io.md.core.TemplateKey.Type;
import io.md.dao.CustomerAppConfig;
import io.md.dao.Event;
import io.md.dao.Event.EventType;
import io.md.dao.EventQuery;
import io.md.dao.ProtoDescriptorDAO;
import io.md.dao.RecordOrReplay;
import io.md.dao.Recording;
import io.md.dao.Recording.RecordingType;
import io.md.dao.Replay;
import io.md.dao.ReqRespMatchResult;
import io.md.injection.DynamicInjectionConfig;
import io.md.services.DSResult;
import io.md.services.DataStore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class TestUtils {

    public static class ReqAndRespEvent {
        @JsonProperty("goldenReq")
        public Event goldenReq;
        @JsonProperty("goldenResp")
        public Event goldenResp;
        @JsonProperty("testResp")
        public Event testResp;
    }

    public static class StaticDataStore implements DataStore {

        Map<String, ReqAndRespEvent> reqIdToReqAndRespEventMap = new HashMap<>();

        public StaticDataStore(List<ReqAndRespEvent> reqAndRespEvents) {
            reqAndRespEvents.forEach(reqAndRespEvent -> {
                reqIdToReqAndRespEventMap
                    .put(reqAndRespEvent.goldenReq.reqId, reqAndRespEvent);
            });
        }

        public Optional<ReqAndRespEvent> getReqAndResp(String reqId){
            return Optional.ofNullable(reqIdToReqAndRespEventMap.get(reqId));
        }

        @Override
        public Optional<RecordOrReplay> getCurrentRecordOrReplay(String customerId, String app,
            String instanceId) {
            return Optional.empty();
        }

        @Override
        public DSResult<Event> getEvents(EventQuery eventQuery) {
            return null;
        }

        @Override
        public Optional<Event> getSingleEvent(EventQuery eventQuery) {
            return Optional.empty();
        }

        @Override
        public Optional<Event> getRespEventForReqEvent(Event reqEvent) {
            return getReqAndResp(reqEvent.reqId).map(reqAndResp -> reqAndResp.goldenResp);
        }

        @Override
        public Optional<Event> getResponseEvent(String reqId) {
            return getReqAndResp(reqId).map(reqAndResp -> reqAndResp.goldenResp);
        }

        @Override
        public CompareTemplate getTemplate(String customerId, String app, String service,
            String apiPath, String templateVersion, Type templateType,
            Optional<EventType> eventType, Optional<String> method, String recordingId)
            throws TemplateNotFoundException {
            return null;
        }

        @Override
        public Optional<DynamicInjectionConfig> getDynamicInjectionConfig(String customerId,
            String app, String version) {
            return Optional.empty();
        }

        @Override
        public Optional<Replay> getReplay(String replayId) {
            return Optional.empty();
        }

        @Override
        public Optional<Recording> getRecording(String recordingId) {
            return Optional.empty();
        }

        @Override
        public Optional<CustomerAppConfig> getAppConfiguration(String customer, String app) {
            return Optional.empty();
        }

        @Override
        public boolean saveResult(ReqRespMatchResult res, String customerId) {
            return false;
        }

        @Override
        public boolean save(Event event) {
            return false;
        }

        @Override
        public boolean save(Stream<Event> eventStream) {
            return false;
        }

        @Override
        public boolean saveReplay(Replay replay) {
            return false;
        }

        @Override
        public boolean deferredDelete(Replay replay) {
            return false;
        }

        @Override
        public void populateCache(CollectionKey collectionKey, RecordOrReplay rr) {

        }

        @Override
        public Optional<ProtoDescriptorDAO> getLatestProtoDescriptorDAO(String customerId,
            String app) {
            return Optional.empty();
        }

        @Override
        public Optional<String> getLatestTemplateSetLabel(String customerId, String app,
            String templateSetName) {
            return Optional.empty();
        }

        @Override
        public Recording copyRecording(String recordingId, Optional<String> name,
            Optional<String> label, Optional<String> templateSetName,
            Optional<String> templateSetLabel, String userId, RecordingType type,
            Optional<Predicate<Event>> eventFilter) throws Exception {
            return null;
        }
    }
}

package io.cube.agent;

import io.md.core.TemplateKey.Type;
import io.md.dao.Event.EventType;
import java.io.IOException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.md.core.CompareTemplate;
import io.md.core.TemplateKey;
import io.md.dao.Event;
import io.md.dao.EventQuery;
import io.md.dao.RecordOrReplay;
import io.md.dao.Recording;
import io.md.dao.Replay;
import io.md.dao.ReqRespMatchResult;
import io.md.injection.DynamicInjectionConfig;
import io.md.services.AbstractDataStore;
import io.md.services.DSResult;
import io.md.services.DataStore;
import io.md.utils.CubeObjectMapperProvider;

/*
 * Created by IntelliJ IDEA.
 * Date: 23/06/20
 */
public class ProxyDataStore extends AbstractDataStore implements DataStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyDataStore.class);

    private final CubeClient cubeClient;
    private final ObjectMapper jsonMapper;
    private final TypeReference<ArrayDSResult<Event>> eventArrayTypeReference =
            new TypeReference<ArrayDSResult<Event>>() {};


    public ProxyDataStore() {
        jsonMapper = CubeObjectMapperProvider.getInstance();
        cubeClient = new CubeClient(jsonMapper);
    }

    @Override
    public Optional<RecordOrReplay> getCurrentRecordOrReplay(String customerId, String app, String instanceId) {
        try {
            return cubeClient.getCurrentRecordOrReplay(customerId, app, instanceId)
                    .map(UtilException.rethrowFunction(template -> jsonMapper.readValue(template,
                            RecordOrReplay.class)));
        } catch (IOException e) {
            LOGGER.error("Exception occurred while getting RecordOrReplay object : ", e);
        }
        return Optional.empty();
    }

    @Override
    public DSResult<Event> getEvents(EventQuery eventQuery) {
        try {
            return cubeClient.getEvents(eventQuery)
                    .map(UtilException.rethrowFunction(result ->
                            (DSResult<Event>)jsonMapper.readValue(result, eventArrayTypeReference)))
                    .orElseGet(() -> new ArrayDSResult<Event>());
        } catch (IOException e) {
            LOGGER.error("Exception occurred while getting events : " + eventQuery.toString(), e);
        }
        return new ArrayDSResult<Event>();
    }

    @Override
    public CompareTemplate getTemplate(String customerId, String app, String service, String apiPath,
                                       String templateVersion, TemplateKey.Type templateType,
                                       Optional<Event.EventType> eventType, Optional<String> method, String recordingId) throws TemplateNotFoundException {
        try {
            return cubeClient.getTemplate(customerId, app, service, apiPath, templateVersion, templateType, eventType, recordingId)
                    .map(UtilException.rethrowFunction(template -> jsonMapper.readValue(template,
                            CompareTemplate.class)))
                    .orElseThrow(() -> new TemplateNotFoundException());
        } catch (IOException e) {
            LOGGER.error("Exception occurred while getting template : " + templateVersion, e);
        }
        throw new TemplateNotFoundException();
    }

    @Override
    public Optional<DynamicInjectionConfig> getDynamicInjectionConfig(String customerId,
                                                                      String app,
                                                                      String version) {
        try {
            return cubeClient.getDynamicInjectionConfig(customerId, app, version)
                    .map(UtilException.rethrowFunction(config -> jsonMapper.readValue(config,
                            DynamicInjectionConfig.class)));
        } catch (IOException e) {
            LOGGER.error("Exception occurred while getting dynamic injection config : " + version, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Replay> getReplay(String replayId) {
        try {
            return cubeClient.getReplay(replayId)
                    .map(UtilException.rethrowFunction(config -> jsonMapper.readValue(config,
                            Replay.class)));
        } catch (IOException e) {
            LOGGER.error("Exception occurred while getting replay : " + replayId, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Recording> getRecording(String recordingId) {
        try {
            return cubeClient.getRecording(recordingId)
                    .map(UtilException.rethrowFunction(config -> jsonMapper.readValue(config,
                            Recording.class)));
        } catch (IOException e) {
            LOGGER.error("Exception occurred while getting replay : " + recordingId, e);
        }
        return Optional.empty();
    }

    @Override
    public boolean saveResult(ReqRespMatchResult reqRespMatchResult) {
        return cubeClient.saveResult(reqRespMatchResult).isPresent();
    }

    @Override
    public boolean save(Event event) {
        return cubeClient.storeEvent(event).isPresent();
    }

    @Override
    public boolean saveReplay(Replay replay) {
        return cubeClient.saveReplay(replay).isPresent();
    }

    @Override
    public boolean deferredDelete(Replay replay) {
        return cubeClient.deferredDelete(replay).isPresent();
    }
}

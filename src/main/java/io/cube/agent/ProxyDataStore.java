package io.cube.agent;

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
                                       Optional<Event.EventType> eventType) throws TemplateNotFoundException {
        try {
            return cubeClient.getTemplate(customerId, app, service, apiPath, templateVersion, templateType, eventType)
                    .map(UtilException.rethrowFunction(template -> jsonMapper.readValue(template,
                            CompareTemplate.class)))
                    .orElseThrow(() -> new TemplateNotFoundException());
        } catch (IOException e) {
            LOGGER.error("Exception occurred while getting template : " + templateVersion, e);
        }
        throw new TemplateNotFoundException();
    }

    @Override
    public Optional<DynamicInjectionConfig> getDynamicInjectionConfig(String s, String s1,
        String s2) {
        //TODO: Needs to be handled properly
        return Optional.empty();
    }

    @Override
    public Optional<Replay> getReplay(String s) {
        //TODO: Needs to be handled properly
        return Optional.empty();
    }

    @Override
    public Optional<Recording> getRecording(String s) {
        //TODO: Needs to be handled properly
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
        //TODO: Needs to be handled properly
        return true;
    }

    @Override
    public boolean deferredDelete(Replay replay) {
        //TODO: Needs to be handled properly
        return true;
    }
}

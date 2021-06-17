/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.cube.agent;

import io.md.core.CollectionKey;
import io.md.dao.*;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Predicate;

import io.md.dao.Recording.RecordingType;
import io.md.logger.LogMgr;

import org.slf4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.md.core.CompareTemplate;
import io.md.core.TemplateKey;
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

    private static final Logger LOGGER = LogMgr.getLogger(ProxyDataStore.class);

    private final CubeClient cubeClient;
    private final ObjectMapper jsonMapper;
    private final TypeReference<ArrayDSResult<Event>> eventArrayTypeReference =
            new TypeReference<ArrayDSResult<Event>>() {};


    public ProxyDataStore() {
        jsonMapper = CubeObjectMapperProvider.getInstance();
        cubeClient = new CubeClient(jsonMapper);
    }

    public void setAuthToken(String authToken) {
        cubeClient.setAuthToken(authToken);
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
    public Optional<CustomerAppConfig> getAppConfiguration(String customerId, String app) {
        try {
            return cubeClient.getAppConfiguration(customerId, app)
                    .map(UtilException.rethrowFunction(config -> jsonMapper.readValue(config,
                            CustomerAppConfig.class)));
        } catch (IOException e) {
            LOGGER.error("Exception occurred while getting Customer App config : " + customerId + " " + app, e);
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
    public boolean saveResult(ReqRespMatchResult reqRespMatchResult, String s) {
        return cubeClient.saveResult(reqRespMatchResult).isPresent();
    }

    @Override
    public boolean save(Event... events) {
        if(events.length==0) return true;
        if(events.length==1) return save(events[0]);

        return cubeClient.storeEvents(events).isPresent();
    }


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

    @Override
    public void populateCache(CollectionKey collectionKey, RecordOrReplay recordOrReplay) {
        cubeClient.populateCache(collectionKey , recordOrReplay);
    }

    /*
      Todo: This method needs to be removed from Datastore interface. After that it can be removed from here.
     */
    @Override
    public Optional<ProtoDescriptorDAO> getLatestProtoDescriptorDAO(String customerId, String app) {

        try {
            return cubeClient.getLatestProtoDescriptorDAO(customerId, app)
                .map(UtilException.rethrowFunction(proto -> jsonMapper.readValue(proto,
                    ProtoDescriptorDAO.class)));
        } catch (IOException e) {
            LOGGER.error(
                "Exception occurred while getting protoDescriptor for customer : " + customerId
                    + " and app : " + app, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> getLatestTemplateSetLabel(String customerId, String app,
        String templateSetName) {
        return cubeClient.getLatestTemplateSetLabel(customerId, app, templateSetName);
    }

    @Override
    public Recording copyRecording(String recordingId, Optional<String> name,
        Optional<String> label, Optional<String> templateSetName, Optional<String> templateSetLabel,
        String userId, RecordingType type, Optional<Predicate<Event>> eventFilter) throws Exception {
        return cubeClient.copyRecording(recordingId, name, label, templateSetName, templateSetLabel,
            userId , type, eventFilter).map(UtilException.rethrowFunction(recordingStr -> jsonMapper
            .readValue(recordingStr, Recording.class))).orElseThrow(() -> new
            Exception("Unable to copy recording"));
    }

}

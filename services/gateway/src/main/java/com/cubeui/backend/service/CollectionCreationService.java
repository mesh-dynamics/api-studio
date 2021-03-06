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

package com.cubeui.backend.service;

import com.cubeui.backend.domain.App;
import com.cubeui.backend.domain.Customer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.md.constants.Constants;
import io.md.dao.Event;
import io.md.dao.Event.EventBuilder;
import io.md.dao.Event.EventBuilder.InvalidEventException;
import io.md.dao.Event.EventType;
import io.md.dao.EventQuery;
import io.md.dao.MDTraceInfo;
import io.md.dao.Recording;
import io.md.dao.Recording.RecordingType;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Slf4j
@Service
@Transactional
public class CollectionCreationService {
  @Autowired
  private CubeServerService cubeServerService;
  @Autowired
  private ObjectMapper jsonMapper;

  @Async("threadPoolTaskExecutor")
  public void createSampleCollectionForCustomer(HttpServletRequest httpServletRequest, Customer customer, App app) {
    String query =  String.format("customerId=%s&app=%s&golden_name=%s&recordingType=%s&archived=%s",
        "CubeCorp", "MovieInfo", "SampleCollection", RecordingType.Golden.toString(), false);
    Optional<Recording> existingRecording = cubeServerService.searchRecording(query);
    existingRecording.ifPresent(recording -> {
      EventQuery.Builder builder = new EventQuery.Builder("CubeCorp", "MovieInfo",
          Arrays.asList(Event.EventType.HTTPRequest, EventType.HTTPResponse));
      builder.withCollection(recording.collection);
      Optional<List<Event>>  responseEvents = cubeServerService.getEvents(builder.build(), httpServletRequest);
      responseEvents.ifPresent(events -> {
        String instanceId = customer.getName().concat("-initial");
        MultiValueMap<String, String> formParams = new LinkedMultiValueMap<>();
        formParams.set("name", recording.name);
        formParams.set("label", new Date().toString());
        formParams.set("userId", customer.getEmail());
        formParams.set("recordingType", RecordingType.Golden.toString());
        ResponseEntity responseEntity = cubeServerService
            .createRecording(httpServletRequest,
                customer.getName(), app.getName(), instanceId,
                Optional.of(formParams));
        Optional<Recording> newRecordingOptional = cubeServerService
            .getRecordingFromResponseEntity(responseEntity, query);
        newRecordingOptional.ifPresent(newRecording -> {
          StringBuilder eventBatchBuilder = new StringBuilder();
          String timestamp = Instant.now().toString();
          events.stream().parallel().forEach(event -> {
            try {
              Event newEvent = createEvent(event, customer.getName(), newRecording.collection, timestamp, instanceId);
              Map<String, Event> map = Map.of("cubeEvent",newEvent);
              eventBatchBuilder.append(jsonMapper.writeValueAsString(map)).append("\n");
            } catch (InvalidEventException e) {
              log.error("Error while creating an event, message=" + e.getMessage());
            } catch (JsonProcessingException e) {
              log.error("Error while processing an event, message=" + e.getMessage());
            }
          });
          String eventBatch = eventBatchBuilder.toString();
          if(!eventBatch.isBlank()) {
            cubeServerService.fetchPostResponse(httpServletRequest, Optional.of(eventBatch), "/cs/storeEventBatch",
                Constants.APPLICATION_X_NDJSON);
          }
        });
      });
    });

  }
  private Event createEvent(Event event, String customerId, String collection, String timestamp, String instanceId)
      throws InvalidEventException {
    final String reqId = event.reqId.concat("-").concat(timestamp);
    EventBuilder eventBuilder = new EventBuilder(customerId, event.app,
        event.service, instanceId, collection,
        new MDTraceInfo(event.getTraceId(), event.spanId, event.parentSpanId),
        event.getRunType(), Optional.of(Instant.now()), reqId, event.apiPath,
        event.eventType, event.recordingType).withRunId(event.runId);
    eventBuilder.setPayload(event.payload);
    eventBuilder.withMetaData(event.metaData);
    eventBuilder.withPayloadFields(event.payloadFields);
    return eventBuilder.createEvent();
  }

}

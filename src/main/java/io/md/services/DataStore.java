/*
 *
 *    Copyright Cube I O
 *
 */

package io.md.services;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

import io.md.core.CollectionKey;
import io.md.core.CompareTemplate;
import io.md.core.TemplateKey.Type;
import io.md.dao.*;
import io.md.injection.DynamicInjectionConfig;


/*
 * Created by IntelliJ IDEA.
 * Date: 14/05/20
 */
public interface DataStore {

    class TemplateNotFoundException extends Exception {
    }

    /**
     * @param customerId
     * @param app
     * @param instanceId
     * @return
     */
    Optional<RecordOrReplay> getCurrentRecordOrReplay(String customerId, String app, String instanceId);

    DSResult<Event> getEvents(EventQuery eventQuery);

    Optional<Event> getSingleEvent(EventQuery eventQuery);

    Optional<Event> getRespEventForReqEvent(Event reqEvent);

    /**
     * @param reqId
     * @return the matching response on the reqId
     */
    Optional<Event> getResponseEvent(String reqId);

    CompareTemplate getTemplate(String customerId, String app, String service, String apiPath,
        String templateVersion, Type templateType,
        Optional<Event.EventType> eventType, Optional<String> method, String recordingId) throws TemplateNotFoundException;

    /**
     *
     * @param customerId
     * @param app
     * @param version
     * @return
     */
    Optional<DynamicInjectionConfig> getDynamicInjectionConfig(String customerId, String app, String version);

    Optional<Replay> getReplay(String replayId);

    Optional<Recording> getRecording(String recordingId);

    Optional<CustomerAppConfig> getAppConfiguration(String customer , String app);

    /**
     * @param res
     * @return
     */
    boolean saveResult(ReqRespMatchResult res, String customerId);

    boolean save(Event event);

    boolean save(Stream<Event> eventStream);

    boolean saveReplay(Replay replay);

    /**
     * @param replay
     * @return
     */
    boolean deferredDelete(Replay replay);

    void populateCache(CollectionKey collectionKey, RecordOrReplay rr);


//    Optional<ProtoDescriptor> getProtoDescriptor(String customer, String app);
    Optional<ProtoDescriptorDAO> getLatestProtoDescriptorDAO(String customerId, String app);

}

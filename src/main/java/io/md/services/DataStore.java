/*
 *
 *    Copyright Cube I O
 *
 */

package io.md.services;

import java.util.Optional;

import io.md.core.CompareTemplate;
import io.md.core.TemplateKey.Type;
import io.md.dao.Event;
import io.md.dao.EventQuery;
import io.md.dao.RecordOrReplay;
import io.md.dao.Recording;
import io.md.dao.Replay;
import io.md.dao.ReqRespMatchResult;
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
        Optional<Event.EventType> eventType) throws TemplateNotFoundException;

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

    /**
     * @param res
     * @return
     */
    boolean saveResult(ReqRespMatchResult res);

    boolean save(Event event);

    boolean saveReplay(Replay replay);

    /**
     * @param replay
     * @return
     */
    boolean deferredDelete(Replay replay);
}

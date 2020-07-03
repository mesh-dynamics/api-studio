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
import io.md.dao.ReqRespMatchResult;


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

    CompareTemplate getTemplate(String customerId, String app, String service, String apiPath,
        String templateVersion, Type templateType,
        Optional<Event.EventType> eventType) throws TemplateNotFoundException;

    /**
     * @param res
     * @return
     */
    boolean saveResult(ReqRespMatchResult res);

    boolean save(Event event);
}

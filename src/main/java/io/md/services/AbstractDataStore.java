/*
 *
 *    Copyright Cube I O
 *
 */

package io.md.services;

import java.util.Optional;

import io.md.core.CompareTemplate;
import io.md.core.TemplateKey;
import io.md.dao.Event;
import io.md.dao.Event.EventType;
import io.md.dao.EventQuery;


/*
 * Created by IntelliJ IDEA.
 * Date: 15/05/20
 */
public abstract class AbstractDataStore implements DataStore {

    @Override
    public Optional<Event> getSingleEvent(EventQuery eventQuery) {
        return getEvents(eventQuery).getObjects().findFirst();
    }

    @Override
    public CompareTemplate getRequestMatchTemplate(Event event
        , String templateVersion) throws TemplateNotFoundException {
        TemplateKey tkey =
            new TemplateKey(templateVersion, event.customerId,
                event.app, event.service, event.apiPath, TemplateKey.Type.RequestMatch);

        return getComparator(tkey, event.eventType).getCompareTemplate();
    }

    @Override
    public Optional<Event> getRespEventForReqEvent(Event reqEvent){
        if (reqEvent.eventType == EventType.JavaRequest) {
            // Request and response is same event for Java requests
            return Optional.of(reqEvent);
        }
        EventQuery.Builder builder = new EventQuery.Builder(reqEvent.customerId, reqEvent.app,
            Event.EventType.getResponseType(reqEvent.eventType));
        EventQuery eventQuery = builder.withCollection(reqEvent.getCollection())
            .withService(reqEvent.service)
            .withTraceId(reqEvent.getTraceId())
            .withReqId(reqEvent.reqId)
            .withLimit(1)
            .build();
        return getSingleEvent(eventQuery);
    }

}

/*
 *
 *    Copyright Cube I O
 *
 */

package com.cube.services;

import java.util.Optional;

import io.md.core.CompareTemplate;
import io.md.dao.Event;
import io.md.dao.EventQuery;

import com.cube.cache.ComparatorCache;
import com.cube.cache.TemplateKey;

/*
 * Created by IntelliJ IDEA.
 * Date: 15/05/20
 */
public abstract class AbstractCubeDataStore implements CubeDataStore {

    @Override
    public Optional<Event> getSingleEvent(EventQuery eventQuery) {
        return getEvents(eventQuery).getObjects().findFirst();
    }

    @Override
    public CompareTemplate getRequestMatchTemplate(Event event
        , String templateVersion) throws ComparatorCache.TemplateNotFoundException {
        TemplateKey tkey =
            new TemplateKey(templateVersion, event.customerId,
                event.app, event.service, event.apiPath, TemplateKey.Type.RequestMatch);

        return getComparator(tkey, event.eventType).getCompareTemplate();
    }

    @Override
    public Optional<Event> getRespEventForReqEvent(Event reqEvent){
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

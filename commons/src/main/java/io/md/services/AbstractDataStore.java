/*
 *
 *    Copyright Cube I O
 *
 */

package io.md.services;

import java.util.Optional;
import java.util.stream.Stream;

import io.md.core.BatchingIterator;
import io.md.dao.Event;
import io.md.dao.Event.EventType;
import io.md.dao.EventQuery;


/*
 * Created by IntelliJ IDEA.
 * Date: 15/05/20
 */
public abstract class AbstractDataStore implements DataStore {

    public final int EVENT_BATCH_SIZE = 200;

    @Override
    public Optional<Event> getSingleEvent(EventQuery eventQuery) {
        return getEvents(eventQuery).getObjects().findFirst();
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

    @Override
    public Optional<Event> getResponseEvent(String reqId) {

        EventQuery.Builder builder = new EventQuery.Builder("*", "*", Event.RESPONSE_EVENT_TYPES);
        builder.withReqId(reqId).withLimit(1);

        return getSingleEvent(builder.build());
    }

    public abstract boolean save(Event... events);

    @Override
    public boolean save(Stream<Event> eventStream){
        return BatchingIterator.batchedStreamOf(eventStream , EVENT_BATCH_SIZE).map(listofEvents-> save(listofEvents.stream().toArray(Event[]::new))).reduce(Boolean::logicalAnd).orElse(true);
    }


}

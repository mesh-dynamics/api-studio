/*
 *
 *    Copyright Cube I O
 *
 */

package com.cube.services;

import java.util.Optional;

import io.md.core.Comparator;
import io.md.core.CompareTemplate;
import io.md.dao.Event;
import io.md.dao.EventQuery;

import com.cube.cache.ComparatorCache;
import com.cube.cache.TemplateKey;
import com.cube.dao.ReqRespMatchResult;
import com.cube.dao.ReqRespStore;

/*
 * Created by IntelliJ IDEA.
 * Date: 14/05/20
 */
public interface DataStore {

    /**
     * @param customerId
     * @param app
     * @param instanceId
     * @return
     */
    Optional<ReqRespStore.RecordOrReplay> getCurrentRecordOrReplay(String customerId, String app, String instanceId);

    Comparator getComparator(TemplateKey key, Event.EventType eventType) throws
        ComparatorCache.TemplateNotFoundException;

    DSResult<Event> getEvents(EventQuery eventQuery);

    Optional<Event> getSingleEvent(EventQuery eventQuery);

    Optional<Event> getRespEventForReqEvent(Event reqEvent);

    CompareTemplate getRequestMatchTemplate(Event event
        , String templateVersion) throws ComparatorCache.TemplateNotFoundException;

    /**
     * @param res
     * @return
     */
    boolean saveResult(ReqRespMatchResult res);

    boolean save(Event event);
}

/*
 *
 *    Copyright Cube I O
 *
 */

package com.cube.services;

import java.util.Optional;

import io.md.core.Comparator;
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
public class CubeDataStoreLocal extends AbstractCubeDataStore implements CubeDataStore {

    private final ReqRespStore rrstore;
    private final ComparatorCache comparatorCache;


    public CubeDataStoreLocal(ReqRespStore rrstore, ComparatorCache comparatorCache) {
        this.rrstore = rrstore;
        this.comparatorCache = comparatorCache;
    }

    @Override
    public Optional<ReqRespStore.RecordOrReplay> getCurrentRecordOrReplay(String customerId, String app, String instanceId) {
        return rrstore.getCurrentRecordOrReplay(Optional.of(customerId),
            Optional.of(app), Optional.of(instanceId));
    }

    @Override
    public Comparator getComparator(TemplateKey key, Event.EventType eventType) throws ComparatorCache.TemplateNotFoundException {
        return comparatorCache.getComparator(key, eventType);
    }

    @Override
    public CubeDSResult<Event> getEvents(EventQuery eventQuery) {
        return rrstore.getEvents(eventQuery);
    }

    @Override
    public boolean saveResult(ReqRespMatchResult res) {
        return rrstore.saveResult(res);
    }

    @Override
    public boolean save(Event event) {
        return rrstore.save(event);
    }
}

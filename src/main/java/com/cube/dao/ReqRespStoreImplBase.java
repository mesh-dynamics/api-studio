/**
 * Copyright Cube I O
 */
package com.cube.dao;

import io.md.constants.ReplayStatus;
import io.md.core.CollectionKey;
import io.md.core.Comparator;
import io.md.core.CompareTemplate;
import io.md.core.TemplateKey;
import io.md.dao.Event;
import io.md.dao.Recording;
import io.md.dao.Replay;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import com.google.common.base.MoreObjects;

import io.md.services.AbstractDataStore;
import io.md.dao.RecordOrReplay;
import io.md.utils.Constants;

/**
 * @author prasad
 *
 */
public abstract class ReqRespStoreImplBase extends AbstractDataStore implements ReqRespStore {

	private static final Logger LOGGER = LogManager.getLogger(ReqRespStoreImplBase.class);

    /* (non-Javadoc)
	 * @see com.cube.dao.ReqRespStore#getCurrentCollection(java.util.Optional, java.util.Optional, java.util.Optional)
	 * For a (cust, app, instance), there is one current collection. Either a recording is going on or a replay or nothing. This
	 * looks up the state and caches it for quick retrieval
	 */
	@Override
	public Optional<String> getCurrentCollection(Optional<String> customerId, Optional<String> app,
			Optional<String> instanceId) {

		return getCurrentRecordOrReplay(customerId, app, instanceId).flatMap(rr -> rr.getCollection());
	}

    @Override
    public Optional<RecordOrReplay> getCurrentRecordOrReplay(String customerId, String app, String instanceId) {
        return getCurrentRecordOrReplay(Optional.of(customerId),
            Optional.of(app), Optional.of(instanceId));
    }

    /* (non-Javadoc)
	 * @see com.cube.dao.ReqRespStore#getCurrentRecordingCollection(java.util.Optional, java.util.Optional, java.util.Optional)
	 */
	@Override
	public Optional<String> getCurrentRecordingCollection(Optional<String> customerId, Optional<String> app,
			Optional<String> instanceId) {
		return getCurrentRecordOrReplay(customerId, app, instanceId).flatMap(rr -> rr.getRecordingCollection());
	}

    @Override
    public Optional<RecordOrReplay> getCurrentRecordOrReplay(Optional<String> customerId, Optional<String> app,
                                                             Optional<String> instanceId) {
	    return getCurrentRecordOrReplay(customerId, app, instanceId, false);
    }

	/* (non-Javadoc)
	 * @see com.cube.dao.ReqRespStore#getCurrentRecordOrReplay(java.util.Optional, java.util.Optional, java.util.Optional)
	 * For a (cust, app, instance), there is one current recording or replay. Either a recording is going on or a replay or nothing. This
	 * looks up the state and caches it for quick retrieval
	 */
	@Override
	public Optional<RecordOrReplay> getCurrentRecordOrReplay(Optional<String> customerId,
		Optional<String> app, Optional<String> instanceId, boolean extendTTL) {

		CollectionKey ckey = new CollectionKey(customerId.orElse(""), app.orElse(""),
			instanceId.orElse(""));

		// in this case matching has to be exact. Null values should match empty strings
		Optional<String> ncustomerid = customerId.or(() -> Optional.of(""));
		Optional<String> napp = app.or(() -> Optional.of(""));
		Optional<String> ninstanceid = instanceId.or(() -> Optional.of(""));
		Optional<RecordOrReplay> cachedrr = retrieveFromCache(ckey, extendTTL);

		return cachedrr.map(cachedRRVal -> {
			LOGGER.debug(
				new ObjectMessage(Map.of(Constants.MESSAGE, "Retrieved Record/Replay from Cache"
					, Constants.CUSTOMER_ID_FIELD, customerId.orElse(Constants.NOT_PRESENT)
					, Constants.APP_FIELD, app.orElse(Constants.NOT_PRESENT)
					, Constants.INSTANCE_ID_FIELD, instanceId.orElse(Constants.NOT_PRESENT)
					, "value", cachedRRVal.toString())));
			return cachedRRVal;
		}).or(() -> {
			// not cached, read from underlying store
			// check if there is a recording going on
			LOGGER.debug(new ObjectMessage(Map.of(Constants.MESSAGE, "Unable to retrieve"
					+ " Record/Replay from Cache, Looking for recording in solr",
				Constants.CUSTOMER_ID_FIELD, customerId.orElse(Constants.NOT_PRESENT),
				Constants.APP_FIELD,
				app.orElse(Constants.NOT_PRESENT), Constants.INSTANCE_ID_FIELD,
				instanceId.orElse(Constants.NOT_PRESENT))));
			Optional<RecordOrReplay> rr = getRecording(ncustomerid, napp, ninstanceid,
				Optional.of(Recording.RecordingStatus.Running), Optional.empty(), Optional.empty(),
                Optional.empty(),Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Collections.emptyList(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
								Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty())
				.getObjects().findFirst()
				.map(recording -> RecordOrReplay.createFromRecording(recording))
				.or(() -> { // no ongoing recording, check replay
					LOGGER.debug(new ObjectMessage(Map.of(Constants.MESSAGE
						, "No running recording, looking for replay in solr"
						, Constants.CUSTOMER_ID_FIELD, customerId.orElse(Constants.NOT_PRESENT)
						, Constants.APP_FIELD, app.orElse(Constants.NOT_PRESENT)
						, Constants.INSTANCE_ID_FIELD, instanceId.orElse(Constants.NOT_PRESENT))));
					return getReplay(ncustomerid, napp, ninstanceid, ReplayStatus.Running)
						.findFirst()
						.map(replay -> RecordOrReplay.createFromReplay(replay));
				});
			//rr.ifPresent(rrv -> currentCollectionMap.put(ckey, rrv));
			rr.ifPresent(rrv -> populateCache(ckey, rrv));
			rr.ifPresentOrElse
				(rrVal ->
						LOGGER.debug(new ObjectMessage(
							Map.of(Constants.MESSAGE, "Retrieved Record/Replay from Solr",
								Constants.CUSTOMER_ID_FIELD, customerId.orElse(Constants.NOT_PRESENT),
								Constants.APP_FIELD,
								app.orElse(Constants.NOT_PRESENT), Constants.INSTANCE_ID_FIELD
								, instanceId.orElse(Constants.NOT_PRESENT), "value", rrVal.toString())))
					, () -> LOGGER.error(new ObjectMessage(
						Map.of(Constants.MESSAGE, "No Record/Replay retrieved from Cache/Solr",
							Constants.CUSTOMER_ID_FIELD, customerId.orElse(Constants.NOT_PRESENT),
							Constants.APP_FIELD,
							app.orElse(Constants.NOT_PRESENT), Constants.INSTANCE_ID_FIELD
							, instanceId.orElse(Constants.NOT_PRESENT)))));
			return rr;
		});
	}


	/* (non-Javadoc)
	 * @see com.cube.dao.ReqRespStore#invalidateCurrentCollectionCache()
	 */
	public void invalidateCurrentCollectionCache(String customerId, String app,
			String instanceId) {
		CollectionKey ckey = new CollectionKey(customerId, app, instanceId);
        removeCollectionKey(ckey);
		//currentCollectionMap.remove(ckey);
	}

	abstract void removeCollectionKey(CollectionKey collectionKey);
    abstract Optional<RecordOrReplay> retrieveFromCache(CollectionKey key, boolean extendTTL);
    //abstract void populateCache(CollectionKey collectionKey, RecordOrReplay rr);
	abstract void updaterFinalReplayStatusInCache(Replay replay);

	/* (non-Javadoc)
	 * @see com.cube.dao.ReqRespStore#saveReplay(com.cube.dao.Replay)
	 */
	@Override
	public boolean deferredDelete(Replay replay) {
		updaterFinalReplayStatusInCache(replay);
		invalidateCurrentCollectionCache(replay.customerId, replay.app, replay.instanceId);
		return true;
	}



	/* (non-Javadoc)
	 * @see com.cube.dao.ReqRespStore#saveRecording(com.cube.dao.Recording)
	 */
	@Override
	public boolean expireRecordingInCache(Recording recording) {
		invalidateCurrentCollectionCache(recording.customerId, recording.app, recording.instanceId);
		return true;
	}


    @Override
    public Comparator getComparator(TemplateKey key) throws TemplateNotFoundException {
        return getComparator(key, Optional.empty());
    }

    @Override
    public Comparator getComparator(TemplateKey key, Event.EventType eventType) throws
        TemplateNotFoundException {
	    return getComparator(key, Optional.ofNullable(eventType));
    }

	@Override
	public CompareTemplate getTemplate(String customerId, String app, String service
		, String apiPath, String templateVersion, TemplateKey.Type templateType,
		Optional<Event.EventType> eventType, Optional<String> method, String recordingId)
		throws TemplateNotFoundException {
		TemplateKey tkey =
			new TemplateKey(templateVersion, customerId,
				app, service, apiPath, templateType, method, recordingId);

		return getComparator(tkey, eventType).getCompareTemplate();
	}


   /* @Override
    public CompareTemplate getTemplate(String customerId, String app, String service, String apiPath,
                                       String templateVersion, TemplateKey.Type templateType,
                                       Optional<Event.EventType> eventType) throws TemplateNotFoundException {
        TemplateKey tkey =
            new TemplateKey(templateVersion, customerId,
                app, service, apiPath, templateType);

        return getComparator(tkey, eventType).getCompareTemplate();
    }*/

	// map from (cust, app, instance) -> collection. collection is empty if there is no current recording or replay
	//private ConcurrentHashMap<CollectionKey, RecordOrReplay> currentCollectionMap = new ConcurrentHashMap<>();
}

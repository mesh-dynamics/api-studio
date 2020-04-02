/**
 * Copyright Cube I O
 */
package com.cube.dao;

import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import com.google.common.base.MoreObjects;

import io.md.dao.Event;

import com.cube.dao.Recording.RecordingStatus;
import com.cube.dao.Replay.ReplayStatus;
import com.cube.utils.Constants;

/**
 * @author prasad
 *
 */
public abstract class ReqRespStoreImplBase implements ReqRespStore {

	private static final Logger LOGGER = LogManager.getLogger(ReqRespStoreImplBase.class);


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



	/* (non-Javadoc)
	 * @see com.cube.dao.ReqRespStore#getCurrentRecordingCollection(java.util.Optional, java.util.Optional, java.util.Optional)
	 */
	@Override
	public Optional<String> getCurrentRecordingCollection(Optional<String> customerId, Optional<String> app,
			Optional<String> instanceId) {
		return getCurrentRecordOrReplay(customerId, app, instanceId).flatMap(rr -> rr.getRecordingCollection());
	}

	@Override
	public Optional<String> getCurrentReplayId(Optional<String> customerId, Optional<String> app, Optional<String> instanceId) {
	    return getCurrentRecordOrReplay(customerId, app, instanceId).flatMap(rr -> rr.getReplayId());
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
				Optional.of(RecordingStatus.Running), Optional.empty())
				.findFirst()
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
    abstract void populateCache(CollectionKey collectionKey, RecordOrReplay rr);


	/* (non-Javadoc)
	 * @see com.cube.dao.ReqRespStore#saveReplay(com.cube.dao.Replay)
	 */
	@Override
	public boolean saveReplay(Replay replay) {
		invalidateCurrentCollectionCache(replay.customerId, replay.app, replay.instanceId);
		return true;
	}



	/* (non-Javadoc)
	 * @see com.cube.dao.ReqRespStore#saveRecording(com.cube.dao.Recording)
	 */
	@Override
	public boolean saveRecording(Recording recording) {
		invalidateCurrentCollectionCache(recording.customerId, recording.app, recording.instanceId);
		return true;
	}



	static protected class CollectionKey {

		/**
		 * @param customerId
		 * @param app
		 * @param instanceId
		 */
		private CollectionKey(String customerId, String app, String instanceId) {
			super();
			this.customerId = customerId;
			this.app = app;
			this.instanceId = instanceId;
		}

		@Override
		public String toString() {
            return MoreObjects.toStringHelper(this).add("customerId" ,  customerId).add("app" , app)
                .add("instanceId" , instanceId).toString();
        }

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((app == null) ? 0 : app.hashCode());
			result = prime * result + ((customerId == null) ? 0 : customerId.hashCode());
			result = prime * result + ((instanceId == null) ? 0 : instanceId.hashCode());
			return result;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			CollectionKey other = (CollectionKey) obj;
			if (app == null) {
				if (other.app != null) {
					return false;
				}
			} else if (!app.equals(other.app)) {
				return false;
			}
			if (customerId == null) {
				if (other.customerId != null) {
					return false;
				}
			} else if (!customerId.equals(other.customerId)) {
				return false;
			}
			if (instanceId == null) {
				if (other.instanceId != null) {
					return false;
				}
			} else if (!instanceId.equals(other.instanceId)) {
				return false;
			}
			return true;
		}

		final String customerId;
		final String app;
		final String instanceId;
	}

	// map from (cust, app, instance) -> collection. collection is empty if there is no current recording or replay
	//private ConcurrentHashMap<CollectionKey, RecordOrReplay> currentCollectionMap = new ConcurrentHashMap<>();
}

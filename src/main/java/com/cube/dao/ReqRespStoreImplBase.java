/**
 * Copyright Cube I O
 */
package com.cube.dao;

import java.util.Optional;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.MoreObjects;

import com.cube.core.RequestComparator;
import com.cube.dao.Recording.RecordingStatus;
import com.cube.dao.Replay.ReplayStatus;

/**
 * @author prasad
 *
 */
public abstract class ReqRespStoreImplBase implements ReqRespStore {

	private static final Logger LOGGER = LogManager.getLogger(ReqRespStoreImplBase.class);


    @Override
    public Stream<Request> getRequests(Request queryrequest, RequestComparator mspec, Optional<Integer> nummatches) {
        return getRequests(queryrequest, mspec, nummatches, Optional.empty());
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



	/* (non-Javadoc)
	 * @see com.cube.dao.ReqRespStore#getCurrentRecordOrReplay(java.util.Optional, java.util.Optional, java.util.Optional)
	 * For a (cust, app, instance), there is one current recording or replay. Either a recording is going on or a replay or nothing. This
	 * looks up the state and caches it for quick retrieval
	 */
	@Override
	public Optional<RecordOrReplay> getCurrentRecordOrReplay(Optional<String> customerId, Optional<String> app,
			Optional<String> instanceId) {

		CollectionKey ckey = new CollectionKey(customerId.orElse(""), app.orElse(""), instanceId.orElse(""));

		// in this case matching has to be exact. Null values should match empty strings
		Optional<String> ncustomerid = customerId.or(() -> Optional.of(""));
		Optional<String> napp = app.or(() -> Optional.of(""));
		Optional<String> ninstanceid = instanceId.or(() -> Optional.of(""));
        Optional<RecordOrReplay> cachedrr = retrieveFromCache(ckey);
		//Optional<RecordOrReplay> cachedrr = Optional.ofNullable(currentCollectionMap.get(ckey));
		String customerAppInstance = "Cust :: " + customerId.orElse("") + " App :: " + app.orElse("") +
            "Instance :: " + instanceId.orElse("");

		//LOGGER.info(String.format("Looking up collection for cust %s, app %s, instance %s", customerId.orElse(""), app.orElse(""), instanceId.orElse("")));
		return cachedrr.map(cachedRRVal -> {
		    LOGGER.info("Retrieved Record/Replay from Cache for " + customerAppInstance + " :: " + cachedRRVal.toString());
		    return cachedRRVal;
        }).or(() -> {
			// not cached, read from underlying store
            // check if there is a recording going on
			Optional<RecordOrReplay> rr = getRecording(ncustomerid, napp, ninstanceid, Optional.of(RecordingStatus.Running))
					.findFirst()
					.map(recording -> RecordOrReplay.createFromRecording(recording))
					.or(() -> { // no ongoing recording, check replay
						LOGGER.info("No running recording, looking up current replay");
						return getReplay(ncustomerid, napp, ninstanceid, ReplayStatus.Running)
								.findFirst()
								.map(replay -> RecordOrReplay.createFromReplay(replay));
					});
			//rr.ifPresent(rrv -> currentCollectionMap.put(ckey, rrv));
            rr.ifPresent(rrv -> populateCache(ckey, rrv));
            rr.ifPresentOrElse
                (rrVal ->
                LOGGER.info("Retrieved Record/Replay from Solr for " + customerAppInstance + " :: " + rrVal.toString())
                    , () -> LOGGER.info("No Record/Replay retrieved from Cache/Solr for " + customerAppInstance));
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
    abstract Optional<RecordOrReplay> retrieveFromCache(CollectionKey key);
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

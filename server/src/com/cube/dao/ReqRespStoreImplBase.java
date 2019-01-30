/**
 * Copyright Cube I O
 */
package com.cube.dao;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cube.dao.Recording.RecordingStatus;
import com.cube.drivers.Replay;
import com.cube.drivers.Replay.ReplayStatus;

/**
 * @author prasad
 *
 */
public abstract class ReqRespStoreImplBase implements ReqRespStore {

	private static final Logger LOGGER = LogManager.getLogger(ReqRespStoreImplBase.class);

	/* (non-Javadoc)
	 * @see com.cube.dao.ReqRespStore#getCurrentCollection(java.util.Optional, java.util.Optional, java.util.Optional)
	 * For a (cust, app, instance), there is one current collection. Either a recording is going on or a replay or nothing. This 
	 * looks up the state and caches it for quick retrieval
	 */
	@Override
	public Optional<String> getCurrentCollection(Optional<String> customerid, Optional<String> app,
			Optional<String> instanceid) {
		
		CollectionKey ckey = new CollectionKey(customerid.orElse(""), app.orElse(""), instanceid.orElse(""));

		// in this case matching has to be exact. Null values should match empty strings
		Optional<String> ncustomerid = customerid.or(() -> Optional.of("")); 
		Optional<String> napp = app.or(() -> Optional.of("")); 
		Optional<String> ninstanceid = instanceid.or(() -> Optional.of("")); 
		
		Optional<Optional<String>> collection = Optional.ofNullable(currentCollectionMap.get(ckey));
		LOGGER.info(String.format("Looking up collection for cust %s, app %s, instance %s", customerid.orElse(""), app.orElse(""), instanceid.orElse("")));
		return collection.orElseGet(() -> {
			// not cached, read from underlying store
			LOGGER.info("Not found in cache, looking up current recording");
			// check if there is a recording going on
			Optional<String> c = getRecording(ncustomerid, napp, ninstanceid, Optional.of(RecordingStatus.Running))
					.findFirst()
					.map(recording -> recording.collection)
					.or(() -> { // no ongoing recording, check replay
						LOGGER.info("No running recording, looking up current replay");
						// Note that replayid is the collection for replay requests/responses
						// replay.collection refers to the original collection
						return getReplay(ncustomerid, napp, ninstanceid, ReplayStatus.Running)
								.findFirst()
								.map(replay -> replay.replayid);
					});
			currentCollectionMap.put(ckey, c);
			return c;
		});
	}



	/* (non-Javadoc)
	 * @see com.cube.dao.ReqRespStore#invalidateCurrentCollectionCache()
	 */
	private void invalidateCurrentCollectionCache(String customerid, String app,
			String instanceid) {
		CollectionKey ckey = new CollectionKey(customerid, app, instanceid);

		currentCollectionMap.remove(ckey);
	}



	/* (non-Javadoc)
	 * @see com.cube.dao.ReqRespStore#saveReplay(com.cube.drivers.Replay)
	 */
	@Override
	public boolean saveReplay(Replay replay) {
		invalidateCurrentCollectionCache(replay.customerid, replay.app, replay.instanceid);
		return true;
	}



	/* (non-Javadoc)
	 * @see com.cube.dao.ReqRespStore#saveRecording(com.cube.dao.Recording)
	 */
	@Override
	public boolean saveRecording(Recording recording) {
		invalidateCurrentCollectionCache(recording.customerid, recording.app, recording.instanceid);
		return true;
	}



	static private class CollectionKey {
		
		/**
		 * @param customerid
		 * @param app
		 * @param instanceid
		 */
		private CollectionKey(String customerid, String app, String instanceid) {
			super();
			this.customerid = customerid;
			this.app = app;
			this.instanceid = instanceid;
		}
		
	

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((app == null) ? 0 : app.hashCode());
			result = prime * result + ((customerid == null) ? 0 : customerid.hashCode());
			result = prime * result + ((instanceid == null) ? 0 : instanceid.hashCode());
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
			if (customerid == null) {
				if (other.customerid != null) {
					return false;
				}
			} else if (!customerid.equals(other.customerid)) {
				return false;
			}
			if (instanceid == null) {
				if (other.instanceid != null) {
					return false;
				}
			} else if (!instanceid.equals(other.instanceid)) {
				return false;
			}
			return true;
		}

		final String customerid; 
		final String app;
		final String instanceid;
	}
	
	// map from (cust, app, instance) -> collection. collection is empty if there is no current recording or replay
	private ConcurrentHashMap<CollectionKey, Optional<String>> currentCollectionMap = new ConcurrentHashMap<>();

}

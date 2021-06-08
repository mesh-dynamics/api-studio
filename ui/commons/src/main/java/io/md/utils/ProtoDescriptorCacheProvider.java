package io.md.utils;

import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import io.md.cache.ProtoDescriptorCache;
import io.md.services.DataStore;

public class ProtoDescriptorCacheProvider {

	private static ProtoDescriptorCache singleInstance;

	public static Optional<ProtoDescriptorCache> getInstance() {
		return Optional.ofNullable(singleInstance);
	}

	public static void instantiateCache(DataStore dataStore) {
			singleInstance = new ProtoDescriptorCache(dataStore);

	}


}

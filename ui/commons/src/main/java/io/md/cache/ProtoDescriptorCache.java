package io.md.cache;

import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import io.md.dao.ProtoDescriptorDAO;
import io.md.logger.LogMgr;
import io.md.services.DataStore;
import io.md.utils.UtilException;
import io.md.utils.Utils;

public class ProtoDescriptorCache {
	private static final Logger LOGGER = LogMgr.getLogger(ProtoDescriptorCache.class);

	private final DataStore dataStore;

	private Cache<ProtoDescriptorKey, ProtoDescriptorDAO> loadingCache
		= CacheBuilder.newBuilder().maximumSize(100).build();

	public ProtoDescriptorCache(DataStore dataStore) {
		this.dataStore = dataStore;
	}

	public void invalidate(ProtoDescriptorKey protoDescriptorKey) {
		loadingCache.invalidate(protoDescriptorKey);
	}

	public void invalidateAll() {
		loadingCache.invalidateAll();
	}

	public static class ProtoDescriptorKey {
		public final String customer;
		public final String app;
		public String collection;

		public ProtoDescriptorKey(String customer, String app,
			String collection) {
			this.customer = customer;
			this.app = app;
			this.collection = collection;
		}

		public int hashCode() {
			return Objects
				.hash(this.customer, this.app, this.collection);
		}

		public boolean equals(Object o) {
			if (!(o instanceof ProtoDescriptorKey)) {
				return false;
			} else {
				ProtoDescriptorKey other = (ProtoDescriptorKey)o;
				return Objects.equals(this.customer, other.customer) &&
					Objects.equals(this.app, other.app) && Objects.equals(this.collection, other.collection);
			}
		}
	}

	public Optional<ProtoDescriptorDAO> get(ProtoDescriptorKey key) {
		try {
			return Optional.of(loadingCache.get(key , () ->
				dataStore.getLatestProtoDescriptorDAO(key.customer, key.app)
					.map(UtilException.rethrowFunction(protoDescriptorDAO -> {
						protoDescriptorDAO.initializeProtoDescriptor();
							return protoDescriptorDAO;
						}
					))
					.orElseThrow(() ->
					new Exception("Unable to find proto descriptor in data store for "
						+ key.customer + " : " + key.app))));
		} catch (Exception e) {
			LOGGER.error("Cannot fetch protoDescriptorDAO",e);
			return Optional.empty();

		}
	}


}

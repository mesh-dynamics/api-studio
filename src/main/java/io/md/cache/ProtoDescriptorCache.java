package io.md.cache;

import java.util.Objects;
import java.util.Optional;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import io.md.core.ProtoDescriptor;
import io.md.services.DataStore;

public class ProtoDescriptorCache {

	private final DataStore dataStore;

	private Cache<ProtoDescriptorKey, ProtoDescriptor> loadingCache
		= CacheBuilder.newBuilder().maximumSize(100).build();

	public ProtoDescriptorCache(DataStore dataStore) {
		this.dataStore = dataStore;
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

	public Optional<ProtoDescriptor> get(ProtoDescriptorKey key) {
		try {
			return Optional.of(loadingCache.get(key , () ->
				dataStore.getProtoDescriptor(key.customer, key.app).orElseThrow(() ->
					new Exception("Unable to find proto descritor in data store for "
						+ key.customer + " : " + key.app))));
		} catch (Exception e) {
			return Optional.empty();
		}
	}


}

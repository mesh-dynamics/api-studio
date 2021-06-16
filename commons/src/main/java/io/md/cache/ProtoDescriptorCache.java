/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

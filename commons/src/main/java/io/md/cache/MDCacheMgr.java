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

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

import io.md.logger.LogMgr;

public class MDCacheMgr {

	private static final Logger LOGGER = LogMgr.getLogger(MDCacheMgr.class);
	private static HashMap<String , MDCache> caches = new HashMap<>();
	public static void register(MDCache cache){

		String cachename = cache.getName();
		LOGGER.info("Registering cache "+cachename);
		if(caches.containsKey(cache.getName())){
			LOGGER.error("Cache registered again. "+cachename);
			throw new IllegalArgumentException("Cache registered again. "+cachename);
		}
		caches.put(cachename, cache);
	}

	public static Object handlePubSub(Map<String,?> meta) throws Exception{

		LOGGER.info("handlePubSub "+meta.toString());
		String cacheName =  meta.containsKey(Constants.CACHE_NAME) ? meta.get(Constants.CACHE_NAME).toString() : null;
		if(cacheName == null){
			LOGGER.info("clearing all caches");
			long total = 0;
			for (MDCache cache : caches.values()){
				total+= cache.clean(meta);
			}
			LOGGER.debug("Total cleared keys "+total);
			return total;
		}
		//removing the cacheName key. meta will be empty if that cache needs to fully cleaned
		meta.remove(Constants.CACHE_NAME);


		MDCache cache = caches.get(cacheName);
		if(cache==null){
			throw new Exception("No Cache found with name "+cacheName);
		}

		long total = cache.clean(meta);
		LOGGER.debug("Total cleared keys "+total);
		return total;

	}
}

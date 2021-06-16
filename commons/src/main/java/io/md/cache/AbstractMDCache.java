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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

import io.md.logger.LogMgr;

public abstract  class AbstractMDCache implements MDCache {

	private static final Logger LOGGER = LogMgr.getLogger(AbstractMDCache.class);
	public AbstractMDCache(){
		MDCacheMgr.register(this);
	}
	// Match all keys Constant
	private static final String ALL_KEYS_REGEX = "*";

	/*
		Provide all the pairs of cache and keys
		Keys are ordered String const values , which we look in meta and construct a actual/regex cache key
	 */
	public abstract List<Pair<PassiveExpiringMap<String , ?> , String[]>> getCacheAndKeys();
	/*
		return the joining string by which all keys are separated
	 */
	public String getJoinBy(){
		return "-";
	};

	/*
		Generate the (key) regex based on meta data passed , which will be used to remove the desired keys from the cache
	 */
	public String getKeyRegex(Map<String,?> keyMeta , String...keys){

		if(keyMeta.isEmpty()) return ALL_KEYS_REGEX;

		String joinBy = getJoinBy();

		boolean[] anyKeyPresent = new boolean[]{false};
		String keysRegex = Arrays.stream(keys).map(key-> {
			if(keyMeta.containsKey(key)) {
				//use this specific value
				anyKeyPresent[0] = true;
				return keyMeta.get(key).toString();
			}
			return "[^"+joinBy+"]+";  //this meta const value can be anything (all)
		}).collect(Collectors.joining(joinBy));

		if(!anyKeyPresent[0]){
			LOGGER.warn("Did not find any cache key in meta "+getName());
			return ALL_KEYS_REGEX;
		}
		return "^" + keysRegex + "$";
	}

	/*
		Remove all the keys from cache which matches given regex
	 */
	public Long cleanRegexKeys(PassiveExpiringMap<String , ?> cache , String keyRegex){

		int preSize = cache.size();
		if(keyRegex.equals(ALL_KEYS_REGEX)){
			LOGGER.info("Clearing all cache for " + getName());
			cache.clear();
		}else{
			Pattern keyPattern = Pattern.compile(keyRegex);
			cache.entrySet().removeIf(e->keyPattern.matcher(e.getKey()).matches());
		}
		return (long)(preSize - cache.size());
	}

	@Override
	/*
		Clean the cache with given meta data passed.
		1. generate the key regex based on the meta
		2. remove the regex matching keys from cache
	 */
	public long clean(Map<String,?> keyMeta) {

		return getCacheAndKeys().stream().reduce(0L , (sum , pair)->{
			String keyRegex = getKeyRegex(keyMeta , pair.getRight());
			LOGGER.info("clearing all keys with regex "+keyRegex + " "+getName());
			return cleanRegexKeys(pair.getKey(), keyRegex);
		} , Long::sum);
	}

	public String createFieldsKey(String... fields){
		return String.join(getJoinBy() , fields);
	}

}

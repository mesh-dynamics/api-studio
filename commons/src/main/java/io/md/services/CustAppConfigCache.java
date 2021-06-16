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

package io.md.services;

import static io.md.cache.Constants.APP;
import static io.md.cache.Constants.CUSTOMER_ID;
import static io.md.cache.Constants.CACHE_TO_LIVE_DUR;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

import io.md.cache.AbstractMDCache;
import io.md.cache.Constants;
import io.md.dao.CustomerAppConfig;
import io.md.logger.LogMgr;

public class CustAppConfigCache extends AbstractMDCache {
	private static final Logger LOGGER = LogMgr.getLogger(CustAppConfigCache.class);
	private final DataStore dStore;
	private static CustAppConfigCache singleton;

	private static PassiveExpiringMap<String , Optional<CustomerAppConfig>> custIdAppConfigsMap = new PassiveExpiringMap<>(
		CACHE_TO_LIVE_DUR.toMillis());
	private List<Pair<PassiveExpiringMap<String , ?> , String[]>> cacheAndKeys = new ArrayList<>(1);
	{
		cacheAndKeys.add(Pair.of(custIdAppConfigsMap , new String[]{CUSTOMER_ID , APP}));
	}

	private CustAppConfigCache(DataStore dataStore){

		this.dStore = dataStore;
	}

	public static CustAppConfigCache getInstance(DataStore dataStore){

		if(singleton!=null) return singleton;
		synchronized (CustAppConfigCache.class){
			if(singleton==null){
				singleton = new CustAppConfigCache(dataStore);
			}
		}
		return singleton;
	}

	public Optional<CustomerAppConfig> getCustomerAppConfig(String customerId , String app){

		final String custAppKey = customerId.concat("-").concat(app);

		Optional <CustomerAppConfig> cfg = custIdAppConfigsMap.get(custAppKey);
		if(cfg==null){
			synchronized (this){
				if(cfg==null) {
					cfg = dStore.getAppConfiguration(customerId , app);
					custIdAppConfigsMap.put(custAppKey , cfg);
				}
			}
		}
		return cfg;
	}


	@Override
	public String getName() {
		return Constants.CUSTOMER_APP_CONFIG;
	}

	@Override
	public List<Pair<PassiveExpiringMap<String, ?>, String[]>> getCacheAndKeys() {
		return cacheAndKeys;
	}
}

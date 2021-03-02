package io.md.services;

import static io.md.cache.Constants.APP;
import static io.md.cache.Constants.CUSTOMER_ID;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

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

	private static PassiveExpiringMap<String , Optional<CustomerAppConfig>> custIdAppConfigsMap = new PassiveExpiringMap<>(30 , TimeUnit.MINUTES);
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

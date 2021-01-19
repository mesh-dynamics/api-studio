package io.md.services;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.slf4j.Logger;

import io.md.dao.CustomerAppConfig;
import io.md.logger.LogMgr;

public class CustAppConfigCache {
	private static final Logger LOGGER = LogMgr.getLogger(CustAppConfigCache.class);
	private final DataStore dStore;
	private static CustAppConfigCache singleton;

	private static PassiveExpiringMap<String , Optional<CustomerAppConfig>> custIdAppConfigsMap = new PassiveExpiringMap<>(30 , TimeUnit.MINUTES);

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


}

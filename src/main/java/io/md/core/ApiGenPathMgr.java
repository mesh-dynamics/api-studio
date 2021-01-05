package io.md.core;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.slf4j.Logger;

import io.md.dao.CustomerAppConfig;
import io.md.logger.LogMgr;
import io.md.services.CustAppConfigCache;
import io.md.services.DataStore;

public class ApiGenPathMgr {

	private static class ApiPathRegex{

		private final String regex;
		private final Pattern pattern ;

		//Todo : validation
		ApiPathRegex(String name){
			this.regex = name ;
			this.pattern = getPattern(name);
		}

		private Pattern getPattern(String regex){

			// should not start or end with /
			return Pattern.compile(regex);
		}

		private void validate(String regex) throws Exception{
			//todo
		}
	}

	private static final Logger LOGGER = LogMgr.getLogger(ApiGenPathMgr.class);
	private final DataStore dStore;
	private final CustAppConfigCache appConfigCache;
	private static ApiGenPathMgr singleton;

	private static PassiveExpiringMap<String , Optional<ApiPathRegex[]> > serviceApiGenPaths = new PassiveExpiringMap<>(30 , TimeUnit.MINUTES);

	private ApiGenPathMgr(DataStore dataStore){
		this.dStore = dataStore;
		this.appConfigCache = CustAppConfigCache.getInstance(dataStore);
	}

	public static ApiGenPathMgr getInstance(DataStore dataStore){

		if(singleton!=null) return singleton;
		synchronized (CustAppConfigCache.class){
			if(singleton==null){
				singleton = new ApiGenPathMgr(dataStore);
			}
		}
		return singleton;
	}

	public String getGenericPath(String customerId , String app , String service , String apiPath){

		String key = String.format("%s-%s-%s" , customerId , app , service);

		// nahi mila in local cache
		Optional<CustomerAppConfig> appCfg =   appConfigCache.getCustomerAppConfig(customerId, app);
		//todo
		return "";
	}



}

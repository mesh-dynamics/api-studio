package io.md.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

import io.md.cache.AbstractMDCache;
import io.md.cache.Constants;
import io.md.dao.Event;
import io.md.logger.LogMgr;
import io.md.services.CustAppConfigCache;
import io.md.services.DataStore;
import io.md.utils.ApiPathRegex;

import static io.md.cache.Constants.*;

public class ApiGenPathMgr extends AbstractMDCache {

	private static final Logger LOGGER = LogMgr.getLogger(ApiGenPathMgr.class);
	private final DataStore dStore;
	private final CustAppConfigCache appConfigCache;
	private static ApiGenPathMgr singleton;

	private static PassiveExpiringMap<String , Optional<ApiPathRegex[]> > serviceApiPathPatterns = new PassiveExpiringMap<>(
		CACHE_TO_LIVE_DUR.toMillis());
	private static PassiveExpiringMap<String , Optional<String>> serviceApiGenPaths = new PassiveExpiringMap<>(
		CACHE_TO_LIVE_DUR.toMillis());
	private List<Pair<PassiveExpiringMap<String , ?> , String[]>> cacheAndKeys = new ArrayList<>(2);
	{
		cacheAndKeys.add(Pair.of(serviceApiPathPatterns , new String[]{CUSTOMER_ID , APP , SERVICE}));
		cacheAndKeys.add(Pair.of(serviceApiGenPaths , new String[]{CUSTOMER_ID , APP , SERVICE , API_PATH}));
	}

	private ApiGenPathMgr(DataStore dataStore){
		this.dStore = dataStore;
		this.appConfigCache = CustAppConfigCache.getInstance(dataStore);
	}

	public static ApiGenPathMgr getInstance(DataStore dataStore){

		if(singleton!=null) return singleton;
		synchronized (ApiGenPathMgr.class){
			if(singleton==null){
				singleton = new ApiGenPathMgr(dataStore);
			}
		}
		return singleton;
	}

	public Optional<String> getGenericPath(Event event){
		return getGenericPath(event.customerId , event.app, event.service, event.apiPath);
	}

	public Optional<String> getGenericPath(String customerId, String app, String service, String apiPath) {

		String apiPathKey = String.format("%s-%s-%s-%s", customerId, app, service, apiPath);
		Optional<String> genPath = serviceApiGenPaths.get(apiPathKey);
		if (genPath != null) {
			return genPath;
		}
		genPath =  generateGenericPath(customerId, app, service, apiPath);
		synchronized (this){
			serviceApiGenPaths.put(apiPathKey , genPath);
		}
		return genPath;
	}

	private Optional<String> generateGenericPath(String customerId, String app, String service, String apiPath){
		String key = String.format("%s-%s-%s", customerId, app, service);
		Optional<ApiPathRegex[]> apiPathRegexes = serviceApiPathPatterns.get(key);
		if(apiPathRegexes==null){
			synchronized (this){
				apiPathRegexes = serviceApiPathPatterns.get(key);
				if(apiPathRegexes==null){
					Optional<Map<String, String[]>> serviceMap = appConfigCache
						.getCustomerAppConfig(customerId, app).flatMap(cfg -> cfg.apiGenericPaths);
					apiPathRegexes = serviceMap.map(m -> m.get(service)).map(paths -> Arrays.stream(paths).map(ApiPathRegex::new).toArray(ApiPathRegex[]::new));
					serviceApiPathPatterns.put(key , apiPathRegexes);
				}
			}

		}

		if (!apiPathRegexes.isPresent()) {
			return Optional.empty();
		}

		for (ApiPathRegex pathRegex : apiPathRegexes.get()) {
			if (pathRegex.matches (apiPath)) {
				return Optional.of(pathRegex.pathRegex);
			}
		}
		return Optional.empty();
	}

	@Override
	public String getName() {
		return Constants.API_GEN_PATH;
	}

	@Override
	public List<Pair<PassiveExpiringMap<String , ?> , String[]>> getCacheAndKeys() {
		return cacheAndKeys;
	}

	@Override
	public long clean(Map<String,?> keyMeta) {
		// It gets the values from appConfigCache so necessary to clean that cache as well.
		return super.clean(keyMeta) + this.appConfigCache.clean(keyMeta);
	}


	//
	//public static void main(String[] args){

		//ApiPathRegex pathRegex = new ApiPathRegex("*/*/*/*/rrrr/dd");
		//System.out.println(pathRegex.matches("gk/*/pk/ss/rrrr/dd"));
	//}




}

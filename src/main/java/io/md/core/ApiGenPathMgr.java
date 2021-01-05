package io.md.core;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.slf4j.Logger;

import io.md.logger.LogMgr;
import io.md.services.CustAppConfigCache;
import io.md.services.DataStore;

public class ApiGenPathMgr {

	private static class ApiPathRegex{

		public final String pathRegex;
		public final Pattern pattern ;

		//Todo : validation
		ApiPathRegex(String name){
			this.pathRegex = name ;
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

	private static PassiveExpiringMap<String , Optional<ApiPathRegex[]> > serviceApiPathPatterns = new PassiveExpiringMap<>(30 , TimeUnit.MINUTES);
	private static PassiveExpiringMap<String , String> serviceApiGenPaths = new PassiveExpiringMap<>(30 , TimeUnit.MINUTES);

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

	public String getGenericPath(String customerId, String app, String service, String apiPath) {

		String apiPathKey = String.format("%s-%s-%s-%s", customerId, app, service, apiPath);
		String genPath = serviceApiGenPaths.get(apiPathKey);
		if (genPath != null) {
			return genPath;
		}
		genPath = generateGenericPath(customerId, app, service, apiPath);
		serviceApiGenPaths.put(apiPathKey , genPath);
		return genPath;
	}

	private String generateGenericPath(String customerId, String app, String service, String apiPath){
		String key = String.format("%s-%s-%s", customerId, app, service);
		Optional<ApiPathRegex[]> apiPathRegexes = serviceApiPathPatterns.get(key);
		if(apiPathRegexes==null){
			Optional<Map<String, String[]>> serviceMap = appConfigCache
				.getCustomerAppConfig(customerId, app).flatMap(cfg -> cfg.apiGenericPaths);
			apiPathRegexes = serviceMap.map(m -> m.get(service)).map(paths -> Arrays.stream(paths).map(ApiPathRegex::new).toArray(ApiPathRegex[]::new));
			serviceApiPathPatterns.put(key , apiPathRegexes);
		}

		if (!apiPathRegexes.isPresent()) {
			return apiPath;
		}

		for (ApiPathRegex pathRegex : apiPathRegexes.get()) {
			if (pathRegex.pattern.matcher(apiPath).matches()) {
				return pathRegex.pathRegex;
			}
		}
		return apiPath;
	}



}

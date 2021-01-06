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

		public  boolean matches(String apiPath){
			return pattern.matcher(apiPath).matches();
		}

		private Pattern getPattern(String apiPathRegex){

			String[] paths = apiPathRegex.split("/");
			int len = paths.length;
			if(len<=1 || apiPathRegex.indexOf('*')==-1) throw new IllegalArgumentException("Not a valid apiPathRegex "+apiPathRegex);
			for(String path : paths){
				if(path.length()>1 && path.indexOf('*')!=-1) throw new IllegalArgumentException("Not a valid apiPathRegex "+apiPathRegex);
			}

			//start
			if(paths[0].equals("*")){

				paths[0] = "^[^/]+"; //start with any characters except '/'
			}else{
				paths[0] = "^"+paths[0];
			}

			//end
			if(paths[len-1].equals("*")){
				paths[len-1] = "[^/]+$";   //ends with any characters except '/'
			}else{
				paths[len-1] = paths[len-1] + "$";
			}
			//middle
			for(int i=1 ; i<len-1 ; i++){
				if(paths[i].equals("*")){
					paths[i] = "[^/]+";
				}
			}
			return Pattern.compile(String.join("/" , paths));
		}


		private void validate(String regex) throws Exception{
			// should not start or end with /
			//todo
			// case /
			// case *
		}
	}

	private static final Logger LOGGER = LogMgr.getLogger(ApiGenPathMgr.class);
	private final DataStore dStore;
	private final CustAppConfigCache appConfigCache;
	private static ApiGenPathMgr singleton;

	private static PassiveExpiringMap<String , Optional<ApiPathRegex[]> > serviceApiPathPatterns = new PassiveExpiringMap<>(30 , TimeUnit.MINUTES);
	private static PassiveExpiringMap<String , Optional<String>> serviceApiGenPaths = new PassiveExpiringMap<>(30 , TimeUnit.MINUTES);

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

	public Optional<String> getGenericPath(String customerId, String app, String service, String apiPath) {

		String apiPathKey = String.format("%s-%s-%s-%s", customerId, app, service, apiPath);
		Optional<String> genPath = serviceApiGenPaths.get(apiPathKey);
		if (genPath != null) {
			return genPath;
		}
		genPath = generateGenericPath(customerId, app, service, apiPath);
		serviceApiGenPaths.put(apiPathKey , genPath);
		return genPath;
	}

	private Optional<String> generateGenericPath(String customerId, String app, String service, String apiPath){
		String key = String.format("%s-%s-%s", customerId, app, service);
		Optional<ApiPathRegex[]> apiPathRegexes = serviceApiPathPatterns.get(key);
		if(apiPathRegexes==null){
			Optional<Map<String, String[]>> serviceMap = appConfigCache
				.getCustomerAppConfig(customerId, app).flatMap(cfg -> cfg.apiGenericPaths);
			apiPathRegexes = serviceMap.map(m -> m.get(service)).map(paths -> Arrays.stream(paths).map(ApiPathRegex::new).toArray(ApiPathRegex[]::new));
			serviceApiPathPatterns.put(key , apiPathRegexes);
		}

		if (!apiPathRegexes.isPresent()) {
			return Optional.empty();
		}

		for (ApiPathRegex pathRegex : apiPathRegexes.get()) {
			if (pathRegex.pattern.matcher(apiPath).matches()) {
				return Optional.of(pathRegex.pathRegex);
			}
		}
		return Optional.empty();
	}

	public static void main(String[] args){

		ApiPathRegex pathRegex = new ApiPathRegex("*/*/*/*/rrrr/dd");
		System.out.println(pathRegex.matches("gk/*/pk/ss/rrrr/dd"));
	}



}

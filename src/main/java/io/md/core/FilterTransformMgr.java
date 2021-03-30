package io.md.core;

import static io.md.cache.Constants.API_PATH;
import static io.md.cache.Constants.APP;
import static io.md.cache.Constants.CACHE_TO_LIVE_DUR;
import static io.md.cache.Constants.CUSTOMER_ID;
import static io.md.cache.Constants.SERVICE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

import io.md.cache.AbstractMDCache;
import io.md.cache.Constants;
import io.md.dao.Event;
import io.md.dao.Event.EventType;
import io.md.dao.FilterTransform;
import io.md.dao.FilterTransform.Transform;
import io.md.logger.LogMgr;
import io.md.services.CustAppConfigCache;
import io.md.services.DataStore;

public class FilterTransformMgr extends AbstractMDCache {

	private static final Logger LOGGER = LogMgr.getLogger(FilterTransformMgr.class);
	private final DataStore dStore;
	private final CustAppConfigCache appConfigCache;
	private static FilterTransformMgr singleton;

	private static final PassiveExpiringMap<String , List<FilterTransform>> serviceApiExactPaths = new PassiveExpiringMap<>(
		CACHE_TO_LIVE_DUR.toMillis());
	private static final PassiveExpiringMap<String , List<FilterTransform>> serviceApiGenRegexPaths = new PassiveExpiringMap<>(
		CACHE_TO_LIVE_DUR.toMillis());
	private static final Map<String , Pattern> compiledPatterns = new HashMap();
	private static final Pattern regexPattern = Pattern.compile("[^a-z0-9 -/]", Pattern.CASE_INSENSITIVE);

	private static Pattern getCompiledPattern(String regex){
		Pattern pattern = compiledPatterns.get(regex);
		if(pattern==null){
			pattern = Pattern.compile(regex);
			compiledPatterns.put(regex , pattern);
		}
		return pattern;
	}

	private List<Pair<PassiveExpiringMap<String , ?> , String[]>> cacheAndKeys = new ArrayList<>(2);
	{
		cacheAndKeys.add(Pair.of(serviceApiExactPaths , new String[]{CUSTOMER_ID , APP , SERVICE , API_PATH}));
		cacheAndKeys.add(Pair.of(serviceApiGenRegexPaths , new String[]{CUSTOMER_ID , APP}));
	}

	private FilterTransformMgr(DataStore dataStore){
		this.dStore = dataStore;
		this.appConfigCache = CustAppConfigCache.getInstance(dataStore);
	}

	public static FilterTransformMgr getInstance(DataStore dataStore){

		if(singleton!=null) return singleton;
		synchronized (ApiGenPathMgr.class){
			if(singleton==null){
				singleton = new FilterTransformMgr(dataStore);
			}
		}
		return singleton;
	}


	@Override
	public List<Pair<PassiveExpiringMap<String, ?>, String[]>> getCacheAndKeys() {
		return cacheAndKeys;
	}

	@Override
	public String getName() {
		return Constants.FILTER_TRANSFORM;
	}
	@Override
	public long clean(Map<String,?> keyMeta) {
		// It gets the values from appConfigCache so necessary to clean that cache as well.
		return super.clean(keyMeta) + this.appConfigCache.clean(keyMeta);
	}

	public void filterTransform(Event e){

		if(e.eventType != EventType.HTTPResponse) return;

		String custAppKey = createFieldsKey(e.customerId , e.app);
		List<FilterTransform> regexTransforms = serviceApiGenRegexPaths.get(custAppKey);

		if(regexTransforms == null){
			// not present in the cache
			regexTransforms = populateCache(e.customerId , e.app);
		}
		String custAppServiceApiPathKey = createFieldsKey(e.customerId , e.app , e.service , e.apiPath);
		List<FilterTransform> exactPathFilters = serviceApiExactPaths.get(custAppServiceApiPathKey);

		if (exactPathFilters == null) {
			// check in regex path
			//Put Optional.empty() in cache to direct result in next fetch
			//None found that service - apiPath
			exactPathFilters = regexTransforms.stream().filter(ft->{
				Pattern servicePattern = getCompiledPattern(ft.filter.getService());
				Pattern apiPathPattern = getCompiledPattern(ft.filter.getApiPath());
				return servicePattern.matcher(e.service).matches() && apiPathPattern.matcher(e.apiPath).matches();
			}).collect(Collectors.toList());
			serviceApiExactPaths.put(custAppServiceApiPathKey,exactPathFilters);
		}
		if (exactPathFilters.isEmpty()) {
			return;
		}

		exactPathFilters.stream().filter(eft -> eft.filter.filter(e)).forEach(eft -> {
			Transform t = eft.transform;
			LOGGER.debug("Matched filter " + eft.filter + " Transform " + t + " for event " + e.reqId);
			t.transformPayload(e.payload);
		});


	}

	private synchronized  List<FilterTransform> populateCache(String customerId , String app){
		List<FilterTransform> filterTransforms = appConfigCache.getCustomerAppConfig(customerId , app).map(c->c.filterTransform).orElse(Collections.EMPTY_LIST);

		List<FilterTransform> regexFilters = new ArrayList<>();
		for(FilterTransform ft : filterTransforms){
			String service = ft.filter.getService();
			String apiPath = ft.filter.getApiPath();

			if(regexPattern.matcher(service).find() || regexPattern.matcher(apiPath).find() ){
				//Regex filters
				regexFilters.add(ft);
			}else{
				String custAppServiceApiPathKey = createFieldsKey(customerId , app , service , apiPath);
				List<FilterTransform> filters = Optional.ofNullable(serviceApiExactPaths.get(custAppServiceApiPathKey)).orElse(new ArrayList<>());
				filters.add(ft);
				serviceApiExactPaths.put(custAppServiceApiPathKey ,filters);
			}
		}
		serviceApiGenRegexPaths.put(createFieldsKey(customerId , app) , regexFilters);
		return regexFilters;
	}
}

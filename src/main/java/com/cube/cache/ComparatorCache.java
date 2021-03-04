/*
 *
 *    Copyright Cube I O
 *
 */

package com.cube.cache;

import static io.md.constants.Constants.FN_RESPONSE_PATH;

import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import io.md.cache.Constants.PubSubContext;
import io.md.cache.MDCache;
import io.md.cache.MDCacheMgr;
import io.md.core.Comparator;
import io.md.core.CompareTemplate;
import io.md.core.CompareTemplate.ComparisonType;
import io.md.core.CompareTemplate.DataType;
import io.md.core.CompareTemplate.PresenceType;
import io.md.core.TemplateEntry;
import io.md.core.TemplateKey;
import io.md.core.TemplateKey.Type;
import io.md.dao.Event.EventType;
import io.md.services.DataStore.TemplateNotFoundException;
import io.md.utils.UtilException;
import io.md.core.Utils;
import io.md.utils.Constants;

import com.cube.core.JsonComparator;
import com.cube.dao.ReqRespStore;
import com.cube.exception.CacheException;
import com.cube.ws.Config;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-11-14
 */
public class ComparatorCache implements MDCache {

    private static final Logger LOGGER = LogManager.getLogger(ComparatorCache.class);

    private final TemplateCache templateCache;
    private final ObjectMapper jsonMapper;
    private final ReqRespStore rrStore;
    private final Config config;
    private final Cache<TemplateKey, Comparator> comparatorCache;
    private static ComparatorCache singleton;
    public static ComparatorCache getInstance(TemplateCache cache, ObjectMapper jsonMapper, ReqRespStore rrStore , Config config){
        if(singleton!=null) return singleton;
        synchronized (ComparatorCache.class){
            if(singleton==null){
                singleton = new ComparatorCache(cache , jsonMapper , rrStore , config);
            }
        }
        return singleton;
    }
    private ComparatorCache(TemplateCache cache, ObjectMapper jsonMapper, ReqRespStore rrStore , Config config) {
        this.templateCache = cache;
        this.jsonMapper = jsonMapper;
        this.rrStore = rrStore;
        this.config = config;

        // we cache the comparators to avoid parsing the template json every time
        this.comparatorCache = CacheBuilder.newBuilder().maximumSize(100).build();


        // default rules for HTTP Request
        CompareTemplate defaultHTTPRequestMatchTemplate = new CompareTemplate();
        defaultHTTPRequestMatchTemplate.addRule(new TemplateEntry(Constants.QUERY_PARAMS_PATH, DataType.Obj,
            PresenceType.Optional, ComparisonType.Equal));
        defaultHTTPRequestMatchTemplate.addRule(new TemplateEntry(Constants.FORM_PARAMS_PATH, DataType.Obj,
            PresenceType.Optional, ComparisonType.Equal));
        defaultHTTPRequestMatchTemplate.addRule(new TemplateEntry(Constants.BODY_PATH, DataType.Default,
            PresenceType.Optional, ComparisonType.Equal));
        defaultHTTPRequestMatchTemplate.addRule(new TemplateEntry(Constants.METHOD_PATH, DataType.Str, PresenceType.Required,
            ComparisonType.Equal));
        defaultHTTPRequestMatchComparator = new JsonComparator(defaultHTTPRequestMatchTemplate, jsonMapper);

        // default rules for HTTP Response
        CompareTemplate defaultHTTPResponseTemplate = new CompareTemplate();
        defaultHTTPResponseTemplate.addRule(new TemplateEntry(Constants.ROOT_PATH, DataType.Default,
            PresenceType.Required, ComparisonType.Equal));
        defaultHTTPResponseTemplate.addRule(new TemplateEntry(Constants.HDR_PATH, DataType.Default,
            PresenceType.Optional, ComparisonType.Ignore));
        defaultHTTPResponseTemplate.addRule(new TemplateEntry("/"+io.md.constants.Constants.PAYLOAD_FIELDS_FIELD, DataType.Default,
            PresenceType.Optional, ComparisonType.Ignore));

        defaultHTTPResponseComparator = new JsonComparator(defaultHTTPResponseTemplate, jsonMapper);

        // default rules for Java Request
        CompareTemplate defaultJavaRequestTemplate = new CompareTemplate();
        defaultJavaRequestTemplate.addRule(new TemplateEntry(Constants.ARGS_PATH, DataType.NrptArray,
            PresenceType.Required, ComparisonType.Equal));
        defaultJavaRequestComparator = new JsonComparator(defaultJavaRequestTemplate, jsonMapper);

        // default rule for Java Response
        CompareTemplate defaultJavaResponseTemplate = new CompareTemplate();
        defaultJavaResponseTemplate.addRule(new TemplateEntry(FN_RESPONSE_PATH, DataType.Obj,
            PresenceType.Required, ComparisonType.Equal));
        defaultJavaResponseComparator = new JsonComparator(defaultJavaResponseTemplate, jsonMapper);

        // default rule for Thrift Request Payload (serialized using gson)
        CompareTemplate defaultThriftRequestTemplate = new CompareTemplate();
        defaultThriftRequestTemplate.addRule(
            new TemplateEntry(Constants.ROOT_PATH, DataType.Obj, PresenceType.Required, ComparisonType.Equal));
        defaultThriftRequestComparator = new JsonComparator(defaultThriftRequestTemplate,
            jsonMapper);

        // default rule for Thrift Response Payload (serialized using gson)
        CompareTemplate defaultThriftResponseTemplate = new CompareTemplate();
        defaultThriftResponseTemplate.addRule(
            new TemplateEntry(Constants.ROOT_PATH, DataType.Obj, PresenceType.Required, ComparisonType.Equal));
        defaultThriftResponseComparator = new JsonComparator(defaultThriftResponseTemplate,
            jsonMapper);

        MDCacheMgr.register(this);
    }

    /**
     * eventType is empty during template rule update and get Existing Rule Api
     * Will return defaults only for Response Compare Template,
     * otherwise return whatever is find in cache/solr
     * @param key
     * @return
     * @throws TemplateNotFoundException
     */
    public Comparator getComparator(TemplateKey key, Optional<EventType> eventType) throws
        TemplateNotFoundException {
        return getComparator(key, eventType, true);
    }

    private Comparator getComparator(TemplateKey key, Optional<EventType> eventTypeOpt, boolean sendDefault) throws TemplateNotFoundException {
        try {
            return comparatorCache.get(key, () -> {
                // fetch default event type if not passed
                EventType eventType =
                    eventTypeOpt.orElseGet(UtilException.rethrowSupplier(() -> getDefaultEventType(key)));
                try {
                    Comparator toReturn = createComparator(key, eventType);
                    LOGGER.info(new ObjectMessage(Map.of(
                        Constants.MESSAGE, "Successfully loaded into cache request comparator",
                        "key", key
                    )));
                    return toReturn;
                } catch (Exception e) {
                    if (!sendDefault) {
                        throw new TemplateNotFoundException();
                    }
                    LOGGER.info(new ObjectMessage(Map.of(
                        Constants.MESSAGE, "Unable to find template in cache, using default",
                        "key", key,
                        Constants.REASON, e.getMessage())));
                    Comparator defaultComparator = getDefaultComparator(eventType, key.getReqOrResp());
                    return createCopyWithAttributeRules(defaultComparator, key);
                }
            });
        } catch (Throwable e) {
            LOGGER.error(new ObjectMessage(Map.of(
                Constants.MESSAGE, "Unhandled exception occurred (re-throwing)"
            )),e);
            throw new TemplateNotFoundException();
        }
    }

    public Comparator getDefaultComparator(EventType eventType, TemplateKey.Type templateKeyType)  throws
        TemplateNotFoundException {
        switch (eventType) {
            case HTTPRequest:
                if(templateKeyType == Type.RequestMatch) {
                    return defaultHTTPRequestMatchComparator;
                } else {
                    return JsonComparator.EMPTY_COMPARATOR;
                }
            case HTTPResponse:
                return defaultHTTPResponseComparator;
            case JavaRequest:
                return defaultJavaRequestComparator;
            case JavaResponse:
                return defaultJavaResponseComparator;
            case ThriftRequest:
                return defaultThriftRequestComparator;
            case ThriftResponse:
                return defaultThriftResponseComparator;
            default:
                LOGGER.error(new ObjectMessage(Map.of(
                    "message", "No default template found",
                    "templateKeyType", templateKeyType
                )));
                throw new TemplateNotFoundException();
        }
    }


    public Comparator createCopyWithAttributeRules(Comparator defaultExisting, TemplateKey key) {
        CompareTemplate fromDefault = new CompareTemplate();
        fromDefault.setRules(defaultExisting.getCompareTemplate().getRules());
        rrStore.getAttributeRuleMap(key)
            .ifPresent(fromDefault::setAppLevelAttributeRuleMap);
        return new JsonComparator(fromDefault , jsonMapper);
    }


    public Comparator createComparator(TemplateKey key, EventType eventType) throws
        ComparatorNotImplementedException, CacheException {
        CompareTemplate compareTemplate = null;

        compareTemplate = templateCache.fetchCompareTemplate(key);

        if (key.getReqOrResp().equals(Type.ResponseCompare) || key.getReqOrResp().equals(Type.RequestCompare)
            || key.getReqOrResp().equals(Type.DontCare)) {
            rrStore.getAttributeRuleMap(key).ifPresent(compareTemplate::setAppLevelAttributeRuleMap);
        }

        switch (eventType) {
            case HTTPRequest:
            case HTTPResponse:
            case JavaRequest:
            case JavaResponse:
            case ThriftRequest:
            case ThriftResponse:
                return new JsonComparator(compareTemplate, jsonMapper);
            default:
                throw new ComparatorNotImplementedException(eventType);
        }
    }

    public void publishInvalidateKey(TemplateKey key){
        //Template Cache is redis based cache. should be done only at 1 instance.
        templateCache.invalidateKey(key);
        //comparatorCache is inMemory Cache and all the cubeio instances needs to cleanup
        config.pubSubMgr.publish(PubSubContext.IN_MEM_CACHE , Map.of(io.md.cache.Constants.CACHE_NAME , CACHE_NAME ,  Constants.TEMPLATE_KEY_FIELD , key));
    }

    public void publishInvalidateAll(){
        //Template Cache is redis based cache. should be done only at 1 instance.
        templateCache.invalidateAll();
        //comparatorCache is inMemory Cache and all the cubeio instances needs to cleanup
        config.pubSubMgr.publish(PubSubContext.IN_MEM_CACHE , Map.of(io.md.cache.Constants.CACHE_NAME , CACHE_NAME));
    }

    private void invalidateKey(TemplateKey key) {
        comparatorCache.invalidate(key);

    }

    private void invalidateAll() {
        comparatorCache.invalidateAll();
    }

    private EventType getDefaultEventType(TemplateKey key) throws TemplateNotFoundException {
        // this will always return request type ... will have to be converted
        // to Request / Response based on the key type
        EventType defaultEventType = Utils.valueOf(EventType.class,
            rrStore.getDefaultEventType(key.getCustomerId()
                , key.getAppId(), key.getServiceId(), key.getPath()).orElseThrow(
                TemplateNotFoundException::new)).orElseThrow(TemplateNotFoundException::new);
        return EventType.mapType(defaultEventType
            , key.isResponseTemplate());
    }

    @Override
    public String getName() {
        return CACHE_NAME;
    }

    @Override
    public long clean(Map<String, ?> meta) {
        LOGGER.debug(meta);
        if(meta == null || meta.isEmpty() || !meta.containsKey(Constants.TEMPLATE_KEY_FIELD)){
            LOGGER.info("invalidating all keys");
            this.invalidateAll();
            return 0 ;
        }
        getTemplateKey(meta).ifPresent(this::invalidateKey);
        return 0;
    }

    private Optional<TemplateKey> getTemplateKey(Map meta){

        try{
            Object keyObj = meta.get(Constants.TEMPLATE_KEY_FIELD);
            String jsonkeyStr = keyObj instanceof String ? keyObj.toString() :  jsonMapper.writeValueAsString(keyObj);
            TemplateKey key = jsonMapper.readValue(jsonkeyStr , TemplateKey.class);
            LOGGER.info("template key "+key);
            return Optional.of(key);

        }catch (Exception e){

            LOGGER.error("Template key parsing error "+e.getMessage() , e);
            return Optional.empty();
        }
    }

    private class ComparatorNotImplementedException extends Exception {
        public final EventType eventType;

        public ComparatorNotImplementedException(EventType eventType) {
            this.eventType = eventType;
        }
    }

    private final Comparator defaultHTTPRequestMatchComparator;
    private final Comparator defaultHTTPResponseComparator;
    private final Comparator defaultJavaRequestComparator;
    private final Comparator defaultJavaResponseComparator;
    private final Comparator defaultThriftRequestComparator;
    private final Comparator defaultThriftResponseComparator;

    private static final String CACHE_NAME = "ComparatorCache";
}

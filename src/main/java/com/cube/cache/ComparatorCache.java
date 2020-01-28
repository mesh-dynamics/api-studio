/*
 *
 *    Copyright Cube I O
 *
 */

package com.cube.cache;

import static com.cube.dao.Event.EventType.HTTPRequest;
import static com.cube.dao.Event.EventType.HTTPResponse;
import static com.cube.dao.Event.EventType.JavaRequest;
import static com.cube.dao.Event.EventType.JavaResponse;
import static com.cube.dao.Event.EventType.ThriftRequest;
import static com.cube.dao.Event.EventType.ThriftResponse;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import com.cube.cache.TemplateKey.Type;
import com.cube.core.Comparator;
import com.cube.core.CompareTemplate;
import com.cube.core.CompareTemplate.ComparisonType;
import com.cube.core.CompareTemplate.DataType;
import com.cube.core.CompareTemplate.PresenceType;
import com.cube.core.JsonComparator;
import com.cube.core.TemplateEntry;
import com.cube.dao.Event.EventType;
import com.cube.dao.ReqRespStore;
import com.cube.exception.CacheException;
import com.cube.utils.Constants;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-11-14
 */
public class ComparatorCache {

    private static final Logger LOGGER = LogManager.getLogger(ComparatorCache.class);

    private final TemplateCache templateCache;
    private final ObjectMapper jsonMapper;
    private final ReqRespStore rrStore;
    private final Cache<TemplateKey, Comparator> comparatorCache;


    public ComparatorCache(TemplateCache cache, ObjectMapper jsonMapper, ReqRespStore rrStore) {
        this.templateCache = cache;
        this.jsonMapper = jsonMapper;
        this.rrStore = rrStore;

        // we cache the comparators to avoid parsing the template json every time
        this.comparatorCache = CacheBuilder.newBuilder().maximumSize(100).build();


        // default rules for HTTP Request
        CompareTemplate defaultHTTPRequestMatchTemplate = new CompareTemplate();
        defaultHTTPRequestMatchTemplate.addRule(new TemplateEntry(Constants.QUERY_PARAMS_PATH, DataType.Obj,
            PresenceType.Optional, ComparisonType.Equal));
        defaultHTTPRequestMatchTemplate.addRule(new TemplateEntry(Constants.FORM_PARAMS_PATH, DataType.Obj,
            PresenceType.Optional, ComparisonType.Equal));
        defaultHTTPRequestMatchTemplate.addRule(new TemplateEntry(Constants.BODY_PATH, DataType.Default,
            PresenceType.Required, ComparisonType.Equal));
        defaultHTTPRequestMatchTemplate.addRule(new TemplateEntry(Constants.METHOD_PATH, DataType.Str, PresenceType.Required,
            ComparisonType.Equal));
        defaultHTTPRequestMatchComparator = new JsonComparator(defaultHTTPRequestMatchTemplate, jsonMapper);

        // default rules for HTTP Response
        CompareTemplate defaultHTTPResponseTemplate = new CompareTemplate();
        defaultHTTPResponseTemplate.addRule(new TemplateEntry(Constants.BODY_PATH, DataType.Default,
            PresenceType.Required, ComparisonType.Equal));
        defaultHTTPResponseComparator = new JsonComparator(defaultHTTPResponseTemplate, jsonMapper);

        // default rules for Java Request
        CompareTemplate defaultJavaRequestTemplate = new CompareTemplate();
        defaultJavaRequestTemplate.addRule(new TemplateEntry(Constants.ARGS_PATH, DataType.NrptArray,
            PresenceType.Required, ComparisonType.Equal));
        defaultJavaRequestComparator = new JsonComparator(defaultJavaRequestTemplate, jsonMapper);

        // default rule for Java Response
        CompareTemplate defaultJavaResponseTemplate = new CompareTemplate();
        defaultJavaResponseTemplate.addRule(new TemplateEntry(Constants.FN_RESPONSE_PATH, DataType.Obj,
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

    }

    public Comparator getComparator(TemplateKey key) throws  TemplateNotFoundException {
        String defaultEventType = rrStore.getDefaultEventType(key.getCustomerId()
            , key.getAppId(), key.getServiceId(), key.getPath()).orElseThrow(
            TemplateNotFoundException::new);
        if ((HTTPResponse.name().equals(defaultEventType)
            || HTTPRequest.name().equals(defaultEventType))
            && Type.ResponseCompare == key.getReqOrResp())
            return getComparator(key, HTTPResponse);
        if ((ThriftResponse.name().equals(defaultEventType)
            || ThriftRequest.name().equals(defaultEventType))
            && Type.ResponseCompare == key.getReqOrResp())
            return getComparator(key, ThriftResponse);
        if ((JavaRequest.name().equals(defaultEventType)
            || JavaResponse.name().equals(defaultEventType))
            && Type.ResponseCompare == key.getReqOrResp())
            return getComparator(key, JavaResponse);
        throw new TemplateNotFoundException();
    }

    public Comparator getComparator(TemplateKey key, EventType eventType) throws TemplateNotFoundException {
        try {
            return comparatorCache.get(key, () -> {
                Comparator toReturn = createComparator(key, eventType);
                LOGGER.info(new ObjectMessage(Map.of(
                    Constants.MESSAGE, "Successfully loaded into cache request comparator",
                    "key", key
                )));
                return toReturn;
            });

        } catch (ExecutionException e) {
            LOGGER.info(new ObjectMessage(Map.of(
                Constants.MESSAGE, "Unable to find template in cache, using default",
                "key", key,
                Constants.REASON, e.getMessage())));
            switch (eventType) {
                case HTTPRequest:
                    if(key.getReqOrResp() == Type.RequestMatch) {
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
                        "key", key
                    )));
                    throw new TemplateNotFoundException();
            }
        } catch (Throwable e) {
            LOGGER.error(new ObjectMessage(Map.of(
                Constants.MESSAGE, "Unhandled exception occured (re-throwing)"
            )),e);
            throw e;
        }
    }


    public Comparator createComparator(TemplateKey key, EventType eventType) throws
        ComparatorNotImplementedException, CacheException {
        CompareTemplate compareTemplate = null;

        compareTemplate = templateCache.fetchCompareTemplate(key);

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

    public void invalidateKey(TemplateKey key) {
        comparatorCache.invalidate(key);
        templateCache.invalidateKey(key);
    }

    public void invalidateAll() {
        comparatorCache.invalidateAll();
        templateCache.invalidateAll();
    }


    static public class TemplateNotFoundException extends Exception {
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
}

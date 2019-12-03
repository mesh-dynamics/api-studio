/*
 *
 *    Copyright Cube I O
 *
 */

package com.cube.cache;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import com.cube.core.Comparator;
import com.cube.core.CompareTemplate;
import com.cube.core.CompareTemplate.ComparisonType;
import com.cube.core.CompareTemplate.DataType;
import com.cube.core.CompareTemplate.PresenceType;
import com.cube.core.JsonComparator;
import com.cube.core.TemplateEntry;
import com.cube.dao.Event.EventType;
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
    private final Cache<TemplateKey, Comparator> comparatorCache;


    public ComparatorCache(TemplateCache cache, ObjectMapper jsonMapper) {
        this.templateCache = cache;
        this.jsonMapper = jsonMapper;

        // we cache the comparators to avoid parsing the template json every time
        this.comparatorCache = CacheBuilder.newBuilder().maximumSize(100).build();


        // default rules for HTTP Request
        CompareTemplate defaultHTTPRequestTemplate = new CompareTemplate();
        defaultHTTPRequestTemplate.addRule(new TemplateEntry(Constants.QUERY_PARAMS_PATH, DataType.Obj,
            PresenceType.Optional, ComparisonType.Equal));
        defaultHTTPRequestTemplate.addRule(new TemplateEntry(Constants.FORM_PARAMS_PATH, DataType.Obj,
            PresenceType.Optional, ComparisonType.Equal));
        defaultHTTPRequestTemplate.addRule(new TemplateEntry(Constants.BODY_PATH, DataType.Default,
            PresenceType.Required, ComparisonType.Equal));
        defaultHTTPRequestTemplate.addRule(new TemplateEntry(Constants.METHOD_PATH, DataType.Str, PresenceType.Required,
            ComparisonType.Equal));
        defaultHTTPRequestComparator = new JsonComparator(defaultHTTPRequestTemplate, jsonMapper);

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
            new TemplateEntry("", DataType.Obj, PresenceType.Required, ComparisonType.Equal));
        defaultThriftRequestComparator = new JsonComparator(defaultThriftRequestTemplate,
            jsonMapper);

        // default rule for Thrift Response Payload (serialized using gson)
        CompareTemplate defaultThriftResponseTemplate = new CompareTemplate();
        defaultThriftResponseTemplate.addRule(
            new TemplateEntry("", DataType.Obj, PresenceType.Required, ComparisonType.Equal));
        defaultThriftResponseComparator = new JsonComparator(defaultThriftResponseTemplate,
            jsonMapper);

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
                Constants.MESSAGE, "Unable to find template, using default",
                "key", key,
                Constants.REASON, e.getMessage())));
            switch (eventType) {
                case HTTPRequest:
                    return defaultHTTPRequestComparator;
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
                Constants.MESSAGE, "Unhandled exception occured (re-throwing)",
                Constants.ERROR, e.getMessage()
            )));
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

    private final Comparator defaultHTTPRequestComparator;
    private final Comparator defaultHTTPResponseComparator;
    private final Comparator defaultJavaRequestComparator;
    private final Comparator defaultJavaResponseComparator;
    private final Comparator defaultThriftRequestComparator;
    private final Comparator defaultThriftResponseComparator;
}

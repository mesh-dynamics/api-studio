package com.cube.core;

import static org.apache.commons.io.FileUtils.readFileToString;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.cube.dao.DataObj;
import com.cube.dao.Event;
import com.cube.dao.Event.EventBuilder.InvalidEventException;
import com.cube.dao.HTTPRequestPayload;
import com.cube.ws.Config;

public class RequestComparatorTest {

    static Config config;
    JSONObject object;
    static ObjectMapper mapper;

    /**
     * @throws java.lang.Exception
     */
    @BeforeAll
    static void setUpBeforeClass() throws Exception {
        config = new Config();
        mapper = config.jsonMapper;
        mapper.registerModule(new JavaTimeModule());
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterAll
    static void tearDownAfterClass() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    void setUp() throws Exception {
        readJSONFile("RequestComparator.json");
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterEach
    void tearDown() throws Exception {
    }

    public void readJSONFile(String url) {
        try {
            File file = new File(JsonComparatorTest.class.getClassLoader().getResource(url).toURI().getPath());
            String data = readFileToString(file, Charset.defaultCharset());
            try {
                object = new JSONObject(data);
            } catch (Exception e) {
                System.out.println(e);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Test method for {@link com.cube.core.Comparator#compare(DataObj, DataObj)} .
     * @throws JsonProcessingException
     * @throws JSONException
     */
    @Test
    @DisplayName("Exact Match Test")
    final void exactMatchTest()
        throws IOException, JSONException, InvalidEventException {
        JSONObject testData = object.getJSONObject("exactMatch");
        matchTest(testData);
    }

    /**
     * Test method for {@link com.cube.core.Comparator#compare(DataObj, DataObj)} .
     * @throws JsonProcessingException
     * @throws JSONException
     */
    @Test
    @DisplayName("DataType NoMatch Test")
    final void dataTypeNoMatchTest()
        throws IOException, JSONException, InvalidEventException {
        JSONObject testData = object.getJSONObject("dataTypeNoMatch");
        matchTest(testData);
    }

    /**
     * Test method for {@link com.cube.core.Comparator#compare(DataObj, DataObj)} .
     * @throws JsonProcessingException
     * @throws JSONException
     */
    @Test
    @DisplayName("Path NoMatch Test")
    final void pathNotFoundTest()
        throws IOException, JSONException, InvalidEventException {
        JSONObject testData = object.getJSONObject("pathNotFound");
        matchTest(testData);
    }

    private void matchTest(JSONObject testData)
        throws IOException, JSONException, InvalidEventException {
        String req1 = testData.get("req1").toString();
        String req2 = testData.get("req2").toString();
        Event request1 = mapper.readValue(object.getJSONObject(req1).toString(), Event.class);
        Event request2 = mapper.readValue(object.getJSONObject(req2).toString(), Event.class);
        compareTest(testData, request1, request2);
    }
    /**
     * Test method for {@link com.cube.core.Comparator#compare(DataObj, DataObj)} .
     * @throws JsonProcessingException
     * @throws JSONException
     */
    @Test
    @DisplayName("Multimap FuzzyMatch Test")
    final void multimapFuzzyMatchTest()
        throws IOException, JSONException, InvalidEventException {
        JSONObject testData = object.getJSONObject("multimapFuzzyMatch");
        multimapMatchTest(testData);
    }

    /**
     * Test method for {@link com.cube.core.Comparator#compare(DataObj, DataObj)} .
     * @throws JsonProcessingException
     * @throws JSONException
     */
    @Test
    @DisplayName("Multimap NoMatch Test")
    final void multimapNoMatchTest()
        throws IOException, JSONException, InvalidEventException {
        JSONObject testData = object.getJSONObject("multimapNoMatch");
        multimapMatchTest(testData);
    }

    private void multimapMatchTest(JSONObject testData)
        throws IOException, JSONException, InvalidEventException {
        String req1 = testData.get("req1").toString();
        String req2 = testData.get("req2").toString();
        Event request1 = mapper.readValue(object.getJSONObject(req1).toString(), Event.class);
        Event request2 = mapper.readValue(object.getJSONObject(req2).toString(), Event.class);
        Event newReq = updateRequestEventHdr(request2, "accept","K");
        compareTest(testData, request1, newReq, List.of("", "/hdrs/accept"));
        newReq = updateRequestEventFormParams(request2, "filmName", "K");
        compareTest(testData, request1, newReq, List.of("", "/formParams/filmName"));
        newReq = updateRequestEventQueryParams(request2, "filmId", "K");
        compareTest(testData, request1, newReq, List.of("", "/queryParams/filmId"));

        // all changes together
        newReq = updateRequestEventHdr(request2, "accept","K");
        newReq = updateRequestEventFormParams(newReq, "filmName", "K");
        newReq = updateRequestEventQueryParams(newReq, "filmId", "K");
        compareTest(testData, request1, newReq);
    }

    /**
     * Test method for {@link com.cube.core.Comparator#compare(DataObj, DataObj)} .
     * @throws JsonProcessingException
     * @throws JSONException
     */
    @Test
    @DisplayName("Root Param No Match Test")
    final void rootParamNoMatchTest()
        throws IOException, JSONException, InvalidEventException {
        JSONObject testData = object.getJSONObject("rootParamNoMatch");
        rootParamMatchTest(testData);
    }

    private void rootParamMatchTest(JSONObject testData)
        throws IOException, JSONException, InvalidEventException {
        String req1 = testData.get("req1").toString();
        Event event1 = mapper.readValue(object.getJSONObject(req1).toString(), Event.class);

        String req2 = testData.get("req2").toString();
        Event event2 = mapper.readValue(object.getJSONObject(req2).toString(), Event.class);
        compareTest(testData, event1, event2, List.of("/method"));

    }

    private void compareTest(JSONObject testData, Event event1, Event event2) throws JsonProcessingException,
        JSONException {
        compareTest(testData, event1, event2, Collections.emptyList());
    }

    private void compareTest(JSONObject testData, Event event1, Event event2, List<String> rulePaths) throws JsonProcessingException, JSONException {
        JSONArray rules = testData.getJSONArray("rules");
        String expected = testData.get("output").toString();
        CompareTemplate template = new CompareTemplate();
        for (int i = 0; i < rules.length(); i++) {
            JSONObject ruleObj = rules.getJSONObject(i);
            String path = ruleObj.getString("path");
            if (rulePaths.contains(path) || rulePaths.isEmpty()) {
                CompareTemplate.DataType dataType = CompareTemplate.DataType.valueOf(ruleObj.getString("dataType"));
                CompareTemplate.PresenceType presenceType = CompareTemplate.PresenceType.valueOf(ruleObj.getString("presenceType"));
                CompareTemplate.ComparisonType comparisonType = CompareTemplate.ComparisonType.valueOf(ruleObj.getString("comparisonType"));
                CompareTemplate.ExtractionMethod extractionMethod = CompareTemplate.ExtractionMethod.Default;
                if (ruleObj.has("extractionMethod")) {
                    extractionMethod = CompareTemplate.ExtractionMethod.valueOf(ruleObj.getString("extractionMethod"));
                }                String customization = ruleObj.getString("customization");
                TemplateEntry rule = new TemplateEntry(path, dataType, presenceType, comparisonType, extractionMethod, Optional.of(customization));
                template.addRule(rule);
            }
        }

        Comparator comparator = new JsonComparator(template, mapper);
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(event1));
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(event2));
        Comparator.MatchType matchType = comparator.compare(event1.getPayload(config),
            event2.getPayload(config)).mt;

        Assertions.assertEquals(expected, matchType.toString());
    }


    private Event updateRequestEventHdr(Event event, String hdrField, String val) throws IOException, InvalidEventException {
        HTTPRequestPayload requestPayload = Utils.getRequestPayload(event, config);
        requestPayload.hdrs.putSingle(hdrField, val);

        return cloneWithPayload(event, requestPayload);

    }

    private Event updateRequestEventFormParams(Event event, String param, String val) throws IOException,
        InvalidEventException {
        HTTPRequestPayload requestPayload = Utils.getRequestPayload(event, config);
        requestPayload.formParams.putSingle(param, val);

        return cloneWithPayload(event, requestPayload);

    }

    private Event updateRequestEventQueryParams(Event event, String param, String val) throws IOException,
        InvalidEventException {
        HTTPRequestPayload requestPayload = Utils.getRequestPayload(event, config);
        requestPayload.queryParams.putSingle(param, val);

        return cloneWithPayload(event, requestPayload);

    }

    private Event cloneWithPayload(Event event, HTTPRequestPayload payload) throws JsonProcessingException, InvalidEventException {
        return new Event.EventBuilder(event.customerId, event.app, event.service, event.instanceId,
            event.getCollection(), event.getTraceId(), event.runType, event.timestamp, event.reqId, event.apiPath, event.eventType)
            .setRawPayloadString(mapper.writeValueAsString(payload))
            .createEvent();
    }

}

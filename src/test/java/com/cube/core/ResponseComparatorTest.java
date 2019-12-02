package com.cube.core;

import static org.apache.commons.io.FileUtils.readFileToString;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.cube.core.Comparator.Match;
import com.cube.core.CompareTemplate.ComparisonType;
import com.cube.core.CompareTemplate.DataType;
import com.cube.core.CompareTemplate.ExtractionMethod;
import com.cube.core.CompareTemplate.PresenceType;
import com.cube.dao.Event;
import com.cube.dao.Event.EventBuilder;
import com.cube.dao.HTTPResponsePayload;
import com.cube.ws.Config;

public class ResponseComparatorTest {

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
        readJSONFile("ResponseComparator.json");
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
     * Test method for  .
     * @throws JsonProcessingException
     * @throws JSONException
     */
    @Test
    @DisplayName("Exact Match Test")
    final void exactMatchTest()
        throws IOException, JSONException, EventBuilder.InvalidEventException {
        JSONObject testData = object.getJSONObject("exactMatch");
        String res1 = testData.get("res1").toString();
        String res2 = testData.get("res2").toString();
        Event response1 = mapper.readValue(object.getJSONObject(res1).toString(), Event.class);
        Event response2 = mapper.readValue(object.getJSONObject(res2).toString(), Event.class);
//        Optional<Response> response1 = config.rrstore.getResponseEvent(res1);
//        Optional<Response> response2 = config.rrstore.getResponseEvent(res2);
        compareTest(testData, response1, response2);
    }

    /**
     * Test method for  .
     * @throws JsonProcessingException
     * @throws JSONException
     */
    @Test
    @DisplayName("Header template test: Positive")
    final void headerTemplatePositiveTest()
        throws IOException, JSONException, EventBuilder.InvalidEventException {
        JSONObject testData = object.getJSONObject("headerTemplatePositive");
        String res1 = testData.get("res1").toString();
        String res2 = testData.get("res2").toString();
        Event response1 = mapper.readValue(object.getJSONObject(res1).toString(), Event.class);
        Event response2 = mapper.readValue(object.getJSONObject(res2).toString(), Event.class);
        compareTest(testData, response1, response2);
    }

    /**
     * Test method for  .
     * @throws JsonProcessingException
     * @throws JSONException
     */
    @Test
    @DisplayName("Header template test: Negative")
    final void headerTemplateNegativeTest()
        throws IOException, JSONException, EventBuilder.InvalidEventException {
        JSONObject testData = object.getJSONObject("headerTemplateNegative");
        String res1 = testData.get("res1").toString();
        String res2 = testData.get("res2").toString();
        Event response1 = mapper.readValue(object.getJSONObject(res1).toString(), Event.class);
        Event response2 = mapper.readValue(object.getJSONObject(res2).toString(), Event.class);
        response2 = updateRequestEventHdr(response2, "content-type", "K");
        compareTest(testData, response1, response2);
    }

    /**
     * Test method for  .
     * @throws JsonProcessingException
     * @throws JSONException
     */
    @Test
    @DisplayName("Same Response body test: Positive")
    final void sameResponseBodyPositiveTest() throws IOException, JSONException {
        JSONObject testData = object.getJSONObject("sameResponseBodyPositive");
        String res1 = testData.get("res1").toString();
        String res2 = testData.get("res2").toString();
        Event response1 = mapper.readValue(object.getJSONObject(res1).toString(), Event.class);
        Event response2 = mapper.readValue(object.getJSONObject(res2).toString(), Event.class);
        compareTest(testData, response1, response2);
    }

    /**
     * Test method for  .
     * @throws JsonProcessingException
     * @throws JSONException
     */
    @Test
    @DisplayName("Same Response body test: Negative")
    final void sameResponseBodyNegativeTest()
        throws IOException, JSONException, EventBuilder.InvalidEventException {
        JSONObject testData = object.getJSONObject("sameResponseBodyNegative");
        String res1 = testData.get("res1").toString();
        Event response1 = mapper.readValue(object.getJSONObject(res1).toString(), Event.class);
        JSONObject body = new JSONObject(Utils.getResponsePayload(response1, config).body);
        body.put("year", 1000);
        body.put("type", "softcopy");
        body.put("pages", 3.14);

        Event response2 = updateResponseEventBody(response1, body.toString());
        compareTest(testData, response1, response2);
    }

    /**
     * Test method for  .
     * @throws JsonProcessingException
     * @throws JSONException
     */
    @Test
    @DisplayName("Different response Body test")
    final void differentResponseBodyTest() throws IOException, JSONException {
        JSONObject testData = object.getJSONObject("differentResponseBody");
        String res1 = testData.get("res1").toString();
        String res2 = testData.get("res2").toString();
        Event response1 = mapper.readValue(object.getJSONObject(res1).toString(), Event.class);
        Event response2 = mapper.readValue(object.getJSONObject(res2).toString(), Event.class);
        compareTest(testData, response1, response2);
    }



    private void compareTest(JSONObject testData, Event event1, Event event2) throws JsonProcessingException,
        JSONException {
        JSONArray rules = testData.getJSONArray("rules");
        String expected = testData.get("output").toString();
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(event1));
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(event2));
        CompareTemplate template = new CompareTemplate();
        for (int i = 0; i < rules.length(); i++) {
            JSONObject ruleObj = rules.getJSONObject(i);
            String path = ruleObj.getString("path");
            DataType dataType = DataType.valueOf(ruleObj.getString("dataType"));
            PresenceType presenceType = PresenceType.valueOf(ruleObj.getString("presenceType"));
            ComparisonType comparisonType = ComparisonType.valueOf(ruleObj.getString("comparisonType"));
            ExtractionMethod extractionMethod = ExtractionMethod.Default;
            if (ruleObj.has("extractionMethod")) {
                extractionMethod = ExtractionMethod.valueOf(ruleObj.getString("extractionMethod"));
            }            String customization = ruleObj.getString("customization");
            TemplateEntry rule = new TemplateEntry(path, dataType, presenceType, comparisonType, extractionMethod, Optional.of(customization));
            template.addRule(rule);
        }

        Comparator comparator = new JsonComparator(template, mapper);
        Match m = comparator.compare(event1.getPayload(config),
            event2.getPayload(config));

        String mjson = config.jsonMapper.writeValueAsString(m);
        JSONAssert.assertEquals(expected, mjson, false);
    }

    private Event updateRequestEventHdr(Event event, String hdrField, String val) throws IOException, EventBuilder.InvalidEventException {
        HTTPResponsePayload responsePayload = Utils.getResponsePayload(event, config);
        responsePayload.hdrs.putSingle(hdrField, val);

        return cloneWithPayload(event, responsePayload);

    }

    private Event updateResponseEventBody(Event event, String body) throws IOException,
        EventBuilder.InvalidEventException {
        HTTPResponsePayload responsePayload = Utils.getResponsePayload(event, config);

        HTTPResponsePayload newPayload = new HTTPResponsePayload(responsePayload.hdrs, responsePayload.status, body);
        return cloneWithPayload(event, newPayload);

    }

    private Event cloneWithPayload(Event event, HTTPResponsePayload payload) throws JsonProcessingException, EventBuilder.InvalidEventException {
        return new Event.EventBuilder(event.customerId, event.app, event.service, event.instanceId,
            event.getCollection(), event.traceId, event.runType, event.timestamp, event.reqId, event.apiPath, event.eventType)
            .setRawPayloadString(mapper.writeValueAsString(payload))
            .createEvent();
    }



}

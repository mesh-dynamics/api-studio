package com.cube.core;

import com.cube.dao.Response;
import com.cube.ws.Config;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Optional;

import com.cube.core.Comparator.Match;
import com.cube.core.CompareTemplate.ComparisonType;
import com.cube.core.CompareTemplate.DataType;
import com.cube.core.CompareTemplate.PresenceType;
import org.skyscreamer.jsonassert.JSONAssert;

import static com.cube.dao.RRBase.APPPATH;
import static com.cube.dao.RRBase.COLLECTIONPATH;
import static com.cube.dao.RRBase.CUSTOMERIDPATH;
import static com.cube.dao.RRBase.HDRPATH;
import static com.cube.dao.RRBase.METAPATH;
import static com.cube.dao.RRBase.REQIDPATH;
import static com.cube.dao.RRBase.RRTYPEPATH;
import static com.cube.dao.RRBase.SERVICEFIELD;
import static com.cube.dao.Request.*;
import static org.apache.commons.io.FileUtils.readFileToString;

public class ResponseComparatorTest {

    static Config config;
    JSONObject object;
    static ObjectMapper mapper;
    static String[] idList;

    /**
     * @throws java.lang.Exception
     */
    @BeforeAll
    static void setUpBeforeClass() throws Exception {
        config = new Config();
        String[] idArr = {
            "72471111-e096-4494-942e-5fa942c07e90",
            "movieinfoea45f119-8b00-4a58-bdca-fce5f8810c38",
            "movieinfod6c58e4e-f7cf-448f-acc7-0b1258125577",
            "movieinfob0573202-5212-4016-ba9d-d2d295812959",
            "movieinfo688542ce-d62a-4115-8274-7cadc900389d",
            "movieinfoef243baf-1e76-494a-b66a-fcc8c8aea97a",
            "movieinfoa6b2c925-fca3-4397-8cd9-2ab0c2143f5c",
            "restwrapjdbc11939c73-78cd-489b-971f-50a073bc1487",
            "restwrapjdbce6acccf2-cf09-499f-a7dc-d6b4a11381ba",
            "restwrapjdbc6467c26f-2b7e-4441-a1f2-3f6d6707e4db"
        };
        idList = idArr;
        mapper = config.jsonmapper;
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
     * Test method for {@link com.cube.core.TemplatedResponseComparator#compare(Response, Response)} .
     * @throws JsonProcessingException
     * @throws JSONException
     */
    @Test
    @DisplayName("Exact Match Test")
    final void exactMatchTest() throws IOException, JSONException {
        JSONObject testData = object.getJSONObject("exactMatch");
        String res1 = testData.get("res1").toString();
        String res2 = testData.get("res2").toString();
        Optional<Response> response1 = config.rrstore.getResponse(res1);
        Optional<Response> response2 = config.rrstore.getResponse(res2);
        compareTest(testData, response1.get(), response2.get());
    }

    /**
     * Test method for {@link com.cube.core.TemplatedResponseComparator#compare(Response, Response)} .
     * @throws JsonProcessingException
     * @throws JSONException
     */
    @Test
    @DisplayName("Header template test: Positive")
    final void headerTemplatePositiveTest() throws IOException, JSONException {
        JSONObject testData = object.getJSONObject("headerTemplatePositive");
        String res1 = testData.get("res1").toString();
        String res2 = testData.get("res2").toString();
        Response response1 = config.rrstore.getResponse(res1).get();
        Response response2 = config.rrstore.getResponse(res2).get();
        compareTest(testData, response1, response2);
    }

    /**
     * Test method for {@link com.cube.core.TemplatedResponseComparator#compare(Response, Response)} .
     * @throws JsonProcessingException
     * @throws JSONException
     */
    @Test
    @DisplayName("Header template test: Negative")
    final void headerTemplateNegativeTest() throws IOException, JSONException {
        JSONObject testData = object.getJSONObject("headerTemplateNegative");
        String res1 = testData.get("res1").toString();
        String res2 = testData.get("res2").toString();
        Response response1 = config.rrstore.getResponse(res1).get();
        Response response2 = config.rrstore.getResponse(res2).get();
        response2.hdrs.putSingle("content-type",response2.hdrs.getFirst("content-type") + "K");
        compareTest(testData, response1, response2);
    }

    /**
     * Test method for {@link com.cube.core.TemplatedResponseComparator#compare(Response, Response)} .
     * @throws JsonProcessingException
     * @throws JSONException
     */
    @Test
    @DisplayName("Same Response body test: Positive")
    final void sameResponseBodyPositiveTest() throws IOException, JSONException {
        JSONObject testData = object.getJSONObject("sameResponseBodyPositive");
        String res1 = testData.get("res1").toString();
        Response response1 = config.rrstore.getResponse(res1).get();
        compareTest(testData, response1, response1);
    }

    /**
     * Test method for {@link com.cube.core.TemplatedResponseComparator#compare(Response, Response)} .
     * @throws JsonProcessingException
     * @throws JSONException
     */
    @Test
    @DisplayName("Same Response body test: Negative")
    final void sameResponseBodyNegativeTest() throws IOException, JSONException {
        JSONObject testData = object.getJSONObject("sameResponseBodyNegative");
        String res1 = testData.get("res1").toString();
        Response response1 = config.rrstore.getResponse(res1).get();
        JSONObject body = new JSONObject(response1.body);
        body.put("year", 1000);
        body.put("type", "softcopy");
        body.put("pages", 3.14);
        Response response2 = new Response(response1.reqid, response1.status, response1.meta, response1.hdrs,
            body.toString(), response1.collection, response1.timestamp, response1.rrtype, response1.customerid, response1.app);
        compareTest(testData, response1, response2);
    }

    /**
     * Test method for {@link com.cube.core.TemplatedResponseComparator#compare(Response, Response)} .
     * @throws JsonProcessingException
     * @throws JSONException
     */
    @Test
    @DisplayName("Different response Body test")
    final void differentResponseBodyTest() throws IOException, JSONException {
        JSONObject testData = object.getJSONObject("differentResponseBody");
        String res1 = testData.get("res1").toString();
        String res2 = testData.get("res2").toString();
        Response response1 = config.rrstore.getResponse(res1).get();
        Response response2 = config.rrstore.getResponse(res2).get();
        compareTest(testData, response1, response2);
    }

    private void compareTest(JSONObject testData, Response response1, Response response2) throws JsonProcessingException, JSONException{
        JSONArray rules = testData.getJSONArray("rules");
        String expected = testData.get("output").toString();
        System.out.println(mapper.writeValueAsString(response1));
        System.out.println(mapper.writeValueAsString(response2));
        CompareTemplate template = new CompareTemplate();
        for (int i = 0; i < rules.length(); i++) {
            JSONObject ruleObj = rules.getJSONObject(i);
            String path = ruleObj.getString("path");
            DataType dataType = DataType.valueOf(ruleObj.getString("dataType"));
            PresenceType presenceType = PresenceType.valueOf(ruleObj.getString("presenceType"));
            ComparisonType comparisonType = ComparisonType.valueOf(ruleObj.getString("comparisonType"));
            String customization = ruleObj.getString("customization");
            TemplateEntry rule;
            if (customization.isEmpty()) {
                rule = new TemplateEntry(path, dataType, presenceType, comparisonType);
            } else {
                rule = new TemplateEntry(path, dataType, presenceType, comparisonType, Optional.of(customization));
            }
            template.addRule(rule);
        }

        TemplatedResponseComparator comparator = new TemplatedResponseComparator(template, mapper);
        Match m = comparator.compare(response1, response2);

        String mjson = config.jsonmapper.writeValueAsString(m);
        JSONAssert.assertEquals(expected, mjson, false);
    }

    /**
     * Test method for {@link com.cube.core.TemplatedResponseComparator#compare(Response, Response)} .
     * @throws JsonProcessingException
     * @throws JSONException
     */
    @Test
    @DisplayName("Default comparison test")
    final void defaultComparisonTest() throws IOException, JSONException {

        for (String id: idList){
            Optional<Response> response = config.rrstore.getResponse(id);
            System.out.println(response.get().body);
        }
//        ObjectMapper mapper = config.jsonmapper;
//        mapper.registerModule(new JavaTimeModule());
//        Optional<Response> response1 = config.rrstore.getResponse(id[1]);
//        System.out.println(mapper.writeValueAsString(response1));
//        Optional<Response> response2 = config.rrstore.getResponse(id[2]);
//        System.out.println(mapper.writeValueAsString(response2));
//        CompareTemplate template = new CompareTemplate();
//        template.addRule(new TemplateEntry("/status", DataType.Str, PresenceType.Required, ComparisonType.Equal));
//        template.addRule(new TemplateEntry(PATHPATH, DataType.Str, PresenceType.Optional, ComparisonType.Equal));
//        template.addRule(new TemplateEntry(QPARAMPATH, DataType.Str, PresenceType.Optional, ComparisonType.Equal));
//        template.addRule(new TemplateEntry(FPARAMPATH, DataType.Str, PresenceType.Optional, ComparisonType.Equal));
//        template.addRule(new TemplateEntry(RRTYPEPATH, DataType.Str, PresenceType.Optional, ComparisonType.Equal));
//        template.addRule(new TemplateEntry(CUSTOMERIDPATH, DataType.Str, PresenceType.Optional, ComparisonType.Equal));
//        template.addRule(new TemplateEntry(APPPATH, DataType.Str, PresenceType.Optional, ComparisonType.Equal));
//        template.addRule(new TemplateEntry(REQIDPATH, DataType.Str, PresenceType.Optional, ComparisonType.EqualOptional));
//        template.addRule(new TemplateEntry(COLLECTIONPATH, CompareTemplate.DataType.Str, PresenceType.Optional, ComparisonType.Equal));
//        template.addRule(new TemplateEntry(METAPATH + "/" + SERVICEFIELD, DataType.Str, PresenceType.Optional, ComparisonType.Equal));
//        template.addRule(new TemplateEntry(HDRPATH+"/"+Config.DEFAULT_TRACE_FIELD, DataType.Str, PresenceType.Optional, ComparisonType.EqualOptional));
//        TemplatedResponseComparator comparator = new TemplatedResponseComparator(template, mapper);
//        Match m = comparator.compare(response1.get(), response2.get());
//        String mjson = config.jsonmapper.writeValueAsString(m);
//        System.out.println(mjson);
    }

}

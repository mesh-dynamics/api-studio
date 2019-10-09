package com.cube.core;

import com.cube.dao.Request;
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
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.Optional;


import static org.apache.commons.io.FileUtils.readFileToString;

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
     * Test method for {@link com.cube.core.TemplatedResponseComparator#compare(Response, Response)} .
     * @throws JsonProcessingException
     * @throws JSONException
     */
    @Test
    @DisplayName("Exact Match Test")
    final void exactMatchTest() throws IOException, JSONException {
        JSONObject testData = object.getJSONObject("exactMatch");
        matchTest(testData);
    }

    /**
     * Test method for {@link com.cube.core.TemplatedResponseComparator#compare(Response, Response)} .
     * @throws JsonProcessingException
     * @throws JSONException
     */
    @Test
    @DisplayName("DataType NoMatch Test")
    final void dataTypeNoMatchTest() throws IOException, JSONException {
        JSONObject testData = object.getJSONObject("dataTypeNoMatch");
        matchTest(testData);
    }

    /**
     * Test method for {@link com.cube.core.TemplatedResponseComparator#compare(Response, Response)} .
     * @throws JsonProcessingException
     * @throws JSONException
     */
    @Test
    @DisplayName("Path NoMatch Test")
    final void pathNotFoundTest() throws IOException, JSONException {
        JSONObject testData = object.getJSONObject("pathNotFound");
        matchTest(testData);
    }

    private void matchTest(JSONObject testData) throws IOException, JSONException {
        String req1 = testData.get("req1").toString();
        String req2 = testData.get("req2").toString();
        Request request1 = mapper.readValue(object.getJSONObject(req1).toString(), Request.class);
        Request request2 = mapper.readValue(object.getJSONObject(req2).toString(), Request.class);
        compareTest(testData, request1, request2);
    }

    /**
     * Test method for {@link com.cube.core.TemplatedResponseComparator#compare(Response, Response)} .
     * @throws JsonProcessingException
     * @throws JSONException
     */
    @Test
    @DisplayName("Multimap FuzzyMatch Test")
    final void multimapFuzzyMatchTest() throws IOException, JSONException {
        JSONObject testData = object.getJSONObject("multimapFuzzyMatch");
        multimapMatchTest(testData);
    }

    /**
     * Test method for {@link com.cube.core.TemplatedResponseComparator#compare(Response, Response)} .
     * @throws JsonProcessingException
     * @throws JSONException
     */
    @Test
    @DisplayName("Multimap NoMatch Test")
    final void multimapNoMatchTest() throws IOException, JSONException {
        JSONObject testData = object.getJSONObject("multimapNoMatch");
        multimapMatchTest(testData);
    }

    private void multimapMatchTest(JSONObject testData) throws IOException, JSONException  {
        String req1 = testData.get("req1").toString();
        String req2 = testData.get("req2").toString();
        Request request1 = mapper.readValue(object.getJSONObject(req1).toString(), Request.class);
        Request request2 = mapper.readValue(object.getJSONObject(req2).toString(), Request.class);
        request2.hdrs.putSingle("accept",request2.hdrs.getFirst("accept") + "K");
        compareTest(testData, request1, request2, Optional.of("/hdr/accept"));
        request2.meta.putSingle("method",request2.hdrs.getFirst("method") + "K");
        compareTest(testData, request1, request2, Optional.of("/meta/method"));
        request2.fparams.putSingle("filmName",request2.fparams.getFirst("filmName") + "K");
        compareTest(testData, request1, request2, Optional.of("/fparams/filmName"));
        request2.qparams.putSingle("filmId",request2.qparams.getFirst("filmName") + "K");
        compareTest(testData, request1, request2, Optional.of("/qparams/filmId"));
        compareTest(testData, request1, request2);
    }

    /**
     * Test method for {@link com.cube.core.TemplatedResponseComparator#compare(Response, Response)} .
     * @throws JsonProcessingException
     * @throws JSONException
     */
    @Test
    @DisplayName("Root Param Fuzzy Match Test")
    final void rootParaFuzzyoMatchTest() throws IOException, JSONException {
        JSONObject testData = object.getJSONObject("rootParamFuzzyMatch");
        rootParamMatchTest(testData);
    }

    /**
     * Test method for {@link com.cube.core.TemplatedResponseComparator#compare(Response, Response)} .
     * @throws JsonProcessingException
     * @throws JSONException
     */
    @Test
    @DisplayName("Root Param No Match Test")
    final void rootParamNoMatchTest() throws IOException, JSONException {
        JSONObject testData = object.getJSONObject("rootParamNoMatch");
        rootParamMatchTest(testData);
    }

    private void rootParamMatchTest(JSONObject testData) throws IOException, JSONException {
        String req1 = testData.get("req1").toString();
        Request request1 = mapper.readValue(object.getJSONObject(req1).toString(), Request.class);
        Optional<String> temp = Optional.of("K");

        Request request2 = new Request(temp.get(), request1.reqid, request1.qparams, request1.fparams, request1.meta, request1.hdrs,
            request1.method, request1.body, request1.collection, request1.timestamp, request1.runType, request1.customerId, request1.app);
        compareTest(testData, request1, request2, Optional.of("/path"));

        request2 = new Request(request1.path, request1.reqid, request1.qparams, request1.fparams, request1.meta, request1.hdrs, temp.get(),
            request1.body, request1.collection, request1.timestamp, request1.runType, request1.customerId, request1.app);
        compareTest(testData, request1, request2, Optional.of("/method"));

        if (request1.reqid.isPresent()) {
            request2 = new Request(request1.path, temp, request1.qparams, request1.fparams, request1.meta, request1.hdrs,
                request1.method, request1.body, request1.collection, request1.timestamp, request1.runType, request1.customerId, request1.app);
            compareTest(testData, request1, request2, Optional.of("/reqid"));
        }

        if (request1.collection.isPresent()) {
            request2 = new Request(request1.path, request1.reqid, request1.qparams, request1.fparams, request1.meta, request1.hdrs,
                request1.method, request1.body, temp, request1.timestamp, request1.runType, request1.customerId, request1.app);
            compareTest(testData, request1, request2, Optional.of("/collection"));
        }

        if (request1.customerId.isPresent()) {
            request2 = new Request(request1.path, request1.reqid, request1.qparams, request1.fparams, request1.meta, request1.hdrs,
                request1.method, request1.body, request1.collection, request1.timestamp, request1.runType, temp, request1.app);
            compareTest(testData, request1, request2, Optional.of("/customerid"));
        }

        if (request1.app.isPresent()) {
            request2 = new Request(request1.path, request1.reqid, request1.qparams, request1.fparams, request1.meta, request1.hdrs,
                request1.method, request1.body, request1.collection, request1.timestamp, request1.runType, request1.customerId,  Optional.of(request1.app.get() + "K"));
            compareTest(testData, request1, request2, Optional.of("/app"));
        }

        request2 = new Request(temp.get(), temp, request1.qparams, request1.fparams, request1.meta, request1.hdrs, temp.get(),
            request1.body, request1.collection, request1.timestamp, request1.runType, temp,  temp);
        compareTest(testData, request1, request2);
    }


    private void compareTest(JSONObject testData, Request response1, Request response2) throws JsonProcessingException, JSONException {
        compareTest(testData, response1, response2, Optional.empty());
    }

    private void compareTest(JSONObject testData, Request response1, Request response2, Optional<String> rulePath) throws JsonProcessingException, JSONException {
        JSONArray rules = testData.getJSONArray("rules");
        String expected = testData.get("output").toString();
        System.out.println(mapper.writeValueAsString(response1));
        System.out.println(mapper.writeValueAsString(response2));
        CompareTemplate template = new CompareTemplate();
        for (int i = 0; i < rules.length(); i++) {
            JSONObject ruleObj = rules.getJSONObject(i);
            String path = ruleObj.getString("path");
            if (rulePath.isEmpty() || (path.equalsIgnoreCase(rulePath.get()))) {
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

        TemplatedRequestComparator comparator = new TemplatedRequestComparator(template, mapper);
        Comparator.MatchType matchType = comparator.compare(response1, response2);

        Assertions.assertEquals(expected, matchType.toString());
    }

    /**
     * Test method for {@link com.cube.core.TemplatedResponseComparator#compare(Response, Response)} .
     * @throws JsonProcessingException
     * @throws JSONException
     */
//    @Test
//    @DisplayName("Get request from Solr")
//    final void getAllRequest() throws JsonProcessingException, JSONException {
//        String[] idList = {
//            "restwrapjdbc2523c0a2-0645-4471-9340-2c4aeef6635e",
//            "restwrapjdbc5f4f257c-5971-402a-a411-ef49bfcec923",
//            "restwrapjdbcd1c51946-f454-4988-9cb0-4d3eb761a0d5",
//            "restwrapjdbccd64786d-1c64-4f50-b376-0b96f9d84eb1",
//            "restwrapjdbcede1e225-f2dd-4378-aa47-fbee9c7c64ef",
//            "restwrapjdbc59b33aff-4e35-47b1-9ead-df9f58407ee7",
//            "reviews7bd68a74-2457-4948-81c9-8b61c851d752",
//            "movieinfod8d4fdb8-7f5a-4610-90ed-4e9267a934ef",
//            "movieinfo45348484-2ef0-47bd-ae94-6069a35e025e",
//            "movieinfo504e9d35-0087-4289-91a0-67333360b7f9"
//        };
//        for (String id: idList){
//            Request request = config.rrstore.getRequest(id).get();
//            System.out.println(mapper.writeValueAsString(request));
//            System.out.println(request.qparams);
//            System.out.println(request.body);
//            System.out.println("-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");
//        }
//    }
}

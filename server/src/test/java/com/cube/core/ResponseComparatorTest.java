package com.cube.core;

import com.cube.dao.Response;
import com.cube.ws.Config;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.json.JSONException;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.Optional;

import com.cube.core.Comparator.Match;
import com.cube.core.CompareTemplate.ComparisonType;
import com.cube.core.CompareTemplate.DataType;
import com.cube.core.CompareTemplate.PresenceType;

import static com.cube.dao.RRBase.APPPATH;
import static com.cube.dao.RRBase.COLLECTIONPATH;
import static com.cube.dao.RRBase.CUSTOMERIDPATH;
import static com.cube.dao.RRBase.HDRPATH;
import static com.cube.dao.RRBase.METAPATH;
import static com.cube.dao.RRBase.REQIDPATH;
import static com.cube.dao.RRBase.RRTYPEPATH;
import static com.cube.dao.RRBase.SERVICEFIELD;
import static com.cube.dao.Request.*;

public class ResponseComparatorTest {

    static Config config;
    static String[] id;

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
        id = idArr;
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
//        readJSONFile("JsonComparator.json");
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterEach
    void tearDown() throws Exception {
    }

    /**
     * Test method for {@link com.cube.core.TemplatedResponseComparator#compare(Response, Response)} .
     * @throws JsonProcessingException
     * @throws JSONException
     */
    @Test
    @DisplayName("Default comparison test")
    final void defaultComparisonTest() throws IOException, JSONException {
        ObjectMapper mapper = config.jsonmapper;
        mapper.registerModule(new JavaTimeModule());
        Optional<Response> response1 = config.rrstore.getResponse(id[0]);
        System.out.println(mapper.writeValueAsString(response1));
        Optional<Response> response2 = config.rrstore.getResponse(id[1]);
        System.out.println(mapper.writeValueAsString(response2));
        CompareTemplate template = new CompareTemplate();
        template.addRule(new TemplateEntry("/body", DataType.Str, PresenceType.Required, ComparisonType.Equal));
        template.addRule(new TemplateEntry(PATHPATH, DataType.Str, PresenceType.Optional, ComparisonType.Equal));
        template.addRule(new TemplateEntry(QPARAMPATH, DataType.Str, PresenceType.Optional, ComparisonType.Equal));
        template.addRule(new TemplateEntry(FPARAMPATH, DataType.Str, PresenceType.Optional, ComparisonType.Equal));
        template.addRule(new TemplateEntry(RRTYPEPATH, DataType.Str, PresenceType.Optional, ComparisonType.Equal));
        template.addRule(new TemplateEntry(CUSTOMERIDPATH, DataType.Str, PresenceType.Optional, ComparisonType.Equal));
        template.addRule(new TemplateEntry(APPPATH, DataType.Str, PresenceType.Optional, ComparisonType.Equal));
        template.addRule(new TemplateEntry(REQIDPATH, DataType.Str, PresenceType.Optional, ComparisonType.EqualOptional));
        template.addRule(new TemplateEntry(COLLECTIONPATH, CompareTemplate.DataType.Str, PresenceType.Optional, ComparisonType.Equal));
        template.addRule(new TemplateEntry(METAPATH + "/" + SERVICEFIELD, DataType.Str, PresenceType.Optional, ComparisonType.Equal));
        template.addRule(new TemplateEntry(HDRPATH+"/"+Config.DEFAULT_TRACE_FIELD, DataType.Str, PresenceType.Optional, ComparisonType.EqualOptional));
        TemplatedResponseComparator comparator = new TemplatedResponseComparator(template, mapper);
        Match m = comparator.compare(response1.get(), response2.get());
        String mjson = config.jsonmapper.writeValueAsString(m);
        System.out.println(mjson);
    }

}

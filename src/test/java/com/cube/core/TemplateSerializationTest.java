package com.cube.core;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import static org.apache.commons.io.FileUtils.readFileToString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import io.md.core.CompareTemplate;
import io.md.core.TemplateEntry;
import io.md.utils.CubeObjectMapperProvider;

import com.cube.dao.ReqRespStore;
import com.cube.golden.TemplateSet;
import com.cube.ws.Config;

public class TemplateSerializationTest {

    private static final Logger LOGGER = LogManager.getLogger(AnalyzeServiceTest.class);

    @Test
    public void testTemplateEntrySerialization(){
        ObjectMapper mapper = CubeObjectMapperProvider.getInstance();
        TemplateEntry te = new TemplateEntry("/body",
                CompareTemplate.DataType.Str,
                CompareTemplate.PresenceType.Required,
                CompareTemplate.ComparisonType.EqualOptional,
                CompareTemplate.ExtractionMethod.Default,
                Optional.empty());
        try {
            String json = mapper.writeValueAsString(te);
            System.out.println(json);
            TemplateEntry te1 = mapper.readValue(json , TemplateEntry.class);
            assertEquals(te1.pt , CompareTemplate.PresenceType.Required);
            assertEquals(te1.ct , CompareTemplate.ComparisonType.EqualOptional);
            assertEquals(te1.path , "/body");
            assertEquals(te1.dt , CompareTemplate.DataType.Str);
            assert(te1.customization.isEmpty());
        } catch (Exception e) {
            Assertions.fail(e);
        }
    }


    @Test
    public void testCompareTemplateSerialization(){
        ObjectMapper mapper = CubeObjectMapperProvider.getInstance();
        CompareTemplate template = new CompareTemplate("");
        String[] paths = {"", "/string", "/int", "/float", "/obj", "/rptArr", "/nrptArr"};
        CompareTemplate.DataType[] dataTypes = {CompareTemplate.DataType.Obj, CompareTemplate.DataType.Str, CompareTemplate.DataType.Int, CompareTemplate.DataType.Float, CompareTemplate.DataType.Obj,
                CompareTemplate.DataType.RptArray, CompareTemplate.DataType.NrptArray};

        Map<String, TemplateEntry> templateEntryMap = new HashMap<>();

        for (int i = 0; i < paths.length; i++) {
            TemplateEntry rule = new TemplateEntry(paths[i], dataTypes[i], CompareTemplate.PresenceType.Required, CompareTemplate.ComparisonType.Ignore);
            templateEntryMap.put(paths[i] , rule);
            template.addRule(rule);
        }

        try {
            String convertedJson = mapper.writeValueAsString(template);
            System.out.println(convertedJson);
            CompareTemplate template1 = mapper.readValue(convertedJson , CompareTemplate.class);
            assertEquals(template1.prefixpath , "");
            template1.getRules().forEach(entry -> {
                        assertEquals(entry.pt, CompareTemplate.PresenceType.Required);
                        assertEquals(entry.ct, CompareTemplate.ComparisonType.Ignore);
                    }
                );

        } catch (IOException e) {
            Assertions.fail(e);
        }

    }

    static final String CUSTID = "Test";
    //static final String APPID = "Test";
    static final String INSTANCEID = "Test";


    @Test
    @DisplayName("Template Version Test")
    public void testTemplateVersion() throws Exception {
        Config config = new Config();
        ReqRespStore reqRespStore = config.rrstore;
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
        String url1 = "TemplateMovieInfoReqNoMatch.json";
        String url2 = "TemplateMovieInfoRespPartialMatch.json";

        //change these as per requirement
        String app = "movieinfo";

        File file1 = new File(JsonComparatorTest.class.getClassLoader()
            .getResource(url1).toURI().getPath());
        File file2 = new File(JsonComparatorTest.class.getClassLoader()
            .getResource(url2).toURI().getPath());
        String data1 = readFileToString(file1, Charset.defaultCharset());
        String data2 = readFileToString(file2, Charset.defaultCharset());

        TemplateRegistries templateRegistries1 = mapper.readValue(data1, TemplateRegistries.class);
        TemplateRegistries templateRegistries2 = mapper.readValue(data2, TemplateRegistries.class);

        TemplateSet tset1 = Utils.templateRegistriesToTemplateSet(templateRegistries1, CUSTID, app, Optional.empty());
        TemplateSet tset2 = Utils.templateRegistriesToTemplateSet(templateRegistries2, CUSTID, app, Optional.empty());

        reqRespStore.saveTemplateSet(tset1);

        LOGGER.info("Created template with version " + tset1.version);

        reqRespStore.saveTemplateSet(tset2);

        LOGGER.info("Created template with version " + tset2.version);

        assertNotEquals(tset1.version, tset2.version);


    }
}

package com.cube.core;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TemplateSerializationTest {

    @Test
    public void testTemplateEntrySerialization(){
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
        TemplateEntry te = new TemplateEntry("/body" , CompareTemplate.DataType.Str ,
                CompareTemplate.PresenceType.Required ,
                CompareTemplate.ComparisonType.EqualOptional , Optional.empty());
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

            e.printStackTrace();
        }
    }


    @Test
    public void testCompareTemplateSerialization(){
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
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
            e.printStackTrace();
        }

    }

}

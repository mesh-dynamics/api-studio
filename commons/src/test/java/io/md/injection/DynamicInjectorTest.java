package io.md.injection;

import static org.apache.commons.io.FileUtils.readFileToString;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.md.dao.DataObj;
import io.md.services.DataStore;
import io.md.utils.CubeObjectMapperProvider;
import io.md.utils.TestUtils.ReqAndRespEvent;
import io.md.utils.TestUtils.StaticDataStore;
import java.io.File;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import junit.framework.TestCase;

public class DynamicInjectorTest extends TestCase {
    ObjectMapper jsonMapper;
    DataStore dummyStore;
    JsonObject jsonObject;
    List<ReqAndRespEvent> reqAndRespEvents;

    public void compare(JsonObject test){
        Map<String, DataObj> extractionMap = new HashMap<>();
        JsonObject expectedExtractionMap;

        try {

            DynamicInjectionConfig diConfig = jsonMapper
                .readValue(test.get("dynamicInjectionConfig").toString(),
                    DynamicInjectionConfig.class);
            expectedExtractionMap = test.getAsJsonObject("extractionMap");
            DynamicInjector di = new DynamicInjector(Optional.of(diConfig), dummyStore, jsonMapper, extractionMap);

            reqAndRespEvents.forEach(reqAndRespEvent -> {
                di.inject(reqAndRespEvent.goldenReq);
                di.extract(reqAndRespEvent.goldenReq, reqAndRespEvent.testResp.payload);
            });

            Gson gsonObj = new Gson();
            Map<String, String> actualExtractionMap = new HashMap<>();
            for (Entry<String, DataObj> entry : extractionMap.entrySet()) {
                String key = entry.getKey();
                DataObj val = entry.getValue();
                // Direct conversion from <String, DataObj> map produces errors.
                actualExtractionMap.put(key, val.serializeDataObj());
            }

            assertEquals(gsonObj.fromJson(gsonObj.toJson(actualExtractionMap), JsonObject.class), expectedExtractionMap);

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    public void testExtract() {
        compare(jsonObject.getAsJsonObject("DynamicExtractionTest"));
    }

    public void readJSONFile(String url) {
        try {
            File file = new File(
                DynamicInjectorTest.class.getClassLoader().getResource(url).toURI().getPath());
            String data = readFileToString(file, String.valueOf(Charset.defaultCharset()));
            try {
                jsonObject = new Gson().fromJson(data, JsonObject.class);

            } catch (Exception e) {
                System.out.println(e);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void setUp() throws Exception {
//        super.setUp();
        jsonMapper = CubeObjectMapperProvider.getInstance();
        readJSONFile("DynamicInjectorTest.json");

        reqAndRespEvents = jsonMapper.readValue(String.valueOf(jsonObject.get("reqAndRespEvents")),
            new TypeReference<List<ReqAndRespEvent>>() {});


        dummyStore = new StaticDataStore(reqAndRespEvents);


        System.out.println(reqAndRespEvents);
    }

}
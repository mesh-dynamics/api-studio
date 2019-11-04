package com.cube.core;

import com.cube.utils.Constants;
import java.io.File;
import java.nio.charset.Charset;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import static org.apache.commons.io.FileUtils.readFileToString;

import com.cube.cache.TemplateKey;
import com.cube.dao.ReqRespStore;
import com.cube.ws.Config;

public class UploadTemplatesForSampleApp {


    public static void main(String[] args) {
        try {
            Config config = new Config();
            ReqRespStore reqRespStore = config.rrstore;
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new Jdk8Module());
            String url = "SampleAppResponseTemplate.json";

            //change these as per requirement
            String customerId = "ravivj";
            String app = "movieinfo";

            File file = new File(JsonComparatorTest.class.getClassLoader()
                    .getResource(url).toURI().getPath());
            String data = readFileToString(file, Charset.defaultCharset());

            JSONArray arr = new JSONArray(data);
            arr.forEach(x ->
            {
                JSONObject elem = (JSONObject) x;
                String path = elem.getString("path");
                String service = elem.getString("service");
                JSONObject template = elem.getJSONObject("template");
                String templateAsString = template.toString();
                reqRespStore.saveCompareTemplate(
                        new TemplateKey(Constants.DEFAULT_TEMPLATE_VER, customerId , app , service , path , TemplateKey.Type.Response)
                 , templateAsString);
            });


        } catch (Exception e) {
            System.out.println(e.getMessage());
        }


    }


}

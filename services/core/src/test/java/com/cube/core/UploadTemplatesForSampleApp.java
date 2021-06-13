/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cube.core;

import static io.md.constants.Constants.DEFAULT_TEMPLATE_VER;
import static org.apache.commons.io.FileUtils.readFileToString;

import java.io.File;
import java.nio.charset.Charset;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import io.md.core.TemplateKey;
import io.md.core.TemplateKey.Type;
import io.md.utils.Constants;

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
                String path = elem.getString(Constants.PATH_FIELD);
                String service = elem.getString(Constants.SERVICE_FIELD);
                JSONObject template = elem.getJSONObject("template");
                String templateAsString = template.toString();
                try {
                    reqRespStore.saveCompareTemplate(
                            new TemplateKey(DEFAULT_TEMPLATE_VER, customerId , app , service , path , Type.ResponseCompare)
                     , templateAsString);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });


        } catch (Exception e) {
            System.out.println(e.getMessage());
        }


    }


}

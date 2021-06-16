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

import java.util.regex.Pattern;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.cube.agent.GsonPatternDeserializer;
import net.dongliu.gson.GsonJava8TypeAdapterFactory;

import com.cube.serialize.GsonSolrDocumentListSerializer;
import com.cube.ws.Config;

public class GsonSerializationTest {

    public static void main(String[] args){
        try {
            Gson gson = new GsonBuilder().registerTypeAdapterFactory(new GsonJava8TypeAdapterFactory())
                .registerTypeAdapter(Pattern.class, new GsonPatternDeserializer()).registerTypeAdapter(SolrDocumentList.class,
                    new GsonSolrDocumentListSerializer()).create();
            Config config = new Config();
            SolrClient Solr = config.solr;

            SolrQuery query = new SolrQuery();
            query.setQuery("*:*");
            query.setRows(5);

            QueryResponse response = Solr.query(query);

            String serialized = gson.toJson(response);

            QueryResponse deserialized = gson.fromJson(serialized, QueryResponse.class);

            System.out.println(deserialized.getResults().getNumFound());

            System.out.println(serialized);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;

import com.cube.ws.Config;

public class DeleteRecordingAndReplays {

    public static void main(String[] args){
        try {
            //Preparing the Solr client
            Config config = new Config();
            SolrClient Solr = config.solr;

            String collection = "test-april-29-6";

            SolrQuery query = new SolrQuery("*:*");
            query.addFilterQuery("collection_s:"+collection);
            query.addFilterQuery("type_s:ReplayMeta");
            query.setRows(200);

            QueryResponse response = Solr.query(query);
            List<String> replayIds = new ArrayList<>();
            response.getResults().forEach(doc -> replayIds.add(doc.getFieldValue("replayid_s").toString()));

            replayIds.forEach(replayid ->
                {
                    try {
                        Solr.deleteByQuery("replayid_s:" + replayid);
                        Solr.deleteByQuery("collection_s:" + replayid);
                    } catch (Exception e) {
                    }
                }
            );

            Solr.deleteByQuery("collection_s:" + collection);

            System.out.println(replayIds.stream().collect(Collectors.joining(" , ")));

            Solr.commit();

            //Solr.deleteById()
        } catch (Exception e) {
            e.printStackTrace();
        }
    }




}

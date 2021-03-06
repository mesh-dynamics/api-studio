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

import java.io.IOException;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;

import com.cube.ws.Config;

public class SolrDelete {

    private static String createDataSize(int msgSize) {
        StringBuilder sb = new StringBuilder(msgSize);
        for (int i=0; i<msgSize; i++) {
            sb.append('a');
        }
        return sb.toString();
    }

    public static void main(String args[]) throws IOException, SolrServerException {
        //Preparing the Solr client
        try {
            Config config = new Config();
            SolrClient Solr = config.solr;

            //Preparing the Solr document
            /*SolrInputDocument doc = new SolrInputDocument();
            doc.setField("test_s", createDataSize(50000));


            //Deleting the documents from Solr
           //
            //
            // Solr.deleteByQuery("*");
            //Solr.deleteByQuery("id:ResponseCompareTemplate--484826313");
            //Solr.deleteByQuery("id:\"ResponseCompareTemplate-1412732366\"");
            //Solr.deleteByQuery("id:\"ResponseCompareTemplate--491986321" +"\"");
            //Solr.deleteByQuery("id:ResponseCompareTemplate-1069006985");
    *//*        Solr.deleteByQuery("id:" +
                    "\"-1763741525\"");
            Solr.deleteByQuery("id:" +
                    "\"-484826313\"");*//*
            //Saving the document

                Solr.add(doc);
            Solr.add(doc);*/
            //Solr.deleteByQuery("collection_s:order-processor-jan-18 AND eventType_s:HTTPRequest");
            Solr.deleteByQuery("id:\"HTTPRequest-restsql/update-req-2019-11-20T14:40:36Z\"");
            Solr.commit();
        } catch (Exception e) {
            System.out.println("Error in saving document to solr " + e.getMessage());
        }

        System.out.println("Documents deleted");
    }


}

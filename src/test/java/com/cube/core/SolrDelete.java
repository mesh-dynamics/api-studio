package com.cube.core;

import java.io.IOException;
import java.util.UUID;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;

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

            SolrQuery solrQuery = new SolrQuery("*:*");
            solrQuery.addFilterQuery("id:TemplateSet-1463391485");
            Solr.query(solrQuery).getResults().forEach(solrDoc -> {
                SolrInputDocument inputDocument = new SolrInputDocument();
                solrDoc.entrySet().stream().forEach(v -> {
                    inputDocument.addField(v.getKey(), v.getValue());
                });

                //System.out.println(inputDocument.getFieldValue("id"));
                inputDocument.addField("template_id_ss" , "RequestMatch-1703379751");
                //inputDocument.setField("customerId_s", "CubeCorp");
                //inputDocument.setField("app_s", "MovieInfo");
                //inputDocument.setField("version_s", "6879ea75-2d74-4d77-a607-daab439cf7f8");
                //inputDocument.setField("id", "AttributeTemplate-" + UUID.randomUUID().toString());
                inputDocument.removeField("_version_");

                try {
                    Solr.add(inputDocument);
                    System.out.println("document Added");
                } catch (SolrServerException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

               /* {
                    "id":"AttributeTemplate-111396919",
                    "attribute_rule_template_s":
                    "{\"attributeRuleMap\":{\"/timestamp\":{\"path\":\"/timestamp\",\"dt\":\"Default\",\"pt\":\"Default\",\"ct\":\"Ignore\",\"em\":\"Default\",\"customization\":null,\"arrayCompKeyPath\":null}}}",
                        "app_s":"random",
                    "customerId_s":"ravivj",
                    "type_s":"AttributeTemplate",
                    "version_s":"e58f3865-bc01-40cb-8b13-1ad4795c15ac",
                    "_version_":1678415374979170304
                },*/
            });

            //Preparing the Solr document
            /*SolrInputDocument doc = new SolrInputDocument();
            doc.setField("test_s", createDataSize(50000));


            //Deleting the documents from Solr
            //Saving the document
            Solr.add(doc);*/
            //Solr.deleteByQuery("id:\"HTTPRequest-restsql/update-req-2019-11-20T14:40:36Z\"");
            Solr.commit();
        } catch (Exception e) {
            System.out.println("Error in saving document to solr " + e.getMessage());
        }

        System.out.println("Task done");
    }


}

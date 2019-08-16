package com.cube.core;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;

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
        String urlString = "http://18.191.135.125:8983/solr/cube";
        SolrClient Solr = new HttpSolrClient.Builder(urlString).build();

        //Preparing the Solr document
        SolrInputDocument doc = new SolrInputDocument();
        doc.setField("test_s", createDataSize(50000));


        //Deleting the documents from Solr
       //
        //
        // Solr.deleteByQuery("*");
        //Solr.deleteByQuery("id:ResponseCompareTemplate--484826313");
        //Solr.deleteByQuery("id:\"ResponseCompareTemplate-1412732366\"");
        //Solr.deleteByQuery("id:\"ResponseCompareTemplate--491986321" +"\"");
        //Solr.deleteByQuery("id:ResponseCompareTemplate-1069006985");
/*        Solr.deleteByQuery("id:" +
                "\"-1763741525\"");
        Solr.deleteByQuery("id:" +
                "\"-484826313\"");*/
        //Saving the document

        try {
            Solr.add(doc);
        } catch (Exception e) {
            System.out.println("Error in saving document to solr of type " + doc.get("id") + " " + e.getMessage());
        }

        Solr.add(doc);

        Solr.commit();
        System.out.println("Documents deleted");
    }


}

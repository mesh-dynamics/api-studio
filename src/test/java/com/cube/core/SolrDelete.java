package com.cube.core;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
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

                Solr.add(doc);
            Solr.add(doc);

            Solr.commit();
        } catch (Exception e) {
            System.out.println("Error in saving document to solr " + e.getMessage());
        }

        System.out.println("Documents deleted");
    }


}

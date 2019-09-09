package com.cube.core;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;

import java.io.IOException;

public class SolrEdit {
    public static void main(String args[]) throws IOException, SolrServerException {
        //Preparing the Solr client
        String urlString = "http://18.191.135.125:8983/solr/cube";
        SolrClient Solr = new HttpSolrClient.Builder(urlString).build();

        SolrQuery query = new SolrQuery("*:*");
        query.addFilterQuery("collection_s:fluentd-test-23");
        Solr.query(query).getResults().forEach(doc ->{
            SolrInputDocument inputDocument = new SolrInputDocument();
            doc.entrySet().stream().forEach(v -> {
                inputDocument.addField(v.getKey(), v.getValue());
            });

            inputDocument.setField("status_s", "Completed");
            inputDocument.removeField("_version_");
            try {
                Solr.add(inputDocument);
                Solr.commit();
            } catch (SolrServerException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}

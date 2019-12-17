package com.cube;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;

public class ToggleReplayMetaState {
    public static void main(String[] args) {
        String urlString = "http://18.191.135.125:8983/solr/cube";
        SolrClient Solr = new HttpSolrClient.Builder(urlString).build();
        String replayId = "ReplayMeta-paawan_09_10_01-1295243d-d07b-4639-9a2c-d5b76bdb5d41";
        String query = "id:" + replayId;
        SolrQuery solrQuery = new SolrQuery(query);
        try {
            QueryResponse response = Solr.query(solrQuery);
            SolrDocumentList documents  = response.getResults();
            if (documents.size() > 0) {
                SolrDocument result = documents.get(0);
                System.out.println(result.getFieldValue("status_s"));
                SolrInputDocument inputDocument = new SolrInputDocument();
                result.entrySet().stream().forEach(v -> {inputDocument.addField(v.getKey() ,v.getValue());});
                inputDocument.setField("status_s" , "Completed");
                Solr.add(inputDocument);
                Solr.commit();
            }
        } catch (Exception e) {
        }
    }

}

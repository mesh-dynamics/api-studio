package com.cube.core;

import java.util.regex.Pattern;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.cube.agent.GsonPatternDeserializer;
import net.dongliu.gson.GsonJava8TypeAdapterFactory;

import com.cube.serialize.GsonSolrDocumentListSerializer;

public class GsonSerializationTest {

    public static void main(String[] args){
        try {
            Gson gson = new GsonBuilder().registerTypeAdapterFactory(new GsonJava8TypeAdapterFactory())
                .registerTypeAdapter(Pattern.class, new GsonPatternDeserializer()).registerTypeAdapter(SolrDocumentList.class,
                    new GsonSolrDocumentListSerializer()).create();
            String urlString = "http://18.191.135.125:8983/solr/cube";
            SolrClient Solr = new HttpSolrClient.Builder(urlString).build();

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

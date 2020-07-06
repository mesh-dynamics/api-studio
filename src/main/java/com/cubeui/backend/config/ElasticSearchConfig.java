package com.cubeui.backend.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticSearchConfig {

  @Value("${elasticsearch.host}")
  public String host;
  @Value("${elasticsearch.port}")
  public int port;

  @Bean
  public RestHighLevelClient client(){
    return new RestHighLevelClient(
        RestClient.builder(
            new HttpHost(host, port, "http")));
  }

}

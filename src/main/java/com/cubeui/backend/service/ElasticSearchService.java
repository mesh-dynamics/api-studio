package com.cubeui.backend.service;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.ResponseEntity.status;

import com.cubeui.backend.domain.DTO.LogStoreDTO;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
public class ElasticSearchService {

  @Autowired
  RestHighLevelClient client;

  public ResponseEntity postLogData(String customerId, LogStoreDTO data) {

    try {
      XContentBuilder builder = XContentFactory.jsonBuilder()
          .startObject()
          .field("customerId", customerId)
          .field("app", data.app)
          .field("instance", data.instance)
          .field("service", data.service)
          .field("version", data.version)
          .field("sourceType", data.sourceType)
          .field("logMessage", data.logMessage)
          .endObject();
      IndexRequest request = new IndexRequest(customerId.toLowerCase()).source(builder);
      IndexResponse response = client.index(request, RequestOptions.DEFAULT);
      return ResponseEntity.ok(response);
    } catch (IOException e) {
      log.error(String.format("Error while posting the data to elastic search, message=%s", e.getMessage()));
      return status(INTERNAL_SERVER_ERROR).body(e);
    }
  }

}

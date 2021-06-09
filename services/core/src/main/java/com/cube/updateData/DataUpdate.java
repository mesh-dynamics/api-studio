package com.cube.updateData;

import com.cube.dao.SolrIterator;
import com.cube.ws.Config;
import java.io.IOException;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;

public class DataUpdate {
  private static final Logger LOGGER = LogManager.getLogger(DataUpdate.class);
  public static void main (String[] args) throws Exception {
    Config  config = new Config();
    SolrIterator.setConfig(config);

    SolrQuery query = new SolrQuery("*:*");
    query.addFilterQuery("((type_s:Event) OR (type_s:ReplayMeta))");
    query.addFilterQuery("*:* NOT runId_s:*");

    SolrIterator.getStream(config.solr, query, Optional.empty(), Optional.empty()).parallel().forEach(doc -> {
      try {
        updateDoc(doc, config.solr);
      } catch (IOException | SolrServerException e) {
        LOGGER.error(String.format("Error while updating the solr doc  for id %s", doc.get("id")), e.getMessage());
      }
    });

  }

  private static void updateDoc(SolrDocument entry, SolrClient solr) throws IOException, SolrServerException {
    String value = ((String) entry.getFieldValue("type_s")).equals("Event") ?
        (String) entry.get("traceId_s") :(String) entry.get("replayId_s");
    entry.setField("runId_s", value);
    entry.remove("_version_");
    SolrInputDocument doc = new SolrInputDocument();
    entry.forEach((key, val) -> doc.setField(key, val));
    solr.add(doc);
    solr.commit(false, true, true);
  }

}

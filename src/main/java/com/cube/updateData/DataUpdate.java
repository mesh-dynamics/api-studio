package com.cube.updateData;

import com.cube.dao.SolrIterator;
import com.cube.ws.Config;
import io.md.core.Utils;
import io.md.utils.CommonUtils;
import java.io.IOException;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.MDHttpSolrClient;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;

public class DataUpdate {
  public static void main (String[] args) throws Exception {
    Config  config = new Config();
    SolrIterator.setConfig(config);

    SolrQuery query = new SolrQuery("*:*");
    query.addFilterQuery("((type_s:Event) OR (type_s:ReplayMeta))");
    query.addFilterQuery("*:* NOT runId_s:*");

    SolrIterator.getStream(config.solr, query, Optional.empty(), Optional.empty()).parallel().forEach(doc -> {
      try {
        updateDoc(doc, config.solr);
      } catch (IOException e) {
        e.printStackTrace();
      } catch (SolrServerException e) {
        e.printStackTrace();
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

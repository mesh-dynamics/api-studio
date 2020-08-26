package org.apache.solr.client.solrj.impl;


import io.md.df.utils.CommonUtils;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.solr.client.solrj.ResponseParser;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.util.NamedList;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.util.List;
import java.util.Map;

public  class MDHttpSolrClient extends HttpSolrClient {

    public MDHttpSolrClient(HttpSolrClient.Builder builder) {
        super(builder);
    }

    protected NamedList<Object> executeMethod(HttpRequestBase method
        , final ResponseParser processor, boolean isV2Api) throws SolrServerException {

        MultivaluedMap<String, String> mdTraceHeaders = new MultivaluedHashMap<>();
        CommonUtils.injectContext(mdTraceHeaders);
        for (Map.Entry<String, List<String>> entry : mdTraceHeaders.entrySet()) {
            for (String entValue : entry.getValue()) {
                method.addHeader(entry.getKey(), entValue);
            }
        }

        return super.executeMethod(method, processor, isV2Api);
    }


    public static class Builder extends HttpSolrClient.Builder {
        public Builder(String solrUrl) {
            super(solrUrl);
        }

        public MDHttpSolrClient build() {
            if (this.baseSolrUrl == null) {
                throw new IllegalArgumentException("Cannot create HttpSolrClient without a valid baseSolrUrl!");
            } else {
                return (MDHttpSolrClient) (this.invariantParams.get("delegation") == null ? new MDHttpSolrClient(this)
                    : new DelegationTokenHttpSolrClient(this));
            }
        }
    }
}

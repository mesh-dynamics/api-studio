/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

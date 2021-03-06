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

package com.cube.core;

import static io.md.constants.Constants.DEFAULT_TEMPLATE_VER;
import static io.md.core.TemplateKey.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.util.ClientUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import io.md.core.CompareTemplate;
import io.md.core.TemplateEntry;
import io.md.core.TemplateKey;

import com.cube.dao.ReqRespStore;
import com.cube.ws.Config;

public class TestPrefixPathQuerySolr {

    private String customerId = "ravivj";
    private String appId = "cube";
    private String serviceName  = "as";

    /**
     * Cache invalidation might become tricky if we allow such keys, maybe we can just invalidate
     * the entire cache after any update
     */

    /**
     * First construct compare-template entries in solr with increasing length of paths.
     * For paths which are not the entire key path , add /* at the end of path key
     * The only difference is the path field in the single template entry object of each compare template
     * , which reflects the length of path in the template key
     * @param pathElements
     * @param reqRespStore
     * @param objectMapper
     * @return
     */
    private String createTemplateEntriesForTest(List<String> pathElements,
                                              ReqRespStore reqRespStore,
                                              ObjectMapper objectMapper) {
        StringBuffer path = new StringBuffer();

        var countWrapper = new Object() {int count = 0;};
        pathElements.forEach(pathElem ->
        {
            path.append((path.length() != 0) ? "/" : "");
            path.append(pathElem);
            String keyPath = path.toString();
            if (countWrapper.count != pathElements.size() - 1) {
                keyPath = keyPath.concat("/*");
            }
            TemplateKey templateKey = new TemplateKey(DEFAULT_TEMPLATE_VER, "ravivj" , "cube"
                    , "as" , keyPath , Type.RequestMatch);
            CompareTemplate template = new CompareTemplate();
            // adding a single template entry to the comparator. The path field of the entry
            // contains the length of the template key path  (which is used to store this entire template)
            template.addRule(new TemplateEntry("/" + ++countWrapper.count, CompareTemplate.DataType.Str ,
                    CompareTemplate.PresenceType.Optional , CompareTemplate.ComparisonType.Ignore));
            try {
                String templateAsJson = objectMapper.writeValueAsString(template);
                reqRespStore.saveCompareTemplate(templateKey , templateAsJson);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            catch (CompareTemplate.CompareTemplateStoreException e) {
                e.printStackTrace();
            }

        });

        return path.toString();
    }

    /**
     * delete template entries previously constructed from longest to shortest path (in the key)
     * Retrieve compare templates (the query will be against the full path) after each delete
     * and check for the path field in the single template entry object of each compare template.
     * The path should repeatedly reflect the decreased path length against which the full path matches.
     * @param fullPath
     * @param pathElements
     * @param reqRespStore
     * @param Solr
     */
    private void deleteLongestPathRepeatedlyTestForTemplateMatch(
            String fullPath,
            List<String> pathElements , ReqRespStore reqRespStore , SolrClient Solr) {
        TemplateKey searchKey =
                new TemplateKey(DEFAULT_TEMPLATE_VER, "ravivj" , "cube" , "as"
                        , fullPath , Type.RequestMatch);


        ArrayList<String> temp = new ArrayList<>();
        temp.addAll(pathElements);
        while(!temp.isEmpty()) {
            Optional<CompareTemplate> template = reqRespStore.getCompareTemplate(searchKey);
            assert(template.isPresent());
            Collection<TemplateEntry> templateEntries = template.get().getRules();
            assert(templateEntries.size() == 1);
            // checking here that the path against which the full path matched
            // decreases in length with each successive delete
            assert(templateEntries.iterator().next().path.equals("/" + temp.size()));
            String pathToDelete = temp.stream().collect(Collectors.joining("/"));
            if (temp.size() != pathElements.size()) {
                pathToDelete = pathToDelete.concat("/*");
            }
            try {
                String deleteQuery = "type_s:RequestCompareTemplate AND ".concat("customerid_s:").concat(customerId)
                        .concat(" AND ").concat(" app_s:").concat(appId)
                        .concat(" AND ").concat("service_s:").concat(serviceName)
                        .concat(" AND ").concat("path_s:\"").concat(ClientUtils.escapeQueryChars(StringEscapeUtils.escapeJava(pathToDelete))).concat("\"");
                System.out.println("Delete Query :: " + deleteQuery);
                Solr.deleteByQuery(deleteQuery);
                Solr.commit();
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }

            System.out.println(pathToDelete);
            int lastIndex = temp.size() - 1;
            temp.remove(lastIndex);
        }

    }



    //@Test
    public void testPrefixPathQueryForTemplate(){
        try {
            Config config = new Config();
            ReqRespStore reqRespStore = config.rrstore;
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new Jdk8Module());

            SolrClient Solr = config.solr;


            List<String> pathElements =
                    Arrays.asList(new String[]{"registerTemplate" , "response"
                    , "moveieinfo" , "ravivj" , "productpage" , "productpage"
                    });
            // first create template entries in the backend for each possible prefix of the given path
            String fullPath = createTemplateEntriesForTest(pathElements , reqRespStore , mapper);
            // delete templates previously created (from longest to shortest) and confirm that the
            // full path matches against the longest possible prefix (of all the remaining)
            deleteLongestPathRepeatedlyTestForTemplateMatch(fullPath , pathElements , reqRespStore , Solr);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }


}

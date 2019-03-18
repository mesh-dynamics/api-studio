package com.cube.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cube.cache.AnalysisTemplateCache;
import com.cube.dao.ReqRespStore;
import com.cube.ws.Config;

/**
 * Class to test retrieval of an analysis template (From solr) against a specific
 * customerId, appId, serviceName, path combination
 */
public class TemplateRetrievalTest {


    public void testTemplateCache(){
        try {
            Config config = new Config();
            ReqRespStore rrStore = config.rrstore;
            AnalysisTemplateCache templateCache = new AnalysisTemplateCache(rrStore);
            CompareTemplate template = templateCache.fetchCompareTemplate("1234" , "bookinfo" , "getAllBooks" , "/new/path");
            String[] paths = {"", "/string", "/int", "/float", "/obj", "/rptArr", "/nrptArr"};
            List<String> pathsFound = new ArrayList<>();
            template.getRules().forEach(rule -> pathsFound.add(rule.path));
            Arrays.asList(paths).forEach(path -> assertTrue(pathsFound.contains(path)));
        } catch (Exception e) {
            // the test has failed with an exception
            System.out.println(e.getMessage());
            assertSame(true, false);
        }
    }


}

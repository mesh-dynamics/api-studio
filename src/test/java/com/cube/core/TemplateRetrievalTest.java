package com.cube.core;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import com.cube.cache.RequestComparatorCache;
import com.cube.cache.TemplateCache;
import com.cube.cache.TemplateKey;
import com.cube.dao.ReqRespStore;
import com.cube.ws.Config;

/**
 * Class to test retrieval of an analysis template (From solr) against a specific
 * customerId, appId, serviceName, path combination
 */
public class TemplateRetrievalTest {


    //TemplateKey{customerId=ravivj, appId=movieinfo, serviceId=movieinfo, path=minfo/returnmovie, type=Response}
    //
    //TemplateKey{customerId=ravivj, appId=movieinfo, serviceId=productpage, path=productpage, type=Response}

    //@Test
    public void testTemplateCache(){
        try {
            Config config = new Config();
            ReqRespStore rrStore = config.rrstore;
            TemplateCache templateCache = new TemplateCache(rrStore, config);
            CompareTemplate template = templateCache.fetchCompareTemplate(new TemplateKey
                    (Optional.empty(), "ravivj" , "movieinfo" , "movieinfo" , "minfo/returnmovie"
                            , TemplateKey.Type.Response));

            template.getRules().forEach(rule -> System.out.println(rule.path));

//            String[] paths = {"", "/string", "/int", "/float", "/obj", "/rptArr", "/nrptArr"};
//            List<String> pathsFound = new ArrayList<>();
//            template.getRules().forEach(rule -> pathsFound.add(rule.path));
//            Arrays.asList(paths).forEach(path -> assertTrue(pathsFound.contains(path)));
        } catch (Exception e) {
            //e.printStackTrace();
            // the test has failed with an exception
            System.out.println(e.getMessage());
            assertSame(true, false);
        }
    }

    //@Test
    public void testRequestComparatorCache() {
        try {
            Config config = new Config();
            ReqRespStore rrStore = config.rrstore;
            TemplateCache templateCache = new TemplateCache(rrStore ,config);
            ObjectMapper objectMapper = config.jsonMapper;
            RequestComparatorCache requestComparatorCache = new RequestComparatorCache(templateCache , objectMapper);
            TemplateKey key = new TemplateKey(Optional.empty(), "ravivj" , "movieinfo"
                    , "productpage" , "productpage" , TemplateKey.Type.Request);
            RequestComparator comparator = requestComparatorCache.getRequestComparator(key , true);
            assertSame(comparator.getCTapp().toString() , "Equal") ;
            assertSame(comparator.getCTcollection().toString() , "Equal") ;
            assertSame(comparator.getCTcustomerid().toString() , "Equal") ;
            assertSame(comparator.getCTpath().toString() , "Equal");
            assertSame(comparator.getCTreqid().toString() , "EqualOptional");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            assertSame(true , false);
        }
    }



}

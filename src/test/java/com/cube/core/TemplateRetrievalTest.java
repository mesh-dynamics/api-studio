package com.cube.core;

import static org.junit.jupiter.api.Assertions.assertSame;

import com.cube.cache.TemplateCache;
import com.cube.cache.TemplateKey;
import com.cube.dao.ReqRespStore;
import com.cube.utils.Constants;
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
                    (Constants.DEFAULT_TEMPLATE_VER, "ravivj" , "movieinfo" , "movieinfo" , "minfo/returnmovie"
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


}

package com.cube.learning;

import com.cube.dao.AnalysisMatchResultQuery;
import com.cube.dao.ReqRespStoreSolr.ReqRespResultsWithFacets;
import com.cube.ws.Config;
import java.util.List;
import javax.ws.rs.core.MultivaluedHashMap;
import org.junit.jupiter.api.Test;

class CompareTemplatesLearnerTest {

//    @Test
    void learnCompareTemplates() throws Exception {
        final Config config = new Config();

        // This replayId is for MovieInfo and only available in dev cluster.
        String replayId = "71672422-fc1a-4b71-acf1-de776841eee9-4ac32854-e7bc-4d49-9278-3bcc285b57a2";

        AnalysisMatchResultQuery analysisMatchResultQuery = new AnalysisMatchResultQuery(replayId,
            new MultivaluedHashMap<>());

        ReqRespResultsWithFacets resultWithFacets = config.rrstore
            .getAnalysisMatchResults(analysisMatchResultQuery);

        CompareTemplatesLearner ctLearner = new CompareTemplatesLearner();

        List<TemplateEntryMeta> finalMetaList = ctLearner.learnCompareTemplates(
            resultWithFacets.result.getObjects());

        System.out.print(config.jsonMapper
            .writerWithDefaultPrettyPrinter()
            .writeValueAsString(finalMetaList));

    }
}

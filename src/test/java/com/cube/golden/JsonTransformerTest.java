package com.cube.golden;

import com.cube.ws.Config;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

class JsonTransformerTest {

    @Test
    void transformResponse() throws IOException {
        Config config = null;
        try {
            config = new Config();
        } catch (Exception e) {
            e.printStackTrace();
        }

        JsonTransformer jsonTransformer = new JsonTransformer(config.jsonMapper);

        // response from golden collection
        String body1 = "[{\"actors_lastnames\":[\"SWANK\",\"HOPKINS\",\"SINATRA\",\"MANSFIELD\",\"ZELLWEGER\"]," +
            "\"display_actors\":[\"NATALIE HOPKINS\",\"GROUCHO SINATRA\",\"ED MANSFIELD\",\"JOE SWANK\"],\"film_id\":552,\"title\":\"MAJESTIC FLOATS\",\"actors_firstnames\":[\"JOE\",\"NATALIE\",\"GROUCHO\",\"ED\",\"JULIA\"],\"film_counts\":[25,32,26,32,16],\"timestamp\":7059275658430054,\"book_info\":{\"reviews\":[{\"rating\":{\"color\":\"black\",\"stars\":5},\"reviewer\":\"Reviewer1\",\"text\":\"An extremely entertaining play by Shakespeare. The slapstick humour is refreshing!\"},{\"rating\":{\"color\":\"black\",\"stars\":4},\"reviewer\":\"Reviewer2\",\"text\":\"Absolutely fun and entertaining. The play lacks thematic depth when compared to other plays by Shakespeare.\"}],\"id\":\"552\"}}]";

        // response from replayed collection
        String body2 = "[{\"display_actors\":[\"NATALIE HOPKINS\",\"GROUCHO SINATRA\",\"ED MANSFIELD\",\"JOE " +
            "SWANK\"],\"film_id\":552,\"title\":\"MAJESTIC FLOATS\",\"actors_firstnames\":[\"JOE\",\"NATALIE\"," +
            "\"GROUCHO\",\"ED\",\"JULIA\"],\"film_counts\":[25,32,26,32,16],\"timestamp\":7059391601149060," +
            "\"book_info\":{\"reviews\":[{\"rating\":{\"color\":\"black\",\"stars\":5},\"testkey\":\"testval\", " +
            "\"reviewer\":\"Reviewer1\",\"text\":\"An extremely entertaining play by Shakespeare. The slapstick humour is refreshing!\"},{\"rating\":{\"color\":\"black\",\"stars\":4},\"reviewer\":\"Reviewer2\",\"text\":\"Absolutely fun and entertaining. The play lacks thematic depth when compared to other plays by Shakespeare.\"}],\"id\":\"552\"}}]";

        // update operations list
        List<ReqRespUpdateOperation> operationsList = Arrays.asList(
            new ReqRespUpdateOperation(OperationType.REMOVE, "/0/actors_lastnames"),
            new ReqRespUpdateOperation(OperationType.REPLACE, "/0/timestamp"),
            new ReqRespUpdateOperation(OperationType.ADD, "/0/book_info/reviews/0/testkey")
        );

        // transform the response by applying the update operations
        JsonNode recRoot = config.jsonMapper.readTree(body1);
        JsonNode replayRoot = config.jsonMapper.readTree(body2);

        JsonNode transformedResponseToBeStored = jsonTransformer.transform(recRoot,
            replayRoot, operationsList);
        String bodyExpected = "[{\"display_actors\":[\"NATALIE HOPKINS\",\"GROUCHO SINATRA\",\"ED MANSFIELD\",\"JOE" +
            " " +
            "SWANK\"],\"film_id\":552,\"title\":\"MAJESTIC FLOATS\",\"actors_firstnames\":[\"JOE\",\"NATALIE\"," +
            "\"GROUCHO\",\"ED\",\"JULIA\"],\"film_counts\":[25,32,26,32,16],\"timestamp\":7059391601149060," +
            "\"book_info\":{\"reviews\":[{\"rating\":{\"color\":\"black\",\"stars\":5},\"testkey\":\"testval\", " +
            "\"reviewer\":\"Reviewer1\",\"text\":\"An extremely entertaining play by Shakespeare. The slapstick humour is refreshing!\"},{\"rating\":{\"color\":\"black\",\"stars\":4},\"reviewer\":\"Reviewer2\",\"text\":\"Absolutely fun and entertaining. The play lacks thematic depth when compared to other plays by Shakespeare.\"}],\"id\":\"552\"}}]";

        // compare output with expected
        JSONAssert.assertEquals(bodyExpected, transformedResponseToBeStored.toString(), false);
    }
}

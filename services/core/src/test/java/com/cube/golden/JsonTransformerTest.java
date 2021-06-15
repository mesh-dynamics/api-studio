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

package com.cube.golden;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import com.fasterxml.jackson.databind.JsonNode;

import io.md.dao.ReqRespUpdateOperation;
import io.md.utils.JsonTransformer;

import com.cube.ws.Config;

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
            new ReqRespUpdateOperation(io.md.dao.ReqRespUpdateOperation.OperationType.REMOVE, "/0/actors_lastnames"),
            new ReqRespUpdateOperation(io.md.dao.ReqRespUpdateOperation.OperationType.REPLACE, "/0/timestamp"),
            new ReqRespUpdateOperation(io.md.dao.ReqRespUpdateOperation.OperationType.ADD, "/0/book_info/reviews/0/testkey")
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

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

package io.cube.agent;

import java.io.IOException;
import java.util.Optional;

import io.md.logger.LogMgr;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.md.dao.Analysis;
import io.md.services.Analyzer;
import io.md.utils.CubeObjectMapperProvider;

/*
 * Created by IntelliJ IDEA.
 * Date: 03/09/20
 */
public class ProxyAnalyzer implements Analyzer {

    private static final Logger LOGGER = LogMgr.getLogger(ProxyAnalyzer.class);

    private final CubeClient cubeClient;
    private final ObjectMapper jsonMapper;

    public ProxyAnalyzer() {
        jsonMapper = CubeObjectMapperProvider.getInstance();
        cubeClient = new CubeClient(jsonMapper);
    }

    public void setAuthToken(String authToken) {
        cubeClient.setAuthToken(authToken);
    }

    @Override
    public Optional<Analysis> analyze(String replayId, Optional<String> templateVersion) {
        try {
            return cubeClient.analyze(replayId, templateVersion)
                    .map(UtilException.rethrowFunction(config -> jsonMapper.readValue(config,
                            Analysis.class)));
        } catch (IOException e) {
            LOGGER.error("Exception occurred while analyzing replay : " + replayId, e);
        }
        return Optional.empty();
    }
}

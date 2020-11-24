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
    public Optional<Analysis> analyze(String replayId) {
        try {
            return cubeClient.analyze(replayId)
                    .map(UtilException.rethrowFunction(config -> jsonMapper.readValue(config,
                            Analysis.class)));
        } catch (IOException e) {
            LOGGER.error("Exception occurred while analyzing replay : " + replayId, e);
        }
        return Optional.empty();
    }
}

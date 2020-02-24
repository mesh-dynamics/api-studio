package com.cube.ws;

import com.cube.dao.Replay;
import com.cube.utils.ReplayTypeEnum;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;

public class ReplayWSTest {
    Config config;

    ReplayWS replayWS;


    @BeforeEach
    public void before() throws Exception{
        config = new Config();
        replayWS = new ReplayWS(config);
    }

    /**
     * Simple test to retrieve Replay Object for replayId
     * we can validate customer Id in cubeUi-backend
     */
    @Test
    public void testReplayObjectFromReplayId() {
        config.rrstore.saveReplay(getReplay().get());
        Response response = replayWS.getReplayObjectFromReplayId("TestReplayId");
        Assertions.assertNotNull(response);
        Assertions.assertEquals(200, response.getStatus());

        Assertions.assertTrue(response.getEntity().toString().contains("cubeCorp"));

    }

    private Optional<Replay>  getReplay() {
        Replay replay = new Replay("/rp", "cubeCorp", "MovieInfp", "test",
            "testCollection", "testUserId", new ArrayList<String>(),
             "TestReplayId", false, "templateVersion", Replay.ReplayStatus.Running,
            new ArrayList<String>(), 0, 0, 0, Instant.now(),
            Optional.empty(), new ArrayList<String>(),
            Optional.empty(), Optional.empty(),
            Optional.empty(), ReplayTypeEnum.HTTP);
        return Optional.of(replay);
    }


}

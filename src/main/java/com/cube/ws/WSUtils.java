/*
 *
 *    Copyright Cube I O
 *
 */

package com.cube.ws;

import java.util.Map;
import java.util.Optional;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.cube.dao.ReqRespStore;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-04-01
 * @author Prasad M D
 */
public class WSUtils {

    static public Optional<Response> checkActiveCollection(ReqRespStore rrstore, Optional<String> customerid,
                                                           Optional<String> app, Optional<String> instanceid,
                                                           Optional<String> userId) {
        Optional<ReqRespStore.RecordOrReplay> recordOrReplay = rrstore.getCurrentRecordOrReplay(customerid, app,
            instanceid);
        Optional<String> rrcollection = recordOrReplay.flatMap(rr -> rr.getRecordingCollection());
        Optional<String> replayId = recordOrReplay.flatMap(rr -> rr.getReplayId());
        String runType = recordOrReplay.map(rr -> rr.isRecording() ? "Recording" : "Replay").orElse("None");

        return rrcollection.map(collection -> {
            // TODO: use constant strings from Ashok's PR once its merged
            Map<String, String> respObj = Map.of("message", runType + " ongoing",
                "customer", customerid.orElse("None"),
                "app", app.orElse("None"),
                "instance", instanceid.orElse("None"),
                "collection", collection,
                "replayId", replayId.orElse("None"),
                "userId", userId.orElse("None"));
            return Response.status(Response.Status.CONFLICT)
                .entity(respObj)
                .build();
        });
    }
}

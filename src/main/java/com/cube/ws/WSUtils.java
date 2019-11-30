/*
 *
 *    Copyright Cube I O
 *
 */

package com.cube.ws;

import com.cube.dao.ReqRespStore;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-04-01
 * @author Prasad M D
 */
public class WSUtils {

    static public Optional<Response> checkActiveCollection(ReqRespStore rrstore, Optional<String> customerId,
                                                           Optional<String> app, Optional<String> instanceId,
                                                           Optional<String> userId) {
        Optional<ReqRespStore.RecordOrReplay> recordOrReplay = rrstore.getCurrentRecordOrReplay(customerId, app,
            instanceId);
        Optional<String> rrcollection = recordOrReplay.flatMap(rr -> rr.getRecordingCollection());
        Optional<String> replayId = recordOrReplay.flatMap(rr -> rr.getReplayId());
        String runType = recordOrReplay.map(rr -> rr.isRecording() ? "Recording" : "Replay").orElse("None");

        return rrcollection.map(collection -> {
            // TODO: use constant strings from Ashok's PR once its merged
            Map<String, String> respObj = Map.of("message", runType + " ongoing",
                "customerId", customerId.orElse("None"),
                "app", app.orElse("None"),
                "instance", instanceId.orElse("None"),
                "collection", collection,
                "replayId", replayId.orElse("None"),
                "userId", userId.orElse("None"));
            return Response.status(Response.Status.CONFLICT)
                .type(MediaType.APPLICATION_JSON)
                .entity(respObj)
                .build();
        });
    }

    public static class BadValueException extends Exception {
        public BadValueException(String message) {
            super(message);
        }
    }
}

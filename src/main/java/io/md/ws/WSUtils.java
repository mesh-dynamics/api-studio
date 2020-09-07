/*
 *
 *    Copyright Cube I O
 *
 */

package io.md.ws;

import java.util.Map;
import java.util.Optional;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.md.dao.RecordOrReplay;
import io.md.services.DataStore;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-04-01
 * @author Prasad M D
 */
public class WSUtils {

    static public Optional<Response> checkActiveCollection(DataStore dataStore, String customerId,
                                                           String app, String instanceId,
                                                           Optional<String> userId) {
        Optional<RecordOrReplay> recordOrReplay = dataStore.getCurrentRecordOrReplay(customerId, app,
            instanceId);
        Optional<String> rrcollection = recordOrReplay.flatMap(rr -> rr.getRecordingCollection());
        Optional<String> replayId = recordOrReplay.flatMap(rr -> rr.getReplayId());
        Optional<String> recordingId = recordOrReplay.flatMap(rr -> rr.getRecordingId());
        String runType = recordOrReplay.map(rr -> rr.isRecording() ? "Recording" : "Replay").orElse("None");

        return rrcollection.map(collection -> {
            // TODO: use constant strings from Ashok's PR once its merged
            Map<String, String> respObj = Map.of("message", runType + " ongoing",
                "customerId", customerId,
                "app", app,
                "instance", instanceId,
                "collection", collection,
                "replayId", replayId.orElse("None"),
	            "recordingId", recordingId.orElse("None"),
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

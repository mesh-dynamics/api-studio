/*
 *
 *    Copyright Cube I O
 *
 */

package com.cube.ws;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.SolrPing;
import org.apache.solr.client.solrj.response.SolrPingResponse;

import io.md.dao.RecordOrReplay;
import io.md.services.DataStore;

import com.cube.utils.Constants;

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

    public static Map solrHealthCheck (SolrClient solr) {
	    try {
		    SolrPing solrPing = new SolrPing();
		    SolrPingResponse solrPingResponse = solrPing.process(solr);
		    int status = solrPingResponse.getStatus();
		    String solrStatusMessage = status==0 ? "Solr server up" : "Solr server not working";
		    return Map.of(Constants.SOLR_STATUS_CODE, status, Constants.SOLR_STATUS_MESSAGE, solrStatusMessage);
	    }
	    catch (IOException ioe) {
		    return Map.of(Constants.SOLR_STATUS_CODE, -1, Constants.SOLR_STATUS_MESSAGE, "Unable to reach Solr server", Constants.ERROR, ioe.getMessage());
	    }
	    catch (SolrServerException sse) {
		    return Map.of(Constants.SOLR_STATUS_CODE, -1, Constants.SOLR_STATUS_MESSAGE, "Unable to reach Solr server", Constants.ERROR, sse.getMessage());
	    }
	}

    public static class BadValueException extends Exception {
        public BadValueException(String message) {
            super(message);
        }
    }
}

/*
 *
 *    Copyright Cube I O
 *
 */

package com.cube.ws;

import java.util.Optional;

import javax.ws.rs.core.Response;

import com.cube.dao.ReqRespStore;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-04-01
 * @author Prasad M D
 */
public class WSUtils {

    static public Optional<Response> checkActiveCollection(ReqRespStore rrstore, Optional<String> customerid,
                                                           Optional<String> app, Optional<String> instanceid) {
        Optional<ReqRespStore.RecordOrReplay> recordOrReplay = rrstore.getCurrentRecordOrReplay(customerid, app,
            instanceid);
        Optional<String> rrcollection = recordOrReplay.flatMap(rr -> rr.getRecordingCollection());
        String rrtype = recordOrReplay.map(rr -> rr.isRecording() ? "Recording" : "Replay").orElse("None");
        return rrcollection.map(collection -> Response.status(Response.Status.CONFLICT)
            .entity(String.format("%s ongoing for customer %s, app %s, instance %s, with collection name %s.", rrtype,
                customerid.orElse("None"), app.orElse("None"), instanceid.orElse("None"),
                collection))
            .build());
    }
}

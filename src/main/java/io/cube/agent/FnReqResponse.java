package io.cube.agent;

import java.time.Instant;
import java.util.Optional;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-05-06
 * @author Prasad M D
 */
class FnReqResponse {

    final String customerId;
    final String app;
    final String instanceId;
    final String service;
    final Optional<String> traceid;
    final int fnSignatureHash;
    final String name;
    final Optional<Instant> prevRespTS;
    final Integer[] argsHash;
    final String[] argVals;
    final String retVal;


    public FnReqResponse(String customerId, String app, String instanceId, String service, Optional<String> traceid,
                         int fnSignatureHash, String name, Optional<Instant> prevRespTS, Integer[] argsHash, String[] argVals, String retVal) {
        this.customerId = customerId;
        this.app = app;
        this.instanceId = instanceId;
        this.service = service;
        this.traceid = traceid;
        this.fnSignatureHash = fnSignatureHash;
        this.name = name;
        this.prevRespTS = prevRespTS;
        this.argsHash = argsHash;
        this.argVals = argVals;
        this.retVal = retVal;
    }
}

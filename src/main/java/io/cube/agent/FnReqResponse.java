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
    final int fnSignatureHash;
    final String name;
    final Optional<String> traceId;
    final Optional<String> spanId;
    final Optional<String> parentSpanId;
    final Optional<Instant> respTS;
    final Integer[] argsHash;
    final String[] argVals;
    final String retVal;


    public FnReqResponse(String customerId, String app, String instanceId, String service,
                         int fnSignatureHash, String name, Optional<String> traceId,
                         Optional<String> spanId, Optional<String> parentSpanId, Optional<Instant> respTS,
                         Integer[] argsHash, String[] argVals, String retVal) {
        this.customerId = customerId;
        this.app = app;
        this.instanceId = instanceId;
        this.service = service;
        this.fnSignatureHash = fnSignatureHash;
        this.name = name;
        this.traceId = traceId;
        this.spanId = spanId;
        this.parentSpanId = parentSpanId;
        this.respTS = respTS;
        this.argsHash = argsHash;
        this.argVals = argVals;
        this.retVal = retVal;
    }
}

package io.cube.agent;

import java.time.Instant;
import java.util.Optional;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-05-06
 * @author Prasad M D
 */
public class FnReqResponse {

    public String customerId;
    public String app;
    public String instanceId;
    public String service;
    public  int fnSignatureHash;
    public  String name;
    public  Optional<String> traceId;
    public  Optional<String> spanId;
    public  Optional<String> parentSpanId;
    public  Optional<Instant> respTS;
    public  Integer[] argsHash;
    public  String[] argVals;
    public  String retVal;


    public FnReqResponse() {

    }

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

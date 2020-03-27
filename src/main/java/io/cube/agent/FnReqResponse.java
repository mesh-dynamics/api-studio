package io.cube.agent;

import io.md.dao.FnReqRespPayload;

import java.time.Instant;
import java.util.Optional;



/*
 * Created by IntelliJ IDEA.
 * Date: 2019-05-06
 * @author Prasad M D
 */
public class FnReqResponse {

    public final String customerId;
    public final String app;
    public final String instanceId;
    public final String service;
    public final  int fnSignatureHash;
    public final  String name;
    public final  Optional<String> traceId;
    public final  Optional<String> spanId;
    public final  Optional<String> parentSpanId;
    public final  Optional<Instant> respTS;
    public final  Integer[] argsHash;
    public final  String[] argVals;
    public final  String retOrExceptionVal;
    public final FnReqRespPayload.RetStatus retStatus;
    public final  Optional<String> exceptionType; // the class name of the exception type in case of Exception


    public FnReqResponse(String customerId, String app, String instanceId, String service,
                         int fnSignatureHash, String name, Optional<String> traceId,
                         Optional<String> spanId, Optional<String> parentSpanId, Optional<Instant> respTS,
                         Integer[] argsHash, String[] argVals, String retOrExceptionVal, FnReqRespPayload.RetStatus retStatus,
                         Optional<String> exceptionType) {
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
        this.retOrExceptionVal = retOrExceptionVal;
        this.retStatus = retStatus;
        this.exceptionType = exceptionType;
    }
}

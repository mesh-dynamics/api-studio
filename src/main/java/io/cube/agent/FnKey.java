package io.cube.agent;

import java.lang.reflect.Method;
import java.util.Optional;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-05-09
 * @author Prasad M D
 */
public class FnKey {

    public FnKey(String customerId, String app, String instanceId, String service, Optional<String> traceId, Method function) {
        this.customerId = customerId;
        this.app = app;
        this.instanceId = instanceId;
        this.service = service;
        this.traceId = traceId;

        this.fnName = function.getName();
        this.signature = Utils.getFunctionSignature(function);

        this.fnSigatureHash = signature.hashCode();

    }

    final String customerId;
    final String app;
    final String instanceId;
    final String service;
    final Optional<String> traceId;
    final String signature;
    final int    fnSigatureHash;
    final String fnName;

}

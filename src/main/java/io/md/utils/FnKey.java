package io.md.utils;

import java.lang.reflect.Method;
import io.md.utils.CommonUtils;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-05-09
 * @author Prasad M D
 */
public class FnKey {

    public FnKey(String customerId, String app, String instanceId, String service, Method function) {
        this.customerId = customerId;
        this.app = app;
        this.instanceId = instanceId;
        this.service = service;
        this.function = function;

        this.fnName = function.getName();
        this.signature = CommonUtils.getFunctionSignature(function);

        this.fnSigatureHash = signature.hashCode();

    }

    public final String customerId;
    public final String app;
    public final String instanceId;
    public final String service;
    public final Method function;
    public final String signature;
    public final int    fnSigatureHash;
    public final String fnName;

}

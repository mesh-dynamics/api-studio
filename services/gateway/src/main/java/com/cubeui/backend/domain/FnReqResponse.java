/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cubeui.backend.domain;

import java.time.Instant;
import java.util.Optional;

public class FnReqResponse {

    public enum RetStatus {
        Success,
        Exception
    }

    public String customerId;
    public String app;
    public String instanceId;
    public String service;
    public int fnSignatureHash;
    public String name;
    public Optional<String> traceId;
    public Optional<String> spanId;
    public Optional<String> parentSpanId;
    public Optional<Instant> respTS;
    public Integer[] argsHash;
    public String[] argVals;
    public String retOrExceptionVal;
    public RetStatus retStatus;
    public Optional<String> exceptionType; // the class name of the exception type in case of Exception

    // for jackson deserialization
    public FnReqResponse() {
    }


    public FnReqResponse(String customerId, String app, String instanceId, String service,
                         int fnSignatureHash, String name, Optional<String> traceId,
                         Optional<String> spanId, Optional<String> parentSpanId, Optional<Instant> respTS,
                         Integer[] argsHash, String[] argVals, String retOrExceptionVal, RetStatus retStatus,
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

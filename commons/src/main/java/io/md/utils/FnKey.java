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

package io.md.utils;

import java.lang.reflect.Method;

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

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

package io.cube.agent;

import io.md.dao.FnReqRespPayload;

import java.time.Instant;
import java.util.Optional;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-05-17
 * @author Prasad M D
 */
public class FnResponseObj {

    final public Object retVal;
    final public Optional<Instant> timeStamp;
    final public FnReqRespPayload.RetStatus retStatus;
    final Optional<String> exceptionType;



    public FnResponseObj(Object retVal, Optional<Instant> timeStamp, FnReqRespPayload.RetStatus retStatus, Optional<String> exceptionType) {
        this.retVal = retVal;
        this.timeStamp = timeStamp;
        this.retStatus = retStatus;
        this.exceptionType = exceptionType;
    }
}

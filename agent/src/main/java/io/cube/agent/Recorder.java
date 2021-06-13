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

import java.util.Optional;

import io.md.dao.Event;
import io.md.dao.FnReqRespPayload.RetStatus;
import io.md.utils.FnKey;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-05-03
 * @author Prasad M D
 */
public interface Recorder {


    /**
     * @param fnKey               The object storing the function key, that will not change on each
     *                            invocation of the function
     * @param args                The arg values
     * @param responseOrException The return value or the exception value
     * @param retStatus           Success or exception
     * @param exceptionType       Type of exception if any
     * @return success status
     */
    boolean record(FnKey fnKey,
        Object responseOrException,
        RetStatus retStatus,
        Optional<String> exceptionType,
        String runId,
        Object... args);

    boolean record(ReqResp httpReqResp);

    boolean record(Event event);
}
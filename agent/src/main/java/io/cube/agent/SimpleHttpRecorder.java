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

import com.google.gson.Gson;

import io.md.dao.Event;


/*
 * Created by IntelliJ IDEA.
 * Date: 2019-05-06
 * @author Prasad M D
 */
public class SimpleHttpRecorder extends AbstractGsonSerializeRecorder {

    private CubeClient cubeClient;

    public SimpleHttpRecorder() throws Exception {
        super();
        this.cubeClient = new CubeClient(jsonMapper);
    }

    @Override
    public boolean record(FnReqResponse fnReqResponse) {
        Optional<String> cubeResponse = cubeClient.storeFunctionReqResp(fnReqResponse);
        return true;
    }

    @Override
    public boolean record(Event event) {
        Optional<String> cubeResponse = cubeClient.storeEvent(event);
        return true;
    }


    @Override
    public boolean record(ReqResp httpReqResp) {
        Optional<String> cubeResponse = cubeClient.storeSingleReqResp(httpReqResp);
        return true;
    }
}

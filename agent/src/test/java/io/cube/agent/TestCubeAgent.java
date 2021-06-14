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

import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.md.dao.Event;
import io.md.utils.FnKey;
import net.dongliu.gson.GsonJava8TypeAdapterFactory;

public class TestCubeAgent {


    public static void main(String[] args) {
        Gson gson = new GsonBuilder().registerTypeAdapterFactory(new GsonJava8TypeAdapterFactory())
                .registerTypeAdapter(Pattern.class, new GsonPatternDeserializer()).create();
        try {
            ConsoleRecorder recorder = ConsoleRecorder.getInstance();
            //SimpleMocker mocker = new SimpleMocker(gson);
            Class classzz = CubeClient.class;
            FnKey fnKey = new FnKey("ravivj", "movieinfo", "dev", "movieinfo"
                    , classzz.getMethod("getMockResponseEvent", Event.class));
//            recorder.record(fnKey , Optional.of("trace_3"), Optional.of("span_3") , Optional.of("parentSpan_3")
//                    , "Simple Response 3" , new Object[] {"1" , "5"});

//            System.out.println(Instant.ofEpochSecond(1557489786L).toString());

            /*Object returnVal = mocker.mock(fnKey, Optional.of("trace_1") , Optional.empty() , Optional.empty() ,
                    Optional.of(Instant.ofEpochSecond(1557489555)), Optional.empty(), new Object[] {"1" , "5"});
            System.out.println(returnVal.toString());*/
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

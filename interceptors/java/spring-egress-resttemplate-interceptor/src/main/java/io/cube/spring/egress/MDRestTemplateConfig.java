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

package io.cube.spring.egress;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.cube.agent.CommonConfig;
import io.cube.agent.IntentResolver;
import io.cube.agent.TraceIntentResolver;
import io.md.utils.CubeObjectMapperProvider;

@Component
public class MDRestTemplateConfig {

	public IntentResolver intentResolver = new TraceIntentResolver();
	
	public final ObjectMapper jsonMapper = CubeObjectMapperProvider.getInstance();

	//Needed to initialize, so that the  Recorder (ConsoleRecorder/ProxyBatchRecorder) is also init,
	//and the disruptor is already ready
	private final CommonConfig configInstance = CommonConfig.getInstance();

}

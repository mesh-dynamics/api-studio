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

package io.cube.agent.samplers;

import java.time.Instant;
import java.util.Optional;

import javax.ws.rs.core.MultivaluedMap;

public class TimeSampler extends Sampler {

	public static final String TYPE = "time";
	private int samplingTime;
	private int delay;

	public TimeSampler(int samplingTime, int delay) {
		this.samplingTime = samplingTime;
		this.delay = delay;
	}

	@Override
	public Optional<String> getFieldCategory() {
		return Optional.empty();
	}

	public static Sampler create(Float samplingTime, int delay) {
		return new TimeSampler(samplingTime.intValue(), delay);
	}

	@Override
	public boolean isSampled(MultivaluedMap<String, String> input) {
		long currentTime = Instant.now().toEpochMilli();
		return (currentTime % delay < samplingTime);

	}
}

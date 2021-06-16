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

import java.util.Optional;
import java.util.Random;

import javax.ws.rs.core.MultivaluedMap;

import io.cube.agent.Utils;

public class SimpleSampler extends Sampler {
	public static final String TYPE = "simple";
	public static final int DEFAULT_SAMPLING_RATE = 1;
	public static final int DEFAULT_SAMPLING_ACCURACY = 10000;

	private final float samplingRate;
	private final Random rnd;

	@Override
	public String toString() {
		return "SimpleSampler{" +
			"samplingRate=" + samplingRate +
			'}';
	}

	public static Sampler create(float samplingRate, int samplingAccuracy) {
		Optional<Sampler> sampler = Utils.getConstSamplerIfValid(samplingRate, samplingAccuracy);
		return sampler.orElse(new SimpleSampler(samplingRate));
	}

	public SimpleSampler(float samplingRate) {
		this.samplingRate = samplingRate;
		this.rnd = new Random();
	}

	@Override
	public Optional<String> getFieldCategory() {
		return Optional.empty();
	}

	@Override
	public boolean isSampled(MultivaluedMap<String, String> samplingInputs) {
		return rnd.nextDouble() <= samplingRate;
	}
}

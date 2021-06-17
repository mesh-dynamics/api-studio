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

import javax.ws.rs.core.MultivaluedMap;

public abstract class Sampler {

	public static final Sampler NEVER_SAMPLE = new Sampler() {
		@Override
		public String toString() {
			return "NEVER_SAMPLE";
		}

		@Override
		public Optional<String> getFieldCategory() {
			return Optional.empty();
		}

		@Override
		public boolean isSampled(MultivaluedMap<String, String> samplingParams) {
			return false;
		}
	};

	public static final Sampler ALWAYS_SAMPLE = new Sampler() {
		@Override
		public String toString() {
			return "ALWAYS_SAMPLE";
		}

		@Override
		public Optional<String> getFieldCategory() {
			return Optional.empty();
		}

		@Override
		public boolean isSampled(MultivaluedMap<String, String> samplingParams) {
			return true;
		}
	};

	public abstract Optional<String> getFieldCategory();

	//input could be a map of headers, query params, apiPath and any other
	//request or response parameter. Keeping it generic by taking in a map.
	public abstract boolean isSampled(MultivaluedMap<String, String> input);
}

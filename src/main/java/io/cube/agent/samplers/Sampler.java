package io.cube.agent.samplers;

import javax.ws.rs.core.MultivaluedMap;

public abstract class Sampler {

	public static final Sampler NEVER_SAMPLE = new Sampler() {
		@Override
		public boolean isSampled(MultivaluedMap<String, String> samplingParams) {
			return false;
		}
	};

	public static final Sampler ALWAYS_SAMPLE = new Sampler() {
		@Override
		public boolean isSampled(MultivaluedMap<String, String> samplingParams) {
			return true;
		}
	};

	public abstract boolean isSampled (MultivaluedMap
		<String, String> samplingParams);
}

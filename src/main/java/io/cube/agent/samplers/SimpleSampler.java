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

	public static Sampler create(float samplingRate, int samplingAccuracy) {
		Optional<Sampler> sampler = Utils.getSampler(samplingRate, samplingAccuracy);
		return sampler.orElse(new SimpleSampler(samplingRate));
	}

	public SimpleSampler(float samplingRate) {
		this.samplingRate = samplingRate;
		this.rnd = new Random();
	}

	@Override
	public String getSamplingID() {
		return null;
	}

	@Override
	public boolean isSampled(MultivaluedMap<String, String> samplingInputs) {
		return rnd.nextDouble() <= samplingRate;
	}
}

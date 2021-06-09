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

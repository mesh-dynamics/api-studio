package io.cube.agent.samplers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.ws.rs.core.MultivaluedMap;

import io.cube.agent.Utils;

/**
 * This sampler is appropriate for high-traffic instrumentation (ex edge web servers that each
 * receive >100K requests)
 *
 * <h3>Implementation</h3>
 *
 * <p>
 * Taken from <a href= "https://github.com/openzipkin/zipkin-java/blob/traceid-sampler/
 * zipkin/src/main/java/zipkin/BoundarySampler.java">Zipkin project</a>
 * </p>
 * <p>This uses modulo samplingAccuracy arithmetic, which allows a minimum probability of
 * 1/samplingAccuracy.
 */

public class BoundarySampler extends Sampler {

	public static final String TYPE = "boundary";

	public static Sampler create(float samplingRate, int samplingAccuracy,
		String samplingID, List<String> samplingParams) {
		Optional<Sampler> sampler = Utils.getSampler(samplingRate, samplingAccuracy);
		return sampler.orElse(
			new BoundarySampler(samplingID, (long) (samplingRate * samplingAccuracy),
				samplingAccuracy, samplingParams));
	}

	private List<String> samplingParams;
	private final long boundary;
	private final int samplingAccuracy;
	private final String samplingID;

	@Override
	public String getSamplingID() {
		return samplingID;
	}

	public BoundarySampler(String samplingID, long boundary, int samplingAccuracy,
		List<String> samplingParams) {
		this.samplingID = samplingID;
		this.boundary = boundary;
		this.samplingAccuracy = samplingAccuracy;
		this.samplingParams = samplingParams;
	}

	private List<String> getSamplingStrings(MultivaluedMap<String, String> samplingInputs) {
		List<String> samplingStrings = new ArrayList<>();
		samplingParams.stream().forEach(field -> {
			List<String> values = samplingInputs.get(field);
			if (values != null) {
				samplingStrings.addAll(values);
			}
		});
		Collections.sort(samplingStrings);
		return samplingStrings;
	}

	@Override
	public boolean isSampled(MultivaluedMap<String, String> samplingInputs) {
		List<String> samplingStrings = getSamplingStrings(samplingInputs);
		long hashId = Math.abs(Objects.hash(samplingStrings.toArray()));
		return hashId % samplingAccuracy <= boundary;
	}
}

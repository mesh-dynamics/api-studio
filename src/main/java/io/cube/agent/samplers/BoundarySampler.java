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
		String fieldCategory, List<String> samplingParams) {
		Optional<Sampler> sampler = Utils.getConstSamplerIfValid(samplingRate, samplingAccuracy);
		return sampler.orElse(
			new BoundarySampler(fieldCategory, (long) (samplingRate * samplingAccuracy),
				samplingAccuracy, samplingParams));
	}

	@Override
	public String toString() {
		return "BoundarySampler{" +
			"samplingParams=" + samplingParams +
			", boundary=" + boundary +
			", samplingAccuracy=" + samplingAccuracy +
			", fieldCategory='" + fieldCategory + '\'' +
			'}';
	}

	private List<String> samplingParams;
	private final long boundary;
	private final int samplingAccuracy;
	private final String fieldCategory;


	public BoundarySampler(String fieldCategory, long boundary, int samplingAccuracy,
		List<String> samplingParams) {
		this.fieldCategory = fieldCategory;
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
	public Optional<String> getFieldCategory() {
		return Optional.of(fieldCategory);
	}

	@Override
	public boolean isSampled(MultivaluedMap<String, String> samplingInputs) {
		List<String> samplingStrings = getSamplingStrings(samplingInputs);
		long hashId = Math.abs(Objects.hash(samplingStrings.toArray()));
		return hashId % samplingAccuracy <= boundary;
	}
}

package io.cube.agent.samplers;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Stream;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang3.tuple.Pair;

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

public class AdaptiveSampler extends Sampler {

	public static final String TYPE = "adaptive";

	public static Sampler create(String samplingID,
		Map<Pair<String, String>, Float> samplingParams) {
		return new AdaptiveSampler(samplingID, samplingParams);
	}

	private Map<Pair<String, String>, Float> samplingParams;
	private final String fieldCategory;
	private Random rnd;

	AdaptiveSampler(String fieldCategory, Map<Pair<String, String>, Float> samplingParams) {
		this.fieldCategory = fieldCategory;
		this.samplingParams = samplingParams;
		this.rnd = new Random();
	}

	@Override
	public Optional<String> getFieldCategory() {
		return Optional.of(fieldCategory);
	}


	private Optional<Float> getSamplingRate(MultivaluedMap<String, String> samplingInputs) {
		Optional<Float> samplingRate = Optional.empty();
		for (Map.Entry<Pair<String, String>, Float> entry : samplingParams.entrySet()) {
			samplingRate = Stream
				.ofNullable(samplingInputs.get(entry.getKey().getLeft())).flatMap(
					Collection::stream
				).flatMap(val -> {
					String samplingValue = entry.getKey().getRight();
					if (samplingValue.equalsIgnoreCase(val) || samplingValue
						.equalsIgnoreCase("other")) {
						return Stream.of(entry.getValue());
					}
					return Stream.empty();
				}).findFirst();
			if (samplingRate.isPresent()) break;
		}

		return samplingRate;
	}

	@Override
	public boolean isSampled(MultivaluedMap<String, String> samplingInputs) {
		Optional<Float> samplingRate = getSamplingRate(samplingInputs);
		return rnd.nextDouble() <= samplingRate.orElse(-1.0f);
	}
}

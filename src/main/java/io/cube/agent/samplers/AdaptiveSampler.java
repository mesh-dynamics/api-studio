package io.cube.agent.samplers;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

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

	public static Sampler create(String samplingID, int samplingAccuracy,
		MultivaluedMap<String, Pair<String, Float>> samplingParams) {
		return new AdaptiveSampler(samplingID, samplingAccuracy, samplingParams);
	}

	private MultivaluedMap<String, Pair<String, Float>> samplingParams;
	private final String samplingID;
	private final int samplingAccuracy;
	private Random rnd;
	private float[] samplingRate = new float[1];

	AdaptiveSampler(String samplingID, int samplingAccuracy,
		MultivaluedMap<String, Pair<String, Float>> samplingParams) {
		this.samplingID = samplingID;
		this.samplingAccuracy = samplingAccuracy;
		this.samplingParams = samplingParams;
		this.rnd = new Random();
	}

	@Override
	public String getSamplingID() {
		return samplingID;
	}


	private float getSamplingRate(MultivaluedMap<String, String> samplingInputs) {
		for (Map.Entry<String, List<Pair<String, Float>>> entry : samplingParams.entrySet()) {
			Optional<Float> samplingRate = entry.getValue().stream()
				.filter(cv -> samplingInputs.get(entry.getKey())
					.stream()
					.anyMatch(inp -> cv.getLeft().equalsIgnoreCase(inp) || cv.getLeft()
						.equalsIgnoreCase("other")))
				.findFirst()
				.map(Pair::getRight);
			if (samplingRate.isPresent()) {
				return samplingRate.get();
			}
		}

		return -1.0f;
	}

	@Override
	public boolean isSampled(MultivaluedMap<String, String> samplingInputs) {
		float samplingRate = getSamplingRate(samplingInputs);
		return rnd.nextDouble() <= samplingRate;
	}
}

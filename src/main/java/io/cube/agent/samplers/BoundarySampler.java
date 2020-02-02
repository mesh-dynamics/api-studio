package io.cube.agent.samplers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import javax.ws.rs.core.MultivaluedMap;

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
 * <p>This uses modulo 10000 arithmetic, which allows a minimum probability of 0.01%.
 */

public class BoundarySampler extends Sampler {

	public static final String TYPE = "boundary";
	private static float samplingRateValue;

	public static Sampler create(float samplingRate, List<String> samplingParams) {
		if (samplingRate == 0) {
			return Sampler.NEVER_SAMPLE;
		}
		if (samplingRate == 1.0) {
			return Sampler.ALWAYS_SAMPLE;
		}
		if (samplingRate < 0.0001f || samplingRate > 1.0) {
			throw new IllegalArgumentException(
				"The sampling rate must be between 0.0001 and 1.0");
		}
		long boundary = (long) (samplingRate * 10000);
		samplingRateValue = samplingRate;
		return new BoundarySampler(boundary, samplingParams);
	}

	private List<String> samplingParams;
	private final long boundary;

	public BoundarySampler(long boundary, List<String> samplingParams) {
		this.boundary = boundary;
		this.samplingParams = samplingParams;
	}

	private List<String> getSamplingStrings(MultivaluedMap<String, String> headers) {
		List<String> samplingStrings = new ArrayList<>();
		samplingParams.stream().forEach(field -> {
			List<String> values = headers.get(field);
			if (values != null) {
				samplingStrings.addAll(values);
			}
		});
		Collections.sort(samplingStrings);
		return samplingStrings;
	}

	@Override
	public boolean isSampled(MultivaluedMap<String, String> headers) {
		List<String> samplingStrings = getSamplingStrings(headers);
		if (samplingStrings.isEmpty()) { //avoid this. Use CountingSampler instead.
			Random random = new Random(System.currentTimeMillis());
			return random.nextDouble() > samplingRateValue;
		}
		long hashId = Math.abs(Objects.hash(samplingStrings.toArray()));
		return hashId % 10000 <= boundary;
	}
}

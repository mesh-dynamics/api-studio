package io.cube.agent.samplers;

import java.util.BitSet;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import javax.ws.rs.core.MultivaluedMap;

/**
 * This sampler is appropriate for low-traffic instrumentation (ex servers that each
 * receive <100K requests). The sampling decision isn't idempotent.
 *
 * <h3>Implementation</h3>
 *
 * <p>
 * Taken from <a href= "https://github.com/openzipkin/zipkin-java/blob/traceid-sampler/
 * zipkin/src/main/java/zipkin/CountingTraceIdSampler.java">Zipkin project</a>
 * </p>
 *
 * <p>
 * This counts to see how many out of 100 requests should be retained. This means that it is
 * accurate in units of 100 requests.
 *
 */

public class CountingSampler extends Sampler {
	public static final String TYPE = "counting";
	public static final String DEFAULT_SAMPLING_RATE = "1";

	public static Sampler create(float samplingRate) {
		if (samplingRate == 0) return Sampler.NEVER_SAMPLE;
		if (samplingRate == 1.0) return Sampler.ALWAYS_SAMPLE;
		if (samplingRate < 0.01f || samplingRate > 1.0) {
			throw new IllegalArgumentException(
				"The sampling rate must be between 0.01 and 1.0");
		}

		return new CountingSampler(samplingRate);
	}

	private final AtomicInteger counter;
	private final BitSet sampleDecisions;

	CountingSampler(double samplingRate) {
		this.counter = new AtomicInteger();
		int cardinality = (int) (samplingRate * 100.0f);
		//can further randomize by resetting this.
		this.sampleDecisions = randomBitSet(100, cardinality, new Random());
	}

	@Override
	public boolean isSampled(MultivaluedMap<String, String> samplingParams) {
		synchronized (this) {
			return sampleDecisions.get(counter.getAndIncrement()%100);
		}
	}

	/**
	 * Reservoir sampling algorithm borrowed from Stack Overflow.
	 *
	 * https://stackoverflow.com/questions/12817946/generate-a-random-bitset-with-n-1s
	 * @param size size of the bit set
	 * @param cardinality cardinality of the bit set
	 * @param rnd random generator
	 * @return a random bitset
	 */
	static BitSet randomBitSet(int size, int cardinality, Random rnd) {
		BitSet result = new BitSet(size);
		int[] chosen = new int[cardinality];
		int i;
		for (i = 0; i < cardinality; ++i) {
			chosen[i] = i;
			result.set(i);
		}
		for (; i < size; ++i) {
			int j = rnd.nextInt(i + 1);
			if (j < cardinality) {
				result.clear(chosen[j]);
				result.set(i);
				chosen[j] = i;
			}
		}
		return result;
	}
}

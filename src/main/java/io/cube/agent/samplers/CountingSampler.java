package io.cube.agent.samplers;

import java.util.BitSet;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import javax.ws.rs.core.MultivaluedMap;

import io.cube.agent.Utils;

/**
 * This sampler is appropriate for low-traffic instrumentation (ex servers that each receive <100K
 * requests). The sampling decision isn't idempotent.
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
 */

public class CountingSampler extends Sampler {

	public static final String TYPE = "counting";

	public static Sampler create(float samplingRate, int samplingAccuracy) {
		Optional<Sampler> sampler = Utils.getSampler(samplingRate, samplingAccuracy);
		if (sampler.isPresent()) {
			return sampler.get();
		}

		return new CountingSampler(samplingRate, samplingAccuracy);
	}

	private final AtomicInteger counter;
	private final BitSet sampleDecisions;
	private final int samplingAccuracy;

	CountingSampler(double samplingRate, int samplingAccuracy) {
		this.counter = new AtomicInteger();
		this.samplingAccuracy = samplingAccuracy;
		int cardinality = (int) (samplingRate * samplingAccuracy);
		//can further randomize by resetting this.
		this.sampleDecisions = randomBitSet(samplingAccuracy, cardinality, new Random());
	}

	@Override
	public boolean isSampled(MultivaluedMap<String, String> samplingParams) {
		synchronized (this) {
			return sampleDecisions.get(counter.getAndIncrement() % samplingAccuracy);
		}
	}

	/**
	 * Reservoir sampling algorithm borrowed from Stack Overflow.
	 * <p>
	 * https://stackoverflow.com/questions/12817946/generate-a-random-bitset-with-n-1s
	 *
	 * @param size        size of the bit set
	 * @param cardinality cardinality of the bit set
	 * @param rnd         random generator
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

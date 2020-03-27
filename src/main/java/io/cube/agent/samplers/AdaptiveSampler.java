package io.cube.agent.samplers;

import java.util.Map;
import java.util.Optional;
import java.util.Random;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang3.tuple.Pair;

/**
 * This sampler is appropriate for high-traffic instrumentation (ex edge web servers that each
 * receive >100K requests)
 *
 *
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
			samplingRate =  Optional.ofNullable(samplingInputs.get(entry.getKey().getLeft()))
				.flatMap(vals -> {
					String samplingValue = entry.getKey().getRight();
					//`other` is a special value to denote everything else
					if (samplingValue.equalsIgnoreCase("other"))
						return Optional.of(entry.getValue());
					return vals.stream()
						.filter(samplingValue::equalsIgnoreCase)
						.findFirst()
						.map(v -> entry.getValue());
				});

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

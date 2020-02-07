package io.cube.agent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.cube.agent.samplers.BoundarySampler;
import io.cube.agent.samplers.CountingSampler;
import io.cube.agent.samplers.Sampler;
import io.cube.agent.samplers.SimpleSampler;

public class SamplerTest {

	@Test
	void testSimpleSampler() {
		int count = 0;
		Sampler sampler = SimpleSampler.create(0.4f, 10000);
		//Run 50k requests and check probability criteria
		for (int i=0; i<50000; i++) {
			boolean result = sampler.isSampled(new MultivaluedHashMap<>());
			if (result) {
				count++;
			}
		}

		//System.out.println("Count Value : " + count);
		float percent = count/50000.0f;
		Assertions.assertTrue(percent > 0.35 && percent < 0.45);
	}

	@Test
	void testCountingSampler() {
		int count = 0;
		Sampler sampler = CountingSampler.create(0.4f, 10000);
		//Run 50k requests and check probability criteria
		for (int i=0; i<50000; i++) {
			boolean result = sampler.isSampled(new MultivaluedHashMap<>());
			if (result) {
				count++;
			}
		}

		//System.out.println("Count Value : " + count.get());
		Assertions.assertTrue(count == (int)(0.4 * 50000));
	}

	@Test
	void testBoundarySampler() {
		int count = 0;
		List<String> headerParams = Arrays.asList("sessionId");
		Sampler sampler = BoundarySampler.create(0.4f, 1000, headerParams);
		MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
		List<String> sessionIDs = generateSessionIDs();
		Map<String, Integer> sampledSessionIDs = new HashMap<>();
		//Run 150k requests and check probability criteria
		for (int i=0; i<1500000; i++) {
			String sessionID = sessionIDs.get(i%1500);
			map.add("sessionId", sessionID);
			boolean result = sampler.isSampled(map);
			if (result) {
				count++;
				sampledSessionIDs.put(sessionID, sampledSessionIDs.getOrDefault(sessionID, 0) + 1);
			}
			map.clear();
		}

		//System.out.println("Count Value : " + count.get());
		float percent = count/1500000.0f;
		Assertions.assertTrue(percent > 0.35 && percent < 0.45);
		//check if the sampling is idempotent
		Assertions.assertTrue(sampledSessionIDs.values().stream().distinct().limit(2).count() <= 1);
	}

	private List<String> generateSessionIDs() {
		List<String> sessionIDs = new ArrayList<>();
		for (int i=0; i<1500; i++) {
			sessionIDs.add(UUID.randomUUID().toString());
		}

		return sessionIDs;
	}

}

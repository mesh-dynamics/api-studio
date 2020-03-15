package io.cube.agent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.cube.agent.samplers.AdaptiveSampler;
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
		Sampler sampler = BoundarySampler.create(0.4f, 1000, "headers", headerParams);
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

	@Test
	void testAdaptiveSamplerInvalidField() {
		int count = 0;
		Map<Pair<String,String>,Float> params = new LinkedHashMap<>();
		params.put(new ImmutablePair<>("source", "aaa"), 0.9f);
		params.put(new ImmutablePair<>("source", "other"), 0.3f);
		Sampler sampler = AdaptiveSampler.create("headers", params);
		MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
		List<String> sessionIDs = generateSessionIDs();
		map.add("sessionId", sessionIDs.get(0));
		Assertions.assertTrue(!sampler.isSampled(map));
	}

	@Test
	void testAdaptiveSamplerValidInput() {
		int myCount = 0;
		int otherCount = 0;
		Map<Pair<String,String>,Float> params = new LinkedHashMap<>();
		params.put(new ImmutablePair<>("source", "aaa"), 0.5f);
		params.put(new ImmutablePair<>("source", "other"), 0.3f);
		Sampler sampler = AdaptiveSampler.create("headers", params);
		MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
		List<String> sessionIDs = generateSessionIDs();
		Map<String, Integer> sampledSessionIDs = new HashMap<>();
		//Run 150k requests and check probability criteria
		for (int i=0; i<150000; i++) {
			String sessionID = sessionIDs.get(i%1500);
			if (i%1500 == 0) {
				sessionID = "aaa";
			}
			map.add("source", sessionID);
			boolean result = sampler.isSampled(map);
			if (result) {
				if (sessionID.equalsIgnoreCase("aaa")) {
					myCount++;
				} else {
					otherCount++;
				}
				sampledSessionIDs.put(sessionID, sampledSessionIDs.getOrDefault(sessionID, 0) + 1);
			}
			map.clear();
		}

		//System.out.println("My Count Value : " + myCount + " Other Count Value : " + otherCount);
		float myPercent = myCount/100.0f;
		float otherPercent = otherCount/149900.0f;
		System.out.println("My percent : " + myPercent);
		System.out.println("Other percent : " + otherPercent);
		Assertions.assertTrue(myPercent > 0.4 && myPercent < 0.6);
		Assertions.assertTrue(otherPercent > 0.25 && otherPercent < 0.35);
		//check if the sampling is idempotent
		//Assertions.assertTrue(sampledSessionIDs.values().stream().distinct().limit(2).count() <= 1);
	}

	@Test
	void testAdaptiveSamplerOtherInput() {
		int count = 0;
		Map<Pair<String,String>,Float> params = new LinkedHashMap<>();
		params.put(new ImmutablePair<>("source", "aaa"), 0.9f);
		params.put(new ImmutablePair<>("source", "other"), 0.3f);
		Sampler sampler = AdaptiveSampler.create("headers", params);
		MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
		List<String> sessionIDs = generateSessionIDs();
		Map<String, Integer> sampledSessionIDs = new HashMap<>();
		//Run 150k requests and check probability criteria
		for (int i=0; i<150000; i++) {
			String sessionID = sessionIDs.get(i%1500);
			map.add("source", sessionID);
			boolean result = sampler.isSampled(map);
			if (result) {
				count++;
				sampledSessionIDs.put(sessionID, sampledSessionIDs.getOrDefault(sessionID, 0) + 1);
			}
			map.clear();
		}

		//System.out.println("Count Value : " + count);
		float percent = count/150000.0f;
		Assertions.assertTrue(percent > 0.25 && percent < 0.35);
		//check if the sampling is idempotent
		//Assertions.assertTrue(sampledSessionIDs.values().stream().distinct().limit(2).count() <= 1);
	}

	private List<String> generateSessionIDs() {
		List<String> sessionIDs = new ArrayList<>();
		for (int i=0; i<1500; i++) {
			sessionIDs.add(UUID.randomUUID().toString());
		}

		return sessionIDs;
	}

}

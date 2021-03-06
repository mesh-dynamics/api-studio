/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.cube.agent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

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
import io.cube.agent.samplers.TimeSampler;

public class SamplerTest {

	@Test
	public void testSimpleSampler() {
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
	public void testCountingSampler() {
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
	public void testTimeSampler() {
		int count = 0;
		//Init sampler with sampling for 5s every 10s
		Sampler sampler = TimeSampler.create(5000f, 10000);
		//Run 30 requests and check probability criteria
		for (int i=0; i<30; i++) {
			//send a request every 1000ms
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			boolean result = sampler.isSampled(new MultivaluedHashMap<>());
			if (result) {
				count++;
			}
		}

		//System.out.println("Count Value : " + count);
		float percent = count/30.0f;
		Assertions.assertTrue(percent > 0.45 && percent < 0.55);
	}

	@Test
	public void testBoundarySampler() {
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
	public void testAdaptiveSamplerInvalidField() {
		int count = 0;
		Map<Pair<String,Pattern>,Float> params = new LinkedHashMap<>();
		params.put(new ImmutablePair<>("source", Pattern.compile("aaa")), 0.9f);
		params.put(new ImmutablePair<>("source", Pattern.compile("other")), 0.3f);
		Sampler sampler = AdaptiveSampler.create("headers", params);
		MultivaluedMap<String, String> map = new MultivaluedHashMap<>();
		List<String> sessionIDs = generateSessionIDs();
		map.add("sessionId", sessionIDs.get(0));
		Assertions.assertTrue(!sampler.isSampled(map));
	}

	@Test
	public void testAdaptiveSamplerValidInput() {
		int myCount = 0;
		int otherCount = 0;
		Map<Pair<String,Pattern>,Float> params = new LinkedHashMap<>();
		params.put(new ImmutablePair<>("source", Pattern.compile("aaa")), 0.5f);
		params.put(new ImmutablePair<>("source", Pattern.compile("other")), 0.3f);
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
		Assertions.assertTrue(myPercent > 0.3 && myPercent < 0.7);
		Assertions.assertTrue(otherPercent > 0.25 && otherPercent < 0.35);
		//check if the sampling is idempotent
		//Assertions.assertTrue(sampledSessionIDs.values().stream().distinct().limit(2).count() <= 1);
	}

	@Test
	public void testAdaptiveSamplerValidRegex() {
		int myCount = 0;
		int otherCount = 0;
		Map<Pair<String,Pattern>,Float> params = new LinkedHashMap<>();
		params.put(new ImmutablePair<>("apiPath", Pattern.compile("ui/.*")), 1.0f);
		params.put(new ImmutablePair<>("apiPath", Pattern.compile("api/.*")), 1.0f);
		params.put(new ImmutablePair<>("apiPath", Pattern.compile("other")), 0.0f);
		Sampler sampler = AdaptiveSampler.create("apiPath", params);
		MultivaluedMap<String, String> inputMap = new MultivaluedHashMap<>();

		inputMap.add("apiPath", "ui/usermanagement");
		Assertions.assertTrue(sampler.isSampled(inputMap));
		inputMap.clear();

		inputMap.add("apiPath", "api.cs");
		Assertions.assertFalse(sampler.isSampled(inputMap));
		inputMap.clear();

		inputMap.add("apiPath", "app.json");
		Assertions.assertFalse(sampler.isSampled(inputMap));
		inputMap.clear();
	}

	@Test
	public void testAdaptiveSamplerOtherInput() {
		int count = 0;
		Map<Pair<String, Pattern>,Float> params = new LinkedHashMap<>();
		params.put(new ImmutablePair<>("source", Pattern.compile("aaa")), 0.9f);
		params.put(new ImmutablePair<>("source", Pattern.compile("other")), 0.3f);
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

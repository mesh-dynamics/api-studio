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

package com.cubeui.backend;

import com.google.common.util.concurrent.AtomicDouble;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.springframework.http.ResponseEntity;

import io.md.constants.Constants;

public class Utils {

	public static MultivaluedMap<String, String> extractTrailerDataFromHeaders(
		ResponseEntity responseEntity) {
		MultivaluedMap<String, String> trailersMultiValuedMap = new MultivaluedHashMap<>();
		responseEntity.getHeaders().entrySet().stream().filter(entry -> entry.getKey().startsWith(
			Constants.MD_TRAILER_HEADER_PREFIX)).forEach(entry -> {
			String realTrailerKey = entry.getKey()
				.substring(Constants.MD_TRAILER_HEADER_PREFIX.length());
			entry.getValue().forEach(
				value -> trailersMultiValuedMap.add(realTrailerKey, String.valueOf(value)));
		});
		return trailersMultiValuedMap;
	}

	public static void addTrailers(ResponseEntity responseEntity, HttpServletResponse response) {
		MultivaluedMap<String, String> trailersMultiValuedMap = extractTrailerDataFromHeaders(
			responseEntity);
		//Add trailers for GRPC handling
		if (response != null) {
			// "Trailer" headers already set in cubeio
			// https://javaee.github.io/tutorial/servlets014b.html
			response.setTrailerFields(() -> {
				Map<String, String> trailersMap = new HashMap<>();
				for (String key : trailersMultiValuedMap.keySet()) {
					trailersMap.put(key, trailersMultiValuedMap.getFirst(key));
				}
				return trailersMap;
			});
		}
	}

	public static double calculate95CI(List<Double> previousMismatches) {
		if (previousMismatches.size() >= com.cubeui.backend.security.Constants.MIN_ENTRIES_FOR_GAUSSIAN) {
			DoubleSummaryStatistics summaryStatistics = previousMismatches.stream()
					.mapToDouble((x) -> x).summaryStatistics();
			double average = summaryStatistics.getAverage();
			double[] sumOfDiff = {0};
			previousMismatches.forEach(r -> {
				double sq = Math.pow(r.doubleValue() - average, 2);
				sumOfDiff[0] += sq;
			});
			int size = previousMismatches.size();
			double sigma = size <= 1 ? 0 : Math.sqrt(sumOfDiff[0] / (size - 1));
			return  Math.min(average + 2 * sigma, 1);
		}
		return 0;
	}
}

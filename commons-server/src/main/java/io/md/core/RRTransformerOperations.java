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

package io.md.core;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.md.dao.HTTPRequestPayload;
import io.md.dao.JsonDataObj;
import io.md.dao.RRTransformer;

import io.md.utils.Constants;


public class RRTransformerOperations {

	private static final Logger LOGGER = LogManager.getLogger(RRTransformerOperations.class);

	// match request/response for transformation
	// Hypothesis is that most requests don't need to be transformed. Hence, separating the check from actual transformation.
	public static void transformRequest(HTTPRequestPayload req, RRTransformer rrTransformer, ObjectMapper jsonMapper) {
		// headers
		try {
			JSONObject hdrs_xfms = rrTransformer.transforms.getJSONObject("requestTransforms");
			if (hdrs_xfms == null) {
				return;
			}
			for (String key : hdrs_xfms.keySet()) {
			    // header keys stored in cube collections are lower case
			    String lowerCaseKey = key.toLowerCase();
			    List<String> initialVals = req.getHdrs().get(lowerCaseKey);
			    List<String> transformedVals = transform(Optional.ofNullable(initialVals), hdrs_xfms.getJSONArray(key));
			    if (!transformedVals.isEmpty()) {
			        req.put(Constants.HDR_PATH + "/" + lowerCaseKey, new JsonDataObj(transformedVals, jsonMapper));
			        //req.hdrs.put(lowerCaseKey, transformedVals);
                }
			}
			// Calling reparse here so that the injected values in HttpRequestPayload fields are propagated to dataObj
			// req.reParse(); // not needed, directly setting the hdr using payload put api
		} catch (Exception e) {
			if (e != null) {
				LOGGER.error(String.format("Error while transforming request: %s %s", req.toString(), e.toString()));
			}
		}
	}

    private static List<String> transform(Optional<List<String>> input, JSONArray xfms) {

        Set<String> out = new HashSet<>(input.orElse(Collections.emptyList()));

        for (int indx = 0; indx < xfms.length(); ++indx) {
            JSONObject pair = xfms.getJSONObject(indx);
            String src = pair.getString("source").trim();
            String target = pair.getString("target").trim();
            if (src.equals("*")) {
                out.clear();
                out.add(target);
            } else {
                out.remove(src);
                out.add(target);
            }
        }
        return out.stream().collect(Collectors.toList());
    }

}

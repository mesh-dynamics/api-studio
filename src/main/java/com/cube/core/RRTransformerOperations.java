package com.cube.core;

import io.md.dao.RRTransformer;
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

import io.md.dao.HTTPRequestPayload;


public class RRTransformerOperations {
	
	private static final Logger LOGGER = LogManager.getLogger(RRTransformerOperations.class);
	
	// match request/response for transformation
	// Hypothesis is that most requests don't need to be transformed. Hence, separating the check from actual transformation. 
	public static void transformRequest(HTTPRequestPayload req, RRTransformer rrTransformer) {
		// headers
		try {
			JSONObject hdrs_xfms = rrTransformer.transforms.getJSONObject("requestTransforms");
			if (hdrs_xfms == null) {
				return;
			}
			for (String key : hdrs_xfms.keySet()) {
			    // header keys stored in cube collections are lower case
			    String lowerCaseKey = key.toLowerCase();
			    List<String> initialVals = req.hdrs.get(lowerCaseKey);
			    List<String> transformedVals = transform(Optional.ofNullable(initialVals), hdrs_xfms.getJSONArray(key));
			    if (!transformedVals.isEmpty()) {
			        req.hdrs.put(lowerCaseKey, transformedVals);
                }
			}
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

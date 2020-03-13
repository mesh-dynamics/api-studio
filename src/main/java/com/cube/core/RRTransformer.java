package com.cube.core;

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


public class RRTransformer {
	
	private static final Logger LOGGER = LogManager.getLogger(RRTransformer.class);

	// TODO: explore and use wiremock's mustache handler capabilities to match and replace values
	// FIX: is there a way to avoid MultivaluedMaps? Just following Request object here.
	
	// TODO: use ObjectMapper as in Config.java. 
	//       however, we then need to structure this class properly
	
	// types of fields to transform
	// TODO: queryParams, formParams, meta, body
	JSONObject transforms; 
	
	public RRTransformer(JSONObject hdrs_xfmer) {
		this.transforms = hdrs_xfmer;
	}
	
	// match request/response for transformation
	// Hypothesis is that most requests don't need to be transformed. Hence, separating the check from actual transformation. 
	public void transformRequest(HTTPRequestPayload req) {
		// headers
		try {
			JSONObject hdrs_xfms = transforms.getJSONObject("requestTransforms");
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
	
    private List<String> transform(Optional<List<String>> input, JSONArray xfms) {

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

package com.cube.core;

import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.cube.dao.Request;
import com.cube.drivers.Replay;

public class RRTransformer {
	
	private static final Logger LOGGER = LogManager.getLogger(RRTransformer.class);

	// TODO: explore and use wiremock's mustache handler capabilities to match and replace values
	// FIX: is there a way to avoid MultivaluedMaps? Just following Request object here.
	
	// types of fields to transform
	// TODO: qparams, fparams, meta, body
	JSONObject transforms; 
	
	public RRTransformer(JSONObject hdrs_xfmer) {
		this.transforms = hdrs_xfmer;
	}
	
	public void updateTransforms(JSONObject xfms) {
		// overwrites previous key, value pairs on collisions
		for (String key : xfms.keySet()) {
			this.transforms.put(key, xfms.get(key));
		}
	}
	
	// match request/response for transformation
	// Hypothesis is that most requests don't need to be transformed. Hence, separating the check from actual transformation. 
	public boolean transformRequest(Request req) {
		// headers
		boolean transformed = false;
		JSONObject hdrs_xfms = transforms.getJSONObject("requestTransforms");
		for (String key : hdrs_xfms.keySet()) {
			if (req.hdrs.containsKey(key)) {
				transformed = transform(req.hdrs.get(key), hdrs_xfms.getJSONArray(key));
			}
		}
		return transformed;
	}
	
	
	private boolean transform(List<String> input, JSONArray xfms) {
		boolean xfmed = false;
		for (int indx = 0; indx < xfms.length(); ++indx) {
			JSONObject pair = xfms.getJSONObject(indx);
			String src = pair.getString("source").trim();
			for (int i = 0; i < input.size(); ++i) {
				if (src.equals("*") || src.equals(input.get(i))) {
					LOGGER.debug(String.format("Replaced input hdr %s with %s", input.get(i), pair.getString("target")));
					input.set(i, pair.getString("target"));
					xfmed = true;
				}
			}
		}
		return xfmed;
	}
}

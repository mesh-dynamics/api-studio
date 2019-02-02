package com.cube.core;

import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;

import org.json.JSONArray;
import org.json.JSONObject;

import com.cube.dao.Request;

public class RRTransformer {

	// TODO: explore and use wiremock's mustache handler capabilities to match and replace values
	// FIX: is there a way to avoid MultivaluedMaps? Just following Request object here.
	
	// types of fields to transform
	// TODO: qparams, fparams, meta, body
	JSONObject hdrs_xfmer; 
	
	public RRTransformer(JSONObject hdrs_xfmer) {
		this.hdrs_xfmer = hdrs_xfmer;
	}
	
	// match request/response for transformation
	// Hypothesis is that most requests don't need to be transformed. Hence, separating the check from actual transformation. 
	public boolean transformRequest(Request req) {
		// headers
		boolean transformed = false;
		for (String key : hdrs_xfmer.keySet()) {
			if (req.hdrs.containsKey(key)) {
				transformed = transform(req.hdrs.get(key), hdrs_xfmer.getJSONArray(key));
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
					input.set(i, pair.getString("target"));
					xfmed = true;
				}
			}
		}
		return xfmed;
	}
}

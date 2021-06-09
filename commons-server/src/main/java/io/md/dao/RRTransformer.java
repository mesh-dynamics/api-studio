package io.md.dao;

import org.json.JSONObject;

public class RRTransformer {
  // TODO: explore and use wiremock's mustache handler capabilities to match and replace values
  // FIX: is there a way to avoid MultivaluedMaps? Just following Request object here.

  // TODO: use ObjectMapper as in Config.java.
  //       however, we then need to structure this class properly

  // types of fields to transform
  // TODO: queryParams, formParams, meta, body
  public JSONObject transforms;
  public RRTransformer(JSONObject hdrs_xfmer) {
    this.transforms = hdrs_xfmer;
  }

  public  RRTransformer() {
    this.transforms = null;
  }
}

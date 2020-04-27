package io.md.dao;

import org.json.JSONObject;

public class RRTransformer {

  JSONObject transforms;
  public RRTransformer(JSONObject hdrs_xfmer) {
    this.transforms = hdrs_xfmer;
  }

  public  RRTransformer() {
    this.transforms = null;
  }
}

package com.cubeiosample.webservices.rest.jersey;

import java.util.HashMap;

import org.json.JSONObject;;

public class HeaderParams {
  private JSONObject hdrs = null;
    // @HeaderParam("end-user")
    public String user;
    // @HeaderParam("x-request-id") String xreq
    public String xreq;
    // @HeaderParam("x-b3-traceid") String xtraceid,
    public String xtraceid;
    // @HeaderParam("x-b3-spanid")
    public String xspanid;
    // @HeaderParam("x-b3-parentspanid")
    public String xparentspanid;
    // @HeaderParam("x-b3-sampled")
    public String xsampled;
    // @HeaderParam("x-b3-flags")
    public String xflags;
    // @HeaderParam("x-ot-span-context")
    public String xotspan;

    HeaderParams(String user, String req, String traceid, String spanid, String parentspanid,
                 String sampled, String flags, String otspan) {
      hdrs = new JSONObject();
      hdrs.put("user", user);
      hdrs.put("xreq", req);
        this.user = user;
        this.xreq = req;
        this.xtraceid = traceid;
        this.xspanid = spanid;
        this.xparentspanid = parentspanid;
        this.xsampled = sampled;
        this.xflags = flags;
        this.xotspan = otspan;
    }
    
    public String signature() {
      return xtraceid;  // TODO: is this the right signature?
    }
}
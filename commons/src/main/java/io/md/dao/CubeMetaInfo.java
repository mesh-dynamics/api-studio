package io.md.dao;

public class CubeMetaInfo {
    public final String customerId;
    public final String appName;
    public final String instance;
    public final String serviceName;

    public CubeMetaInfo(String customerId, String instance, String appName, String serviceName) {
      this.customerId = customerId;
      this.appName = appName;
      this.instance = instance;
      this.serviceName = serviceName;
    }


}



package io.cube.agent;

public class CubeMetaInfo {
    final String customerId;
    final String appName;
    final String instance;
    final String serviceName;

    public CubeMetaInfo(String customerId, String instance, String appName, String serviceName) {
      this.customerId = customerId;
      this.appName = appName;
      this.instance = instance;
      this.serviceName = serviceName;
    }


}



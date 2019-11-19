package io.cube.agent;

public class CubeMetaInfo {
    final String customerId;
    final String appName;
    final String instanceId;
    final String serviceName;

    public CubeMetaInfo(String customerId, String instanceId, String appName, String serviceName) {
      this.customerId = customerId;
      this.appName = appName;
      this.instanceId = instanceId;
      this.serviceName = serviceName;
    }


}



package io.cube.agent.logger;

public class CubeDeployment {
    public String app;
    public String instance;
    public String service;
    public String customerId;
    public String version;

    public CubeDeployment(String app , String instance , String service , String customerId,  String version ){
        this.app =app ;
        this.instance = instance;
        this.service = service;
        this.customerId = customerId;
        this.version = version;
    }

}

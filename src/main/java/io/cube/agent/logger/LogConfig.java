package io.cube.agent.logger;

import io.cube.agent.CommonConfig;

public class LogConfig {

    /*
    private static String source = "NA";
    public static void init(String s){
        source = s;
    }
    */

    private static LogConfig singleton;
    public static LogConfig getInstance(){
        if(singleton==null){
            synchronized (LogConfig.class){
                if(singleton==null){
                    singleton = new LogConfig();
                }
            }
        }
        return singleton;
    }

    protected CubeDeployment cubeDeployment;
    protected boolean msgPackTransport = true;

    public void update(){
        this.cubeDeployment = new CubeDeployment(CommonConfig.app , CommonConfig.instance , CommonConfig.serviceName , CommonConfig.customerId , CommonConfig.version);
    }

    private LogConfig(){
        this.cubeDeployment = new CubeDeployment(CommonConfig.app , CommonConfig.instance , CommonConfig.serviceName , CommonConfig.customerId , CommonConfig.version);
    }
}

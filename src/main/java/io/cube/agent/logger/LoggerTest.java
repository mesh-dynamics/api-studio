package io.cube.agent.logger;

import io.cube.agent.CommonConfig;
import org.slf4j.Logger;

import java.util.HashMap;

class TestThread extends Thread {
    private static Logger logger = CubeLoggerFactory.getLogger(TestThread.class);
    private int num;

    public TestThread(int num){
        this.num = num;
    }

    public void run(){
        while(true){
            try{
                Thread.sleep((long)(500 * Math.random()));
            }catch(Exception e){
                e.printStackTrace();
            }

            logger.warn("I am thread  "+ num + " "+Math.random());
        }
    }
}

public class LoggerTest {
    public static void main(String[] args){

        //init();

        Logger logger = CubeLoggerFactory.getLogger(LoggerTest.class);

        new TestThread(1).start();
        //new TestThread(2).start();
        //new TestThread(3).start();
        //new TestThread(4).start();

        while(true){

            try{
                Thread.sleep(5000);
            }catch(Exception e){
                e.printStackTrace();
            }

            logger.warn("My name is gk "+Math.random());



        }


    }

    private static void init(){

        //io.md.service.endpoint=http://localhost:8081/api/;student.service.url=http://localhost:8085/;io.md.customer=CubeCorp;io.md.app=CourseApp;io.md.instance=test;io.md.servicename=course1;io.md.pollingconfig.pollserver=true
        HashMap<String , String> ccmMap = new HashMap<>();

        String cloudNameProp = "io.md.cloudname";
        String cloudName = System.getenv(cloudNameProp);
        if(cloudName==null) cloudName = "Default cloud";

        String serviceInstanceProp = "io.md.serviceinstance";
        String serviceInstance = System.getenv(serviceInstanceProp);
        if(serviceInstance==null) serviceInstance = "Default service instance";


        ccmMap.put("io.md.customer", "CubeCorp");
        ccmMap.put("io.md.app", "CourseApp" );
        ccmMap.put("io.md.servicename", "course1");
        ccmMap.put("io.md.instance", "test");

        //ccmMap.put("io.md.authtoken", "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJNZXNoREFnZW50VXNlckBjdWJlY29ycC5pbyIsInJvbGVzIjpbIlJPTEVfVVNFUiJdLCJ0eXBlIjoicGF0IiwiY3VzdG9tZXJfaWQiOjMsImlhdCI6MTU4OTgyODI4NiwiZXhwIjoxOTA1MTg4Mjg2fQ.Xn6JTEIAi58it6iOSZ0G7u2waK6a_c-Elpk_cpWsK9s");
        //ccmMap.put("io.md.authtoken", "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJnYXVyYXYua3Vsc2hyZXNodGhhQG1lc2hkeW5hbWljcy5pbyIsInJvbGVzIjpbIlJPTEVfVVNFUiJdLCJpYXQiOjE2MDA3NzUwMTUsImV4cCI6MTYwOTQxNTAxNX0.KxZ6CvVbNXcwJTA1_7T4zxmgS-t7pn6yTVSBcWEPY90");

        ccmMap.put("io.md.service.endpoint", "http://localhost:8081/api/");

        ccmMap.put(serviceInstanceProp, serviceInstance);
        ccmMap.put(cloudNameProp, cloudName);

        CommonConfig.clientMetaDataMap.putAll(ccmMap);

    }
}

/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.cube.agent.logger;

import io.cube.agent.CommonConfig;
import io.md.logger.LogMgr;
import org.slf4j.Logger;

import java.util.HashMap;

class TestThread extends Thread {
    private static Logger logger = LogMgr.getLogger(TestThread.class);
    private int num;

    public TestThread(int num){
        this.num = num;
    }

    public void run(){
        while(true){
            try{
                Thread.sleep((long)(20000 * 1/*Math.random()*/));
            }catch(Exception e){
                e.printStackTrace();
            }

            logger.warn("I am thread  "+ num + " "+Math.random());
        }
    }
}

public class LoggerTest {
    private static int k = 0;

    public static int getNum(){
        return k++;
    }
    public static void main(String[] args){


        //init();
        System.out.println("Gaurav");

        String tag = CommonConfig.tag;

        Logger logger = LogMgr.getLogger(LoggerTest.class);

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

            logger.warn("My name is gk "+getNum());



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

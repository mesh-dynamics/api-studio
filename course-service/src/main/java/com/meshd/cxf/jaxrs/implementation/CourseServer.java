package com.meshd.cxf.jaxrs.implementation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;

import io.cube.agent.ClientUtils;
import io.cube.apachecxf.ingress.MDLoggingFilter;
import io.cube.apachecxf.ingress.MDTracingFilter;


public class  CourseServer {
    public static void main(String args[]) throws Exception {
        JAXRSServerFactoryBean factoryBean = new JAXRSServerFactoryBean();
        factoryBean.setResourceClasses(CourseRepository.class);
        factoryBean.setResourceProvider(new SingletonResourceProvider(new CourseRepository()));
        factoryBean.setProviders(
            Arrays.asList(new JacksonJaxbJsonProvider(), new MDTracingFilter(), new MDLoggingFilter()));
        factoryBean.setAddress("http://0.0.0.0:8084/");
        Server server = factoryBean.create();

        //Initialize agent

        Map<String, String> ccmMap = new HashMap<>();

        String cloudNameProp = "io.md.cloudname";
        String cloudName = System.getenv(cloudNameProp);
        if(cloudName==null) cloudName = "Default cloud";

        String serviceInstanceProp = "io.md.serviceinstance";
        String serviceInstance = System.getenv(serviceInstanceProp);
        if(serviceInstance==null) serviceInstance = "Default service instance";

        ccmMap.put("io.md.customer", "CubeCorp");
        ccmMap.put("io.md.app", "CourseApp");
        ccmMap.put("io.md.servicename", "course1");
        ccmMap.put("io.md.instance", "prod");
        ccmMap.put("io.md.authtoken", "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJNZXNoREFnZW50VXNlckBjdWJlY29ycC5pbyIsInJvbGVzIjpbIlJPTEVfVVNFUiJdLCJ0eXBlIjoicGF0IiwiY3VzdG9tZXJfaWQiOjMsImlhdCI6MTU4OTgyODI4NiwiZXhwIjoxOTA1MTg4Mjg2fQ.Xn6JTEIAi58it6iOSZ0G7u2waK6a_c-Elpk_cpWsK9s");
        ccmMap.put("io.md.service.endpoint", "https://demo.dev.cubecorp.io/api/");

        ccmMap.put(serviceInstanceProp, serviceInstance);
        ccmMap.put(cloudNameProp, cloudName);
        ClientUtils.initialize(ccmMap);

        System.out.println("Server ready...");
    }
}

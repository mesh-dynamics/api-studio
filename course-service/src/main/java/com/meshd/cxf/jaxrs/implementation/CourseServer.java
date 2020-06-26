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

        // Initialize agent

        String serviceInstanceProp = "io.md.serviceinstance";
        String serviceInstance = System.getenv(serviceInstanceProp);
        if(serviceInstance==null) serviceInstance = "Default service instance";
        String cloudNameProp = "io.md.cloudname";
        String cloudName = System.getenv(cloudNameProp);
        if(cloudName==null) cloudName = "Default cloud";

        Map<String, String> initMap = new HashMap();
        initMap.put(serviceInstanceProp, serviceInstance);
        initMap.put(cloudNameProp, cloudName);
        ClientUtils.initialize(initMap);

        System.out.println("Server ready...");
    }
}

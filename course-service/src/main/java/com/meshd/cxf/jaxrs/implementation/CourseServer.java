package com.meshd.cxf.jaxrs.implementation;

import java.util.List;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;

import com.cube.interceptor.apachecxf.ingress.LoggingFilter;
import com.cube.interceptor.apachecxf.ingress.TracingFilter;


public class  CourseServer {
    public static void main(String args[]) throws Exception {
        JAXRSServerFactoryBean factoryBean = new JAXRSServerFactoryBean();
        factoryBean.setResourceClasses(CourseRepository.class);
        factoryBean.setResourceProvider(new SingletonResourceProvider(new CourseRepository()));
        factoryBean.setProviders(List.of(new JacksonJaxbJsonProvider(), new TracingFilter(), new LoggingFilter()));
//        factoryBean.setAddress("http://localhost:8084/");
        factoryBean.setAddress("http://0.0.0.0:8084/");
        Server server = factoryBean.create();

        System.out.println("Server ready...");
//        Thread.sleep(60 * 1000);
//        System.out.println("Server exiting");
//        server.destroy();
//        System.exit(0);
    }
}

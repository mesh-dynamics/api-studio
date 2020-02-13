package com.baeldung.cxf.jaxrs.implementation;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;


public class RestfulServer {
  public static void main(String args[]) throws Exception {
    JAXRSServerFactoryBean factoryBean = new JAXRSServerFactoryBean();
    factoryBean.setResourceClasses(StudentRepository.class);
    factoryBean.setResourceProvider(new SingletonResourceProvider(new StudentRepository()));
    factoryBean.setProvider(new JacksonJaxbJsonProvider());
    factoryBean.setAddress("http://localhost:8081/");
    Server server = factoryBean.create();

    System.out.println("Server ready...");
//        Thread.sleep(60 * 1000);
//        System.out.println("Server exiting");
//        server.destroy();
//        System.exit(0);
  }
}

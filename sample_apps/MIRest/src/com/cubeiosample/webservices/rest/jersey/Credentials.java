package com.cubeiosample.webservices.rest.jersey;

import java.io.Serializable;

public class Credentials implements Serializable {
  /**
   * 
   */
  //private static final long serialVersionUID = 1L;
  
  String username;
  String password;
  public Credentials() {}
  
  public void setUsername(String u) {
    username = u;
  }
  
}

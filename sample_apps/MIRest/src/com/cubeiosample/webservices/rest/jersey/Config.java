package com.cubeiosample.webservices.rest.jersey;

import java.util.Properties;

import javax.inject.Singleton;

import org.apache.log4j.Logger;


@Singleton
public class Config {

    final static Logger LOGGER = Logger.getLogger(Config.class);
    private static final String CONFFILE = "conf/MIRest.conf";

	final Properties properties;
	
	// TODO: make sure all these flags are also passed through ISTIO yaml files
	// since the conf file is not being loaded correctly.
	    
    // mysql properties
    public String MYSQL_HOST = "sakila2.cnt3lftdrpew.us-west-2.rds.amazonaws.com";  // "localhost";
    public String MYSQL_PORT = "3306";
    public String MYSQL_USERNAME = "cube";
    //private static String MYSQL_PWD = "cubeio";  // local docker host pwd
    public String MYSQL_PWD = "cubeio12";  // AWS RDS pwd

    // restwrapjdbc
    public String RESTWRAPJDBC_URI = "http://restwrapjdbc:8080/restsql";

    // Flags
    public boolean USE_PREPARED_STMTS = true;    
    public boolean USE_KUBE = false;
    
    //public static boolean USE_JDBC_SERVICE = true;
    public boolean GET_BOOK_REVIEWS = false;
    public boolean USE_CACHING = false;
    public boolean USE_TOKEN_AUTHENTICATION = false;
    
    public boolean ADD_TRACING_HEADERS = false;
	
	public Config() {
		LOGGER.info("Creating config");
		properties = new java.util.Properties();
		
		try {
            properties.load(this.getClass().getClassLoader().
                    getResourceAsStream(CONFFILE));
		} catch(Exception eta){
            LOGGER.error(String.format("Not able to load config file %s; using defaults", CONFFILE), eta);
		}
        // mysql properties
        // host, username, pwd
		String host = this.getProperty("MYSQL_HOST");
        MYSQL_HOST = (host == null) ? MYSQL_HOST : host;
        
        String port = this.getProperty("MYSQL_PORT");
        MYSQL_PORT = (port == null) ? MYSQL_PORT : host;
        
        String username = this.getProperty("MYSQL_USERNAME");
        MYSQL_USERNAME = (username == null) ? MYSQL_USERNAME : username;
        
        String pwd = this.getProperty("MYSQL_PWD");
        MYSQL_PWD = (pwd == null) ? MYSQL_PWD : pwd;      
        
		// restwrapjdbc uri
        String restwrapjdbc_uri = this.getProperty("RESTWRAPJDBC_URI");
        RESTWRAPJDBC_URI = (restwrapjdbc_uri == null) ? RESTWRAPJDBC_URI : restwrapjdbc_uri;      
		
		// Flags
		// use jdbc service
        // get book reviews
        
        // use kube
        // USE_KUBE can also be set via the ISTIO yaml config
        String useKube = this.getProperty("USE_KUBE");
        if (useKube == null || !useKube.equalsIgnoreCase("TRUE")) {
        	USE_KUBE = false;
        } else {
        	USE_KUBE = true;
        }
        
        // additional services from bookinfo
        String getBookReviews = this.getProperty("GET_BOOK_REVIEWS");
        if (getBookReviews == null || !getBookReviews.equalsIgnoreCase("TRUE")) {
        	GET_BOOK_REVIEWS = false;
        } else {
        	GET_BOOK_REVIEWS = true;
        }
        
		// use caching
        String useCaching = this.getProperty("USE_CACHING");
        if (useCaching == null || !useCaching.equalsIgnoreCase("TRUE")) {
        	USE_CACHING = false;
        } else {
        	USE_CACHING = true;
        }
        
        String useTokenAuth = this.getProperty("USE_TOKEN_AUTHENTICATION");
        if (useTokenAuth == null || !useTokenAuth.equalsIgnoreCase("TRUE")) {
        	USE_TOKEN_AUTHENTICATION = false;
        } else {
        	USE_TOKEN_AUTHENTICATION = true;
        }
        
        String addHeaders = this.getProperty("ADD_TRACING_HEADERS");
        if (addHeaders == null || !addHeaders.equalsIgnoreCase("TRUE")) {
        	ADD_TRACING_HEADERS = false;
        } else {
        	ADD_TRACING_HEADERS = true;
        }
	}

	public String getProperty(String key)
	{
		String value = null;
		try {
			value = this.properties.getProperty(key);
		} catch (Exception eta) {
			LOGGER.info(String.format("Could not find value for key:%s", key));
		}
		return value;
	}

}


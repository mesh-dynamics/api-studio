package com.cubeiosample.webservices.rest.jersey;

import java.util.Properties;

import javax.inject.Singleton;

import org.apache.log4j.Logger;


@Singleton
public class Config {

    final static Logger LOGGER = Logger.getLogger(Config.class);
    private static final String CONFFILE = "/MIRest.conf";

	final Properties properties;
			
    // mysql properties
    public String MYSQL_HOST = "sakila2.cnt3lftdrpew.us-west-2.rds.amazonaws.com";  // "localhost";
    public String MYSQL_PORT = "3306";
    public String MYSQL_DBNAME = "sakila";
    public String MYSQL_USERNAME = "cube";
    //private static String MYSQL_PWD = "cubeio";  // local docker host pwd
    public String MYSQL_PWD = "cubeio12";  // AWS RDS pwd

    // restwrapjdbc
    public String RESTWRAPJDBC_URI = "http://restwrapjdbc:8080/restsql";

    // Flags
    public boolean USE_KUBE = false;
    
    //public static boolean USE_JDBC_SERVICE = true;
    public boolean GET_BOOK_REVIEWS = false;
    public boolean USE_CACHING = false;
    public boolean USE_TOKEN_AUTHENTICATION = false;
    
    public boolean ADD_TRACING_HEADERS = false;
    
    // Behavioral change flags
    public boolean DISPLAYNAME_LASTFIRST = true;
    public boolean RATINGS_5PT_SCALE = true;
    public int NUM_ACTORS_TO_DISPLAY = 2;
    public boolean CONCAT_BUG = false;
	
	public Config() {
		LOGGER.info("Creating config");
		configureUseKube();
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
        MYSQL_PORT = (port == null) ? MYSQL_PORT : port;
        
        String mysqlDbName = this.getProperty("MYSQL_DBNAME");
        MYSQL_DBNAME = (mysqlDbName == null) ? MYSQL_DBNAME : mysqlDbName;

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
        
        // display name settings
        String displayNameSetting = this.getProperty("DISPLAYNAME_LASTFIRST");
        if (displayNameSetting == null || !displayNameSetting.equalsIgnoreCase("true")) {
        	DISPLAYNAME_LASTFIRST = false;
        } else {
        	DISPLAYNAME_LASTFIRST = true;
        }

        // USE_5PT_SCALE
        String use5PtScale = this.getProperty("RATINGS_5PT_SCALE");
        if (use5PtScale == null || !use5PtScale.equalsIgnoreCase("false")) {
        	RATINGS_5PT_SCALE = true;
        } else {
        	RATINGS_5PT_SCALE = false;
        }
        
        String concatBug = this.getProperty("CONCAT_BUG");
        if (concatBug == null || concatBug.equalsIgnoreCase("true")) {
        	CONCAT_BUG = true;
        } else {
        	CONCAT_BUG = false;
        }

        // NUM_ACTORS_TO_DISPLAY
        String numActorsToDisplay = this.getProperty("NUM_ACTORS_TO_DISPLAY");
        if (numActorsToDisplay != null) {
        	NUM_ACTORS_TO_DISPLAY = Integer.parseInt(numActorsToDisplay);
        } 
        
        if (USE_KUBE) {
        	overrideConfigWithKubeSettings();
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
	
	
    private void configureUseKube() {
    	String useKube = System.getenv("USE_KUBE");
       	LOGGER.debug("use_kube value:" + useKube);
        if (useKube != null && useKube.equalsIgnoreCase("true")) {
    	    USE_KUBE = true;
    	} else {
    	    USE_KUBE = false;
    	}
    }

	
	private void overrideConfigWithKubeSettings() {
		String host = System.getenv("MYSQL_HOST");
		if (host != null) {
			MYSQL_HOST = host;
		}

		String mysqlPort = System.getenv("MYSQL_PORT");
		if (mysqlPort != null) {
			MYSQL_PORT = mysqlPort;
		}
		
		String mysqlDb = System.getenv("MYSQL_DBNAME");
		if (mysqlDb != null) {
			MYSQL_DBNAME = mysqlDb;
		}
		
        String username = System.getenv("MYSQL_USERNAME");
        if (username != null) {
        	MYSQL_USERNAME = username;
        }
        
        String pwd = System.getenv("MYSQL_PWD");
        if (pwd != null) {
        	MYSQL_PWD = pwd;      
        }
        
		// restwrapjdbc uri
        String restwrapjdbc_uri = System.getenv("RESTWRAPJDBC_URI");
        if (restwrapjdbc_uri != null) {
        	RESTWRAPJDBC_URI = restwrapjdbc_uri;
        }
		
        String getBookReviews = System.getenv("GET_BOOK_REVIEWS");
        if (getBookReviews != null) {
        	if (getBookReviews.equalsIgnoreCase("TRUE")) {
        		GET_BOOK_REVIEWS = true;
        	} else {
        		GET_BOOK_REVIEWS = false;
        	}
		}
        
		// use caching
        String useCaching = System.getenv("USE_CACHING");
        if (useCaching != null) {
	        if (useCaching.equalsIgnoreCase("TRUE")) {
	        	USE_CACHING = true;
	        } else {
	        	USE_CACHING = false;
	        }
        }
        
        String useTokenAuth = System.getenv("USE_TOKEN_AUTHENTICATION");
        if (useTokenAuth != null) {
	        if (useTokenAuth.equalsIgnoreCase("TRUE")) {
	        	USE_TOKEN_AUTHENTICATION = true;
	        } else {
	        	USE_TOKEN_AUTHENTICATION = false;
	        }
        }
        
        String addHeaders = System.getenv("ADD_TRACING_HEADERS");
        if (addHeaders != null) {
	        if (addHeaders.equalsIgnoreCase("TRUE")) {
	        	ADD_TRACING_HEADERS = true;
	        } else {
	        	ADD_TRACING_HEADERS = false;
	        }
        }
        
        // display name settings
        String displayNameSetting = System.getenv("DISPLAYNAME_LASTFIRST");
        if (displayNameSetting != null) {
	        if (displayNameSetting.equalsIgnoreCase("true")) {
	        	DISPLAYNAME_LASTFIRST = true;
	        } else {
	        	DISPLAYNAME_LASTFIRST = false;
	        }
        }

        // USE_5PT_SCALE
        String use5PtScale = System.getenv("RATINGS_5PT_SCALE");
        if (use5PtScale != null) {
	        if (use5PtScale.equalsIgnoreCase("true")) {
	        	RATINGS_5PT_SCALE = true;
	        } else {
	        	RATINGS_5PT_SCALE = false;
	        }
        }
        
        String concatBug = System.getenv("CONCAT_BUG");
        if (concatBug != null) {
            if (concatBug.equalsIgnoreCase("true")) {
                CONCAT_BUG = true;
            } else {
                CONCAT_BUG = false;
            }
        }
        // NUM_ACTORS_TO_DISPLAY
        String numActorsToDisplay = System.getenv("NUM_ACTORS_TO_DISPLAY");
        if (numActorsToDisplay != null) {
        	NUM_ACTORS_TO_DISPLAY = Integer.parseInt(numActorsToDisplay);
        } 
	}
	

    public String baseDbUri() {
      	return "jdbc:mysql://" + MYSQL_HOST + ":" + MYSQL_PORT + "/sakila";
    }
      
    public String userName() {
       return MYSQL_USERNAME;
    }
      
    public String passwd() {
       return MYSQL_PWD;
    }

}


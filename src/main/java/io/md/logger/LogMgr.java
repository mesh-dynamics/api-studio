package io.md.logger;

import org.slf4j.Logger;

public class LogMgr {

    /*
     Default Slf4J Log Factory
     */
    private LoggerFactory factory = new Slf4jLoggerFactory();
    private static final LogMgr singleton = new LogMgr();

    public static LogMgr getInstance(){
        return singleton;
    }

    //set different log factory. called by agent to set
    public void setFactory(LoggerFactory loggerFactory){
        this.factory = loggerFactory;
    }

    /*
      Direct Utility method to get Logger from current LoggerFactory
     */
    public static Logger getLogger(Class<?> clazz){

        return singleton.factory.getLogger(clazz);
    }
}

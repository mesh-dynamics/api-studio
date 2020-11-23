package io.md.logger;

import org.slf4j.Logger;

public class LogMgr {

    /*
     Default Slf4J Log Factory
     */
    private volatile LoggerFactory factory = new Slf4jLoggerFactory();
    private static final LogMgr singleton = new LogMgr();
    private static volatile boolean loggerCalled = false;

    public static LogMgr getInstance(){
        return singleton;
    }

    //set different log factory. called by agent to set
    public void setFactory(LoggerFactory loggerFactory){
        if(loggerCalled) throw new UnsupportedOperationException("setFactory should be the first call to logMgr before any logger instantiation");
        this.factory = loggerFactory;
    }

    /*
      Direct Utility method to get Logger from current LoggerFactory
     */
    public static Logger getLogger(Class<?> clazz){
        loggerCalled = true;
        return singleton.factory.getLogger(clazz);
    }
}

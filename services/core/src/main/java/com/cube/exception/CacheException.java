package com.cube.exception;

/**
 * Exception related to retrieving analysis templates from solr
 */
public class CacheException extends Exception {

    public CacheException(String msg , Throwable e) {
        super(msg , e);
    }

    public CacheException(String msg) {
        super(msg);
    }
}

package com.cube.exception;

/**
 * Exception related to retrieving analysis templates from solr
 */
public class CacheException extends Exception {

    public CacheException(String msg , Exception e) {
        super(msg , e);
    }
}

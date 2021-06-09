package com.cube.exception;

public class DataObjException extends Exception {

    public DataObjException() {
        super();
    }

    public DataObjException(String s) {
        super(s);
    }

    public DataObjException(Exception e) {
        super(e);
    }

    public DataObjException(String message, Exception e) {
        super(message , e);
    }
}

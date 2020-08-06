/*
 *
 *    Copyright Cube I O
 *
 */

package com.cube.exception;

/*
 * Created by IntelliJ IDEA.
 * Date: 05/08/20
 */
public class ParameterException extends Exception {
    public ParameterException(String s) {
        super(s);
    }

    public ParameterException(String message, Exception e) {
        super(message , e);
    }

}

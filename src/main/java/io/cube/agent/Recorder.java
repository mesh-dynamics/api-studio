package io.cube.agent;

import java.lang.reflect.Method;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-05-03
 * @author Prasad M D
 */
public interface Recorder {


    /**
     * @param function The function object corresponding to the function being recorded
     * @param args The arg values
     * @param response The return value
     * @return success status
     */
    boolean record(Method function, Object response, Object... args);

}

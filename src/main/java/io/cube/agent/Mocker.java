package io.cube.agent;

import java.lang.reflect.Method;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-05-06
 * @author Prasad M D
 */
public interface Mocker {

    /**
     * @param function The function to mocked
     * @param args The argument values as Java objects
     * @return The response value as Java object
     */
    Object mock(String traceid, Method function, Object... args);

}

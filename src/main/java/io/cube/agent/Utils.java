package io.cube.agent;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.stream.Collectors;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-05-06
 * @author Prasad M D
 */
class Utils {

    static String getFunctionSignature(Method function) {
        String fnName = function.getName();
        String className = function.getDeclaringClass().getName();

        String fullName = className + '.' + fnName;

        return Arrays.stream(function.getGenericParameterTypes()).map(Type::getTypeName)
                .collect(Collectors.joining(",", fullName + "(", ")"));

    }
}

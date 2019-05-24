package io.cube.agent;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.stream.Collectors;

import javax.ws.rs.client.Invocation;

import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;

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

    public static void addTraceHeaders(Tracer tracer, Invocation.Builder requestBuilder, String requestType) {
        Tags.SPAN_KIND.set(tracer.activeSpan(), Tags.SPAN_KIND_CLIENT);
        Tags.HTTP_METHOD.set(tracer.activeSpan(), requestType);
        //Tags.HTTP_URL.set(tracer.activeSpan(), requestBuilder.toString());
        tracer.inject(tracer.activeSpan().context(), Format.Builtin.HTTP_HEADERS, new RequestBuilderCarrier(requestBuilder));
    }

}

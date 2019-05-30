package io.cube.agent;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.ws.rs.client.Invocation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-05-06
 * @author Prasad M D
 */
class Utils {

    private static final String BAGGAGE_INTENT = "intent";
    private static final String INTENT_RECORD = "record";
    private static final String INTENT_MOCK = "mock";
    private static final String NO_INTENT = "normal";

    private static final Logger LOGGER = LogManager.getLogger(Utils.class);

    static String getFunctionSignature(Method function) {
        String fnName = function.getName();
        String className = function.getDeclaringClass().getName();
        String fullName = className + '.' + fnName;
        return Arrays.stream(function.getGenericParameterTypes()).map(Type::getTypeName)
                .collect(Collectors.joining(",", fullName + "(", ")"));

    }

    public static void addTraceHeaders(Invocation.Builder requestBuilder, String requestType) {
        if (GlobalTracer.isRegistered()) {
            Tracer tracer = GlobalTracer.get();
            Tags.SPAN_KIND.set(tracer.activeSpan(), Tags.SPAN_KIND_CLIENT);
            Tags.HTTP_METHOD.set(tracer.activeSpan(), requestType);
            //Tags.HTTP_URL.set(tracer.activeSpan(), requestBuilder.toString());
            if (tracer.activeSpan() != null) {
                Span activeSpan = tracer.activeSpan();
                String currentIntent = activeSpan.getBaggageItem(BAGGAGE_INTENT);
                activeSpan.setBaggageItem(BAGGAGE_INTENT , null);
                tracer.inject(activeSpan.context(),
                        Format.Builtin.HTTP_HEADERS, new RequestBuilderCarrier(requestBuilder));
                activeSpan.setBaggageItem(BAGGAGE_INTENT , currentIntent);
            }
        }
    }

    public static Optional<Span> getCurrentSpan() {
        Optional<Span> currentSpan = Optional.empty();
        if (GlobalTracer.isRegistered()) {
            Tracer tracer = GlobalTracer.get();
            currentSpan =  Optional.ofNullable(tracer.activeSpan());
        }
        return currentSpan;
    }

    public static Optional<String> getCurrentIntentFromScope() {
        Optional<String> currentIntent =  getCurrentSpan().flatMap(span -> Optional.
                ofNullable(span.getBaggageItem(BAGGAGE_INTENT)));
        LOGGER.info("Got intent from trace (in agent) :: " + currentIntent.orElse(" N/A"));
        return currentIntent;
    }

    public static boolean isIntentToRecord() {
        return getCurrentIntentFromScope().orElse("").equalsIgnoreCase(INTENT_RECORD);
    }

    public static boolean isIntentToMock() {
        return getCurrentIntentFromScope().orElse("").equalsIgnoreCase(INTENT_MOCK);
    }

}

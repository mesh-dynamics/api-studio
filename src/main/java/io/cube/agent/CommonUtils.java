package io.cube.agent;

import io.jaegertracing.Configuration;
import io.jaegertracing.internal.JaegerSpanContext;
import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapExtractAdapter;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MultivaluedMap;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-05-06
 * @author Prasad M D
 */
public class CommonUtils {

    private static final String BAGGAGE_INTENT = "intent";
    private static final String INTENT_RECORD = "record";
    private static final String INTENT_MOCK = "mock";
    private static final String NO_INTENT = "normal";

    private static final Logger LOGGER = LogManager.getLogger(CommonUtils.class);

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

    public static Scope startServerSpan(MultivaluedMap<String, String> rawHeaders, String operationName) {
        // format the headers for extraction
        Tracer tracer = GlobalTracer.get();
        final HashMap<String, String> headers = new HashMap<String, String>();
        rawHeaders.forEach((k , v) -> {if (v.size() > 0) {headers.put(k, v.get(0));}});
        Tracer.SpanBuilder spanBuilder;
        try {
            SpanContext parentSpanCtx = tracer.extract(Format.Builtin.HTTP_HEADERS, new TextMapExtractAdapter(headers));
            if (parentSpanCtx == null) {
                spanBuilder = tracer.buildSpan(operationName);
            } else {
                spanBuilder = tracer.buildSpan(operationName).asChildOf(parentSpanCtx);
            }
        } catch (IllegalArgumentException e) {
            spanBuilder = tracer.buildSpan(operationName);
        }
        // TODO could add more tags like http.url
        return spanBuilder.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER).startActive(true);
    }

    public static JaegerTracer init(String service) {
        Configuration.SamplerConfiguration samplerConfig = Configuration.SamplerConfiguration.fromEnv()
                .withType(ConstSampler.TYPE)
                .withParam(1);

        Configuration.ReporterConfiguration reporterConfig = Configuration.ReporterConfiguration.fromEnv()
                .withLogSpans(true);

        Configuration.CodecConfiguration codecConfiguration = Configuration.CodecConfiguration.fromString("B3");

        Configuration config = new Configuration(service)
                .withSampler(samplerConfig)
                .withReporter(reporterConfig).withCodec(codecConfiguration);
        return config.getTracer();
    }

    private static Optional<JaegerSpanContext> getCurrentContext() {
        if (GlobalTracer.isRegistered()) {
            Tracer currentTracer = GlobalTracer.get();
            if (currentTracer.activeSpan() != null && currentTracer.activeSpan().context() != null) {
                return Optional.of((JaegerSpanContext) currentTracer.activeSpan().context());
            } else {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    public static Optional<String> longToStr(long l) {
        try {
            return Optional.of(String.valueOf(l));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static Optional<String> getCurrentTraceId() {
        return getCurrentContext().map(JaegerSpanContext::getTraceId);
    }

    public static Optional<String> getCurrentSpanId() {
        return getCurrentContext().flatMap(jaegerSpanContext ->  longToStr(jaegerSpanContext.getSpanId()));
    }

    public static Optional<String> getParentSpanId() {
        return getCurrentContext().flatMap(jaegerSpanContext ->  longToStr(jaegerSpanContext.getParentId()));
    }

    public static <T> CompletableFuture<List<T>> sequence(List<CompletableFuture<T>> futures) {
        CompletableFuture<Void> allDoneFuture =
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
        return allDoneFuture.thenApply(v ->
                futures.stream().
                        map(future -> future.join()).
                        collect(Collectors.<T>toList())
        );
    }

    public static List<String> getCaseInsensitiveMatches(MultivaluedMap<String , String> mMap
            , String possibleKey) {
        // TODO : use case insensitive maps in all these cases
        String searchKey = StringUtils.removeStart(possibleKey ,"/");
        return mMap.entrySet().stream().filter(entry -> entry.getKey().equalsIgnoreCase(searchKey)).findFirst().map(
                entry -> entry.getValue()).orElse(Collections.emptyList());
    }

    public static Optional<String> findFirstCaseInsensitiveMatch(MultivaluedMap<String,String> mMap, String possibleKey) {
        return getCaseInsensitiveMatches(mMap,possibleKey).stream().findFirst();
    }

    public static Optional<String> getTraceId (MultivaluedMap<String,String> mMap) {
        return findFirstCaseInsensitiveMatch(mMap, CommonConfig.DEFAULT_TRACE_FIELD);
    }

}

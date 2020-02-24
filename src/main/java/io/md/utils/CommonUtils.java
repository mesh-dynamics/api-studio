package io.md.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;
import org.apache.thrift.TBase;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.jaegertracing.Configuration;
import io.jaegertracing.Configuration.CodecConfiguration;
import io.jaegertracing.internal.JaegerSpanContext;
import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.md.constants.Constants;
import io.md.dao.CubeMetaInfo;
import io.md.dao.Event;
import io.md.dao.Event.EventBuilder;
import io.md.dao.Event.RunType;
import io.md.dao.MDTraceInfo;
import io.md.tracer.HTTPHeadersCarrier;
import io.md.tracer.MDGlobalTracer;
import io.md.tracer.MDTextMapCodec;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.Format.Builtin;
import io.opentracing.propagation.TextMapAdapter;
import io.opentracing.tag.Tags;


public class CommonUtils {

	private static final Logger LOGGER = LogManager.getLogger(CommonUtils.class);

	public static <T extends Enum<T>> Optional<T> valueOf(Class<T> clazz, String name) {
		return EnumSet.allOf(clazz).stream().filter(v -> v.name().equals(name))
			.findAny();
	}

	static String getFunctionSignature(Method function) {
		String fnName = function.getName();
		String className = function.getDeclaringClass().getName();
		String fullName = className + '.' + fnName;
		return Arrays.stream(function.getGenericParameterTypes()).map(Type::getTypeName)
			.collect(Collectors.joining(",", fullName + "(", ")"));

	}

	public static void addTraceHeaders(Invocation.Builder requestBuilder, String requestType) {
		if (MDGlobalTracer.isRegistered()) {
			Tracer tracer = MDGlobalTracer.get();

			Span span = null;
			//Added for the JDBC init case, but also to segregate
			//any calls without a span to a default span.
			if (tracer.activeSpan() == null) {
				MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
				span = startServerSpan(headers, "dummy-span");
				Scope scope = activateSpan(span);
			}

			Tags.SPAN_KIND.set(tracer.activeSpan(), Tags.SPAN_KIND_CLIENT);
			Tags.HTTP_METHOD.set(tracer.activeSpan(), requestType);
			//Tags.HTTP_URL.set(tracer.activeSpan(), requestBuilder.toString());
			if (tracer.activeSpan() != null) {
				Span activeSpan = tracer.activeSpan();
				String currentIntent = activeSpan.getBaggageItem(Constants
					.ZIPKIN_HEADER_BAGGAGE_INTENT_KEY);
				activeSpan.setBaggageItem(Constants
					.ZIPKIN_HEADER_BAGGAGE_INTENT_KEY, null);
				tracer.inject(activeSpan.context(),
					Format.Builtin.HTTP_HEADERS, new RequestBuilderCarrier(requestBuilder));
				activeSpan.setBaggageItem(Constants
					.ZIPKIN_HEADER_BAGGAGE_INTENT_KEY, currentIntent);
			}

			if (span != null) {
				span.finish();
			}

		}
	}

	public static void injectContext(MultivaluedMap<String, String> headers) {
		if (MDGlobalTracer.isRegistered()) {
			Tracer tracer = MDGlobalTracer.get();
			Span activeSpan = tracer.activeSpan();
			if (activeSpan != null) {
				Tags.SPAN_KIND.set(activeSpan, Tags.SPAN_KIND_CLIENT);
				tracer.inject(activeSpan.context(),
					Format.Builtin.HTTP_HEADERS, new HTTPHeadersCarrier(headers));
			}
		}
	}

	public static Optional<Span> getCurrentSpan() {
		Optional<Span> currentSpan = Optional.empty();
		if (MDGlobalTracer.isRegistered()) {
			Tracer tracer = MDGlobalTracer.get();
			currentSpan = Optional.ofNullable(tracer.activeSpan());
		}
		return currentSpan;
	}


/*
	public static String getConfigIntent() {
		return CommonConfig.intent;
	}
*/

/*	public static String getCurrentIntent() {
		return getCurrentIntentFromScope().orElse(getConfigIntent());
	}*/

	public static Optional<String> getCurrentIntentFromScope() {
		Optional<String> currentIntent = getCurrentSpan().flatMap(span -> Optional.
			ofNullable(span.getBaggageItem(Constants.ZIPKIN_HEADER_BAGGAGE_INTENT_KEY))).or(() ->
			fromEnvOrSystemProperties(Constants.MD_INTENT_PROP));
		currentIntent.ifPresent(
			intent -> LOGGER.debug(Map.of(Constants.MESSAGE, "Intent from trace", "intent",
				intent)));
		return currentIntent;
	}

/*	public static boolean isIntentToRecord() {
		return getCurrentIntent().equalsIgnoreCase(Constants.INTENT_RECORD);
	}

	public static boolean isIntentToMock() {
		return getCurrentIntent().equalsIgnoreCase(Constants.INTENT_MOCK);
	}*/

	public static Scope activateSpan(Span span) {
		// TODO assuming that a tracer has been registered already with MDGlobalTracer
		Tracer tracer = MDGlobalTracer.get();
		return tracer.scopeManager().activate(span);
	}

	public static Span startClientSpan(String operationName, Map<String, String> tags) {
		Tracer tracer = MDGlobalTracer.get();
		Tracer.SpanBuilder spanBuilder = tracer.buildSpan(operationName);
		tags.forEach(spanBuilder::withTag);
		return spanBuilder.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT).start();
	}

	public static Span startClientChildSpan(String operationName, SpanContext parentContext,
		Map<String, String> tags) {
		LOGGER.info("PARENT SPAN CONTEXT WAS PASSED FROM REQUEST TO RESPONSE");
		Tracer tracer = MDGlobalTracer.get();
		Tracer.SpanBuilder spanBuilder = tracer.buildSpan(operationName).
			asChildOf(parentContext);
		tags.forEach(spanBuilder::withTag);
		return spanBuilder.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT)
			.withTag("runId" , "async-log-10k")
			.start();
	}

	public static Span startClientSpan(String operationName) {
		// TODO assuming that a tracer has been registered already with MDGlobalTracer
		return startClientSpan(operationName, Collections.emptyMap());
	}

	public static Span startServerSpan(MultivaluedMap<String, String> rawHeaders,
		String operationName) {
		// TODO assuming that a tracer has been registered already with MDGlobalTracer
		// format the headers for extraction
		Tracer tracer = MDGlobalTracer.get();
		final HashMap<String, String> headers = new HashMap<String, String>();
		rawHeaders.forEach((k, v) -> {
			if (v.size() > 0) {
				headers.put(k, v.get(0));
			}
		});
		Tracer.SpanBuilder spanBuilder;
		try {
			SpanContext parentSpanCtx = tracer
				.extract(Format.Builtin.HTTP_HEADERS, new TextMapAdapter(headers));
			if (parentSpanCtx == null) {
				spanBuilder = tracer.buildSpan(operationName).ignoreActiveSpan();
			} else {
				spanBuilder = tracer.buildSpan(operationName).asChildOf(parentSpanCtx);
			}
		} catch (IllegalArgumentException e) {
			spanBuilder = tracer.buildSpan(operationName);
		}
		// TODO could add more tags like http.url
		return spanBuilder.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER)
			.withTag("runId" , "async-log-10k")
			.start();
	}

	public static JaegerTracer init(String service) {
		Configuration.SamplerConfiguration samplerConfig = Configuration.SamplerConfiguration
			.fromEnv()
			.withType(ConstSampler.TYPE)
			.withParam(1);

		Configuration.ReporterConfiguration reporterConfig = Configuration.ReporterConfiguration
			.fromEnv()
			.withLogSpans(true);

//		Configuration.CodecConfiguration codecConfiguration = Configuration.CodecConfiguration
//			.fromString("B3");

		Configuration.CodecConfiguration codecConfiguration = new CodecConfiguration().withCodec(
			Builtin.HTTP_HEADERS, MDTextMapCodec.builder().build());

		Configuration config = new Configuration(service)
			.withSampler(samplerConfig)
			.withReporter(reporterConfig).withCodec(codecConfiguration);
		return config.getTracer();
	}

	private static Optional<JaegerSpanContext> getCurrentContext() {
		if (MDGlobalTracer.isRegistered()) {
			Tracer currentTracer = MDGlobalTracer.get();
			if (currentTracer.activeSpan() != null
				&& currentTracer.activeSpan().context() != null) {
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
		return getCurrentContext()
			.flatMap(jaegerSpanContext -> longToStr(jaegerSpanContext.getSpanId()));
	}

	public static Optional<String> getParentSpanId() {
		return getCurrentContext()
			.flatMap(jaegerSpanContext -> longToStr(jaegerSpanContext.getParentId()));
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

	public static List<String> getCaseInsensitiveMatches(MultivaluedMap<String, String> mMap
		, String possibleKey) {
		// TODO : use case insensitive maps in all these cases
		String searchKey = StringUtils.removeStart(possibleKey, "/");
		return mMap.entrySet().stream().filter(entry -> entry.getKey().equalsIgnoreCase(searchKey))
			.findFirst().map(
				entry -> entry.getValue()).orElse(Collections.emptyList());
	}

	public static Optional<String> findFirstCaseInsensitiveMatch(
		MultivaluedMap<String, String> mMap, String possibleKey) {
		return getCaseInsensitiveMatches(mMap, possibleKey).stream().findFirst();
	}

	public static Optional<String> getTraceId (MultivaluedMap<String,String> mMap) {
		return findFirstCaseInsensitiveMatch(mMap, Constants.ZIPKIN_TRACE_HEADER);
	}


	public static JsonObject createPayload(Object responseOrException, Gson gson, Object... args) {
		JsonObject payloadObj = new JsonObject();
		payloadObj.add("args", createArgsJsonArray(gson, args));
		payloadObj.addProperty("response", gson.toJson(responseOrException));
		LOGGER.info(new ObjectMessage(Map.of("function_payload", payloadObj.toString())));
		return payloadObj;
	}

	public static Optional<Event> createEvent(FnKey fnKey, MDTraceInfo mdTraceInfo,
		RunType rrType, Optional<Instant> timestamp, JsonObject payload) {

		EventBuilder eventBuilder = new EventBuilder(fnKey.customerId, fnKey.app,
			fnKey.service, fnKey.instanceId, "NA",
			mdTraceInfo, rrType, timestamp, "NA",
			fnKey.signature, Event.EventType.JavaRequest);
		eventBuilder.setRawPayloadString(payload.toString());
		return eventBuilder.createEventOpt();
	}

	public static JsonArray createArgsJsonArray(Gson gson, Object... argVals) {
		JsonArray argsArray = new JsonArray();
		Arrays.stream(argVals).forEach(arg -> argsArray.add(gson.toJson(arg)));
		return argsArray;
	}

	public static Optional<String> fromEnvOrSystemProperties(String propertyName) {
		return Optional.ofNullable(System.getenv(propertyName)).or(() ->
			Optional.ofNullable(System.getProperty(propertyName)));
	}

	public static CubeMetaInfo cubeMetaInfoFromEnv() throws Exception {
		String customerId = fromEnvOrSystemProperties(Constants.MD_CUSTOMER_PROP)
			.orElseThrow(() -> new Exception("Customer Id Not Found in Env"));
		String instance = fromEnvOrSystemProperties(Constants.MD_INSTANCE_PROP)
			.orElseThrow(() -> new Exception("Instance Id Not Found in Env"));
		String appName = fromEnvOrSystemProperties(Constants.MD_APP_PROP)
			.orElseThrow(() -> new Exception("Cube App Name Not Found in Env"));
		String serviceName = fromEnvOrSystemProperties(Constants.MD_SERVICE_PROP)
			.orElseThrow(() -> new Exception("Cube Service Name Not Found in Env"));
		return new CubeMetaInfo(customerId, instance, appName, serviceName);
	}

	public static MDTraceInfo mdTraceInfoFromContext() {
		return new MDTraceInfo(getCurrentTraceId().orElse(null)
			, getCurrentSpanId().orElse(null), getParentSpanId().orElse(null));
	}

	public static Span startServerSpan(io.md.tracing.thriftjava.Span span, String methodName) {
		Tracer tracer = MDGlobalTracer.get();
		Tracer.SpanBuilder spanBuilder;
		try {
			//span.getBaggage().forEach((x,y) -> {System.out.println("Baggage Key :: " +  x + " :: Baggage Value :: "  +y);});
			JaegerSpanContext parentSpanCtx = new JaegerSpanContext(span.traceIdHigh,
				span.traceIdLow, span.spanId, span.parentSpanId,
				(byte) span.flags);
			parentSpanCtx = parentSpanCtx.withBaggage(span.baggage);
			spanBuilder = tracer.buildSpan(methodName).asChildOf(parentSpanCtx);
		} catch (Exception e) {
			spanBuilder = tracer.buildSpan(methodName);
		}
		// TODO could add more tags like http.url
		return spanBuilder.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER).start();
	}

	//TODO assuming that the name of the field/argument containing will always be span
	//this might again require extra config to indicate the field containing span
	public static String traceIdFromThriftSpan(TBase spanContainingObject) {
		try {
			Class<?> clazz = spanContainingObject.getClass();
			Field field = clazz.getField(
				Constants.THRIFT_SPAN_ARGUMENT_NAME); //Note, this can throw an exception if the field doesn't exist.
			io.md.tracing.thriftjava.Span span = (io.md.tracing.thriftjava.Span) field
				.get(spanContainingObject);
			JaegerSpanContext spanContext = new JaegerSpanContext(span.traceIdHigh, span.traceIdLow,
				span.spanId, span.parentSpanId,
				(byte) span.flags);
			return spanContext.getTraceId();
		} catch (Exception e) {
			return null;
		}
		//spanContainingObject.getFieldValue()
	}

}


package io.md.utils;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import io.md.dao.FnReqRespPayload;
import io.md.dao.MDTraceInfo;
import io.md.tracer.HTTPHeadersCarrier;
import io.md.tracer.MDGlobalTracer;
import io.md.tracer.MDTextMapCodec;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.noop.NoopTracerFactory;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.Format.Builtin;
import io.opentracing.propagation.TextMapAdapter;
import io.opentracing.tag.Tags;


public class CommonUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(CommonUtils.class);

	private static final Tracer NOOPTracer = NoopTracerFactory.create();

	public static <T extends Enum<T>> Optional<T> valueOf(Class<T> clazz, String name) {
		return EnumSet.allOf(clazz).stream().filter(v -> v.name().equals(name))
			.findAny();
	}

	private static final JaegerSpanContext defSpanContext;
	private static final MDTraceInfo defTraceInfo;

	private static final String defTraceIdAsHexStr = strToHexStr(Constants.DEFAULT_TRACE_ID)
		.orElse("");
	private static final String defSpanIdAsHexStr = strToHexStr(Constants.DEFAULT_SPAN_ID)
		.orElse("");
	private static final String defparentSpanIdAsHexStr = strToHexStr(
		Constants.DEFAULT_PARENT_SPAN_ID).orElse("");

	static {
		defSpanContext = new JaegerSpanContext(0L, getDefaultTraceId(),
			getDefaultSpanId(), getDefaultParentSpanId(), (byte) 0);
		defTraceInfo = new MDTraceInfo(defTraceIdAsHexStr, defSpanIdAsHexStr,
			defparentSpanIdAsHexStr);
	}

	static String getFunctionSignature(Method function) {
		String fnName = function.getName();
		String className = function.getDeclaringClass().getName();
		String fullName = className + '.' + fnName;
		return Arrays.stream(function.getGenericParameterTypes()).map(Type::getTypeName)
			.collect(Collectors.joining(",", fullName + "(", ")"));

	}

	public static void addTraceHeaders(HttpRequest requestBuilder, String requestType) {
		if (MDGlobalTracer.isRegistered()) {
			Tracer tracer = MDGlobalTracer.get();

			Span span = null;
			Scope scope = null;
			//Added for the JDBC init case, but also to segregate
			//any calls without a span to a default span.
			if (tracer.activeSpan() == null) {
				MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
				span = startServerSpan(headers, "dummy-span");
				scope = activateSpan(span);
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
					Format.Builtin.HTTP_HEADERS, new RequestCarrier(requestBuilder));
				activeSpan.setBaggageItem(Constants
					.ZIPKIN_HEADER_BAGGAGE_INTENT_KEY, currentIntent);
			}

			if (scope != null) {
				scope.close();
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
			ofNullable(span.getBaggageItem(Constants.ZIPKIN_HEADER_BAGGAGE_INTENT_KEY)));

		if (!currentIntent.isPresent()) {
			currentIntent = fromEnvOrSystemProperties(Constants.MD_INTENT_PROP);
		}

		currentIntent.ifPresent(
			intent -> LOGGER.debug("Intent from trace : ".concat(intent)));
		return currentIntent;
	}


	public static String getDFSuffixBasedOnApp(String key, String app) {
		 if (app != null && app.equalsIgnoreCase("Cube")) {
		 	return key + "-df";
		 } else {
		 	return  key;
		 }
	}

//	public static String getDFSuffixBasedOnApp(String key, String app) {
//		if ("Cube".equalsIgnoreCase(app) && !key.endsWith("-df")) {
//			return key + "-df";
//		} else {
//				if (key.endsWith("-df")) {
//					return key.substring(0, key.length() - 3);
//				} else {
//					return key;
//				}
//		}
//	}

/*	public static boolean isIntentToRecord() {
		return getCurrentIntent().equalsIgnoreCase(Constants.INTENT_RECORD);
	}

	public static boolean isIntentToMock() {
		return getCurrentIntent().equalsIgnoreCase(Constants.INTENT_MOCK);
	}*/


	public static Scope activateSpan(Span span, boolean noop) {
		// TODO assuming that a tracer has been registered already with MDGlobalTracer
		Tracer tracer = noop ? NOOPTracer : MDGlobalTracer.get();
		return tracer.scopeManager().activate(span);
	}

	public static Scope activateSpan(Span span) {
		return activateSpan(span, false);
	}


	public static Span startClientSpan(String operationName, SpanContext parentContext,
		Map<String, String> tags, boolean noop) {
		// TODO assuming that a tracer has been registered already with MDGlobalTracer
		Tracer tracer = noop ? NOOPTracer : MDGlobalTracer.get();
		Tracer.SpanBuilder spanBuilder = tracer.buildSpan(operationName);
		if (parentContext != null) {
			spanBuilder.asChildOf(parentContext);
		}
		if (tags != null) {
			tags.forEach(spanBuilder::withTag);
		}
		return spanBuilder.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT)
			.start();
	}

	public static Span startClientSpan(String operationName) {
		return startClientSpan(operationName, null, null, false);
	}

	public static Span startClientSpan(String operationName, boolean noop) {
		return startClientSpan(operationName, null, null, noop);
	}

	public static Span startClientSpan(String operationName, Map<String, String> tags,
		boolean noop) {
		return startClientSpan(operationName, null, tags, noop);
	}

	public static Span startClientSpan(String operationName, SpanContext parentContext,
		boolean noop) {
		return startClientSpan(operationName, parentContext, null, noop);
	}

	public static Span startClientSpan(String operationName, Map<String, String> tags) {
		return startClientSpan(operationName, null, tags, false);
	}

	public static Span startServerSpan(MultivaluedMap<String, String> rawHeaders,
		String operationName, boolean noop) {
		// TODO assuming that a tracer has been registered already with MDGlobalTracer
		// format the headers for extraction
		Tracer tracer = noop ? NOOPTracer : MDGlobalTracer.get();
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
				spanBuilder = tracer.buildSpan(operationName);
			} else {
				spanBuilder = tracer.buildSpan(operationName).asChildOf(parentSpanCtx);
			}
		} catch (IllegalArgumentException e) {
			spanBuilder = tracer.buildSpan(operationName);
		}
		// TODO could add more tags like http.url
		return spanBuilder.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER)
			.start();
	}

	public static Span startServerSpan(MultivaluedMap<String, String> rawHeaders,
		String operationName) {
		return startServerSpan(rawHeaders, operationName, false);
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

	public static Optional<String> strToHexStr(String str) {
		try {
			return Optional.of(String
				.valueOf(Hex.encodeHex(str.getBytes(StandardCharsets.UTF_8))));
		} catch (Exception ex) {
			return Optional.empty();
		}
	}

	public static Optional<Long> hexStrtoLong(String hexStr) {
		try {
			return Optional.of(Long.parseLong(hexStr, 16));
		} catch (NumberFormatException ex) {
			LOGGER.error("Number format exception", ex);
			return Optional.empty();
		}
	}

	public static long getDefaultTraceId() {
		return hexStrtoLong(defTraceIdAsHexStr).orElse(-1L);
	}

	public static long getDefaultSpanId() {
		return hexStrtoLong(defSpanIdAsHexStr).orElse(-1L);
	}

	public static long getDefaultParentSpanId() {
		return hexStrtoLong(defparentSpanIdAsHexStr).orElse(-1L);
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

	public static Optional<String> getTraceId(MultivaluedMap<String, String> mMap) {
		return findFirstCaseInsensitiveMatch(mMap, Constants.ZIPKIN_TRACE_HEADER);
	}


	public static JsonObject createPayload(Object responseOrException, Gson gson, Object... args) {
		JsonObject payloadObj = new JsonObject();
		payloadObj.add("args", createArgsJsonArray(gson, args));
		payloadObj.addProperty("response", gson.toJson(responseOrException));
		LOGGER.info("function_payload : ".concat(payloadObj.toString()));
		return payloadObj;
	}

	public static Optional<Event> createEvent(FnKey fnKey, MDTraceInfo mdTraceInfo,
		Event.RunType rrType, Optional<Instant> timestamp, FnReqRespPayload payload) {
		String reqId = fnKey.service == null ? "" : fnKey.service
			.concat("-")
			.concat(mdTraceInfo.traceId == null ? "" : mdTraceInfo.traceId)
			.concat("-")
			.concat(UUID.randomUUID().toString());
		Event.EventBuilder eventBuilder = new Event.EventBuilder(fnKey.customerId, fnKey.app,
			fnKey.service, fnKey.instanceId, Constants.NOT_APPLICABLE,
			mdTraceInfo, rrType, timestamp, reqId,
			fnKey.signature, Event.EventType.JavaRequest);
		eventBuilder.setPayload(payload);
		return eventBuilder.createEventOpt();
	}

	public static JaegerSpanContext createDefSpanContext() {
		return defSpanContext;
	}

	public static JsonArray createArgsJsonArray(Gson gson, Object... argVals) {
		JsonArray argsArray = new JsonArray();
		Arrays.stream(argVals).forEach(arg -> argsArray.add(gson.toJson(arg)));
		return argsArray;
	}

	public static Optional<String> fromEnvOrSystemProperties(String propertyName) {
		Optional<String> ret = Optional.ofNullable(System.getenv(propertyName));
		return ret.isPresent() ? ret : Optional.ofNullable(System.getProperty(propertyName));
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
			, getCurrentSpanId().orElse(null), getParentSpanId().orElse(null), getBaggageItems().orElse(null));
	}

	private static Optional<Iterable<Entry<String, String>>> getBaggageItems() {
		return getCurrentContext()
				.map(jaegerSpanContext -> jaegerSpanContext.baggageItems());
	}

	public static MDTraceInfo getDefaultTraceInfo() {
		return defTraceInfo;
	}

	public static MultivaluedMap<String, String> buildTraceInfoMap(String serviceName,
		MDTraceInfo mdTraceInfo, String xRequestId) {
		String cRequestId = serviceName.concat("-")
			.concat(mdTraceInfo.traceId == null ? "" : mdTraceInfo.traceId).concat("-").concat(
				UUID.randomUUID().toString());

		MultivaluedMap<String, String> metaMap = new MultivaluedHashMap<>();
		metaMap.add(Constants.DEFAULT_REQUEST_ID, cRequestId);
		if (mdTraceInfo.traceId != null) {
			metaMap.add(Constants.DEFAULT_TRACE_FIELD, mdTraceInfo.traceId);
		}
		if (mdTraceInfo.spanId != null) {
			metaMap.add(Constants.DEFAULT_SPAN_FIELD, mdTraceInfo.spanId);
		}
		if (mdTraceInfo.parentSpanId != null) {
			metaMap.add(Constants.DEFAULT_PARENT_SPAN_FIELD, mdTraceInfo.parentSpanId);
		}
		if (xRequestId != null) {
			metaMap.add(Constants.X_REQUEST_ID, xRequestId);
		}
		return metaMap;
	}

	public static String getEgressServiceName(URI uri) {
		return uri.getPort() != -1
			? String.join(":", uri.getHost(), String.valueOf(uri.getPort()))
			: uri.getHost();
	}

}


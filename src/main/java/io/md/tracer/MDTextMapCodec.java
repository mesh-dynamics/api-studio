package io.md.tracer;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.jaegertracing.internal.JaegerObjectFactory;
import io.jaegertracing.internal.JaegerSpanContext;
import io.jaegertracing.internal.exceptions.EmptyTracerStateStringException;
import io.jaegertracing.internal.exceptions.MalformedTracerStateStringException;
import io.jaegertracing.internal.exceptions.TraceIdOutOfBoundException;
import io.jaegertracing.internal.propagation.PrefixedKeys;
import io.jaegertracing.spi.Codec;
import io.md.utils.CommonUtils;
import io.opentracing.propagation.TextMap;

public class MDTextMapCodec implements Codec<TextMap> {


	private static final Logger LOGGER = LogManager.getLogger(MDTextMapCodec    .class);

	/**
	 * Key used to store serialized span context representation
	 */
	private static final String SPAN_CONTEXT_KEY = "md-trace-id";

	/**
	 * Key prefix used for baggage items
	 */
	private static final String BAGGAGE_KEY_PREFIX = "mdctx-";

	private final String contextKey;

	private final String baggagePrefix;

	private static final PrefixedKeys keys = new PrefixedKeys();

	/**
	 * Object factory used to construct JaegerSpanContext subclass instances.
	 */
	private final JaegerObjectFactory objectFactory;

	private MDTextMapCodec(Builder builder) {
		this.contextKey = builder.spanContextKey;
		this.baggagePrefix = builder.baggagePrefix;
		this.objectFactory = builder.objectFactory;
	}

	static JaegerSpanContext contextFromString(String value)
		throws MalformedTracerStateStringException, EmptyTracerStateStringException {
		if (value == null || value.equals("")) {
			throw new EmptyTracerStateStringException();
		}

		String[] parts = value.split(":");
		if (parts.length != 4) {
			throw new MalformedTracerStateStringException(value);
		}

		String traceId = parts[0];
		if (traceId.length() > 32 || traceId.length() < 1) {
			throw new TraceIdOutOfBoundException("Trace id [" + traceId + "] length is not withing 1 and 32");
		}

		// TODO(isaachier): When we drop Java 1.6 support, use Long.parseUnsignedLong instead of using BigInteger.
		return new JaegerSpanContext(
			high(traceId),
			new BigInteger(traceId, 16).longValue(),
			new BigInteger(parts[1], 16).longValue(),
			new BigInteger(parts[2], 16).longValue(),
			new BigInteger(parts[3], 16).byteValue());
	}

	/**
	 * Parses a full (low + high) traceId, trimming the lower 64 bits.
	 * @param hexString a full traceId
	 * @return the long value of the higher 64 bits for a 128 bit traceId or 0 for 64 bit traceIds
	 */
	private static long high(String hexString) {
		if (hexString.length() > 16) {
			int highLength = hexString.length() - 16;
			String highString = hexString.substring(0, highLength);
			return new BigInteger(highString, 16).longValue();
		}
		return 0L;
	}

	/**
	 * Encode context into a string.
	 * @param context Span context to encode.
	 * @return Encoded string representing span context.
	 */
	public static String contextAsString(JaegerSpanContext context) {
		int intFlag = context.getFlags() & 0xFF;
		return new StringBuilder()
			.append(context.getTraceId()).append(":")
			.append(Long.toHexString(context.getSpanId())).append(":")
			.append(Long.toHexString(context.getParentId())).append(":")
			.append(Integer.toHexString(intFlag))
			.toString();
	}

	@Override
	public void inject(JaegerSpanContext spanContext, TextMap carrier) {
		carrier.put(contextKey, encodedValue(contextAsString(spanContext)));
		for (Map.Entry<String, String> entry : spanContext.baggageItems()) {
			carrier.put(keys.prefixedKey(entry.getKey(), baggagePrefix), encodedValue(entry.getValue()));
		}
	}

	@Override
	public JaegerSpanContext extract(TextMap carrier) {
		JaegerSpanContext context = null;
		Map<String, String> baggage = null;
		String debugId = null;
		for (Map.Entry<String, String> entry : carrier) {
			// TODO there should be no lower-case here
			String key = entry.getKey().toLowerCase(Locale.ROOT);
			if (key.equals(contextKey)) {
				context = contextFromString(decodedValue(entry.getValue()));
			} else if (key.startsWith(baggagePrefix)) {
				if (baggage == null) {
					baggage = new HashMap<String, String>();
				}
				baggage.put(keys.unprefixedKey(key, baggagePrefix), decodedValue(entry.getValue()));
			}
		}
		if (debugId == null && baggage == null) {
			return context;
		}
		return objectFactory.createSpanContext(
			context == null ? 0L : context.getTraceIdHigh(),
			context == null ? 0L : context.getTraceIdLow(),
			context == null ? 0L : context.getSpanId(),
			context == null ? 0L : context.getParentId(),
			context == null ? (byte)0 : context.getFlags(),
			baggage,
			debugId);
	}

	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		buffer
			.append("MDTextMapCodec{")
			.append("contextKey=")
			.append(contextKey)
			.append(',')
			.append("baggagePrefix=")
			.append(baggagePrefix)
			.append('}');
		return buffer.toString();
	}

	private String encodedValue(String value) {
		try {
			return URLEncoder.encode(value, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// not much we can do, try raw value
			return value;
		}
	}

	private String decodedValue(String value) {
		try {
			return URLDecoder.decode(value, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// not much we can do, try raw value
			return value;
		}
	}

	/**
	 * Returns a builder for TextMapCodec.
	 *
	 * @return Builder
	 */
	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String spanContextKey = SPAN_CONTEXT_KEY;
		private String baggagePrefix = BAGGAGE_KEY_PREFIX;
		private JaegerObjectFactory objectFactory = new JaegerObjectFactory();

		public Builder withSpanContextKey(String spanContextKey) {
			this.spanContextKey = spanContextKey;
			return this;
		}

		public Builder withBaggagePrefix(String baggagePrefix) {
			this.baggagePrefix = baggagePrefix;
			return this;
		}

		/**
		 * Set object factory to use for construction of JaegerSpanContext subclass instances.
		 *
		 * @param objectFactory JaegerObjectFactory subclass instance.
		 */
		public Builder withObjectFactory(JaegerObjectFactory objectFactory) {
			this.objectFactory = objectFactory;
			return this;
		}

		public MDTextMapCodec build() {
			return new MDTextMapCodec(this);
		}
	}
}

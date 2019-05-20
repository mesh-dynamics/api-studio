/**
 * Copyright Cube I O
 */
package com.cube.core;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;

import io.jaegertracing.Configuration;
import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.opentracing.Scope;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapExtractAdapter;
import io.opentracing.tag.Tags;

import com.cube.ws.Config;

/**
 * @author prasad
 *
 */
public class Utils {

	public static <T extends Enum<T>> Optional<T> valueOf(Class<T> clazz, String name) {
	    return EnumSet.allOf(clazz).stream().filter(v -> v.name().equals(name))
	                    .findAny();
	}

	// copied from jdk.internal.net.http.common.Utils, since it is private there and we
	// need this list
	// TODO: Always keep this in sync
    private static final Set<String> DISALLOWED_HEADERS_SET;

    static {
        // A case insensitive TreeSet of strings.
        TreeSet<String> treeSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        treeSet.addAll(Set.of("connection", "content-length",
                "date", "expect", "from", "host", "origin",
                "referer", "upgrade",
                "via", "warning"));
        DISALLOWED_HEADERS_SET = Collections.unmodifiableSet(treeSet);
    }

    public static final Predicate<String>
            ALLOWED_HEADERS = (header) -> !DISALLOWED_HEADERS_SET.contains(header);

	/**
	 * @param s
	 * @return
	 */
	public static Optional<Integer> strToInt(String s) {
		try {
			return Optional.ofNullable(Integer.valueOf(s));
		} catch (Exception e) {
			return Optional.empty();
		}
	}


	public static Optional<Double> strToDouble(String s) {
		try {
			return Optional.ofNullable(Double.valueOf(s));
		} catch (Exception e) {
			return Optional.empty();
		}
	}


    public static Optional<Long> strToLong(String s) {
        try {
            return Optional.ofNullable(Long.valueOf(s));
        } catch (Exception e) {
            return Optional.empty();
        }
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


    public static IntNode intToJson(Integer val) {
		return IntNode.valueOf(val);
    }

	public static TextNode strToJson(String val) {
		return TextNode.valueOf(val);
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
	    return findFirstCaseInsensitiveMatch(mMap,Config.DEFAULT_TRACE_FIELD);
    }

    public static Scope startServerSpan(Tracer tracer, javax.ws.rs.core.HttpHeaders httpHeaders, String operationName) {
        // format the headers for extraction
        MultivaluedMap<String, String> rawHeaders = httpHeaders.getRequestHeaders();
        final HashMap<String, String> headers = new HashMap<String, String>();
        for (String key : rawHeaders.keySet()) {
            headers.put(key, rawHeaders.get(key).get(0));
        }

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

}

package io.cube.utils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.log4j.Logger;

import io.jaegertracing.Configuration;
import io.jaegertracing.Configuration.CodecConfiguration;
import io.jaegertracing.Configuration.ReporterConfiguration;
import io.jaegertracing.Configuration.SamplerConfiguration;
import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.internal.samplers.ConstSampler;
import io.opentracing.Scope;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;
import io.opentracing.propagation.TextMapExtractAdapter;
import io.opentracing.tag.Tags;


public final class Tracing {
  final static Logger LOGGER = Logger.getLogger(Tracing.class);
    private Tracing() {
    }

    public static JaegerTracer init(String service) {
      LOGGER.debug("Init JaegerTracer: " + service.toString());
      SamplerConfiguration samplerConfig = SamplerConfiguration.fromEnv()
          .withType(ConstSampler.TYPE)
          .withParam(1);

      ReporterConfiguration reporterConfig = ReporterConfiguration.fromEnv()
          .withLogSpans(true);

      CodecConfiguration codecConfiguration = CodecConfiguration.fromString("B3");

      Configuration config = new Configuration(service)
          .withSampler(samplerConfig)
          .withReporter(reporterConfig).withCodec(codecConfiguration);
      LOGGER.debug("Returning JaegerTracer: " + config.toString());
      return config.getTracer();
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
    

    public static TextMap requestBuilderCarrier(final Builder builder) {
        return new TextMap() {
            @Override
            public Iterator<Map.Entry<String, String>> iterator() {
                throw new UnsupportedOperationException("carrier is write-only");
            }

            @Override
            public void put(String key, String value) {
                builder.header(key, value);
            }
        };
    }
    
    public static void addTraceHeaders(Tracer tracer, Builder requestBuilder, String requestType) {  
      Tags.SPAN_KIND.set(tracer.activeSpan(), Tags.SPAN_KIND_CLIENT);
      Tags.HTTP_METHOD.set(tracer.activeSpan(), requestType);
      //Tags.HTTP_URL.set(tracer.activeSpan(), requestBuilder.toString());
      tracer.inject(tracer.activeSpan().context(), Format.Builtin.HTTP_HEADERS, new RequestBuilderCarrier(requestBuilder));
    }
       
}
package io.md.tracer.handlers;

import io.md.constants.Constants;

public class JaegerTraceHandler extends  JaegerStyleTracerHandler{
    @Override
    public String getTracekey(String app) {
        return Constants.JAEGER_SPAN_CONTEXT_KEY;
    }

    @Override
    public String getParentSpankey(String app) {
        return Constants.JAEGER_BAGGAGE_PARENT_SPAN;
    }
    @Override
    public Tracer getTracer() {
        return Tracer.Jaeger;
    }
}

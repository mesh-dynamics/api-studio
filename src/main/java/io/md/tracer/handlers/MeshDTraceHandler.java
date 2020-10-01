package io.md.tracer.handlers;

import io.md.constants.Constants;
import io.md.utils.CommonUtils;

public class MeshDTraceHandler extends  JaegerStyleTracerHandler{
    @Override
    public String getTracekey(String app) {
        return CommonUtils.getDFSuffixBasedOnApp(Constants.MD_TRACE_FIELD, app);
    }

    @Override
    public String getParentSpankey(String app) {
        return CommonUtils.getDFSuffixBasedOnApp(Constants.MD_BAGGAGE_PARENT_SPAN, app);
    }

    @Override
    public Tracer getTracer() {
        return Tracer.MeshD;
    }
}

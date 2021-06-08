package io.md.tracer;


import io.md.logger.LogMgr;
import org.slf4j.Logger;

import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.noop.NoopTracer;
import io.opentracing.noop.NoopTracerFactory;
import io.opentracing.propagation.Format;

public class MDGlobalTracer implements Tracer {
	private static final Logger LOGGER = LogMgr.getLogger(MDGlobalTracer.class);
	private static final MDGlobalTracer INSTANCE = new MDGlobalTracer();
	private static volatile Tracer tracer = NoopTracerFactory.create();

	private MDGlobalTracer() {
	}

	public static Tracer get() {
		return INSTANCE;
	}

	public static synchronized void register(Tracer tracer) {
		if (tracer == null) {
			throw new NullPointerException("Cannot register MDGlobalTracer <null>.");
		} else if (tracer instanceof MDGlobalTracer) {
			LOGGER.info("Attempted to register the MDGlobalTracer as delegate"
				+ " of itself.");
		} else if (isRegistered() && !MDGlobalTracer.tracer.equals(tracer)) {
			throw new IllegalStateException("There is already a current MD Tracer registered.");
		} else {
			MDGlobalTracer.tracer = tracer;
		}
	}

	public static synchronized boolean isRegistered() {
		return !(tracer instanceof NoopTracer);
	}

	@Override
	public ScopeManager scopeManager() {
		return tracer.scopeManager();
	}

	@Override
	public Span activeSpan() {
		return tracer.activeSpan();
	}

	@Override
	public Scope activateSpan(Span span) {
		return tracer.activateSpan(span);
	}

	@Override
	public SpanBuilder buildSpan(String s) {
		return tracer.buildSpan(s);
	}

	@Override
	public <C> void inject(SpanContext spanContext, Format<C> format, C c) {
		tracer.inject(spanContext, format, c);
	}

	@Override
	public <C> SpanContext extract(Format<C> format, C c) {
		return tracer.extract(format, c);
	}

	@Override
	public void close() {
		tracer.close();
	}
}

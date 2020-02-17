package io.md.tracer;

import java.util.logging.Level;
import java.util.logging.Logger;

import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.noop.NoopTracer;
import io.opentracing.noop.NoopTracerFactory;
import io.opentracing.propagation.Format;

public class MDTracer implements Tracer {
	private static final Logger LOGGER = Logger.getLogger(MDTracer.class.getName());
	private static final MDTracer INSTANCE = new MDTracer();
	private static volatile Tracer tracer = NoopTracerFactory.create();

	private MDTracer() {
	}

	public static Tracer get() {
		return INSTANCE;
	}

	public static synchronized void register(Tracer tracer) {
		if (tracer == null) {
			throw new NullPointerException("Cannot register MDTracer <null>.");
		} else if (tracer instanceof MDTracer) {
			LOGGER.log(Level.FINE, "Attempted to register the MDTracer as delegate of itself.");
		} else if (isRegistered() && !MDTracer.tracer.equals(tracer)) {
			throw new IllegalStateException("There is already a current MD Tracer registered.");
		} else {
			MDTracer.tracer = tracer;
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
}

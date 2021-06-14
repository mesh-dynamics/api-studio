/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.md.utils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.apache.thrift.TBase;

import io.jaegertracing.internal.JaegerSpanContext;
import io.md.constants.Constants;
import io.md.tracer.MDGlobalTracer;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;


public class Utils {

	public static Map<String, Object> extractThriftParams(String thriftApiPath) {
		Map<String, Object> params = new HashMap<>();
		if (thriftApiPath != null) {
			String[] splitResult = thriftApiPath.split("::");
			String methodName = splitResult[0];
			String argsClassName = splitResult[1];
			params.put(Constants.THRIFT_METHOD_NAME, methodName);
			params.put(Constants.THRIFT_CLASS_NAME, argsClassName);
		}
		return params;
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

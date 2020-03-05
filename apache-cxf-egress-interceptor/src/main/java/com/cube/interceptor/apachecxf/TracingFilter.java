package com.cube.interceptor.apachecxf.egress;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Priority;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.RuntimeDelegate;
import javax.ws.rs.ext.RuntimeDelegate.HeaderDelegate;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.impl.RuntimeDelegateImpl;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;

import io.md.utils.CommonUtils;

@Priority(4000)
public class TracingFilter implements ClientRequestFilter {

	@Override
	public void filter(ClientRequestContext clientRequestContext) throws IOException {
		Message message = JAXRSUtils.getCurrentMessage();
		CommonUtils.injectContext(getModifiableStringHeaders(message));
	}


	//Util functions to get Modifiable map, so that we can inject the MD context.
	//Not available in Apache CXF 2.7.18, so copying from the latest cxf utils
	//https://github.com/apache/cxf/blob/master/rt/frontend/jaxrs/src/main/java/org/apache/cxf/jaxrs/utils/HttpUtils.java
	@SuppressWarnings("unchecked")
	public static <T> MultivaluedMap<String, T> getModifiableStringHeaders(Message m) {
		MultivaluedMap<String, Object> headers = getModifiableHeaders(m);
		convertHeaderValuesToString(headers, false);
		return (MultivaluedMap<String, T>) headers;
	}

	public static MultivaluedMap<String, Object> getModifiableHeaders(Message m) {
		Map<String, List<Object>> headers = CastUtils
			.cast((Map<?, ?>) m.get(Message.PROTOCOL_HEADERS));
		return new MetadataMap<String, Object>(headers, false, false, true);
	}

	public static void convertHeaderValuesToString(Map<String, List<Object>> headers,
		boolean delegateOnly) {
		if (headers == null) {
			return;
		}
		RuntimeDelegate rd = getOtherRuntimeDelegate();
		if (rd == null && delegateOnly) {
			return;
		}
		for (Map.Entry<String, List<Object>> entry : headers.entrySet()) {
			List<Object> values = entry.getValue();
			for (int i = 0; i < values.size(); i++) {
				Object value = values.get(i);

				if (value != null && !(value instanceof String)) {

					HeaderDelegate<Object> hd = getHeaderDelegate(rd, value);

					if (hd != null) {
						value = hd.toString(value);
					} else if (!delegateOnly) {
						value = value.toString();
					}

					try {
						values.set(i, value);
					} catch (UnsupportedOperationException ex) {
						// this may happen if an unmodifiable List was set via Map put
						List<Object> newList = new ArrayList<>(values);
						newList.set(i, value);
						// Won't help if the map is unmodifiable in which case it is a bug anyway
						headers.put(entry.getKey(), newList);
					}

				}

			}
		}

	}

	public static RuntimeDelegate getOtherRuntimeDelegate() {
		try {
			RuntimeDelegate rd = RuntimeDelegate.getInstance();
			return rd instanceof RuntimeDelegateImpl ? null : rd;
		} catch (Throwable t) {
			return null;
		}
	}

	public static HeaderDelegate<Object> getHeaderDelegate(Object o) {
		return getHeaderDelegate(RuntimeDelegate.getInstance(), o);
	}

	@SuppressWarnings("unchecked")
	public static HeaderDelegate<Object> getHeaderDelegate(RuntimeDelegate rd, Object o) {
		return rd == null ? null : (HeaderDelegate<Object>) rd.createHeaderDelegate(o.getClass());
	}
}
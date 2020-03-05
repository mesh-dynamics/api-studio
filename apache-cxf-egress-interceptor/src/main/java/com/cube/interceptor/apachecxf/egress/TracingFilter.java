package com.cube.interceptor.apachecxf.egress;

import static com.cube.interceptor.utils.Utils.getModifiableStringHeaders;

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
}
package io.cube.spring.ingress;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpMethod;
import org.springframework.web.util.ContentCachingRequestWrapper;

public class CachedBodyHttpServletRequest extends ContentCachingRequestWrapper {

	private HttpServletRequest cachedRequest;

	public CachedBodyHttpServletRequest(HttpServletRequest request) {
		super(request);
		this.cachedRequest = request;
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		cacheInputStream();
		return new CachedBodyServletInputStream(super.getContentAsByteArray());
	}

	private void cacheInputStream() throws IOException {
		IOUtils.toByteArray(super.getInputStream());
	}

	@Override
	public  Map<String, String[]> getParameterMap() {
		//If the request is not form-urlencoded,
		if (isFormPost()) {
			return super.getParameterMap();
		} else {
			//This is the case where request params are sent as Query String
			//We should not cache the Inputstream, so we call the original
			//request's getParameterMap() method
			return cachedRequest.getParameterMap();
		}
	}

	private boolean isFormPost() {
		String contentType = this.getContentType();
		return contentType != null && contentType.contains("application/x-www-form-urlencoded") && HttpMethod.POST.matches(this.getMethod());
	}
}

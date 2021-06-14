package io.cube.spring.ingress;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import org.springframework.web.util.ContentCachingRequestWrapper;

public class CachedBodyHttpServletRequest extends ContentCachingRequestWrapper {
	private boolean isCached;

	public CachedBodyHttpServletRequest(HttpServletRequest request) {
		super(request);
		this.isCached = false;
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		cacheInputStream();
		return new CachedBodyServletInputStream(super.getContentAsByteArray());
	}

	private void cacheInputStream() throws IOException {
		if (!this.isCached) {
			//This is called to load the parameter map before
			//consuming the inputstream. Otherwise  request with
			//content-type x-form-url-encoded, will return empty parameter map.
			super.getParameterMap();
			InputStream is = super.getInputStream();
			//To make the ContentCachingRequestWrapper to cache.
			while(is.read() != -1);
			this.isCached = true;
		}
	}

}

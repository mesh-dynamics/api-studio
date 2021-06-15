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

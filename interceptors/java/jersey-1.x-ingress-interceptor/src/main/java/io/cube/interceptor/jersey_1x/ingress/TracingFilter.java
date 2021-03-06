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

package io.cube.interceptor.jersey_1x.ingress;


import javax.annotation.Priority;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import io.md.logger.LogMgr;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;

import io.md.constants.Constants;
import io.md.utils.CommonUtils;
import io.opentracing.Scope;
import io.opentracing.Span;


@Provider
@Priority(1000)
public class TracingFilter implements ContainerRequestFilter, ContainerResponseFilter {

	private static Config config;
	private static final Logger LOGGER = LogMgr.getLogger(TracingFilter.class);

	public static final String scopeKey
			= Constants.SERVICE_FIELD.concat(Constants.MD_SCOPE);
	public static final String spanKey
			= Constants.SERVICE_FIELD.concat(Constants.MD_CHILD_SPAN);

	static {
			config = new Config();
	}

	@Override
	public ContainerRequest filter(ContainerRequest containerRequest) {
		try {
			//start a md-child-span
			MultivaluedMap<String, String> requestHeaders = containerRequest.getRequestHeaders();
			Span currentSpan = CommonUtils.startServerSpan(requestHeaders,
					Constants.SERVICE_FIELD.concat(Constants.MD_CHILD_SPAN));
			Scope scope = CommonUtils.activateSpan(currentSpan);


			String sampleBaggageItem = currentSpan.getBaggageItem(Constants.MD_IS_SAMPLED);
			if (sampleBaggageItem == null) {
				//root span
				boolean isSampled = Utils.isSampled(requestHeaders);
				currentSpan.setBaggageItem(Constants.MD_IS_SAMPLED, String.valueOf(isSampled));
			} else if (!BooleanUtils.toBoolean(sampleBaggageItem) && Config.commonConfig.samplerVeto) {
				currentSpan.setBaggageItem(Constants.MD_IS_VETOED,
						String.valueOf(Utils.isSampled(requestHeaders)));
			}

			CommonUtils.injectContext(requestHeaders);

			containerRequest.getProperties().put(scopeKey, scope);
		} catch (Exception e) {
			LOGGER.error("Exception occured during setting up span!", e);
		}
		return containerRequest;
	}

	@Override
	public ContainerResponse filter(ContainerRequest containerRequest,
		ContainerResponse containerResponse) {
		try {
			Object obj = containerRequest.getProperties().get(scopeKey);
			if (obj != null) {
				((Scope) obj).close();
				containerRequest.getProperties().remove(scopeKey);
			}

			Object spanObj = containerRequest.getProperties().get(spanKey);
			if (spanObj != null) {
				((Span) spanObj).finish();
			}

		} catch (Exception e) {
			LOGGER.error("Exception occured during closing span!", e);
		}

		return containerResponse;
	}
}
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

import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.http.HttpRequest;

import io.opentracing.propagation.TextMap;

public class RequestCarrier implements TextMap {

	private final HttpRequest requestBuilder;

	public RequestCarrier(HttpRequest requestBuilder) {
		this.requestBuilder = requestBuilder;
	}

	@Override
	public Iterator<Entry<String, String>> iterator() {
		throw new UnsupportedOperationException("carrier is write-only");
	}


	@Override
	public void put(String key, String value) {
		requestBuilder.setHeader(key, value);
	}
}

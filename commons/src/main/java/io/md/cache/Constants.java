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

package io.md.cache;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import io.md.utils.CommonUtils;
import io.md.utils.Utils;

/*
   Constants defined for PubSub purpose
 */
public class Constants {

	private static final String CACHE_DUR_SEC = "CACHE_DUR_SEC";

	private static final long CACHE_IN_MEM_DURATION_SEC = CommonUtils.fromEnvOrSystemProperties(CACHE_DUR_SEC).flatMap(Utils::strToLong).orElse(30*60L);

	public static final Duration CACHE_TO_LIVE_DUR = Duration.ofSeconds(CACHE_IN_MEM_DURATION_SEC);

	public static final String DYNAMIC_INJECTION = "DynamicInjection";

	public static final String API_GEN_PATH = "ApiGenPath";

	public static final String CUSTOMER_APP_CONFIG = "CustomerAppConfig";

	public static final String TRACER = "Tracer";

	public static final String FILTER_TRANSFORM = "FilterTransform";


	public enum PubSubContext {

		IN_MEM_CACHE
	}

	public static final String CACHE_NAME = "cacheName";

	public static final String CUSTOMER_ID = "customerId";
	public static final String APP = "app";
	public static final String SERVICE = "service";
	public static final String VERSION = "version";
	public static final String API_PATH = "apiPath";





}

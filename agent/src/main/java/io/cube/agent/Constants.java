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

package io.cube.agent;

public class Constants {

	public static final String TEXT_PLAIN = "text/plain";
	public static final String APPLICATION_JSON = "application/json";
	public static final String APPLICATION_FORM_URL_ENCODED = "application/x-www-form-urlencoded";

	public static final String MD_POLLINGCONFIG_FILEPATH = "io.md.pollingconfig.filepath";
	public static final String MD_POLLINGCONFIG_DELAY = "io.md.pollingconfig.delay";
	public static final String MD_POLLINGCONFIG_FETCHCONFIGAPIURI = "io.md.pollingconfig.fetchconfigapiuri";
	public static final String MD_POLLINGCONFIG_ACKCONFIGAPIURI = "io.md.pollingconfig.ackconfigapiuri";
	public static final String MD_POLLINGCONFIG_POLLSERVER = "io.md.pollingconfig.pollserver";
	public static final String MD_POLLINGCONFIG_RETRYCOUNT = "io.md.pollingconfig.retrycount";


	public static final String ENCRYPTION_CONF_PROP = "io.md.encryptionconfig.path";
	public static final String MD_PERFORMANCE_TEST = "io.md.performance.test";
	public static final String SAMPLER_CONF_PROP = "io.md.samplerconfig";
	public static final String SERVICES_TO_MOCK_PROP = "io.md.mock.services";
	public static final String AUTH_TOKEN_PROP = "io.md.authtoken";
	public static final String CUSTOMERID_HEADER = "CustomerId";
	public static final String AUTHORIZATION_HEADER = "Authorization";
	public static final String RING_BUFFER_SIZE_PROP = "io.md.disruptor.size";

	//Logger Related
	public static final String MD_LOGGERCONFIG_URI = "io.md.logger.uri";
	public static final String MD_LOGGERCONFIG_ENABLE = "io.md.logger.enable";
	public static final String MD_LOGGERCONFIG_LEVEL = "io.md.logger.level";




	public static final String RING_BUFFER_OUTPUT_PROP = "io.md.disruptor.output.type";
	public static final String RING_BUFFER_OUTPUT_FILE_NAME = "io.md.disruptor.output.file.name";
	public static final String DISRUPTOR_LOG_FILE_MAX_SIZE_PROP = "io.md.disruptor.output.file.maxsizebytes";
	public static final String DISRUPTOR_LOG_FILE_MAX_BACKUPS_PROP = "io.md.disruptor.output.file.backupnumber";
	public static final String DISRUPTOR_CONSUMER_MEMORY_BUFFER_SIZE = "io.md.disruptor.event.memory.buffer.size";

	public static final String RECORDER_PROP = "io.md.recorder";

	//Node Selection
	public static final String MD_NODE_SELECTION_CONFIG = "io.md.nodeselectionconfig";

	public static final String CONFIG_VERSION_FIELD = "version";
	public static final String CONFIG_TAG_FIELD = "tag";

	public static final String MD_FETCH_AGENT_CONFIG_API_PATH= "cs/fetchAgentConfig/";
	public static final String MD_ACK_CONFIG_API_PATH= "cs/ackConfigApplication/";
}

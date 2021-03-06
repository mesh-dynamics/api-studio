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

import java.text.NumberFormat;

public class Constants {
    public static final String APPLICATION_X_NDJSON = "application/x-ndjson";
    public static final String APPLICATION_X_MSGPACK = "application/x-msgpack";
    public static final String DEFAULT_TRACE_FIELD = "x-b3-traceid";
    public static final String DEFAULT_SPAN_FIELD = "x-b3-spanid";
    public static final String DEFAULT_PARENT_SPAN_FIELD = "x-b3-parentspanid";


    //API STRINGS
    public static final String REQUEST = "request";
    public static final String RESPONSE = "response";
    public static final String CONTENT_TYPE = "content-type";
    public static final String BODY = "body";
    public static final String STATUS = "status";
    public static final String SUCCESS = "success";
    public static final String FAIL = "fail";
    public static final String ERROR = "error";
    public static final String DATA = "data";
    public static final String RESPONSE_OBJ = "responseObj";
    public static final String MESSAGE_ID = "messageId";
    public static final String MESSAGE = "message";
    public static final String REASON = "reason";
    public static final String EXCEPTION_STACK = "exceptionStack";
    public static final String REQ_MATCH_TYPE = "reqMatchType";
    public static final String RESP_MATCH_TYPE = "respMatchType";
    public static final String INCLUDE_DIFF = "includeDiff";

    //MESSAGE KEYS
    public static final String CLASS_NAME = "className";

    //MESSAGE_IDs
    public static final String EVENT_NOT_FOUND = "EVENT_NOT_FOUND";
    public static final String STORE_EVENT_FAILED = "STORE_EVENT_FAILED";
    public static final String INVALID_EVENT = "INVALID_EVENT";
    public static final String INVALID_INPUT = "INVALID_INPUT";
    public static final String INVALID_HEX_PAYLOAD = "INVALID_HEX_PAYLOAD";
    public static final String TEMPLATE_STORE_FAILED = "TEMPLATE_STORE_FAILED";
    public static final String TEMPLATE_META_STORE_FAILED = "TEMPLATE_META_STORE_FAILED";
    public static final String UPDATE_RECORDING_OPERATION_FAILED = "UPDATE_RECORDING_OPERATION_FAILED";
    public static final String ANALYSIS_NOT_FOUND = "ANALYSIS_NOT_FOUND";
    public static final String RECORDING_NOT_FOUND = "RECORDING_NOT_FOUND";
    public static final String TEMPLATE_NOT_FOUND = "TEMPLATE_NOT_FOUND";
    public static final String SERVICE_HEALTH_STATUS = "SERVICE_HEALTH_STATUS";
    public static final String SOLR_STATUS_CODE = "SOLR_STATUS_CODE";
    public static final String SOLR_STATUS_MESSAGE = "SOLR_STATUS_MESSAGE";
    public static final String SOLR_STORE_FAILED = "SOLR_STORE_FAILED";


    //EXCEPTIONS
    public static final String IO_EXCEPTION = "IO_EXCEPTION";
    public static final String RUNTIME_EXCEPTION = "RUNTIME_EXCEPTION";
    public static final String JSON_PARSING_EXCEPTION = "JSON_PARSING_EXCEPTION";

    public static final String DECODING_EXCEPTION = "DECODING_EXCPETION";
    public static final String BAD_VALUE_EXCEPTION = "BAD_VALUE_EXCEPTION";
    public static final String GENERIC_EXCEPTION = "GENERIC_EXCEPTION";
    public static final String RECORDING_SAVE_FAILURE_EXCEPTION = "RECORDING_SAVE_FAILURE_EXCEPTION";
    public static final String RECORDING_SAME_NAME_EXCEPTION = "RECORDING_SAME_NAME_EXCEPTION";


    // Cryptography
    // Algos https://docs.oracle.com/javase/8/docs/technotes/guides/security/crypto/CryptoSpec.html#Cipher
    public static final String AES_CBC_PKCS5_ALGO = "AES/CBC/PKCS5Padding";
    public static final String AES_CTR_PKCS5_ALGO = "AES/CTR/PKCS5Padding";
    public static final String AES_CIPHER_KEY_TYPE = "AES";
    public static final String DEFAULT_PASS_PHRASE = "WubbaLubbaDubDub";
    public static final String CIPHER_KEY_TYPE_FIELD = "cipherKeyType";



    //JSON FETCH PATH
    public static final String CUSTOMER_ID_PATH = "/customerId";
    public static final String APP_PATH = "/app";
    public static final String SERVICE_PATH = "/service";
    public static final String INSTANCE_ID_PATH = "/instanceId";
    public static final String COLLECTION_PATH = "/collection";
    public static final String TRACE_ID_PATH = "/traceId";
    public static final String RUN_TYPE_PATH = "/runType";
    public static final String TIMESTAMP_PATH = "/timestamp";
    public static final String REQ_ID_PATH = "/reqId";
    public static final String API_PATH_PATH = "/apiPath";
    public static final String EVENT_TYPE_PATH = "/eventType";
    public static final String QUERY_PARAMS_PATH = "/queryParams";
    public static final String FORM_PARAMS_PATH = "/formParams";
    public static final String PATH_PATH = "/path";
    public static final String PATH_SEGMENTS_PATH = "/pathSegments";
    public static final String METHOD_PATH = "/method";
    public static final String ARGS_PATH = "/argVals";
    public static final String HDR_PATH = "/hdrs";
    public static final String TRLS_PATH = "/trls";
    public static final String META_PATH = "/meta";
    public static final String BODY_PATH = "/body";

    public static final String THRIFT_CLASS_NAME = "thriftClassName";
    public static final String THRIFT_METHOD_NAME  = "thirftMethodName";
    public static final String CLASS_LOADER = "classLoader";


    public static final String REQUEST_MATCH_RULES = "requestMatchRules";
    public static final String REQUEST_COMPARE_RULES = "requestCompareRules";
    public static final String RESPONSE_COMPARE_RULES = "responseCompareRules";
    public static final String REQ_RESP_MATCH_RESULT = "ReqRespMatchResult";


    //COMMON STRINGS
    public static final String CUSTOMER_ID_FIELD = "customerId";
    public static final String APP_FIELD = "app";
    public static final String ASYNC_FIELD = "async";
    public static final String SERVICE_FIELD = "service";
    public static final String INSTANCE_ID_FIELD = "instanceId";
    public static final String COLLECTION_FIELD = "collection";
    public static final String RUN_TYPE_FIELD = "runType";
    public static final String RUN_ID_FIELD = "runId";
    public static final String PATH_FIELD = "path";
    public static final String PATHS_FIELD = "paths";
    public static final String MOCK_SERVICES_FIELD ="mockServices";
    public static final String EXCLUDE_PATH_FIELD = "excludePaths";
    public static final String API_PATH_FIELD = "apiPath";
    public static final String REQ_ID_FIELD = "reqId";
    public static final String REQ_IDS_FIELD = "reqIds";
    public static final String REPLAY_ID_FIELD = "replayId";
    public static final String TIMESTAMP_FIELD = "timestamp";
    public static final String TRACE_ID_FIELD = "traceId";
    public static final String EVENT_TYPE_FIELD = "eventType";
    public static final String JSON_PATH_FIELD = "jsonPath";
    public static final String USER_ID_FIELD = "userId";
    public static final String END_POINT_FIELD= "endPoint";
    public static final String TEST_CONFIG_NAME_FIELD = "testConfigName";
    public static final String RECORD_REQ_ID_FIELD = "recordReqId";
    public static final String REPLAY_REQ_ID_FIELD = "replayReqId";
    public static final String START_FIELD = "start";
    public static final String END_DATE_FIELD = "endDate";
    public static final String START_DATE_FIELD = "startDate";
    public static final String NUM_RESULTS_FIELD = "numResults";
    public static final String METHOD_FIELD = "method";
    public static final String LIMIT_FIELD = "limit";
    public static final String OFFSET_FIELD = "offset";
    public static final String JAR_PATH_FIELD = "jarPath";
    public static final String REPLAY_TYPE_FIELD = "replayType";
    public static final String SAMPLE_RATE_FIELD = "sampleRate";
    public static final String INTERM_SERVICE_FIELD = "intermService";
    public static final String TRANSFORMS_FIELD = "transforms";
    public static final String ANALYZE_FIELD = "analyze";
    public static final String TRACE_PROPAGATION = "tracePropagation";
    public static final String INSERT_AFTER_SEQ_ID = "insertAfterSeqId";
    public static final String STORE_TO_DATASTORE = "storeToDatastore";
    public static final String IGNORE_STATIC_CONTENT = "ignoreStaticContent";


    public static final String GENERATED_CLASS_JAR_PATH_FIELD ="generated_class_jar_path"  ;
    public static final String TEMPLATE_KEY_FIELD = "templateKey";
    public static final String OLD_TEMPLATE_SET_ID = "oldTemplateSetId";
    public static final String OLD_TEMPLATE_SET_VERSION = "oldTemplateSetVersion";
    public static final String NEW_TEMPLATE_SET_VERSION = "newTemplateSetVersion";
    public static final String RECORDING_ID = "recordingId";
    public static final String RECORDING_TYPE_FIELD = "recordingType";
    public static final String RECORDING_UPDATE_OPERATION_SET_ID = "recUpdateOpSetId";
    public static final String RECORDING_UPDATE_API_OPERATION_SET_ID = "recUpdateApiPathOpSetId";

    public static final String ROOT_RECORDING_FIELD = "root_recording_id";
    public static final String PARENT_RECORDING_FIELD = "parent_recording_id";
    public static final String GOLDEN_NAME_FIELD = "golden_name";
    public static final String GOLDEN_LABEL_FIELD = "label";
    public static final String CODE_VERSION_FIELD = "code_version";
    public static final String BRANCH_FIELD = "branch";
    public static final String TAGS_FIELD = "tags";
    public static final String ARCHIVED_FIELD = "archived";
    public static final String GIT_COMMIT_ID_FIELD = "git_commit_id";
    public static final String COLLECTION_UPD_OP_SET_ID_FIELD = "collection_upd_op_set_id";
    public static final String TEMPLATE_UPD_OP_SET_ID_FIELD = "template_upd_op_set_id";
    public static final String GOLDEN_COMMENT_FIELD = "golden_comment";
    public static final String VERSION_FIELD = "version";
    public static final String SRC_REQUEST_ID = "src-request-id";
    public static final String REQUEST_ID = "request-id";
    public static final String CUBE_HEADER_PREFIX = "c-";
    public static final String DYNACMIC_INJECTION_CONFIG_VERSION_FIELD = "dynamicInjectionConfigVersion";
    public static final String EXTRACTION_METAS_JSON_FIELD = "extractionMetasJson";
    public static final String INJECTION_METAS_JSON_FIELD = "injectionMetasJson";
    public static final String EXTERNAL_INJECTION_EXTRACTIONS_JSON_FIELD = "externalInjectionExtractionsJson";
    public static final String STATIC_INJECTION_MAP_FIELD = "staticInjectionMap";
    public static final String PROTO_DESCRIPTOR_FILE_FIELD = "protoDescriptorFile";



    public static final String ROOT_PATH = "";

	public static final String NOT_PRESENT = "Absent";
	public static final String EVENT_STRING = "eventAsString";
    public static final String PAYLOAD = "payload" ;
	public static final String DIFF_VALUE_FIELD = "diffVal";
    public static final String DIFF_FROM_STR_FIELD = "diffFromStr";
    public static final String DIFF_FROM_VALUE_FIELD = "diffFromVal";
    public static final String DIFF_OP_FIELD = "diffOp";
    public static final String DIFF_PATH_FIELD = "diffPath";
    public static final String DIFF_RESOLUTION_FIELD =  "diffRes";
    public static final String DIFF_TYPE_FIELD = "diffType";
    public static final String REQ_COMP_RES_META = "reqCmpResMeta" ;
    public static final String REQ_COMP_RES_TYPE = "reqCmpResType";
    public static final String REQUEST_DIFF = "requestDiff";
    public static final String RESPONSE_DIFF = "responseDiff";
    public static final String FACETS = "facets";
    public static final String DIFF_RES_FACET = "diffResFacets";
    public static final String SERVICE_FACET = "serviceFacets";
    public static final String PATH_FACET = "pathFacets";
    public static final String DIFF_PATH_FACET = "diffPathFacets";
    public static final String SAMPLING_FACET = "samplingFacets";

	public static final String REPLAY_TRACE_ID = "replayTraceId";
	public static final String REC_TRACE_ID = "recordTraceId";
	public static final String SPAN_ID_FIELD = "spanId";
    public static final String PARENT_SPAN_ID_FIELD = "parentSpanId";

    // VariableSources
    public static final String REDIS_SHADOW_KEY_PREFIX = "shadowKey:";
    public static final String REDIS_STATUS_KEY_PREFIX = "statusKey:";
    public static final String CONFIG_JSON = "config_json";
	public static final String TAG_FIELD = "tag";
	public static final String RESET_TAG_FIELD = "resettag";
	public static final String AGENT_ID = "agentId" ;
    public static final String INFO = "info";
    public static final String EVENT_META_DATA = "eventMetaData";
    public static final String EVENT_META_DATA_KEY_FIELD = "eventMetaDataKey";
    public static final String CONFIG_ACK_DATA_KEY_FIELD = "configAckDataKey";
    public static final int AGENT_ACK_DEFAULT_DELAY_SEC = 30;

    // for using template set name and labels
    public static final String TEMPLATE_SET_NAME = "templateSetName";
    public static final String TEMPLATE_SET_LABEL = "templateSetLabel";

    // for async polling
    public static final String IS_POLL_REQUEST_METADATA = "isPollRequest";
    public static final String POLL_REQUEST_MAX_RETRIES_METADATA = "pollMaxRetries";
    public static final String POLL_REQUEST_RETRY_INTERVAL_METADATA = "pollRetryIntervalSec";
    public static final String POLL_REQUEST_RESP_JSON_PATH_METADATA = "pollRespJsonPath";
    public static final String POLL_REQUEST_RESP_COMPARATOR_METADATA = "pollRespComparator";
    public static final String POLL_REQUEST_RESP_COMPARISON_VALUE_METADATA = "pollRespCompValue";
    public static final String POLL_REQUEST_TRUE = "true";

    public enum POLL_REQUEST_COMPARISON_OPERATOR {

        equals {
            @Override
            public boolean compare(String s1, String s2) throws Exception {
                return s1.equals(s2);
            }
        }, //equals
        gt {
            @Override
            public boolean compare(String s1, String s2) throws Exception {
                return numberFormat.parse(s1).doubleValue() >
                    numberFormat.parse(s2).doubleValue() ;
            }
        }, // greater than
        lt {
            @Override
            public boolean compare(String s1, String s2) throws Exception {
                return numberFormat.parse(s1).doubleValue() <
                    numberFormat.parse(s2).doubleValue();
            }
        }; // less than


        NumberFormat numberFormat = NumberFormat.getInstance();
        abstract public boolean compare(String s1, String s2) throws Exception;
    };


}

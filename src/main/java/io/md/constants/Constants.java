package io.md.constants;

import io.md.tracer.MDTextMapCodec;

public class Constants {
	public static final String APPLICATION_X_NDJSON = "application/x-ndjson";
	public static final String APPLICATION_X_MSGPACK = "application/x-msgpack";
	public static final String APPLICATION_GRPC = "application/grpc";
	public static final String IMAGE_JPEG = "image/jpeg";
	public static final String IMAGE_PNG = "image/png";
	public static final String DEFAULT_REQUEST_ID = "c-request-id";
	public static final String DEFAULT_TRACE_FIELD = "x-b3-traceid";
	public static final String DEFAULT_SPAN_FIELD = "x-b3-spanid";
	public static final String DEFAULT_PARENT_SPAN_FIELD = "x-b3-parentspanid";
	public static final String X_REQUEST_ID = "x-request-id";
	public static final String PARENT_SPAN_ID_PROP_FIELD = "parent-span-id";
	public static final String TRAILER_HEADER = "Trailer";
	public static final String MD_TRAILER_HEADER_PREFIX = "md_trailer_header_";


	//public static final String DEFAULT_BAGGAGE_PARENT_SPAN = "baggage-parent-span-id";
	public static final String MD_BAGGAGE_PARENT_SPAN = MDTextMapCodec.BAGGAGE_KEY_PREFIX_BASE + io.md.constants.Constants.MD_PARENT_SPAN;
	public static final String MD_TRACE_FIELD = MDTextMapCodec.SPAN_CONTEXT_BASE;

	public static final String ZIPKIN_TRACE_FIELD = "x-b3-traceid";
	public static final String ZIPKIN_SPAN_FIELD = "x-b3-spanid";
	public static final String ZIPKIN_BAGGAGE_KEY_PREFIX = "baggage-";
	public static final String ZIPKIN_BAGGAGE_PARENT_SPAN = ZIPKIN_BAGGAGE_KEY_PREFIX + PARENT_SPAN_ID_PROP_FIELD;

	public static final String JAEGER_SPAN_CONTEXT_KEY = "uber-trace-id";
	public static final String JAEGER_BAGGAGE_KEY_PREFIX = "uberctx-";
	public static final String JAEGER_BAGGAGE_PARENT_SPAN = JAEGER_BAGGAGE_KEY_PREFIX + PARENT_SPAN_ID_PROP_FIELD;

	public static final String DATADOG_TRACE_FIELD = "x-datadog-trace-id";
	public static final String DATADOG_SPAN_FIELD = "x-datadog-parent-id";
	public static final String DATADOG_BAGGAGE_KEY_PREFIX = "ot-baggage-";
	public static final String DATADOG_BAGGAGE_PARENT_SPAN = DATADOG_BAGGAGE_KEY_PREFIX + PARENT_SPAN_ID_PROP_FIELD;



	public static final String DEFAULT_TEMPLATE_VER = "DEFAULT";

	//API STRINGS
	public static final String REQUEST = "request";
	public static final String RESPONSE = "response";
	public static final String CONTENT_TYPE = "content-type";
	public static final String BODY = "body";
	public static final String STATUS = "status";
	public static final String METHOD = "method";
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
	public static final String MATCH_TYPE = "matchType";
	public static final String INCLUDE_DIFF = "includeDiff";
	public static final String LOWER_BOUND = "lowerBound";
	public static final String MATCHED_REQUEST_ID = "matchedRequestId";


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



	//EXCEPTIONS
	public static final String IO_EXCEPTION = "IO_EXCEPTION";
	public static final String RUNTIME_EXCEPTION = "RUNTIME_EXCEPTION";
	public static final String JSON_PARSING_EXCEPTION = "JSON_PARSING_EXCEPTION";

	public static final String DECODING_EXCEPTION = "DECODING_EXCPETION";
	public static final String BAD_VALUE_EXCEPTION = "BAD_VALUE_EXCEPTION";
	public static final String GENERIC_EXCEPTION = "GENERIC_EXCEPTION";
	public static final String RECORDING_SAVE_FAILURE_EXCEPTION = "RECORDING_SAVE_FAILURE_EXCEPTION";


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
	public static final String METHOD_PATH = "/method";
	public static final String STATUS_PATH = "/status";
	public static final String GRPC_STATUS_PATH = "/trls/grpc-status";

	public static final String ARGS_PATH = "/args";
	public static final String FN_RESPONSE_PATH = "/retOrExceptionVal";
	public static final String HDR_PATH = "/hdr";
	public static final String META_PATH = "/meta";
	public static final String BODY_PATH = "/body";

	//COMMON STRINGS
	public static final String CUSTOMER_ID_FIELD = "customerId";
	public static final String APP_FIELD = "app";
	public static final String SERVICE_FIELD = "service";
	public static final String RUN_ID_FIELD = "runId";
	public static final String INSTANCE_ID_FIELD = "instanceId";
	public static final String COLLECTION_FIELD = "collection";
	public static final String TYPE_FIELD = "type";
	public static final String RUN_TYPE_FIELD = "runType";
	public static final String PATH_FIELD = "path";
	public static final String API_PATH_FIELD = "apiPath";
	public static final String REQ_ID_FIELD = "reqId";
	public static final String REPLAY_ID_FIELD = "replayId";
	public static final String TIMESTAMP_FIELD = "timestamp";
	public static final String TRACE_ID_FIELD = "traceId";
	public static final String PAYLOAD_KEY_FIELD = "payloadKey";
	public static final String SPAN_ID_FIELD = "spanId";
	public static final String PARENT_SPAN_ID_FIELD = "parentSpanId";
	public static final String EXCLUDE_PATH_FIELD = "excludePath";

	public static final String PAYLOAD_FIELDS_FIELD = "payloadFields";
	public static final String PAYLOAD_FIELDS_FIELD_PATH = "/"+PAYLOAD_FIELDS_FIELD;
	public static final String START_TIMESTAMP_FIELD = "startTimestamp";
	public static final String START_TIMESTAMP_FIELD_PATH = "/"+START_TIMESTAMP_FIELD;
	public static final String END_TIMESTAMP_FIELD = "endTimestamp";
	public static final String END_TIMESTAMP_FIELD_PATH = "/"+END_TIMESTAMP_FIELD;

	public static final String EVENT_TYPE_FIELD = "eventType";
	public static final String JSON_PATH_FIELD = "jsonPath";
	public static final String USER_ID_FIELD = "userId";
	public static final String RECORD_REQ_ID_FIELD = "recordReqId";
	public static final String REPLAY_REQ_ID_FIELD = "replayReqId";
	public static final String START_FIELD = "start";
	public static final String END_DATE_FIELD = "endDate";
	public static final String NUM_RESULTS_FIELD = "numResults";
	public static final String METHOD_FIELD = "method";
	public static final String LIMIT_FIELD = "limit";
	public static final String OFFSET_FIELD = "offset";
	public static final String JAR_PATH_FIELD = "jarPath";
	public static final String REPLAY_TYPE_FIELD = "replayType";
	public static final String GENERATED_CLASS_JAR_PATH_FIELD ="generated_class_jar_path";
	public static final String HEADERS = "headers";
	public static final String QUERY_PARAMS = "queryParams";
	public static final String IS_NODE_SELECTED = "isNodeSelected";
	public static final String RECORDING_TYPE_FIELD = "recordingType";
	public static final String SCORE_FIELD = "score";
	public static final String SEQID_FIELD = "seqId";
	public static final String ID_FIELD = "id";
	public static final String TRACER_FIELD = "tracer";
	public static final String API_GEN_PATHS_FIELD = "apiGenericPaths";
	public static final String PROTO_FILE_MAP_FIELD ="protoFileMap";


	public static final String ROOT_RECORDING_FIELD = "root_recording_id";
	public static final String PARENT_RECORDING_FIELD = "parent_recording_id";
	public static final String GOLDEN_NAME_FIELD = "golden_name";
	public static final String CODE_VERSION_FIELD = "code_version";
	public static final String BRANCH_FIELD = "branch";
	public static final String TAGS_FIELD = "tags";
	public static final String ARCHIVED_FIELD = "archived";
	public static final String GIT_COMMIT_ID_FIELD = "git_commit_id";
	public static final String COLLECTION_UPD_OP_SET_ID_FIELD = "collection_upd_op_set_id";
	public static final String TEMPLATE_UPD_OP_SET_ID_FIELD = "template_upd_op_set_id";
	public static final String GOLDEN_COMMENT_FIELD = "golden_comment";
	public static final String TEMPLATE_VERSION_FIELD = "version";
	public static final String MD_EXTERNAL_ID_FIELD = "io.md.externalIdField";


	public static final String ROOT_PATH = "";

	public static final String NOT_PRESENT = "Absent";
	public static final String EVENT_STRING = "eventAsString";
	public static final String DEFAULT_COLLECTION = "defaultCollection";
	public static final String NOT_APPLICABLE = "NA";


	public static final String DEFAULT_TRACE_ID = "traceId";
	public static final String DEFAULT_SPAN_ID = "spanId";
	public static final String DEFAULT_PARENT_SPAN_ID = "pSpanId";

	//Properties
	public static final String MD_SERVICE_ENDPOINT_PROP = "io.md.service.endpoint";
	public static final String MD_RECORD_SERVICE_PROP = "io.md.service.record";
	public static final String MD_MOCK_SERVICE_PROP = "io.md.service.mock";
	public static final String MD_READ_TIMEOUT_PROP = "io.md.read.timeout";
	public static final String MD_CONNECT_TIMEOUT_PROP = "io.md.connect.timeout";
	public static final String MD_RETRIES_PROP = "io.md.connect.retries";
	public static final String MD_CUSTOMER_PROP = "io.md.customer";
	public static final String MD_APP_PROP = "io.md.app";
	public static final String MD_INSTANCE_PROP = "io.md.instance";
	public static final String MD_SERVICE_PROP = "io.md.servicename";
	public static final String MD_INTENT_PROP = "io.md.intent";
	public static final String MD_ENCRYPTION_CONFIG_PATH = "io.md.encryptionconfig.path";
	public static final String MD_API_PATH_PROP = "io.md.apiPath";
	public static final String MD_TRACE_META_MAP_PROP = "io.md.traceMetaMap" ;
	public static final String MD_RESPONSE_HEADERS_PROP = "io.md.responseHeaders";
	public static final String MD_STATUS_PROP = "io.md.status";
	public static final String MD_BODY_PROP = "io.md.body";
	public static final String MD_LOG_STREAM_PROP = "io.md.logStream";

	//Sampling Related
	public static final String MD_SAMPLE_REQUEST = "io.md.sampleRequest";
	public static final String MD_SAMPLER_TYPE = "io.md.sampler.type";
	public static final String MD_SAMPLER_RATE = "io.md.sampler.rate";
	public static final String MD_SAMPLER_ACCURACY = "io.md.sampler.accuracy";
	public static final String MD_SAMPLER_HEADER_PARAMS = "io.md.sampler.headerParams";
	public static final String MD_SAMPLER_VETO = "io.md.sampler.veto";

	//Header Baggage/ Intent
	public static final String ZIPKIN_HEADER_BAGGAGE_INTENT_KEY = "intent";
	public static final String INTENT_RECORD = "record";
	public static final String INTENT_MOCK = "mock";
	public static final String NO_INTENT = "normal";
	public static final String REPLAY = "replay";
	public static final String ZIPKIN_TRACE_HEADER = "x-b3-traceid";
	public static final String ZIPKIN_SPAN_HEADER = "x-b3-spanid";
	public static final String ZIPKIN_PARENT_SPAN_HEADER = "x-b3-parentspanid";
	public static final String MD_SCOPE = "md-scope";
	public static final String MD_CHILD_SPAN = "md-child-span";
	public static final String MD_TRACE_INFO = "md-trace-info";
	public static final String MD_IS_SAMPLED = "md-sampled";
	public static final String MD_IS_VETOED = "md-vetoed";
	public static final String MD_QUERY_PARAMS = "md-query-params";
	public static final String MD_PARENT_SPAN = "md-parent-span";

	//Thrift Related
	public static final String THRIFT_SPAN_ARGUMENT_NAME = "meshd_span";

	// Param keys to Data Object Factory
	public static final String GSON_OBJECT = "gson";
	public static final String OBJECT_MAPPER = "objectMapper";
	public static final String THRIFT_CLASS_NAME = "thriftClassName";
	public static final String THRIFT_METHOD_NAME  = "thirftMethodName";
	public static final String CLASS_LOADER = "classLoader";

	// Cryptography
	// Algos https://docs.oracle.com/javase/8/docs/technotes/guides/security/crypto/CryptoSpec.html#Cipher
	public static final String AES_CBC_PKCS5_ALGO = "AES/CBC/PKCS5Padding";
	public static final String AES_CTR_PKCS5_ALGO = "AES/CTR/PKCS5Padding";
	public static final String AES_CIPHER_KEY_TYPE = "AES";
	public static final String DEFAULT_PASS_PHRASE = "WubbaLubbaDubDub";
	public static final String CIPHER_KEY_TYPE_FIELD = "cipherKeyType";


	// Performance Spans For Ingress / Egress filters
	public static final String PROCESS_REQUEST_INGRESS = "reqProcessIngress";
	public static final String PROCESS_RESPONSE_INGRESS = "respProcessIngress";
	public static final String COPY_REQUEST_BODY_INGRESS = "reqBodyIngress";
	public static final String COPY_RESPONSE_BODY_INGRESS = "respBodyIngress";
	public static final String CREATE_REQUEST_EVENT_INGRESS = "reqEventCreateIngress";
	public static final String CREATE_RESPONSE_EVENT_INGRESS = "respEventCreateIngress";
	public static final String LOG_REQUEST_EVENT_INGRESS = "reqEventLogIngress";
	public static final String LOG_RESPONSE_EVENT_INGRESS = "respEventLogIngress";


	public static final String PROCESS_REQUEST_EGRESS = "reqProcessEgress";
	public static final String PROCESS_RESPONSE_EGRESS = "respProcessEgress";
	public static final String COPY_REQUEST_BODY_EGRESS = "reqBodyEgress";
	public static final String COPY_RESPONSE_BODY_EGRESS = "respBodyEgress";
	public static final String CREATE_REQUEST_EVENT_EGRESS = "reqEventCreateEgress";
	public static final String CREATE_RESPONSE_EVENT_EGRESS = "respEventCreateEgress";
	public static final String LOG_REQUEST_EVENT_EGRESS = "reqEventLogEgress";
	public static final String LOG_RESPONSE_EVENT_EGRESS = "respEventLogEgress";


	public static final String LOG4J_LOG = "log4jLog";
	public static final String ENCRYPT_PAYLOAD = "encryptPayload";


	public static final String MULTIPART_FIELD_TYPE = "field";
	public static final String MULTIPART_FILE_TYPE = "file";
	public static final String MULTIPART_VALUE = "value";
	public static final String MULTIPART_TYPE = "type";
	public static final String MULTIPART_FILENAME = "filename";
	public static final String MULTIPART_CONTENT_TYPE = "content-type";

	public static final String SPREADSHEET_XML = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
	public static final String PDF  = "application/pdf";

	public static final String HARD_DELETE = "hardDelete";

	public static final String GOLDEN_REQUEST = "Golden.Request";
	public static final String GOLDEN_RESPONSE = "Golden.Response";
	public static final String TESTSET_REQUEST = "TestSet.Request";
	public static final String TESTSET_RESPONSE = "TestSet.Response";
	public static final String TEMP_DIR = "/tmp";
	public static final Integer GRPC_SUCCESS_STATUS_CODE = 0;
	public static final String PARENTSPANID_SPECIAL_CHARACTERS = "ffffffffffffffff";
	public static final String HTTP_POST = "POST";
	public static final String GRPC_DEFAULT_HTTP_MEHTOD = HTTP_POST;




}
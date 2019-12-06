package com.cube.utils;

public class Constants {
    public static final String APPLICATION_X_NDJSON = "application/x-ndjson";
    public static final String APPLICATION_X_MSGPACK = "application/x-msgpack";
    public static final String DEFAULT_TRACE_FIELD = "x-b3-traceid";
    public static final String DEFAULT_SPAN_FIELD = "x-b3-spanid";
    public static final String DEFAULT_PARENT_SPAN_FIELD = "x-b3-parentspanid";

    public static final String DEFAULT_TEMPLATE_VER = "DEFAULT";

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
    public static final String ARGS_PATH = "/args";
    public static final String FN_RESPONSE_PATH = "/response";
    public static final String HDR_PATH = "/hdr";
    public static final String META_PATH = "/meta";
    public static final String BODY_PATH = "/body";

    public static final String THRIFT_CLASS_NAME = "thriftClassName";
    public static final String THRIFT_METHOD_NAME  = "thirftMethodName";
    public static final String CLASS_LOADER = "classLoader";


    //COMMON STRINGS
    public static final String CUSTOMER_ID_FIELD = "customerId";
    public static final String APP_FIELD = "app";
    public static final String SERVICE_FIELD = "service";
    public static final String INSTANCE_ID_FIELD = "instanceId";
    public static final String COLLECTION_FIELD = "collection";
    public static final String RUN_TYPE_FIELD = "runType";
    public static final String PATH_FIELD = "path";
    public static final String API_PATH_FIELD = "apiPath";
    public static final String REQ_ID_FIELD = "reqId";
    public static final String REPLAY_ID_FIELD = "replayId";
    public static final String TIMESTAMP_FIELD = "timestamp";
    public static final String TRACE_ID_FIELD = "traceId";
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


}

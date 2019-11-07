package com.cube.utils;

public class Constants {
    public static final String APPLICATION_X_NDJSON = "application/x-ndjson";
    public static final String APPLICATION_X_MSGPACK = "application/x-msgpack";
    public static final String DEFAULT_TEMPLATE_VER = "DEFAULT";

    //API RESPONSE STRINGS
    public static final String STATUS = "status";
    public static final String SUCCESS = "success";
    public static final String FAIL = "fail";
    public static final String ERROR = "error";
    public static final String DATA = "data";
    public static final String RESPONSE_OBJ = "responseObj";
    public static final String MESSAGE_ID = "messageId";
    public static final String MESSAGE = "message";


    //MESSAGE_IDs
    public static final String EVENT_NOT_FOUND = "EVENT_NOT_FOUND";
    public static final String STORE_EVENT_FAILED = "STORE_EVENT_FAILED";
    public static final String INVALID_EVENT = "INVALID_EVENT";
    public static final String INVALID_INPUT = "INVALID_INPUT";
    public static final String TEMPLATE_STORE_FAILED = "TEMPLATE_STORE_FAILED";
    public static final String TEMPLATE_META_STORE_FAILED = "TEMPLATE_META_STORE_FAILED";



    //EXCEPTIONS
    public static final String IO_EXCEPTION = "IO_EXCEPTION";
    public static final String RUNTIME_EXCEPTION = "RUNTIME_EXCEPTION";
    public static final String JSON_PARSING_EXCEPTION = "JSON_PARSING_EXCEPTION";

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

}

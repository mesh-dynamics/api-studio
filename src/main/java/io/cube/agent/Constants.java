package io.cube.agent;

public class Constants {

	//Properties
	public static final String MD_RECORD_SERVICE_PROP = "io.md.service.record";
	public static final String CUBE_MOCK_SERVICE_PROP = "io.md.service.mock";
	public static final String READ_TIMEOUT_PROP = "io.md.read.timeout";
	public static final String CONNECT_TIMEOUT_PROP = "io.md.connect.timeout";
	public static final String RETRIES_PROP = "io.md.connect.retries";
	public static final String MD_CUSTOMER_PROP = "io.md.customer";
	public static final String MD_APP_PROP = "io.md.app";
	public static final String MD_INSTANCE_PROP = "io.md.instance";
	public static final String MD_SERVICE_PROP = "io.md.service";
	public static final String MD_INTENT_PROP = "io.md.intent";

	//Header Baggage/ Intent
	public static final String ZIPKIN_HEADER_BAGGAGE_INTENT_KEY = "intent";
	public static final String INTENT_RECORD = "record";
	public static final String INTENT_MOCK = "mock";
	public static final String NO_INTENT = "normal";
	public static final String ZIPKIN_TRACE_HEADER = "x-b3-traceid";
	public static final String ZIPKIN_SPAN_HEADER = "x-b3-spanid";
	public static final String ZIPKIN_PARENT_SPAN_HEADER = "x-b3-parentspanid";

	//Thrift Related
	public static final String THRIFT_SPAN_ARGUMENT_NAME = "meshd_span";


	//COMMON STRINGS
	public static final String CUSTOMER_ID_FIELD = "customerId";
	public static final String APP_FIELD = "app";
	public static final String SERVICE_FIELD = "service";
	public static final String INSTANCE_ID_FIELD = "instanceId";
	public static final String RUN_TYPE_FIELD = "runType";
	public static final String PATH_FIELD = "path";
	public static final String API_PATH_FIELD = "apiPath";
	public static final String TIMESTAMP_FIELD = "timestamp";
	public static final String TYPE_FIELD = "type";
	public static final String METHOD_FIELD = "method";
	public static final String META_FIELD = "meta";
	public static final String MESSAGE = "message";
	public static final String REASON = "reason";
}


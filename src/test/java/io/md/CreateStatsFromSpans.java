package io.md;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.glassfish.jersey.internal.guava.MoreObjects.ToStringHelper;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CreateStatsFromSpans {

	public static void main(String[] args) {
		try {

			ObjectMapper mapper = new ObjectMapper();
			mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
			JsonFactory jsonFactory = new JsonFactory();
			jsonFactory.setCodec(mapper);
			JsonParser parser = jsonFactory.createParser(new FileInputStream("/Users/ravivj/100k.txt"));

			//parser.read

			/*parser.nextToken();*/ // start object
			parser.nextToken();
			System.out.println(parser.currentToken().name());
			// start object
			parser.nextToken();
			System.out.println(parser.currentToken().name());
			// data field name
			parser.nextToken();
			System.out.println(parser.currentToken().name());
			// start array
			int objectCount = 0;
			List<Long> respBodySpans = new ArrayList<>();
			List<Long> respLogSpans = new ArrayList<>();
			List<Long> respCreateEventSpans = new ArrayList<>();
			List<Long> respProcessingSpans = new ArrayList<>();
			while (parser.nextToken() != JsonToken.END_ARRAY) {
				System.out.println(parser.currentToken().name());

				Trace trace = parser.readValueAs(Trace.class);
				objectCount ++;
				System.out.println("Trace ID:: " + trace.traceID);
				if (trace.spans != null) {
					trace.spans.forEach(span -> {
						//if (span.operationName.equals("respBody"))
						System.out.println("Span ID :: " + span.spanID + " operationName :: " + span.operationName);
						if (span.operationName.equals("reqLog")) {
							respProcessingSpans.add(span.duration);
						} else if (span.operationName.equals("reqEventCreate")) {
							respCreateEventSpans.add(span.duration);
						} else if (span.operationName.equals("reqEventLog")) {
							respLogSpans.add(span.duration);
						} else if (span.operationName.equals("reqBody")) {
							respBodySpans.add(span.duration);
						}
					});
				}
				//processDataObject(parser);
			}

			Collections.sort(respBodySpans);
			Collections.sort(respLogSpans);
			Collections.sort(respCreateEventSpans);
			Collections.sort(respProcessingSpans);


			System.out.println("Req Body Copy :: " + createStats(respBodySpans));
			System.out.println("Req Event Create :: " + createStats(respCreateEventSpans));
			System.out.println("Req Logging :: " + createStats(respLogSpans));
			System.out.println("Req Total Processing :: " + createStats(respProcessingSpans));
			System.out.println("Object count ::" + objectCount);
			parser.nextToken();
			// end array

			parser.nextToken(); // end object

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static class Stats {
		double average;
		long median;
		long percentile_90;
		long percentile_99;

		public String toString() {
			return new ToStringBuilder(this , ToStringStyle.NO_CLASS_NAME_STYLE).append(" average", this.average*1.0/1000 + " ms ")
				.append(" median" , this.median*1.0/1000 + " ms ")
				.append(" 90_percentile" , this.percentile_90*1.0/1000 + " ms ")
				.append(" 99_percentile"  , this.percentile_99*1.0/1000 + " ms ").toString();
		}
	}

	public static Stats createStats(List<Long> arr) {
		Stats stats = new Stats();
		long sum = 0;
		for (int i = 0; i < arr.size() ; i++) {
			if (i == arr.size()*.9) {
				stats.percentile_90 = arr.get(i);
			}
			if (i == arr.size()*.99) {
				stats.percentile_99 = arr.get(i);
			}
			if (i == arr.size()*.5) {
				stats.median = arr.get(i);
			}
			sum += arr.get(i);
		}

		stats.average = sum*1.0/arr.size();
		return stats;

	}

	static class Reference {
		public Reference() {

		}
	}

	static class Tag {
		public Tag() {

		}

		public String key;
		public String type;
		public String value;
	}

	static class Field {
		public Field() {

		}

		public String key;
		public String type;
		public String value;
	}

	static class Log {

		public Log() {

		}
		public Long timestamp;
		public List<Field> fields;
	}

	static class Warning {
		public Warning() {

		}
	}

	static class Span {

		public Span() {

		}

		public String traceID;
		public String spanID;
		public int flags;
		public String operationName;
		public List<Reference> references;
		public Long startTime;
		public Long duration;
		public List<Tag> tags;
		public List<Log> logs;
		public String processID;
		public List<Warning> warnings;
	}

	static class Trace {

		public Trace() {

		}

		public String traceID;
		public List<Span> spans;
	}

	/**
	 *{
	 * 	"data": [{
	 * 		"traceID": "646f44bbcb066f97",
	 * 		"spans": [{
	 * 			"traceID": "646f44bbcb066f97",
	 * 			"spanID": "646f44bbcb066f97",
	 * 			"flags": 1,
	 * 			"operationName": "service-md-childspan",
	 * 			"references": [],
	 * 			"startTime": 1582526110694000,
	 * 			"duration": 3471,
	 * 			"tags": [{
	 * 				"key": "runId",
	 * 				"type": "string",
	 * 				"value": "async-log-100k"
	 *            }, {
	 * 				"key": "span.kind",
	 * 				"type": "string",
	 * 				"value": "server"
	 *            }, {
	 * 				"key": "sampler.type",
	 * 				"type": "string",
	 * 				"value": "const"
	 *            }, {
	 * 				"key": "sampler.param",
	 * 				"type": "bool",
	 * 				"value": true
	 *            }, {
	 * 				"key": "internal.span.format",
	 * 				"type": "string",
	 * 				"value": "proto"
	 *            }],
	 * 			"logs": [{
	 * 				"timestamp": 1582526110694000,
	 * 				"fields": [{
	 * 					"key": "event",
	 * 					"type": "string",
	 * 					"value": "baggage"
	 *                }, {
	 * 					"key": "key",
	 * 					"type": "string",
	 * 					"value": "md-sampled"
	 *                }, {
	 * 					"key": "value",
	 * 					"type": "string",
	 * 					"value": "true"
	 *                }]
	 *            }],
	 * 			"processID": "p1",
	 * 			"warnings": null* 		}],
	 * 		"processes": {
	 * 			"p1": {
	 * 				"serviceName": "tracer",
	 * 				"tags": [{
	 * 					"key": "hostname",
	 * 					"type": "string",
	 * 					"value": "movieinfo-v1-b85b96779-q5xjt"
	 *                }, {
	 * 					"key": "ip",
	 * 					"type": "string",
	 * 					"value": "100.96.117.128"
	 *                }, {
	 * 					"key": "jaeger.version",
	 * 					"type": "string",
	 * 					"value": "Java-1.1.0"
	 *                }]
	 *            }* 		},
	 * 		"warnings": null
	 * 	}],
	 * 	"total": 0,
	 * 	"limit": 0,
	 * 	"offset": 0,
	 * 	"errors": null
	 * }
	 */

	public static void processDataObject(JsonParser parser) {
		try {
			//if (parser.nextToken() != JsonToken.START_OBJECT) return;
			while (parser.nextToken() != JsonToken.END_OBJECT) {
				String fieldName = parser.getCurrentName();
				if (fieldName.equals("traceID")) {
					System.out.println("TraceId :: " + parser.nextToken().asString());
				} else if (fieldName.equals("spanID")) {
					System.out.println();
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


}

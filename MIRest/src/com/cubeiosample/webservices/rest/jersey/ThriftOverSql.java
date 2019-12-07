package com.cubeiosample.webservices.rest.jersey;

import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.json.JSONArray;
import org.json.JSONObject;

import com.cubeio.thriftwrapjdbc.ThriftWrapJDBC;

import io.jaegertracing.internal.JaegerSpan;
import io.jaegertracing.thrift.internal.reporters.protocols.JaegerThriftSpanConverter;
import io.jaegertracing.thriftjava.Span;
import io.opentracing.Scope;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;

public class ThriftOverSql {

	private ThriftWrapJDBC.Client thriftClient;
	private Tracer tracer = null;
	private Config config = null;

	final static Logger LOGGER = Logger.getLogger(ThriftOverSql.class);

	public ThriftOverSql(Tracer tracer, Config config) {
		this.tracer = tracer;
		this.config = config;
		TTransport transport = new TSocket("thriftwrapjdbc", 9090);
		try {
			transport.open();
			TProtocol protocol = new TBinaryProtocol(transport);
			thriftClient = new ThriftWrapJDBC.Client(protocol);
		} catch (TTransportException e) {
			LOGGER.error("Unable to start thrift client");
		}

		initializeJDBCService();
	}


	public static Scope startClientSpan(String operationName) {
		Tracer tracer = GlobalTracer.get();
		Tracer.SpanBuilder spanBuilder = tracer.buildSpan(operationName);
		return spanBuilder.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT)
			.startActive(true);
	}

	public Span getCurrentSpan() {
		JaegerSpan currentSpan = (JaegerSpan) tracer.activeSpan();
		LOGGER.info(
			"Jaeger Span (In Thrift Over Sql), Before Client Call :: " + currentSpan.toString());
		Span toReturn = JaegerThriftSpanConverter.convertSpan(currentSpan);
		LOGGER
			.info("Thrift Span (In Thrift Over Sql), Before Client Call :: " + toReturn.toString());
		return toReturn;
	}

	private void initializeJDBCService() {
		String username = config.userName();
		String pwd = config.passwd();
		String uri = config.baseDbUri();
		LOGGER.debug("init jdbc service tracer: ");
		LOGGER.debug(tracer.toString());
		try {
			String response = thriftClient.initialize(username, pwd, uri, getCurrentSpan());
			LOGGER.debug(
				"intialized jdbc service " + uri + "; " + username + "; " + response);
		} catch (TException e) {
			LOGGER.error("Unable to initialize db connection pool in thrift server");
		}
	}


	public String getHealth() {
		try {
			return thriftClient.health(getCurrentSpan());
		} catch (Exception e) {
			return "Error while getting health";
		}
	}


	public JSONArray executeQuery(String query, JSONArray params) {
		try {
			String response = thriftClient.query(query, params.toString(), getCurrentSpan());
			JSONArray result = new JSONArray(response);
			if (result != null) {
				LOGGER.debug(
					"Query: " + query + "; " + params.toString() + "; NumRows=" + result.length());
			}
			return result;
		} catch (Exception e) {
			LOGGER.error(String
				.format("Execute query failed: %s, params: %s; %s", query, params, e.toString()));
		}
		return new JSONArray("[{}]");
	}


	public JSONObject executeUpdate(String query, JSONArray params) {
		try {
			JSONObject body = new JSONObject();
			body.put("query", query);
			body.put("params", params);
			String updateResponse = thriftClient.update(body.toString(), getCurrentSpan());
			// TODO: figure out the best way of extracting json array from the entity
			JSONObject result = new JSONObject(updateResponse);
			LOGGER.debug("Update: " + query + "; " + params.toString() + "; " + result.toString());
			return result;
		} catch (Exception e) {
			LOGGER.error(
				String.format("Update failed: %s, params: %s; %s", query, params, e.toString()));
		}
		return new JSONObject("{\"num_updates\": \"-1\"}");
	}


}

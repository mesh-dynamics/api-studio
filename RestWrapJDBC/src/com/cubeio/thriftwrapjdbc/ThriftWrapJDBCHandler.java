package com.cubeio.thriftwrapjdbc;

import java.util.Map;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.json.JSONArray;
import org.json.JSONObject;

import com.cubeio.thriftwrapjdbc.ThriftWrapJDBC.Iface;

import io.cube.agent.CommonUtils;
import io.cube.utils.ConnectionPool;
import io.cube.utils.Tracing;
import io.jaegertracing.internal.JaegerTracer;
import io.cube.tracing.thriftjava.Span;
import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;

public class ThriftWrapJDBCHandler implements Iface {

	final static Logger LOGGER;
	private static ConnectionPool jdbcPool = null;
	static final JaegerTracer tracer;


	static {
		LOGGER = Logger.getLogger(ThriftWrapJDBC.class);
		tracer = Tracing.init("ThriftWrapJDBC");
		GlobalTracer.register(tracer);
		BasicConfigurator.configure();
		String MYSQL_USERNAME = "cube";
		String MYSQL_PWD = "cubeio12";  // AWS RDS pwd
		String MYSQL_URI = "jdbc:mysql://sakila2.cnt3lftdrpew.us-west-2.rds.amazonaws.com:3306/sakila";
		initJdbc(MYSQL_URI, MYSQL_USERNAME, MYSQL_PWD);
	}


	static private void initJdbc(String uri, String username, String passwd) {
		try {
			jdbcPool = new ConnectionPool();
			jdbcPool.setUpPool(uri, username, passwd);
			LOGGER.info("mysql uri: " + uri);
			LOGGER.info(jdbcPool.getPoolStatus());
		} catch (Exception e) {
			LOGGER.error("connection pool creation failed; " + e.toString());
		}
	}


	@Override
	public String health(Span span) throws TException {
		return null;
	}

	@Override
	public String initialize(String username, String password, String uri, Span span)
		throws PoolCreationException, TException {
		try (Scope scope = CommonUtils.startServerSpan(span, "initialize")) {
			scope.span().setTag("initialize", uri + "; " + username + "; <pwd>");
			initJdbc(uri, username, password);
			return (new JSONObject(Map.of("status", "Connection Pool Created"))).toString();
		} catch (Exception e) {
			throw new PoolCreationException("Error occurred while creating pool " + e.getMessage());
		}
	}

	@Override
	public String query(String query, String params, Span span)
		throws GenericThriftWrapException, TException {
		JSONArray result = null;
		JSONArray jsonParams = new JSONArray(params);
		LOGGER.info("Thrift Span Received :: " + span.toString());
		try (Scope scope = CommonUtils.startServerSpan(span, "serverQuery")) {
			scope.span().setTag("query", query);
			LOGGER.info("Jaeger Converted Span :: " + scope.span().toString());
			//LOGGER.info("INTENT :: " + scope.span().getBaggageItem("intent"));
			result = jdbcPool.executeQuery(query, jsonParams);
			LOGGER.info(jdbcPool.getPoolStatus());
			return result.toString();
		} catch (Exception e) {
			throw new GenericThriftWrapException(
				"Query failed: " + query + "; " + params + "; " + e.toString());
		}
	}

	@Override
	public String update(String queryAndParam, Span span)
		throws GenericThriftWrapException, TException {
		JSONObject result = null;
		try (Scope scope = CommonUtils.startServerSpan(span, "updateQuery")) {
			scope.span().setTag("update", queryAndParam);
			JSONObject queryAndParams = new JSONObject(queryAndParam);
			JSONArray params = queryAndParams.getJSONArray("params");
			String query = queryAndParams.getString("query");
			LOGGER.debug("Update stmt: " + query + "; params: " + params.toString());
			result = jdbcPool.executeUpdate(query, params);
			LOGGER.info(jdbcPool.getPoolStatus());
			return result.toString();
		} catch (Exception e) {
			//result = new JSONObject();
			//LOGGER.error("Update query failed: " + queryAndParam + "; " + e.toString());
			//result.put("num_updates", -10);
			//result.put("exception", e.toString());
			throw new GenericThriftWrapException(
				"Update query failed: " + queryAndParam + "; " + e.toString());
		}
	}

}

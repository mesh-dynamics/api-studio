package com.cubeiosample.webservices.rest.jersey;

import io.cube.utils.RestUtils;
import io.opentracing.Tracer;

import java.util.Properties;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.uri.UriComponent;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class RestOverSql {

	private Client restClient = null;
	private WebTarget restJDBCService = null;
	private Tracer tracer = null;
	private Config config = null;

	final static Logger LOGGER = Logger.getLogger(RestOverSql.class);


	Properties properties;

	public RestOverSql(Tracer tracer, Config config) {
		this.tracer = tracer;
		this.config = config;
		ClientConfig clientConfig = new ClientConfig()
			.property(ClientProperties.READ_TIMEOUT, 100000)
			.property(ClientProperties.CONNECT_TIMEOUT, 10000);
		restClient = ClientBuilder.newClient(clientConfig);

		LOGGER.debug("RESTWRAPJDBC_URI is " + config.RESTWRAPJDBC_URI);
		restJDBCService = restClient.target(config.RESTWRAPJDBC_URI);

		initializeJDBCService();
	}


	private void initializeJDBCService() {
		String username = config.userName();
		String pwd = config.passwd();
		String uri = config.baseDbUri();
		LOGGER.debug("init jdbc service tracer: ");
		LOGGER.debug(tracer.toString());
		Response response = RestUtils.callWithRetries(tracer,
			restJDBCService.path("initialize").queryParam("username", username)
				.queryParam("password", pwd).queryParam("uri", uri)
				.request(MediaType.APPLICATION_JSON), null, "GET", 3, config.ADD_TRACING_HEADERS);
		LOGGER.debug(
			"intialized jdbc service " + uri + "; " + username + "; " + response.getStatus() + "; "
				+ response.readEntity(String.class));
		response.close();
	}


	public String getHealth() {
		Response response = RestUtils.callWithRetries(tracer,
			restJDBCService.path("health").request(MediaType.APPLICATION_JSON), null, "GET", 3,
			config.ADD_TRACING_HEADERS);
		String result = response.readEntity(String.class);
		response.close();
		return result;
	}


	public JSONArray executeQuery(String query, JSONArray params) {
		Response response = null;
		try {
			response = RestUtils.callWithRetries(tracer,
				restJDBCService.path("query").queryParam("querystring", query).queryParam("params",
					UriComponent
						.encode(params.toString(), UriComponent.Type.QUERY_PARAM_SPACE_ENCODED))
					.request(MediaType.APPLICATION_JSON),
				null, "GET", 3, config.ADD_TRACING_HEADERS);
			JSONArray result = new JSONArray(response.readEntity(String.class));
			if (result != null) {
				LOGGER.debug(
					"Query: " + query + "; " + params.toString() + "; NumRows=" + result.length());
			}
			response.close();
			return result;
		} catch (Exception e) {
			LOGGER.error(String
				.format("Execute query failed: %s, params: %s; %s", query, params, e.toString()));
			if (response != null) {
				LOGGER.error(response.toString());
			}
		}
		// empty array.
		return new JSONArray("[{}]");
	}


	public JSONObject executeUpdate(String query, JSONArray params) {
		Response response = null;
		try {
			JSONObject body = new JSONObject();
			body.put("query", query);
			body.put("params", params);
			response = RestUtils
				.callWithRetries(tracer, restJDBCService.path("update").request(), body, "POST", 3,
					config.ADD_TRACING_HEADERS);

			// TODO: figure out the best way of extracting json array from the entity
			JSONObject result = new JSONObject(response.readEntity(String.class));
			LOGGER.debug("Update: " + query + "; " + params.toString() + "; " + result.toString());
			response.close();
			return result;
		} catch (Exception e) {
			LOGGER.error(
				String.format("Update failed: %s, params: %s; %s", query, params, e.toString()));
			if (response != null) {
				LOGGER.error(response.toString());
			}
		}
		return new JSONObject("{\"num_updates\": \"-1\"}");
	}

	// parameter binding methods
	public static void addStringParam(JSONArray params, String value) {
		try {
			JSONObject param = new JSONObject();
			param.put("index", params.length() + 1);
			param.put("type", "string");
			param.put("value", value);
			params.put(param);
		} catch (Exception e) {
			LOGGER.error(String
				.format("addStringParam failed: params: %s, value: %s", params.toString(), value));
		}
	}

	public static void addIntegerParam(JSONArray params, Integer value) {
		try {
			JSONObject param = new JSONObject();
			param.put("index", params.length() + 1);
			param.put("type", "integer");
			param.put("value", value);
			params.put(param);
		} catch (Exception e) {
			LOGGER.error(String
				.format("addStringParam failed: params: %s, value: %d", params.toString(), value));
		}
	}

	public static void addDoubleParam(JSONArray params, Double value) throws JSONException {
		try {
			JSONObject param = new JSONObject();
			param.put("index", params.length() + 1);
			param.put("type", "double");
			param.put("value", value);
			params.put(param);
		} catch (Exception e) {
			LOGGER.error(String
				.format("addStringParam failed: params: %s, value: %f", params.toString(), value));
		}
	}

}

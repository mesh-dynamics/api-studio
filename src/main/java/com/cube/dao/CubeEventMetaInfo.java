package com.cube.dao;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.md.dao.Event;
import com.cube.utils.Constants;

public class CubeEventMetaInfo {

	private Optional<String> customer = Optional.empty();
	private Optional<String> app = Optional.empty();
	private Optional<String> instance = Optional.empty();
	private  Optional<String> service = Optional.empty();
	private  Optional<String> path = Optional.empty();
	private  Optional<String> runType = Optional.empty();
	private Optional<String> reqId = Optional.empty();
	private Optional<String> collection = Optional.empty();
	private Optional<String> traceId = Optional.empty();
	private Optional<String> eventType = Optional.empty();
	private Optional<String> eventStr = Optional.empty();

	public CubeEventMetaInfo() {
	}

	public CubeEventMetaInfo(Event e) {
		this.customer = Optional.ofNullable(e.customerId);
		this.app = Optional.ofNullable(e.app);
		this.instance = Optional.of(e.instanceId);
		this.reqId = Optional.ofNullable(e.reqId);
		this.service = Optional.ofNullable(e.service);
		this.path = Optional.ofNullable(e.apiPath);
		this.runType = Optional.ofNullable(e.runType).map(Enum::name);
		this.collection = Optional.ofNullable(e.getCollection());
		this.traceId = Optional.ofNullable(e.getTraceId());
		this.eventType = Optional.ofNullable(e.eventType).map(Enum::name);
		this.eventStr = Optional.of(e.toString());
	}

	public Optional<String> getService() {
		return service;
	}

	public void setService(Optional<String> service) {
		this.service = service;
	}

	public Optional<String> getPath() {
		return path;
	}

	public void setPath(Optional<String> path) {
		this.path = path;
	}

	public Optional<String> getRunType() {
		return runType;
	}

	public void setRunType(Optional<String> runType) {
		this.runType = runType;
	}

	public Optional<String> getReqId() {
		return reqId;
	}

	public void setReqId(Optional<String> reqId) {
		this.reqId = reqId;
	}

	public Optional<String> getCollection() {
		return collection;
	}

	public void setCollection(Optional<String> collection) {
		this.collection = collection;
	}

	public Optional<String> getCustomer() {
		return customer;
	}

	public void setCustomer(Optional<String> customer) {
		this.customer = customer;
	}

	public Optional<String> getApp() {
		return app;
	}

	public void setApp(Optional<String> app) {
		this.app = app;
	}

	public Optional<String> getInstance() {
		return instance;
	}

	public void setInstance(Optional<String> instance) {
		this.instance = instance;
	}

	public Optional<String> getTraceId() {
		return traceId;
	}

	public void setTraceId(Optional<String> traceId) {
		this.traceId = traceId;
	}

	public Optional<String> getEventType() {
		return eventType;
	}

	public void setEventType(Optional<String> eventType) {
		this.eventType = eventType;
	}

	public Optional<String> getEventStr() {
		return eventStr;
	}

	public void setEventStr(Optional<String> eventStr) {
		this.eventStr = eventStr;
	}

	public Map<String, String> getPropertiesMap() {
		Map<String, String> properties = new HashMap<>(Map.of(
			Constants.CUSTOMER_ID_FIELD, customer.orElse(Constants.NOT_PRESENT)
			, Constants.APP_FIELD, app.orElse(Constants.NOT_PRESENT)
			, Constants.INSTANCE_ID_FIELD, instance.orElse(Constants.NOT_PRESENT)
			, Constants.SERVICE_FIELD, service.orElse(Constants.NOT_PRESENT)
			, Constants.PATH_FIELD, path.orElse(Constants.NOT_PRESENT)
			, Constants.EVENT_TYPE_FIELD, eventType.orElse(Constants.NOT_PRESENT)
			, Constants.REQ_ID_FIELD, reqId.orElse(Constants.NOT_PRESENT)
			, Constants.COLLECTION_FIELD, collection.orElse(Constants.NOT_PRESENT)
			, Constants.EVENT_STRING, eventStr.orElse(Constants.NOT_PRESENT)
			, Constants.TRACE_ID_FIELD, traceId.orElse(Constants.NOT_PRESENT)
		));
		properties.put(Constants.RUN_TYPE_FIELD, runType.orElse(Constants.NOT_PRESENT));
		return properties;
	}
}

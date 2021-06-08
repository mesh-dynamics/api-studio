package io.md.core;

import java.util.HashMap;
import java.util.Map;

public class ConfigApplicationAcknowledge {

	public final String customerId;
	public final String app;
	public final String service;
	public final String instanceId;

	// Here you can send
	public final Map<String, String> acknowledgeInfo;

	public ConfigApplicationAcknowledge() {
		this.customerId = null;
		this.app = null;
		this.service = null;
		this.instanceId = null;
		acknowledgeInfo = new HashMap<>();
	}

	public ConfigApplicationAcknowledge(String customerId, String app, String service
		, String instanceId, Map<String, String> extraInfo) {
		this.customerId = customerId;
		this.app = app;
		this.service = service;
		this.instanceId = instanceId;
		this.acknowledgeInfo = extraInfo;
	}


}

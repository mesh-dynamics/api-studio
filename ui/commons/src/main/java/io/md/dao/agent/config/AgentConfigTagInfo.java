package io.md.dao.agent.config;

public class AgentConfigTagInfo {

	public final String customerId;
	public final String app;
	public final String service;
	public final String instanceId;
	public final String tag;

	public AgentConfigTagInfo() {
		this.customerId = null;
		this.app = null;
		this.service = null;
		this.instanceId = null;
		this.tag = null;
	};

	public AgentConfigTagInfo(String customerId, String app
		, String service, String instance, String tag) {
		this.customerId = customerId;
		this.app = app;
		this.service = service;
		this.instanceId = instance;
		this.tag = tag;
	}



}

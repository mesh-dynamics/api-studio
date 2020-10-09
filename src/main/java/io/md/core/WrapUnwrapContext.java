package io.md.core;

import io.md.dao.ProtoDescriptorDAO;

public class WrapUnwrapContext {

	public final ProtoDescriptorDAO protoDescriptor;
	public final String service;
	public final String method;
	public final boolean isRequest;

	public WrapUnwrapContext(ProtoDescriptorDAO protoDescriptor, String service, String method,
							 boolean isRequest) {
		this.protoDescriptor = protoDescriptor;
		this.service = service;
		this.method = method;
		this.isRequest = isRequest;
	}

}

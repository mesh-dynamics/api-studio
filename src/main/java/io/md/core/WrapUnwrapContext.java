package io.md.core;

public class WrapUnwrapContext {

	public final ProtoDescriptor protoDescriptor;
	public final String service;
	public final String method;
	public final boolean isRequest;

	public WrapUnwrapContext(ProtoDescriptor protoDescriptor, String service, String method,
							 boolean isRequest) {
		this.protoDescriptor = protoDescriptor;
		this.service = service;
		this.method = method;
		this.isRequest = isRequest;
	}

}

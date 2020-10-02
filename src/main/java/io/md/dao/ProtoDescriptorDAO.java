package io.md.dao;


public class ProtoDescriptorDAO {
	public Integer version;
	public final String customerId;
	public final String app;
	public final String encodedFile;

	public ProtoDescriptorDAO() {
		this.customerId = null;
		this.version = null;
		this.app = null;
		this.encodedFile = null;
	}

	public ProtoDescriptorDAO(String customerId, String app, String encodedFile) {
		this.customerId = customerId;
		this.app = app;
		this.encodedFile = encodedFile;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

}

package io.md.dao;


public class ProtoDescriptorDAO {
	public Integer version;
	public final String customerId;
	public final String app;
	public String encodedFile;

	public ProtoDescriptorDAO() {
		this.customerId = null;
		this.version = null;
		this.app = null;
		this.encodedFile = null;
	}

	public ProtoDescriptorDAO(String customerId, String app) {
		this.customerId = customerId;
		this.app = app;
	}

	public ProtoDescriptorDAO setEncodedFile(String encodedFile) {
		this.encodedFile = encodedFile;
		return this;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

}

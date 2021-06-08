package io.md.core;

import org.apache.commons.lang3.Validate;

import io.md.dao.ProtoDescriptorDAO;


public class ValidateProtoDescriptorDAO {

	public static void validate(ProtoDescriptorDAO protoDescriptorDAO) {
		Validate.notBlank(protoDescriptorDAO.customerId);
		Validate.notBlank(protoDescriptorDAO.app);
		Validate.notNull(protoDescriptorDAO.version);
		Validate.notBlank(protoDescriptorDAO.encodedFile);
	}
}






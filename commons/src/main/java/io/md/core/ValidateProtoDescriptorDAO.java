/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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






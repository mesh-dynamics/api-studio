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

package io.md.cryptography;

import java.util.Map;

import io.md.constants.Constants;

public class EncryptionAlgorithmFactory {
	public static EncryptionAlgorithm build(String algorithm, String passPhrase, Map<String, Object> metaData) {
		switch (algorithm) {
			case Constants.AES_CBC_PKCS5_ALGO:
			case Constants.AES_CTR_PKCS5_ALGO:
				return new JcaEncryption(algorithm, metaData.get(Constants.CIPHER_KEY_TYPE_FIELD).toString(), passPhrase);
			default:
				return new JcaEncryption(passPhrase);
		}
	}

}

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

package com.cube.cryptography;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.md.cryptography.JcaEncryption;
import io.md.utils.Constants;

public class JcaEncryptionBasicTest {

	@Test
	public void test()
	{
		final String passPhrase = "pAsSwOrD@!#!";

		String originalString = "\"I am lord Voldemort. I come from @ HogWards ! \"";
		JcaEncryption encrypter = new JcaEncryption(Constants.AES_CBC_PKCS5_ALGO, Constants.AES_CIPHER_KEY_TYPE, passPhrase);
		Optional<String> encryptedString = encrypter.encrypt(originalString);
		Assertions.assertNotEquals(Optional.empty(), encryptedString);
		Optional<String> decryptedString = encrypter.decrypt(encryptedString.get());
		Assertions.assertNotEquals(Optional.empty(), encryptedString);

		System.out.println(originalString);
		System.out.println(encryptedString.get());
		System.out.println(decryptedString.get());
	}

}

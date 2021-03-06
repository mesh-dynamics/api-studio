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

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import io.md.logger.LogMgr;
import org.slf4j.Logger;

import io.md.constants.Constants;

/*
 Class for implementing encryption algorithms
 Reference - https://www.veracode.com/blog/research/encryption-and-decryption-java-cryptography
 https://howtodoinjava.com/security/java-aes-encryption-example/
 */
public class JcaEncryption implements EncryptionAlgorithm {

	private static final Logger LOGGER = LogMgr.getLogger(EncryptionAlgorithm.class);


	public JcaEncryption(String jcaAlgorithm, String cipherKeyType, String passPhrase) {
		this.jcaAlgorithm = jcaAlgorithm;
		this.cipherKeyType = cipherKeyType;
		this.passPhrase = passPhrase;
		setKey(passPhrase);
		//SecureRandom random = new SecureRandom();
		//byte[] ivBytes = new byte[16];
		//random.nextBytes(ivBytes);
		// Set constant to avoid randomization - So that any instance of this class  with same
		// passphrase generates some encryption/decryption otherwise can use above commented code.
		byte[] ivBytes = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
		initialisationVector = new IvParameterSpec(ivBytes);
	}

	public JcaEncryption() {
		this(Constants.AES_CBC_PKCS5_ALGO, Constants.AES_CIPHER_KEY_TYPE, Constants.DEFAULT_PASS_PHRASE);
	}

	public JcaEncryption(String passPhrase) {
		this(Constants.AES_CBC_PKCS5_ALGO, Constants.AES_CIPHER_KEY_TYPE, passPhrase);
	}

	public final String jcaAlgorithm;
	public final String cipherKeyType;
	public final String passPhrase;
	private SecretKeySpec secretKey;
	private byte[] key;
	private IvParameterSpec initialisationVector;

	public void setKey(String passPhrase)
	{
		MessageDigest sha = null;
		try {
			key = passPhrase.getBytes("UTF-8");
			sha = MessageDigest.getInstance("SHA-256");
			key = sha.digest(key);
			key = Arrays.copyOf(key, 16);
			secretKey = new SecretKeySpec(key, cipherKeyType);
		}
		catch (Exception e) {
			LOGGER.error("Error while setting key", e);
		}
	}

	public Optional<String> encrypt(String strToEncrypt)
	{
		try
		{
			Cipher cipher = Cipher.getInstance(jcaAlgorithm);
			cipher.init(Cipher.ENCRYPT_MODE, secretKey, initialisationVector);
			String encryptedString = Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes()));
			return Optional.of(encryptedString);
		}
		catch (Exception e)
		{
			LOGGER.error("Error while encrypting", e);
		}
		return Optional.empty();
	}

	public Optional<String> decrypt(String strToDecrypt)
	{
		try
		{
			Cipher cipher = Cipher.getInstance(jcaAlgorithm);
			cipher.init(Cipher.DECRYPT_MODE, secretKey, initialisationVector);
			return Optional.of(new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt))));
		}
		catch (Exception e)
		{
			LOGGER.error("Error while decrypting", e);

		}
		return Optional.empty();
	}

	@Override
	public Optional<byte[]> encrypt(byte[] byteArrayToEncrypt) {
		try
		{
			Cipher cipher = Cipher.getInstance(jcaAlgorithm);
			cipher.init(Cipher.ENCRYPT_MODE, secretKey, initialisationVector);
			return Optional.of(Base64.getEncoder().encode(cipher.doFinal(byteArrayToEncrypt)));
		}
		catch (Exception e)
		{
			LOGGER.error("Error while encrypting", e);
		}
		return Optional.empty();
	}

	@Override
	public Optional<byte[]> decrypt(byte[] byteArrayToDecrypt) {
		try
		{
			Cipher cipher = Cipher.getInstance(jcaAlgorithm);
			cipher.init(Cipher.DECRYPT_MODE, secretKey, initialisationVector);
			return Optional.of(cipher.doFinal(Base64.getDecoder().decode(byteArrayToDecrypt)));
		}
		catch (Exception e)
		{
			LOGGER.error("Error while decrypting", e);

		}
		return Optional.empty();
	}
}

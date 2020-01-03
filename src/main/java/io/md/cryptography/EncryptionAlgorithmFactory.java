package io.md.cryptography;

import org.json.JSONObject;

import io.md.constants.Constants;

public class EncryptionAlgorithmFactory {
	public static EncryptionAlgorithm build(String algorithm, String passPhrase, JSONObject metaData) {
		switch (algorithm) {
			case Constants.AES_CBC_PKCS5_ALGO:
			case Constants.AES_CTR_PKCS5_ALGO:
				return new JcaEncryption(algorithm, metaData.getString(Constants.CIPHER_KEY_TYPE_FIELD), passPhrase);
			default:
				return new JcaEncryption(passPhrase);
		}
	}

}

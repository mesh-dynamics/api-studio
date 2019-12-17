package com.cube.cryptography;

import org.json.JSONObject;

public class EncryptionAlgorithmFactory {
	public static EncryptionAlgorithm build(String algorithm, String passPhrase, JSONObject metaData) {
		switch (algorithm) {
			case "AES/CBC/PKCS5Padding":
			case "AES/CTR/PKCS5Padding":
				return new JcaEncryption(algorithm, metaData.getString("cipherKeyType"), passPhrase);
			default:
				return new JcaEncryption(passPhrase);
		}
	}

}

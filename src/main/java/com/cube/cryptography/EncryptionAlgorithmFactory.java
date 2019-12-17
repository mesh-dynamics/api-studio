package com.cube.cryptography;

import org.json.JSONObject;
import com.cube.utils.Constants;
public class EncryptionAlgorithmFactory {
	public static EncryptionAlgorithm build(String algorithm, String passPhrase, JSONObject metaData) {
		switch (algorithm) {
			case Constants.AES_CBC_PKCS5_ALGO:
			case Constants.AES_CTR_PKCS5_ALGO:
				return new JcaEncryption(algorithm, metaData.getString("cipherKeyType"), passPhrase);
			default:
				return new JcaEncryption(passPhrase);
		}
	}

}

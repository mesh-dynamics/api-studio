package com.cube.cryptography;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.md.cryptography.JcaEncryption;

import com.cube.utils.Constants;

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
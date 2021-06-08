package io.md.cryptography;

import java.util.Optional;

public interface EncryptionAlgorithm {
	Optional<String> encrypt(String strToEncrypt);

	Optional<String> decrypt(String strToDecrypt);

	Optional<byte[]> encrypt(byte[] byteArrayToEncrypt);

	Optional<byte[]> decrypt(byte[] byteArrayToEncrypt);

}

package io.md.cryptography;

import java.util.Optional;

public interface EncryptionAlgorithm {
	Optional<String> encrypt(String strToEncrypt);

	Optional<String> decrypt(String strToDecrypt);

}

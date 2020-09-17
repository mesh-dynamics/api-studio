package com.cube.cryptography;

import static org.apache.commons.io.FileUtils.readFileToString;

import java.io.File;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.md.cryptography.EncryptionAlgorithm;
import io.md.cryptography.EncryptionAlgorithmFactory;
import io.md.cryptography.JcaEncryption;
import io.md.dao.Event;
import io.md.dao.Payload;

import com.cube.ws.Config;

public class EventFieldEncryptionTest {
	static Config config;
	JSONObject object;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		config = new Config();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	void setUp() throws Exception {
		readJSONFile("EncryptionConfig.json");
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterEach
	void tearDown() throws Exception {
	}


	public void readJSONFile(String url) {
		try {
			File file = new File(JcaEncryption.class.getClassLoader().getResource(url).toURI().getPath());
			String data = readFileToString(file, Charset.defaultCharset());
			try {
				object = new JSONObject(data);
			} catch (Exception e) {
				System.out.println(e);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void test()
	{
		try {
			Config config = new Config();
			Optional<Event> eventOptional = config.rrstore
				.getResponseEvent("restwrapjdbc-mock-314cffbc-d7c2-4893-8381-328cfa3ce118");
			Assertions.assertNotEquals(Optional.empty(), eventOptional);
			Event event = eventOptional.get();
			JSONObject services = object.getJSONObject("services");
			String passPhrase = object.getString("passPhrase");
			if(services.has(event.service)) {
				JSONObject jsonPaths = services.getJSONObject(event.service);
				jsonPaths.keySet().forEach(jsonPath -> {
					JSONObject algoDetails = jsonPaths.getJSONObject(jsonPath);
					String algoName = algoDetails.getString("algorithm");
					JSONObject metaData = algoDetails.getJSONObject("metaData");
					Map<String, Object> metaDataMap = new HashMap<>();
					metaDataMap.put("cipherKeyType",  metaData.get("cipherKeyType"));
					EncryptionAlgorithm encryptionAlgorithm = EncryptionAlgorithmFactory
						.build(algoName, passPhrase, metaDataMap);
					Payload payload = event.payload;
					try {
						Assertions.assertEquals(payload.getValAsString("/status"), "200");
						payload.encryptField(jsonPath, encryptionAlgorithm);
						Assertions.assertNotEquals(payload.getValAsString("/status"), "200");
					} catch (Exception e) {
						e.printStackTrace();
					}
				});

				jsonPaths.keySet().forEach(jsonPath -> {
					JSONObject algoDetails = jsonPaths.getJSONObject(jsonPath);
					String algoName = algoDetails.getString("algorithm");
					JSONObject metaData = algoDetails.getJSONObject("metaData");
					Map<String, Object> metaDataMap = new HashMap<>();
					metaDataMap.put("cipherKeyType",  metaData.get("cipherKeyType"));
					EncryptionAlgorithm encryptionAlgorithm = EncryptionAlgorithmFactory
						.build(algoName, passPhrase, metaDataMap);
					Payload payload = event.payload;
					try {
						Assertions.assertNotEquals(payload.getValAsString("/status"), "200");
						payload.decryptField(jsonPath, encryptionAlgorithm);
						Assertions.assertEquals(payload.getValAsString("/status"), "200");
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
			}

		} catch (Exception e) {
			Assertions.fail("Exception occured", e);
		}


	}


}

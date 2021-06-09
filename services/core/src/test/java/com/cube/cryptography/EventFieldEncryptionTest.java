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

import com.fasterxml.jackson.databind.ObjectMapper;

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
			ObjectMapper jsonMapper = config.jsonMapper;
//			Optional<Event> eventOptional = config.rrstore
//				.getResponseEvent("movieinfo-3ab8628aae25e01455101a63baecb427-9557461b-ecbc-4f94-a012-6ada0087378c");
//			Assertions.assertNotEquals(Optional.empty(), eventOptional);
//			Event event = eventOptional.get();
			String eventAsJson = "{\"customerId\":\"CubeCorp\",\"app\":\"MovieInfo\",\"service\":\"movieinfo\",\"instanceId\":\"a88ae53fe879d0d8ccd05a4ddc1c7b7cbd446ed4\",\"collection\":\"904f4351-5f87-4b5d-95f9-095b6c04b617-86bd7053-2078-4bc7-b018-aae2627323df\",\"traceId\":\"3ab8628aae25e01455101a63baecb427\",\"spanId\":null,\"parentSpanId\":\"NA\",\"runType\":\"Replay\",\"timestamp\":1607087667.111000000,\"reqId\":\"movieinfo-3ab8628aae25e01455101a63baecb427-9557461b-ecbc-4f94-a012-6ada0087378c\",\"apiPath\":\"minfo/rentmovie\",\"eventType\":\"HTTPResponse\",\"payload\":[\"HTTPResponsePayload\",{\"hdrs\":{\":status\":[\"200\"],\"content-length\":[\"50\"],\"content-type\":[\"application/json\"],\"date\":[\"Fri, 04 Dec 2020 13:14:17 GMT\"],\"x-envoy-upstream-service-time\":[\"1022\"]},\"status\":200,\"body\":{\"inventory_id\":19759,\"rent\":1.98,\"num_updates\":1},\"payloadState\":\"UnwrappedDecoded\",\"payloadFields\":[\"/status:200\"]}],\"recordingType\":\"Replay\",\"metaData\":{\"score\":\"1.0\"},\"runId\":\"904f4351-5f87-4b5d-95f9-095b6c04b617-86bd7053-2078-4bc7-b018-aae2627323df 2020-12-04T13:14:02.098586Z\",\"payloadFields\":[\"/status:200\"]}";
			Event event = jsonMapper.readValue(eventAsJson, Event.class);
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

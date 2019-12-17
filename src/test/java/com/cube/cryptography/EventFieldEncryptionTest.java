package com.cube.cryptography;

import static org.apache.commons.io.FileUtils.readFileToString;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Optional;

import org.apache.solr.common.SolrDocument;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.cube.dao.DataObj;
import com.cube.dao.Event;
import com.cube.dao.EventQuery;
import com.cube.dao.ReqRespStoreSolr;
import com.cube.utils.Constants;
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
			Optional<Event> eventOptional = config.rrstore.getResponseEvent("OrderReceiver-14766e909f539fd1e9ebba339efe313a-5e4dbd37-2fb6-45ea-8c52-aa1dd69a28e4");
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
					EncryptionAlgorithm encryptionAlgorithm = EncryptionAlgorithmFactory.build(algoName, passPhrase, metaData);
					DataObj payload = event.getPayload(config);
					System.out.println(payload);
					payload.encryptField(jsonPath, encryptionAlgorithm);
					System.out.println(payload);
				});

				jsonPaths.keySet().forEach(jsonPath -> {
					JSONObject algoDetails = jsonPaths.getJSONObject(jsonPath);
					String algoName = algoDetails.getString("algorithm");
					JSONObject metaData = algoDetails.getJSONObject("metaData");
					EncryptionAlgorithm encryptionAlgorithm = EncryptionAlgorithmFactory.build(algoName, passPhrase, metaData);
					DataObj payload = event.getPayload(config);
					System.out.println(payload);
					payload.decryptField(jsonPath, encryptionAlgorithm);
					System.out.println(payload);
				});
			}

		} catch (Exception e) {
			e.printStackTrace();
		}


	}


}

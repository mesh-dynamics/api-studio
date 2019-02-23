/**
 * Copyright Cube I O
 */
package com.cube.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cube.core.Comparator.Match;
import com.cube.ws.Config;
import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * @author prasad
 *
 */
class JsonComparatorTest  {

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
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterEach
	void tearDown() throws Exception {
	}

	/**
	 * Test method for {@link com.cube.core.JsonComparator#compare(java.lang.String, java.lang.String)}.
	 * @throws JsonProcessingException 
	 */
	@Test
	final void testCompare1() throws JsonProcessingException {
		String json1 = "{\"hdr\": {\"h1\":\"h1v1\", \"h2\":10, \"h3\":5.456}, \"body\": {\"b1\":\"test123\", \"b2\":[1,3,3]}}";
		String json2 = "{\"hdr\": {\"h1\":\"h1v1\", \"h2\":10, \"h3\":5.458}, \"body\": {\"b1\":\"test456\", \"b2\":[1,2,3]}, \"b3\":{\"a1\":\"a1v1\", \"a2\":15}}";
		
		JsonCompareTemplate template = new JsonCompareTemplate();
		
		JsonComparator comparator = new JsonComparator(template, config.jsonmapper);
		
		Match m = comparator.compare(json1, json2);
		
		String mjson = config.jsonmapper.writeValueAsString(m);
		
		System.out.println("match = " + mjson);
		
		//fail("Not yet implemented"); // TODO
	}
	
	static Config config;

}

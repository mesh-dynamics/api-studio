/**
 * Copyright Cube I O
 */
package com.cube.core;

import org.json.JSONException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import com.cube.core.Comparator.Match;
import com.cube.core.JsonCompareTemplate.ComparisonType;
import com.cube.core.JsonCompareTemplate.DataType;
import com.cube.core.JsonCompareTemplate.PresenceType;
import com.cube.core.JsonCompareTemplate.TemplateEntry;
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
	 * @throws JSONException 
	 */
	@Test
	@DisplayName("Default comparison test")
	final void testCompare1() throws JsonProcessingException, JSONException {
		String json1 = "{\"hdr\": {\"h1\":\"h1v1\", \"h2\":10, \"h3\":5.456}, \"body\": {\"b1\":\"test123\", \"b2\":[1,3,3]}}";
		String json2 = "{\"hdr\": {\"h1\":\"h1v1\", \"h2\":10, \"h3\":5.458}, \"body\": {\"b1\":\"test456\", \"b2\":[1,2,3]}, \"b3\":{\"a1\":\"a1v1\", \"a2\":15}}";
		
		JsonCompareTemplate template = new JsonCompareTemplate();
		
		JsonComparator comparator = new JsonComparator(template, config.jsonmapper);
		
		Match m = comparator.compare(json1, json2);
		
		String mjson = config.jsonmapper.writeValueAsString(m);
		
		System.out.println("match = " + mjson);
		
		String expected = "{\"mt\":\"FuzzyMatch\",\"matchmeta\":\"[{\\\"op\\\":\\\"replace\\\",\\\"path\\\":\\\"/hdr/h3\\\",\\\"value\\\":5.458,\\\"fromValue\\\":5.456,\\\"resolution\\\":\\\"OK\\\"},{\\\"op\\\":\\\"replace\\\",\\\"path\\\":\\\"/body/b1\\\",\\\"value\\\":\\\"test456\\\",\\\"fromValue\\\":\\\"test123\\\",\\\"resolution\\\":\\\"OK\\\"},{\\\"op\\\":\\\"add\\\",\\\"path\\\":\\\"/body/b2/1\\\",\\\"value\\\":2,\\\"resolution\\\":\\\"OK\\\"},{\\\"op\\\":\\\"remove\\\",\\\"path\\\":\\\"/body/b2/3\\\",\\\"value\\\":3,\\\"resolution\\\":\\\"OK\\\"},{\\\"op\\\":\\\"add\\\",\\\"path\\\":\\\"/b3\\\",\\\"value\\\":{\\\"a1\\\":\\\"a1v1\\\",\\\"a2\\\":15},\\\"resolution\\\":\\\"OK\\\"}]\"}";
		
		JSONAssert.assertEquals(expected, mjson, false);
	}

	/**
	 * Test method for {@link com.cube.core.JsonComparator#compare(java.lang.String, java.lang.String)}.
	 * @throws JsonProcessingException 
	 * @throws JSONException 
	 */
	@Test
	@DisplayName("Strict equality comparison negative test")
	final void testCompare2() throws JsonProcessingException, JSONException {
		String json1 = "{\"hdr\": {\"h1\":\"h1v1\", \"h2\":10, \"h3\":5.456}, \"body\": {\"b1\":\"test123\", \"b2\":[1,3,3]}}";
		String json2 = "{\"hdr\": {\"h1\":\"h1v1\", \"h2\":10, \"h3\":5.458}, \"body\": {\"b1\":\"test456\", \"b2\":[1,2,3]}, \"b3\":{\"a1\":\"a1v1\", \"a2\":15}}";
		
		JsonCompareTemplate template = new JsonCompareTemplate();
		TemplateEntry rule = new TemplateEntry("", DataType.Obj, PresenceType.Required, ComparisonType.Equal);
		template.addRule(rule);
		
		JsonComparator comparator = new JsonComparator(template, config.jsonmapper);
		
		Match m = comparator.compare(json1, json2);
		
		String mjson = config.jsonmapper.writeValueAsString(m);
		
		System.out.println("match = " + mjson);
		
		String expected = "{\"mt\":\"NoMatch\",\"matchmeta\":\"[{\\\"op\\\":\\\"replace\\\",\\\"path\\\":\\\"/hdr/h3\\\",\\\"value\\\":5.458,\\\"fromValue\\\":5.456,\\\"resolution\\\":\\\"ERR_ValMismatch\\\"},{\\\"op\\\":\\\"replace\\\",\\\"path\\\":\\\"/body/b1\\\",\\\"value\\\":\\\"test456\\\",\\\"fromValue\\\":\\\"test123\\\",\\\"resolution\\\":\\\"ERR_ValMismatch\\\"},{\\\"op\\\":\\\"add\\\",\\\"path\\\":\\\"/body/b2/1\\\",\\\"value\\\":2,\\\"resolution\\\":\\\"ERR_NotExpected\\\"},{\\\"op\\\":\\\"remove\\\",\\\"path\\\":\\\"/body/b2/3\\\",\\\"value\\\":3,\\\"resolution\\\":\\\"OK\\\"},{\\\"op\\\":\\\"add\\\",\\\"path\\\":\\\"/b3\\\",\\\"value\\\":{\\\"a1\\\":\\\"a1v1\\\",\\\"a2\\\":15},\\\"resolution\\\":\\\"ERR_NotExpected\\\"}]\"}";
		
		JSONAssert.assertEquals(expected, mjson, false);
	}

	/**
	 * Test method for {@link com.cube.core.JsonComparator#compare(java.lang.String, java.lang.String)}.
	 * @throws JsonProcessingException 
	 * @throws JSONException 
	 */
	@Test
	@DisplayName("Strict equality comparison positive test")
	final void testCompare3() throws JsonProcessingException, JSONException {
		String json1 = "{\"hdr\": {\"h1\":\"h1v1\", \"h2\":10, \"h3\":5.456}, \"body\": {\"b1\":\"test123\", \"b2\":[1,3,3]}}";
		String json2 = json1;
		
		JsonCompareTemplate template = new JsonCompareTemplate();
		TemplateEntry rule = new TemplateEntry("", DataType.Obj, PresenceType.Required, ComparisonType.Equal);
		template.addRule(rule);
		
		JsonComparator comparator = new JsonComparator(template, config.jsonmapper);
		
		Match m = comparator.compare(json1, json2);
		
		String mjson = config.jsonmapper.writeValueAsString(m);
		
		System.out.println("match = " + mjson);
		
		String expected = "{\"mt\":\"ExactMatch\",\"matchmeta\":\"[]\"}";
		
		JSONAssert.assertEquals(expected, mjson, false);
	}

	static Config config;

}

/**
 * Copyright Cube I O
 */
package com.cube.core;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import com.cube.core.Comparator.Match;
import com.cube.core.CompareTemplate.ComparisonType;
import com.cube.core.CompareTemplate.DataType;
import com.cube.core.CompareTemplate.PresenceType;
import com.cube.ws.Config;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.File;
import java.nio.charset.Charset;

import static org.apache.commons.io.FileUtils.readFileToString;


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
		readFile("JsonComparator.json");
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterEach
	void tearDown() throws Exception {
	}

	JSONObject object;

	public void readFile(String url) {
		try {
			File file = new File(JsonComparatorTest.class.getClassLoader().getResource(url).toURI().getPath());
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

	/**
	 * Test method for {@link com.cube.core.JsonComparator#compare(java.lang.String, java.lang.String)}.
	 * @throws JsonProcessingException 
	 * @throws JSONException 
	 */
	@Test
	@DisplayName("Default comparison test")
	final void testCompare1() throws JsonProcessingException, JSONException {
		JSONObject test1Data = object.getJSONObject("test1");
		String json1 = test1Data.get("json1").toString();
		String json2 = test1Data.get("json2").toString();
		CompareTemplate template = new CompareTemplate();
		
		JsonComparator comparator = new JsonComparator(template, config.jsonmapper);
		
		Match m = comparator.compare(json1, json2);
		
		String mjson = config.jsonmapper.writeValueAsString(m);
		
		System.out.println("match = " + mjson);

		String expected = test1Data.get("output").toString();

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
		JSONObject test1Data = object.getJSONObject("test1");
		String json1 = test1Data.get("json1").toString();
		String json2 = test1Data.get("json2").toString();
		
		CompareTemplate template = new CompareTemplate();
		TemplateEntry rule = new TemplateEntry("", DataType.Obj, PresenceType.Required, ComparisonType.Equal);
		template.addRule(rule);
		
		JsonComparator comparator = new JsonComparator(template, config.jsonmapper);
		
		Match m = comparator.compare(json1, json2);
		
		String mjson = config.jsonmapper.writeValueAsString(m);
		
		System.out.println("match = " + mjson);

		String expected = object.getJSONObject("test2").get("output").toString();

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
		JSONObject test1Data = object.getJSONObject("test1");
		String json1 = test1Data.get("json1").toString();
		String json2 = json1;
		
		CompareTemplate template = new CompareTemplate();
		TemplateEntry rule = new TemplateEntry("", DataType.Obj, PresenceType.Required, ComparisonType.Equal);
		template.addRule(rule);
		
		JsonComparator comparator = new JsonComparator(template, config.jsonmapper);
		
		Match m = comparator.compare(json1, json2);
		
		String mjson = config.jsonmapper.writeValueAsString(m);
		
		System.out.println("match = " + mjson);
		
		String expected = object.getJSONObject("test3").get("output").toString();

		JSONAssert.assertEquals(expected, mjson, false);
	}

	/**
	 * Test method for {@link com.cube.core.JsonComparator#compare(java.lang.String, java.lang.String)}.
	 * @throws JsonProcessingException
	 * @throws JSONException
	 */
	@Test
	@DisplayName("Missing Field: Default")
	final void testCompare4() throws JsonProcessingException, JSONException {
		JSONObject test4Data = object.getJSONObject("test4");
		String json1 = test4Data.get("json1").toString();
		String json2 = test4Data.get("json2").toString();

		CompareTemplate template = new CompareTemplate();

		JsonComparator comparator = new JsonComparator(template, config.jsonmapper);

		Match m = comparator.compare(json1, json2);

		String mjson = config.jsonmapper.writeValueAsString(m);

		System.out.println("match = " + mjson);

		String expected = test4Data.get("output").toString();

		JSONAssert.assertEquals(expected, mjson, false);
	}

    /**
     * Test method for {@link com.cube.core.JsonComparator#compare(java.lang.String, java.lang.String)}.
     * @throws JsonProcessingException
     * @throws JSONException
     */
    @Test
    @DisplayName("Missing Field: Optional")
    final void testCompare5() throws JsonProcessingException, JSONException {
		JSONObject test4Data = object.getJSONObject("test4");
		String json1 = test4Data.get("json1").toString();
		String json2 = test4Data.get("json2").toString();

        CompareTemplate template = new CompareTemplate();
        TemplateEntry rule = new TemplateEntry("/body/b2", DataType.RptArray, PresenceType.Optional, ComparisonType.Equal);
        template.addRule(rule);

        JsonComparator comparator = new JsonComparator(template, config.jsonmapper);

        Match m = comparator.compare(json1, json2);

        String mjson = config.jsonmapper.writeValueAsString(m);

        System.out.println("match = " + mjson);

        String expected = object.getJSONObject("test5").get("output").toString();
        JSONAssert.assertEquals(expected, mjson, false);
    }

    /**
     * Test method for {@link com.cube.core.JsonComparator#compare(java.lang.String, java.lang.String)}.
     * @throws JsonProcessingException
     * @throws JSONException
     */
    @Test
    @DisplayName("Missing Field: Required")
    final void testCompare6() throws JsonProcessingException, JSONException {
		JSONObject test4Data = object.getJSONObject("test4");
		String json1 = test4Data.get("json1").toString();
		String json2 = test4Data.get("json2").toString();

        CompareTemplate template = new CompareTemplate();
        TemplateEntry rule = new TemplateEntry("/body/b2", DataType.RptArray, PresenceType.Required, ComparisonType.Equal);
        template.addRule(rule);

        JsonComparator comparator = new JsonComparator(template, config.jsonmapper);

        Match m = comparator.compare(json1, json2);

        String mjson = config.jsonmapper.writeValueAsString(m);

        System.out.println("match = " + mjson);

        String expected = object.getJSONObject("test6").get("output").toString();
        JSONAssert.assertEquals(expected, mjson, false);
    }
	/**
	 * Test method for {@link com.cube.core.JsonComparator#compare(java.lang.String, java.lang.String)}.
	 * @throws JsonProcessingException
	 * @throws JSONException
	 */
	@Test
	@DisplayName("Strict Validations test - negative")
	final void testCompare7() throws JsonProcessingException, JSONException {
		String json1 = "{\n" +
				"        \"string\": \"5c80e878323659a64d123db4\",\n" +
				"        \"int\": 35,\n" +
				"        \"float\": 41.280324,\n" +
				"        \"obj\": { \"name\": \"Steele Dominguez\"},\n" +
				"        \"rptArr\": [\n" +
				"            \"dolore\",\n" +
				"            \"irure\"\n" +
				"        ],\n" +
				"        \"nrptArr\": [\n" +
				"            { \"id\": 0 },\n" +
				"            { \"name\": \"Steele Dominguez\"}\n" +
				"        ]\n" +
				"    }";
		String json2 = "{\n" +
				"        \"string\": 123,\n" +
				"        \"int\": \"35\",\n" +
				"        \"float\": 41,\n" +
				"        \"obj\": \"not an object\",\n" +
				"        \"rptArr\": 123,\n" +
				"        \"nrptArr\": [\n" +
				"            \"string\",\n" +
				"            123\n" +
				"        ]\n" +
				"    }";
		CompareTemplate template = new CompareTemplate();
		String[] paths = {"", "/string", "/int", "/float", "/obj", "/rptArr", "/nrptArr"};
		DataType[] dataTypes = {DataType.Obj, DataType.Str, DataType.Int, DataType.Float, DataType.Obj, DataType.RptArray, DataType.NrptArray};
		for (int i = 0; i < paths.length; i++) {
			TemplateEntry rule = new TemplateEntry(paths[i], dataTypes[i], PresenceType.Required, ComparisonType.Ignore);
			template.addRule(rule);
		}

		JsonComparator comparator = new JsonComparator(template, config.jsonmapper);

		Match m = comparator.compare(json1, json2);

		String mjson = config.jsonmapper.writeValueAsString(m);

		System.out.println("match = " + mjson);

		String expected = "{\"mt\":\"NoMatch\",\"matchmeta\":\"[{\\\"op\\\":\\\"noop\\\",\\\"path\\\":\\\"/int\\\",\\\"value\\\":\\\"35\\\",\\\"resolution\\\":\\\"ERR_ValTypeMismatch\\\"},{\\\"op\\\":\\\"noop\\\",\\\"path\\\":\\\"/rptArr\\\",\\\"value\\\":123,\\\"resolution\\\":\\\"ERR_ValTypeMismatch\\\"},{\\\"op\\\":\\\"noop\\\",\\\"path\\\":\\\"/string\\\",\\\"value\\\":123,\\\"resolution\\\":\\\"ERR_ValTypeMismatch\\\"},{\\\"op\\\":\\\"noop\\\",\\\"path\\\":\\\"/float\\\",\\\"value\\\":41,\\\"resolution\\\":\\\"ERR_ValTypeMismatch\\\"},{\\\"op\\\":\\\"noop\\\",\\\"path\\\":\\\"/obj\\\",\\\"value\\\":\\\"not an object\\\",\\\"resolution\\\":\\\"ERR_ValTypeMismatch\\\"},{\\\"op\\\":\\\"replace\\\",\\\"path\\\":\\\"/string\\\",\\\"value\\\":123,\\\"fromValue\\\":\\\"5c80e878323659a64d123db4\\\",\\\"resolution\\\":\\\"OK_Ignore\\\"},{\\\"op\\\":\\\"replace\\\",\\\"path\\\":\\\"/int\\\",\\\"value\\\":\\\"35\\\",\\\"fromValue\\\":35,\\\"resolution\\\":\\\"OK_Ignore\\\"},{\\\"op\\\":\\\"replace\\\",\\\"path\\\":\\\"/float\\\",\\\"value\\\":41,\\\"fromValue\\\":41.280324,\\\"resolution\\\":\\\"OK_Ignore\\\"},{\\\"op\\\":\\\"replace\\\",\\\"path\\\":\\\"/obj\\\",\\\"value\\\":\\\"not an object\\\",\\\"fromValue\\\":{\\\"name\\\":\\\"Steele Dominguez\\\"},\\\"resolution\\\":\\\"OK_Ignore\\\"},{\\\"op\\\":\\\"replace\\\",\\\"path\\\":\\\"/rptArr\\\",\\\"value\\\":123,\\\"fromValue\\\":[\\\"dolore\\\",\\\"irure\\\"],\\\"resolution\\\":\\\"OK_Ignore\\\"},{\\\"op\\\":\\\"replace\\\",\\\"path\\\":\\\"/nrptArr/0\\\",\\\"value\\\":\\\"string\\\",\\\"fromValue\\\":{\\\"id\\\":0},\\\"resolution\\\":\\\"OK_Ignore\\\"},{\\\"op\\\":\\\"replace\\\",\\\"path\\\":\\\"/nrptArr/1\\\",\\\"value\\\":123,\\\"fromValue\\\":{\\\"name\\\":\\\"Steele Dominguez\\\"},\\\"resolution\\\":\\\"OK_Ignore\\\"}]\"}";
		JSONAssert.assertEquals(expected, mjson, false);
	}

	/**
	 * Test method for {@link com.cube.core.JsonComparator#compare(java.lang.String, java.lang.String)}.
	 * @throws JsonProcessingException
	 * @throws JSONException
	 */
	@Test
	@DisplayName("Strict Validations test - positive")
	final void testCompare8() throws JsonProcessingException, JSONException {
		String json1 = "{\n" +
				"        \"string\": \"5c80e878323659a64d123db4\",\n" +
				"        \"int\": 35,\n" +
				"        \"float\": 41.280324,\n" +
				"        \"obj\": { \"name\": \"Steele Dominguez\"},\n" +
				"        \"rptArr\": [\n" +
				"            \"dolore\",\n" +
				"            \"irure\"\n" +
				"        ],\n" +
				"        \"nrptArr\": [\n" +
				"            { \"id\": 0 },\n" +
				"            { \"name\": \"Steele Dominguez\"}\n" +
				"        ]\n" +
				"    }";
		String json2 = json1;

		CompareTemplate template = new CompareTemplate();
		String[] paths = {"", "/string", "/int", "/float", "/obj", "/rptArr", "/nrptArr"};
		DataType[] dataTypes = {DataType.Obj, DataType.Str, DataType.Int, DataType.Float, DataType.Obj, DataType.RptArray, DataType.NrptArray};
		for (int i = 0; i < paths.length; i++) {
			TemplateEntry rule = new TemplateEntry(paths[i], dataTypes[i], PresenceType.Required, ComparisonType.Ignore);
			template.addRule(rule);
		}

		JsonComparator comparator = new JsonComparator(template, config.jsonmapper);

		Match m = comparator.compare(json1, json2);

		String mjson = config.jsonmapper.writeValueAsString(m);

		System.out.println("match = " + mjson);

		String expected = "{\"mt\":\"ExactMatch\",\"matchmeta\":\"[]\"}";
		JSONAssert.assertEquals(expected, mjson, false);
	}

	static Config config;

}

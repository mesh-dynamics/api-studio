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
import java.util.Optional;

import static org.apache.commons.io.FileUtils.readFileToString;


/**
 * @author prasad
 *
 */
class JsonComparatorTest  {

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
		readFile("JsonComparator.json");
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterEach
	void tearDown() throws Exception {
	}

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
	final void defaultComparisonTest() throws JsonProcessingException, JSONException {
		JSONObject testData = object.getJSONObject("defaultComparison");
		String json1 = testData.get("json1").toString();
		String json2 = testData.get("json2").toString();

		CompareTemplate template = new CompareTemplate();
		JsonComparator comparator = new JsonComparator(template, config.jsonmapper);
		
		Match m = comparator.compare(json1, json2);
		
		String mjson = config.jsonmapper.writeValueAsString(m);
		String expected = testData.get("output").toString();
		JSONAssert.assertEquals(expected, mjson, false);
	}

	/**
	 * Test method for {@link com.cube.core.JsonComparator#compare(java.lang.String, java.lang.String)}.
	 * @throws JsonProcessingException 
	 * @throws JSONException 
	 */
	@Test
	@DisplayName("Strict equality comparison negative test")
	final void strictEqualityComparisonNegativeTest() throws JsonProcessingException, JSONException {
		JSONObject testData = object.getJSONObject("defaultComparison");
		String json1 = testData.get("json1").toString();
		String json2 = testData.get("json2").toString();
		
		CompareTemplate template = new CompareTemplate();
		TemplateEntry rule = new TemplateEntry("", DataType.Obj, PresenceType.Required, ComparisonType.Equal);
		template.addRule(rule);
		JsonComparator comparator = new JsonComparator(template, config.jsonmapper);
		
		Match m = comparator.compare(json1, json2);
		
		String mjson = config.jsonmapper.writeValueAsString(m);
		String expected = object.getJSONObject("strictEqualityComparisonNegative").get("output").toString();
		JSONAssert.assertEquals(expected, mjson, false);
	}

	/**
	 * Test method for {@link com.cube.core.JsonComparator#compare(java.lang.String, java.lang.String)}.
	 * @throws JsonProcessingException 
	 * @throws JSONException 
	 */
	@Test
	@DisplayName("Strict equality comparison positive test")
	final void strictEqualityComparisonPositiveTest() throws JsonProcessingException, JSONException {
		JSONObject testData = object.getJSONObject("defaultComparison");
		String json1 = testData.get("json1").toString();
		String json2 = json1;
		
		CompareTemplate template = new CompareTemplate();
		TemplateEntry rule = new TemplateEntry("", DataType.Obj, PresenceType.Required, ComparisonType.Equal);
		template.addRule(rule);
		JsonComparator comparator = new JsonComparator(template, config.jsonmapper);
		
		Match m = comparator.compare(json1, json2);
		
		String mjson = config.jsonmapper.writeValueAsString(m);
		String expected = object.getJSONObject("exactMatch").get("output").toString();
		JSONAssert.assertEquals(expected, mjson, false);
	}

	/**
	 * Test method for {@link com.cube.core.JsonComparator#compare(java.lang.String, java.lang.String)}.
	 * @throws JsonProcessingException
	 * @throws JSONException
	 */
	@Test
	@DisplayName("Equality optional comparison test")
	final void equalOptionalComparisonTest() throws JsonProcessingException, JSONException {
		JSONObject testData = object.getJSONObject("defaultComparison");
		String json1 = testData.get("json1").toString();
		String json2 = testData.get("json2").toString();;

		CompareTemplate template = new CompareTemplate();
		TemplateEntry rule = new TemplateEntry("", DataType.Obj, PresenceType.Required, ComparisonType.EqualOptional);
		template.addRule(rule);
		JsonComparator comparator = new JsonComparator(template, config.jsonmapper);

		Match m = comparator.compare(json1, json2);

		String mjson = config.jsonmapper.writeValueAsString(m);
		String expected = object.getJSONObject("equalOptionalComparison").get("output").toString();
		JSONAssert.assertEquals(expected, mjson, false);
	}

	/**
	 * Test method for {@link com.cube.core.JsonComparator#compare(java.lang.String, java.lang.String)}.
	 * @throws JsonProcessingException
	 * @throws JSONException
	 */
	@Test
	@DisplayName("Custom comparison test - Positive")
	final void customComparisonPositiveTest() throws JsonProcessingException, JSONException {
		JSONObject testData = object.getJSONObject("customComparisonPositive");
		String json1 = testData.get("json1").toString();
		String json2 = testData.get("json2").toString();;

		CompareTemplate template = new CompareTemplate();
		TemplateEntry emailRule = new TemplateEntry("/email", DataType.Str, PresenceType.Required, ComparisonType.CustomRegex, Optional.of(".+\\@.+\\..+"));
		TemplateEntry roundRule = new TemplateEntry("/round", DataType.Float, PresenceType.Required, ComparisonType.CustomRound, Optional.of("2"));
		TemplateEntry ceilingRule = new TemplateEntry("/ceiling", DataType.Float, PresenceType.Required, ComparisonType.CustomCeil, Optional.of("2"));
		TemplateEntry floorRule = new TemplateEntry("/floor", DataType.Float, PresenceType.Required, ComparisonType.CustomFloor, Optional.of("2"));
		template.addRule(emailRule);
		template.addRule(roundRule);
		template.addRule(ceilingRule);
		template.addRule(floorRule);

		JsonComparator comparator = new JsonComparator(template, config.jsonmapper);

		Match m = comparator.compare(json1, json2);

		String mjson = config.jsonmapper.writeValueAsString(m);
		String expected = testData.get("output").toString();
		JSONAssert.assertEquals(expected, mjson, false);
	}

	/**
	 * Test method for {@link com.cube.core.JsonComparator#compare(java.lang.String, java.lang.String)}.
	 * @throws JsonProcessingException
	 * @throws JSONException
	 */
	@Test
	@DisplayName("Custom comparison test - Negative")
	final void customComparisonNegativeTest() throws JsonProcessingException, JSONException {
		JSONObject testData = object.getJSONObject("customComparisonNegative");
		String json1 = testData.get("json1").toString();
		String json2 = testData.get("json2").toString();;

		CompareTemplate template = new CompareTemplate();
		TemplateEntry emailRule = new TemplateEntry("/email", DataType.Str, PresenceType.Required, ComparisonType.CustomRegex, Optional.of(".+\\@.+\\..+"));
		TemplateEntry roundRule = new TemplateEntry("/round", DataType.Float, PresenceType.Required, ComparisonType.CustomRound, Optional.of("2"));
		TemplateEntry ceilingRule = new TemplateEntry("/ceiling", DataType.Float, PresenceType.Required, ComparisonType.CustomCeil, Optional.of("2"));
		TemplateEntry floorRule = new TemplateEntry("/floor", DataType.Float, PresenceType.Required, ComparisonType.CustomFloor, Optional.of("2"));
		template.addRule(emailRule);
		template.addRule(roundRule);
		template.addRule(ceilingRule);
		template.addRule(floorRule);

		JsonComparator comparator = new JsonComparator(template, config.jsonmapper);

		Match m = comparator.compare(json1, json2);

		String mjson = config.jsonmapper.writeValueAsString(m);
		String expected = testData.get("output").toString();
		JSONAssert.assertEquals(expected, mjson, false);
	}

	/**
	 * Test method for {@link com.cube.core.JsonComparator#compare(java.lang.String, java.lang.String)}.
	 * @throws JsonProcessingException
	 * @throws JSONException
	 */
	@Test
	@DisplayName("Missing Field: Default")
	final void missingFieldDefaultTest() throws JsonProcessingException, JSONException {
		JSONObject testData = object.getJSONObject("missingFieldDefault");
		String json1 = testData.get("json1").toString();
		String json2 = testData.get("json2").toString();

		CompareTemplate template = new CompareTemplate();
		JsonComparator comparator = new JsonComparator(template, config.jsonmapper);

		Match m = comparator.compare(json1, json2);

		String mjson = config.jsonmapper.writeValueAsString(m);
		String expected = testData.get("output").toString();
		JSONAssert.assertEquals(expected, mjson, false);
	}

    /**
     * Test method for {@link com.cube.core.JsonComparator#compare(java.lang.String, java.lang.String)}.
     * @throws JsonProcessingException
     * @throws JSONException
     */
    @Test
    @DisplayName("Missing Field: Optional")
    final void missingFieldOptionalTest() throws JsonProcessingException, JSONException {
		JSONObject testData = object.getJSONObject("missingFieldDefault");
		String json1 = testData.get("json1").toString();
		String json2 = testData.get("json2").toString();

        CompareTemplate template = new CompareTemplate();
        TemplateEntry rule = new TemplateEntry("/body/b2", DataType.RptArray, PresenceType.Optional, ComparisonType.Equal);
        template.addRule(rule);
        JsonComparator comparator = new JsonComparator(template, config.jsonmapper);

        Match m = comparator.compare(json1, json2);

        String mjson = config.jsonmapper.writeValueAsString(m);
        String expected = object.getJSONObject("missingFieldOptional").get("output").toString();
        JSONAssert.assertEquals(expected, mjson, false);
    }

    /**
     * Test method for {@link com.cube.core.JsonComparator#compare(java.lang.String, java.lang.String)}.
     * @throws JsonProcessingException
     * @throws JSONException
     */
    @Test
    @DisplayName("Missing Field: Required")
    final void missingFieldRequiredTest() throws JsonProcessingException, JSONException {
		JSONObject testData = object.getJSONObject("missingFieldDefault");
		String json1 = testData.get("json1").toString();
		String json2 = testData.get("json2").toString();

        CompareTemplate template = new CompareTemplate();
        TemplateEntry rule = new TemplateEntry("/body/b2", DataType.RptArray, PresenceType.Required, ComparisonType.Equal);
        template.addRule(rule);
        JsonComparator comparator = new JsonComparator(template, config.jsonmapper);

        Match m = comparator.compare(json1, json2);

        String mjson = config.jsonmapper.writeValueAsString(m);
        String expected = object.getJSONObject("missingFieldRequired").get("output").toString();
        JSONAssert.assertEquals(expected, mjson, false);
    }
	/**
	 * Test method for {@link com.cube.core.JsonComparator#compare(java.lang.String, java.lang.String)}.
	 * @throws JsonProcessingException
	 * @throws JSONException
	 */
	@Test
	@DisplayName("Strict Validations test - negative")
	final void validationNegativeTest() throws JsonProcessingException, JSONException {
		JSONObject testData = object.getJSONObject("validationNegative");
		String json1 = testData.get("json1").toString();
		String json2 = testData.get("json2").toString();

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
		String expected = testData.get("output").toString();
		JSONAssert.assertEquals(expected, mjson, false);
	}

	/**
	 * Test method for {@link com.cube.core.JsonComparator#compare(java.lang.String, java.lang.String)}.
	 * @throws JsonProcessingException
	 * @throws JSONException
	 */
	@Test
	@DisplayName("Strict Validations test - positive")
	final void validationPositiveTest() throws JsonProcessingException, JSONException {
		JSONObject testData = object.getJSONObject("validationNegative");
		String json1 = testData.get("json1").toString();
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
		String expected = object.getJSONObject("exactMatch").get("output").toString();
		JSONAssert.assertEquals(expected, mjson, false);
	}

}

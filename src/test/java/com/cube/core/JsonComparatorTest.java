/**
 * Copyright Cube I O
 */
package com.cube.core;

import static org.apache.commons.io.FileUtils.readFileToString;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.md.core.Comparator.Match;
import io.md.core.CompareTemplate;
import io.md.core.CompareTemplate.ComparisonType;
import io.md.core.CompareTemplate.DataType;
import io.md.core.CompareTemplate.ExtractionMethod;
import io.md.core.CompareTemplate.PresenceType;
import io.md.core.TemplateEntry;

import com.cube.ws.Config;


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
        readJSONFile("JsonComparator.json");
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterEach
	void tearDown() throws Exception {
	}

	public void readJSONFile(String url) {
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
    //Todo: Add a testcase for combination of fields missing from LHS and different comparison type / presence type in RHS

	private void compareTest(JSONObject testData) throws JsonProcessingException, JSONException{
		String json1 = testData.get("json1").toString();
		String json2 = testData.get("json2").toString();
		JSONArray rules = testData.getJSONArray("rules");
		String expected = testData.get("output").toString();

		CompareTemplate template = new CompareTemplate();

		for (int i = 0; i < rules.length(); i++) {
            JSONObject ruleObj = rules.getJSONObject(i);
            String path = ruleObj.getString("path");
            DataType dataType = DataType.valueOf(ruleObj.getString("dataType"));
            PresenceType presenceType = PresenceType.valueOf(ruleObj.getString("presenceType"));
            ComparisonType comparisonType = ComparisonType.valueOf(ruleObj.getString("comparisonType"));
            ExtractionMethod extractionMethod = ExtractionMethod.Default;
            if (ruleObj.has("extractionMethod")) {
                extractionMethod = ExtractionMethod.valueOf(ruleObj.getString("extractionMethod"));
            }
            String customization = ruleObj.getString("customization");
			String arrayElementKeyPath = null;
			try {
				arrayElementKeyPath = ruleObj.getString("arrayCompKeyPath");
			} catch (Exception e) {

			}
            TemplateEntry rule = new TemplateEntry(path, dataType, presenceType
	            , comparisonType, extractionMethod, Optional.of(customization)
	            , Optional.ofNullable(arrayElementKeyPath));
            template.addRule(rule);
        }

		JsonComparator comparator = new JsonComparator(template, config.jsonMapper);
		Match m = comparator.compare(json1, json2);
		m = new Match(m.mt, m.matchmeta, m.diffs);
		String mjson = config.jsonMapper.writeValueAsString(m);
		JSONAssert.assertEquals(expected, mjson, JSONCompareMode.NON_EXTENSIBLE);
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
		compareTest(testData);
	}

	/**
	 * Test method for {@link com.cube.core.JsonComparator#compare(java.lang.String, java.lang.String)}.
	 * @throws JsonProcessingException
	 * @throws JSONException
	 */
	@Test
	@DisplayName("Strict equality comparison test - Negative")
	final void strictEqualityComparisonNegativeTest() throws JsonProcessingException, JSONException {
		JSONObject testData = object.getJSONObject("strictEqualityComparisonNegative");
		compareTest(testData);
	}

	/**
	 * Test method for {@link com.cube.core.JsonComparator#compare(java.lang.String, java.lang.String)}.
	 * @throws JsonProcessingException
	 * @throws JSONException
	 */
	@Test
	@DisplayName("Strict equality comparison test - Positive")
	final void strictEqualityComparisonPositiveTest() throws JsonProcessingException, JSONException {
		JSONObject testData = object.getJSONObject("strictEqualityComparisonPositive");
		compareTest(testData);
	}

	/**
	 * Test method for {@link com.cube.core.JsonComparator#compare(java.lang.String, java.lang.String)}.
	 * @throws JsonProcessingException
	 * @throws JSONException
	 */
	@Test
	@DisplayName("Equality optional comparison test")
	final void equalOptionalComparisonTest() throws JsonProcessingException, JSONException {
		JSONObject testData = object.getJSONObject("equalOptionalComparison");
		compareTest(testData);
	}

    @Test
    @DisplayName("Equality optional comparison test 2")
    final void equalOptionalComparisonTest2() throws JsonProcessingException, JSONException {
        JSONObject testData = object.getJSONObject("equalOptionalComparison2");
        compareTest(testData);
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
		compareTest(testData);
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
		compareTest(testData);
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
		compareTest(testData);
	}

    /**
     * Test method for {@link com.cube.core.JsonComparator#compare(java.lang.String, java.lang.String)}.
     * @throws JsonProcessingException
     * @throws JSONException
     */
    @Test
    @DisplayName("Missing Field: Optional")
    final void missingFieldOptionalTest() throws JsonProcessingException, JSONException {
		JSONObject testData = object.getJSONObject("missingFieldOptional");
		compareTest(testData);
    }

    /**
     * Test method for {@link com.cube.core.JsonComparator#compare(java.lang.String, java.lang.String)}.
     * @throws JsonProcessingException
     * @throws JSONException
     */
    @Test
    @DisplayName("Missing Field: Required")
    final void missingFieldRequiredTest() throws JsonProcessingException, JSONException {
		JSONObject testData = object.getJSONObject("missingFieldRequired");
		compareTest(testData);
    }

    /**
     * Test method for {@link com.cube.core.JsonComparator#compare(java.lang.String, java.lang.String)}.
     * @throws JsonProcessingException
     * @throws JSONException
     */
    @Test
    @DisplayName("Missing LHS test")
    final void missingLHSTest() throws JsonProcessingException, JSONException {
        JSONObject testData = object.getJSONObject("missingLHS");
        compareTest(testData);
    }

	/**
	 * Test method for {@link com.cube.core.JsonComparator#compare(java.lang.String, java.lang.String)}.
	 * @throws JsonProcessingException
	 * @throws JSONException
	 */
	@Test
	@DisplayName("Strict Validations test - Negative")
	final void validationNegativeTest() throws JsonProcessingException, JSONException {
		JSONObject testData = object.getJSONObject("validationNegative");
		compareTest(testData);
	}

	/**
	 * Test method for {@link com.cube.core.JsonComparator#compare(java.lang.String, java.lang.String)}.
	 * @throws JsonProcessingException
	 * @throws JSONException
	 */
	@Test
	@DisplayName("Strict Validations test - Positive")
	final void validationPositiveTest() throws JsonProcessingException, JSONException {
		JSONObject testData = object.getJSONObject("validationPositive");
		compareTest(testData);
	}

	/**
	 * Test method for {@link com.cube.core.JsonComparator#compare(java.lang.String, java.lang.String)}.
	 * @throws JsonProcessingException
	 * @throws JSONException
	 */
	@Test
	@DisplayName("Inheritance test")
	final void inheritanceTest() throws JsonProcessingException, JSONException {
		JSONObject testData = object.getJSONObject("inheritance");
		compareTest(testData);
	}

	/**
	 * Test method for {@link com.cube.core.JsonComparator#compare(java.lang.String, java.lang.String)}.
	 * @throws JsonProcessingException
	 * @throws JSONException
	 */
	@Test
	@DisplayName("Repeating Array test")
	final void repeatingArrayTest() throws JsonProcessingException, JSONException {
		JSONObject testData = object.getJSONObject("repeatingArray");
		compareTest(testData);
	}

    /**
     * Test method for {@link com.cube.core.JsonComparator#compare(java.lang.String, java.lang.String)}.
     * @throws JsonProcessingException
     * @throws JSONException
     */
    @Test
    @DisplayName("Array test")
    final void arrayTest() throws JsonProcessingException, JSONException {
        JSONObject testData = object.getJSONObject("arrayDiff");
        compareTest(testData);
    }

	/**
	 * Test method for {@link com.cube.core.JsonComparator#compare(java.lang.String, java.lang.String)}.
	 * @throws JsonProcessingException
	 * @throws JSONException
	 */
	@Test
	@DisplayName("Unordered Array test")
	final void unorderedArrayTest() throws JsonProcessingException, JSONException {
		JSONObject testData = object.getJSONObject("unorderedArrayDiff");
		compareTest(testData);
	}

    @Test
    @DisplayName("Unordered Array Object test")
    final void unorderedMixedArrayObjectTest() throws JsonProcessingException, JSONException {
        JSONObject testData = object.getJSONObject("unorderedMixedArrayObjectDiff");
        compareTest(testData);
    }

    @Test
    @DisplayName("Unordered Array Element Missing test")
    final void unorderedArrayDiffElemMissing() throws JsonProcessingException, JSONException {
        JSONObject testData = object.getJSONObject("unorderedArrayDiffElemMissing");
        compareTest(testData);
    }

	@Test
	@DisplayName("Unordered Array Jumbled Order test")
    final void unorderedArrayJumbledOrder()  throws JsonProcessingException, JSONException {
	    JSONObject testData = object.getJSONObject("unorderedArrayJumbledOrder");
	    compareTest(testData);
    }
}

package com.cube.core.attribute.rule;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.md.core.AttributeRuleMap;
import io.md.core.CompareTemplate;
import io.md.core.TemplateEntry;

import com.cube.cache.TemplateKey;
import com.cube.ws.Config;

public class AttributeMapTest {

	static String ruleMapId;

    static String baseVersion;

    static String appId;

    static Config config;

    @BeforeAll
	public  static void setUp() throws Exception {

        baseVersion = UUID.randomUUID().toString();

		Map<String, TemplateEntry> ruleMap = new HashMap<>();

		// public TemplateEntry(@JsonProperty("path") String path, @JsonProperty("dt") DataType dt
		// , @JsonProperty("pt") PresenceType pt, @JsonProperty("ct") ComparisonType ct
		// , @JsonProperty("em") ExtractionMethod em
		// , @JsonProperty("customization") Optional<String> customization) {
		//
		ruleMap.put("timestamp" , new TemplateEntry("", CompareTemplate.DataType.Default,
			CompareTemplate.PresenceType.Default, CompareTemplate.ComparisonType.Ignore
			, CompareTemplate.ExtractionMethod.Default, Optional.empty()));

		AttributeRuleMap attributeRuleMap = new AttributeRuleMap(ruleMap);

		config = new Config();

		baseVersion = UUID.randomUUID().toString();

		appId = UUID.randomUUID().toString();

		TemplateKey templateKey = new TemplateKey(baseVersion, "ravivj"
			, "random" , "NA", "NA"
			, TemplateKey.Type.RequestCompare);

		String ruleMapAsString = config.jsonMapper.writeValueAsString(attributeRuleMap);

		ruleMapId = config.rrstore.saveAttributeRuleMap(templateKey, ruleMapAsString);

		System.out.println("Created Rule Map with Id " + ruleMapId);
	}


	@Test
	public  void testRetrieve() {
        Optional<AttributeRuleMap> ruleMapOpt = config.rrstore.getAttributeRuleMap( new TemplateKey(baseVersion, "ravivj"
            , "random" , "NA", "NA"
            , TemplateKey.Type.RequestCompare));

        Assertions.assertEquals(ruleMapOpt.get().getRule("timestamp").get().ct
            , CompareTemplate.ComparisonType.Ignore);
	}


}

package com.cube.core.attribute.rule;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import io.md.dao.TemplateSet;

import io.md.core.AttributeRuleMap;
import io.md.core.CompareTemplate;
import io.md.core.CompareTemplate.ComparisonType;
import io.md.core.TemplateEntry;
import io.md.core.TemplateKey;
import io.md.core.TemplateKey.Type;
import io.md.dao.ReqRespUpdateOperation.OperationType;
import io.md.utils.Utils;

import com.cube.golden.SingleTemplateUpdateOperation;
import com.cube.golden.TemplateEntryOperation;
import com.cube.golden.TemplateEntryOperation.RuleType;
import com.cube.golden.TemplateUpdateOperationSet;
import com.cube.golden.transform.TemplateSetTransformer;
import com.cube.ws.Config;

public class AttributeMapTest {

	static String ruleMapId;

	static String baseTemplateVersion;

	static String app;

	static String customer;

	static Config config;

	@BeforeAll
	public  static void setUp() throws Exception {

		Map<String, TemplateEntry> ruleMap = new HashMap<>();

		// public TemplateEntry(@JsonProperty("path") String path, @JsonProperty("dt") DataType dt
		// , @JsonProperty("pt") PresenceType pt, @JsonProperty("ct") ComparisonType ct
		// , @JsonProperty("em") ExtractionMethod em
		// , @JsonProperty("customization") Optional<String> customization) {
		//
		ruleMap.put("/timestamp" , new TemplateEntry("/timestamp", CompareTemplate.DataType.Default,
			CompareTemplate.PresenceType.Optional, CompareTemplate.ComparisonType.Ignore
			, CompareTemplate.ExtractionMethod.Default, Optional.empty(), Optional.empty()));

		AttributeRuleMap attributeRuleMap = new AttributeRuleMap(ruleMap);

		config = new Config();

		baseTemplateVersion = UUID.randomUUID().toString();

		app = "random";

		customer = "ravivj";

		TemplateKey templateKey = new TemplateKey(baseTemplateVersion, customer
			, app , "NA", "NA"
			, Type.DontCare);

		String ruleMapAsString = config.jsonMapper.writeValueAsString(attributeRuleMap);

		ruleMapId = config.rrstore.saveAttributeRuleMap(templateKey, ruleMapAsString);

		System.out.println("Created Rule Map with Id " + ruleMapId);
	}


	@Test
	public  void testRetrieve() {
		Optional<AttributeRuleMap> ruleMapOpt = config.rrstore.getAttributeRuleMap( new TemplateKey(
			baseTemplateVersion, "ravivj"
			, "random" , "NA", "NA"
			, Type.DontCare));

		Assertions.assertEquals(ruleMapOpt.get().getRule("/timestamp").get().ct
			, CompareTemplate.ComparisonType.Ignore);
	}

	@Test public void testUpdate() throws Exception {


		TemplateSetTransformer templateSetTransformer = new TemplateSetTransformer();
//		CompareTemplate compareTemplate = config.comparatorCache.getDefaultComparator(EventType.HTTPResponse, key).getCompareTemplate();


		TemplateSet templateSet = new TemplateSet(customer, app, Instant.now(),
			Collections.EMPTY_LIST, Optional.empty(), baseTemplateVersion, LocalDateTime
			.now().format(Utils.templateLabelFormatter));

		TemplateKey key = new TemplateKey(
			templateSet.version, customer
			, app , "NA", "NA"
			, Type.DontCare);


		String templateUpdateOperationSetId = UUID.randomUUID().toString();
		TemplateEntryOperation templateEntryOperation = new TemplateEntryOperation(
			OperationType.REPLACE, "/timestamp",
			Optional.of(new TemplateEntry("/timestamp", CompareTemplate.DataType.Default,
				CompareTemplate.PresenceType.Optional, ComparisonType.Equal
				, CompareTemplate.ExtractionMethod.Default, Optional.empty(), Optional.empty())), RuleType.ATTRIBUTERULE
		);

		SingleTemplateUpdateOperation singleTemplateUpdateOperation = new SingleTemplateUpdateOperation(
			List.of(templateEntryOperation));

		HashMap templateUpdateMap = new HashMap();
		templateUpdateMap.put(key, singleTemplateUpdateOperation);

		TemplateUpdateOperationSet templateUpdateOperationSet = new TemplateUpdateOperationSet(templateUpdateOperationSetId,
			templateUpdateMap);

		TemplateSet updatedTemplateSet = templateSetTransformer.updateTemplateSet(
			templateSet, templateUpdateOperationSet, config.rrstore);

		Assertions.assertEquals(updatedTemplateSet.appAttributeRuleMap.get().getRule("/timestamp").get().ct
			, ComparisonType.Equal);
	}

}

package com.cube.core.attribute.rule;

import com.cube.cache.TemplateKey;
import com.cube.dao.ReqRespStore;
import io.md.core.AttributeRuleMap;
import io.md.core.CompareTemplate;
import io.md.core.TemplateEntry;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.cube.ws.Config;

public class AttributeMapSaveTest {

    public static void main(String[] args) {
        try {

            Map<String, TemplateEntry> ruleMap = new HashMap<>();

            // public TemplateEntry(@JsonProperty("path") String path, @JsonProperty("dt") DataType dt
            // , @JsonProperty("pt") PresenceType pt, @JsonProperty("ct") ComparisonType ct
            // , @JsonProperty("em") ExtractionMethod em
            // , @JsonProperty("customization") Optional<String> customization) {
            //
            ruleMap.put("/timestamp" , new TemplateEntry("/timestamp", CompareTemplate.DataType.Default,
                CompareTemplate.PresenceType.Default, CompareTemplate.ComparisonType.Ignore
                , CompareTemplate.ExtractionMethod.Default, Optional.empty()));

            AttributeRuleMap attributeRuleMap = new AttributeRuleMap(ruleMap);

            Config config = new Config();

            TemplateKey templateKey = new TemplateKey("Default", "ravivj"
                , "random" , "NA", "NA"
                , TemplateKey.Type.RequestCompare);

            String ruleMapAsString = config.jsonMapper.writeValueAsString(attributeRuleMap);

            String ruleMapId = config.rrstore.saveAttributeRuleMap(templateKey, ruleMapAsString);

            System.out.println("Created Rule Map with Id " + ruleMapId);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

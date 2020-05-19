package com.cube.core.attribute.rule;

import java.util.Optional;

//import io.md.core.AttributeRuleMap;
import io.md.core.Comparator;
import io.md.core.TemplateEntry;

import com.cube.cache.TemplateKey;
import com.cube.cache.TemplateKey.Type;
import com.cube.ws.Config;


public class RetrieveAttributeRuleMap {


	public static void main(String[] args) {
		try {
			Config config = new Config();

			TemplateKey key = new TemplateKey("Default"
				, "ravivj" , "random" , "randomSerive"
				, "randomPath" , Type.ResponseCompare);

			Comparator comparator = config.rrstore.getComparator(key);

			TemplateEntry entry = comparator.getCompareTemplate().getRule("/timestamp");

			System.out.println(entry.ct);

			/*Optional<AttributeRuleMap> ruleMap = config.rrstore.getAttributeRuleMap(new TemplateKey("Default"
				, "ravivj" , "random" , "NA" , "NA" , Type.ResponseCompare));
			ruleMap.ifPresent(rMap -> rMap.getRule("timestamp").ifPresent(rule ->
			{System.out.println(rule.ct);}));*/

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}

/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cube.core.attribute.rule;

//import io.md.core.AttributeRuleMap;
import static io.md.core.TemplateKey.*;

import io.md.core.Comparator;
import io.md.core.TemplateEntry;
import io.md.core.TemplateKey;

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

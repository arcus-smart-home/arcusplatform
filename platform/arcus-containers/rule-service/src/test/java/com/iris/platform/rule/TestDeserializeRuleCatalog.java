/*
 * Copyright 2019 Arcus Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * 
 */
package com.iris.platform.rule;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.google.inject.Inject;
import com.iris.capability.registry.CapabilityRegistryModule;
import com.iris.platform.rule.catalog.RuleCatalog;
import com.iris.platform.rule.catalog.RuleTemplate;
import com.iris.platform.rule.catalog.selector.SelectorGenerator;
import com.iris.platform.rule.catalog.serializer.RuleCatalogDeserializer;
import com.iris.resource.Resources;
import com.iris.test.IrisTestCase;
import com.iris.test.Modules;

/**
 * 
 */
@Modules( CapabilityRegistryModule.class )
public class TestDeserializeRuleCatalog extends IrisTestCase {

   @Inject RuleCatalogDeserializer deserializer;

   @Test
   public void testRuleCatalog() throws Exception {
	   deserializeRuleFile();
   }
   
   private Map<String, RuleCatalog> deserializeRuleFile() throws IOException {
	   try (InputStream is = Resources.open("classpath:/rule-catalog.xml")) {
	         return deserializer.deserialize(is);
	   }
	   
   }
   
   @Test
   public void testRuleCategory() throws Exception {
	   Map<String, RuleCatalog> ruleCatalog = deserializeRuleFile();
	   List<String> allErrors = new ArrayList<String>();
	   ruleCatalog.forEach((k, curRuleCatalog) -> {
		   List<String> errors = validateCategories(curRuleCatalog);
		   allErrors.addAll(errors);
	   }); 
	   if(!allErrors.isEmpty()) {
		   fail("Here is a list of category related errors:\n"+allErrors);
	   }
   }
   
   @Test
   public void testRuleDescription( ) throws Exception {
	   Map<String, RuleCatalog> ruleCatalog = deserializeRuleFile();
	   List<String> allErrors = new ArrayList<String>();
	   ruleCatalog.forEach((k, curRuleCatalog) -> {
		   List<String> errors = validateDescription(curRuleCatalog);
		   allErrors.addAll(errors);
	   }); 
	   if(!allErrors.isEmpty()) {
		   fail("Here is a list of description related errors:\n"+allErrors);
	   }
   }
   
   
   
   private List<String> validateDescription(RuleCatalog ruleCatalog) {
	   List<String> errs = new ArrayList<String>();
		List<RuleTemplate> allTemplates = ruleCatalog.getTemplates();
		allTemplates.forEach((curTemplate) -> {
			   String curDescription = curTemplate.getTemplate();
			   Map<String, SelectorGenerator> selectors = curTemplate.getOptions();
			   
			   selectors.forEach((k, v) -> {
				   if(!curDescription.contains("${"+k+"}")) {
					   //fail(String.format("%s defined in rule '%s' is not defined in the <categories> section.", curCategory, curTemplate.getName()));
					   errs.add(String.format("selector named %s defined in rule '%s' is not defined in the description [%s].", k, curTemplate.getName(), curDescription));
				   }
			   });
		   });
		   return errs;
   }
	

	private List<String> validateCategories(RuleCatalog ruleCatalog) {
		List<String> errs = new ArrayList<String>();
		Set<String> categories = ruleCatalog.getCategories();
		List<RuleTemplate> allTemplates = ruleCatalog.getTemplates();
		allTemplates.forEach((curTemplate) -> {
			   Set<String> curCats = curTemplate.getCategories();
			   curCats.forEach((curCategory) -> {
				   if(!categories.contains(curCategory)) {
					   //fail(String.format("%s defined in rule '%s' is not defined in the <categories> section.", curCategory, curTemplate.getName()));
					   errs.add(String.format("%s defined in rule '%s' is not defined in the <categories> section.", curCategory, curTemplate.getName()));
				   }
			   });
		   });
		   return errs;
	   	}
	}


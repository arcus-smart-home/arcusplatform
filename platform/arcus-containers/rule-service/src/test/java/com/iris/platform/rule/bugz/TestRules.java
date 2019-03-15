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
package com.iris.platform.rule.bugz;

import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.iris.common.rule.Rule;
import com.iris.common.rule.RuleContext;
import com.iris.common.rule.simple.SimpleRule;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.io.json.JSON;
import com.iris.messages.address.Address;
import com.iris.messages.model.ChildId;
import com.iris.messages.model.SimpleModelStore;
import com.iris.platform.rule.RuleEnvironment;
import com.iris.platform.rule.StatefulRuleDefinition;
import com.iris.platform.rule.catalog.action.config.ActionConfig;
import com.iris.platform.rule.catalog.condition.config.ConditionConfig;
import com.iris.platform.rule.catalog.serializer.json.RuleConfigJsonModule;
import com.iris.platform.rule.environment.PlaceExecutorEventLoop;
import com.iris.platform.rule.environment.PlatformRuleContext;
import com.iris.platform.rule.environment.RuleModelStore;
import com.iris.test.IrisMockTestCase;
import com.iris.test.IrisTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

@Modules({ RuleConfigJsonModule.class })
@Mocks({ PlatformMessageBus.class })
public class TestRules extends IrisMockTestCase {
	@Inject PlatformMessageBus bus;
/*
49cec811-4788-4c95-b6e7-9242179edf30 | rule | 38 |   null | 
{"type":"actions","actionConfigs":[{"type":"for-each-model","actions":[{"type":"set-attribute","address":"${address}","attributeName":"swit:state","attributeValue":"ON","attributeType":"enum\u003cON,OFF\u003e","duration":300,"unit":"SECONDS","conditionQuery":"base:address \u003d\u003d \u0027${motion}\u0027 AND mot:motion \u003d\u003d \u0027NONE\u0027","reevaluateCondition":false}],"modelQuery":"base:address \u003d\u003d \u0027DRIV:dev:b720ca06-5627-4355-bbe3-2eaf59b2cc16\u0027 and swit:state !\u003d \u0027ON\u0027","targetVariable":"address"}]} |               null | 
{"type":"value-change","attribute":"mot:motion","oldValue":null,"newValue":"DETECTED","query":"base:address \u003d\u003d \u0027${motion}\u0027"} | 2016-05-19 22:47:37+0000 | When the stairs  detects motion, then turn the stairs  to on for 5 mins. | 2016-11-22 16:26:42+0000 | Motion Detected, Activate Switch |         null |        False |            null |         False |         null |        1e1f0d | 
{"_accountId":"c2852ae2-ced8-47ba-be78-8c9d56a2dd17","motion":"DRIV:dev:a3d5a9a1-3860-4296-93c7-121e5ebab147","_ruleName":"Motion Detected, Activate Switch","_stillFiring:0":[],"for awhile":"300","address:b720ca06-5627-4355-bbe3-2eaf59b2cc16:0":"DRIV:dev:b720ca06-5627-4355-bbe3-2eaf59b2cc16","_firing":false,"_placeId":"49cec811-4788-4c95-b6e7-9242179edf30","state":"ON","switch":"DRIV:dev:b720ca06-5627-4355-bbe3-2eaf59b2cc16"}  
*/
	
	@Test
	public void testRestoreRule() {
		StatefulRuleDefinition definition = new StatefulRuleDefinition();
		definition.setId(new ChildId(UUID.fromString("49cec811-4788-4c95-b6e7-9242179edf30"), 38));
		definition.setAction(JSON.fromJson("{\"type\":\"actions\",\"actionConfigs\":[{\"type\":\"for-each-model\",\"actions\":[{\"type\":\"set-attribute\",\"address\":\"${address}\",\"attributeName\":\"swit:state\",\"attributeValue\":\"ON\",\"attributeType\":\"enum\u003cON,OFF\u003e\",\"duration\":300,\"unit\":\"SECONDS\",\"conditionQuery\":\"base:address \u003d\u003d \u0027${motion}\u0027 AND mot:motion \u003d\u003d \u0027NONE\u0027\",\"reevaluateCondition\":false}],\"modelQuery\":\"base:address \u003d\u003d \u0027DRIV:dev:b720ca06-5627-4355-bbe3-2eaf59b2cc16\u0027 and swit:state !\u003d \u0027ON\u0027\",\"targetVariable\":\"address\"}]}", ActionConfig.class));
		definition.setCondition(JSON.fromJson("{\"type\":\"value-change\",\"attribute\":\"mot:motion\",\"oldValue\":null,\"newValue\":\"DETECTED\",\"query\":\"base:address \u003d\u003d \u0027${motion}\u0027\"}", ConditionConfig.class));
		
		RuleContext context = 
				PlatformRuleContext
					.builder()
					.withLogger(LoggerFactory.getLogger(TestRules.class))
					.withVariables((Map<String, Object>) JSON.fromJson("{\"_accountId\":\"c2852ae2-ced8-47ba-be78-8c9d56a2dd17\",\"motion\":\"DRIV:dev:a3d5a9a1-3860-4296-93c7-121e5ebab147\",\"_ruleName\":\"Motion Detected, Activate Switch\",\"_stillFiring:0\":[],\"for awhile\":\"300\",\"address:b720ca06-5627-4355-bbe3-2eaf59b2cc16:0\":\"DRIV:dev:b720ca06-5627-4355-bbe3-2eaf59b2cc16\",\"_firing\":false,\"_placeId\":\"49cec811-4788-4c95-b6e7-9242179edf30\",\"state\":\"ON\",\"switch\":\"DRIV:dev:b720ca06-5627-4355-bbe3-2eaf59b2cc16\"}", Map.class))
					.withSource(Address.fromString(definition.getAddress()))
					.withPlaceId(definition.getPlaceId())
					.withPlatformBus(bus)
					.withModels(new RuleModelStore())
					.withEventLoop(new PlaceExecutorEventLoop(null, null))
					.withTimeZone(TimeZone.getDefault())
					.build();
//				new SimpleContext(definition.getPlaceId(), Address.fromString(definition.getAddress()), LoggerFactory.getLogger(TestRules.class));
//		context.
		RuleEnvironment environment = null;
		
		Rule rule = new SimpleRule(context, definition.createCondition(environment), definition.createAction(environment), Address.fromString(definition.getAddress()));
		rule.activate();
	}
}


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
package com.iris.platform.rule.catalog.action;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.iris.common.rule.action.ActionContext;
import com.iris.common.rule.simple.SimpleContext;
import com.iris.messages.address.Address;
import com.iris.platform.rule.catalog.action.config.ActionConfig;
import com.iris.platform.rule.catalog.template.TemplatedValue;

public class TestLogActionTemplate extends BaseActionTest {
   
   ActionContext context;
   Address source;
   
   @Before
   public void init(){
      this.source = Address.platformService(UUID.randomUUID(), "rule");
      this.context = new SimpleContext(UUID.randomUUID(), this.source, LoggerFactory.getLogger(TestLogActionTemplate.class));
   }

   @Test
   public void testLogAction() throws Exception {
      LogTemplate template = new LogTemplate();
      template.setMessage(TemplatedValue.text("hello ${something}"));
      ActionConfig config = template.generateActionConfig(ImmutableMap.of("something","world"));
      ActionConfig config2 = serializeDeserialize(config);
      assertEquals(config,config2);
      config2.createAction(ImmutableMap.of()).execute(context);
   }
}


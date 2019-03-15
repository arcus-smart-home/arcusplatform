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
package com.iris.common.rule.simple;

import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.iris.common.rule.RuleContext;
import com.iris.common.rule.trigger.TestThresholdTrigger;
import com.iris.messages.address.Address;

public class TestOverrideNamespaceContext extends Assert {
   
   private RuleContext delegate=null;
   private RuleContext context1=null;
   private RuleContext context2=null;

   @Before
   public void setUp() {
      delegate = new SimpleContext(UUID.randomUUID(), Address.platformService(UUID.randomUUID(), "rule"), LoggerFactory.getLogger(TestThresholdTrigger.class));
      delegate.setVariable("_baseVar", "basevalue");
      delegate.clearDirty();
      context1 = new NamespaceContext("context1", delegate);
      context2 = new NamespaceContext("context2", delegate);
   }

   @Test
   public void testIsDirty(){
      assertFalse("Context should not be dirty",context1.isDirty());
      context1.setVariable("dirty", "dirty");
      assertTrue("Context should be dirty",context1.isDirty());
      context1.clearDirty();
      context1.setVariable("dirty", "dirty");
      assertFalse("Context should not be dirty",context1.isDirty());

   }
   
   @Test
   public void testOverrideContext(){
      context1.setVariable("test", "sometest1");
      context2.setVariable("test", "sometest2");
      assertEquals("sometest1", context1.getVariable("test"));
      assertTrue("Context should be dirty",context1.isDirty());
      assertTrue("Context should be dirty",context2.isDirty());
   }
   
   @Test
   public void testOverrideGetVariables(){
      context1.setVariable("testvar", "value1");
      context2.setVariable("testvar", "value2");
      Map<String,Object>variables=context1.getVariables();
      assertEquals(ImmutableMap.<String, Object>of("_baseVar", "basevalue","testvar","value1"),variables);
      Map<String,Object>variables2=context2.getVariables();
      assertEquals(ImmutableMap.<String, Object>of("_baseVar", "basevalue","testvar","value2"),variables2);
   }
   
   @Test
   public void testChainedGetVariables(){
      RuleContext chained = new NamespaceContext("double", context1);
      chained.setVariable("chainedvar", "value3");
      assertEquals("value3", chained.getVariable("chainedvar"));
      assertEquals(ImmutableMap.<String, Object>of("_baseVar", "basevalue","chainedvar","value3"),chained.getVariables());
   }
   
   @Test
   public void testNamespaceResolving(){
      String address = "DRIV:dev:06f728d9-da0b-4c29-ba9b-435a2eb8c176";
      String uuid = "06f728d9-da0b-4c29-ba9b-435a2eb8c176";
      RuleContext sequential = new NamespaceContext("0", delegate);
      RuleContext foreach = new NamespaceContext(uuid, sequential);
      RuleContext sequential2 = new NamespaceContext("0", foreach);
      sequential2.setVariable("address", address);
      assertEquals(address,sequential2.getVariables().get("address"));
   }
}


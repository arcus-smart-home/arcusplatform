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
package com.iris.common.rule.matcher;

import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.iris.common.rule.simple.SimpleContext;
import com.iris.messages.address.Address;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.model.Model;
import com.iris.model.predicate.Predicates;

/**
 * 
 */
public class TestModelPredicateMatcher extends Assert {
   SimpleContext context;
   ModelPredicateMatcher matcher;
   
   @Before
   public void setUp() {
      context = new SimpleContext(UUID.randomUUID(), Address.platformService(UUID.randomUUID(), "rule"), LoggerFactory.getLogger(TestModelPredicateMatcher.class));
      
      matcher = new ModelPredicateMatcher(
            Predicates.typeEquals("test"),
            Predicates.attributeEquals("test:test", true)
      );
   }

   @Test
   public void testEmptyContext() {
      assertFalse(matcher.isSatisfiable(context));
      assertFalse(matcher.matches(context));
   }

   @Test
   public void testNoApplicableModels() {
      context.createModel(DeviceCapability.NAMESPACE, UUID.randomUUID());
      context.createModel(HubCapability.NAMESPACE, UUID.randomUUID());
      
      assertFalse(matcher.isSatisfiable(context));
      assertFalse(matcher.matches(context));
   }

   @Test
   public void testNoMatches() {
      context.createModel(DeviceCapability.NAMESPACE, UUID.randomUUID());
      context.createModel("test", UUID.randomUUID());
      
      assertTrue(matcher.isSatisfiable(context));
      assertFalse(matcher.matches(context));
   }

   @Test
   public void testMatch() {
      context.createModel(DeviceCapability.NAMESPACE, UUID.randomUUID());
      Model model = context.createModel("test", UUID.randomUUID());
      model.setAttribute("test:test", true);
      
      assertTrue(matcher.isSatisfiable(context));
      assertTrue(matcher.matches(context));
   }

}


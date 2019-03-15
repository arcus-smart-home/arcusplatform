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
package com.iris.model.predicate;

import java.util.Collections;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.messages.capability.Capability;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;
import com.iris.util.IrisCollections;

/**
 * 
 */
public class TestInstancePredicate extends Assert {
   Predicate<Model> predicate = Predicates.hasA("test");
   
   @Test
   public void testNullModel() throws Exception {
      assertFalse(predicate.apply(null));
   }

   @Test
   public void testNullInstances() throws Exception {
      Model model = new SimpleModel();
      assertFalse(predicate.apply(model));
   }

   @Test
   public void testEmptyInstances() throws Exception {
      Model model = new SimpleModel();
      model.setAttribute(Capability.ATTR_INSTANCES, Collections.emptyMap());
      assertFalse(predicate.apply(model));
   }

   @Test
   public void testWrongInstances() throws Exception {
      Model model = new SimpleModel();
      model.setAttribute(Capability.ATTR_INSTANCES, ImmutableMap.<String, Set<String>>of(
      		"i1", ImmutableSet.<String>of(),
      		"i2", ImmutableSet.of("cap1"),
      		"i3", ImmutableSet.of("cap1", "cap2")
		));
      assertFalse(predicate.apply(model));
   }

   @Test
   public void testRightInstance() throws Exception {
      Model model = new SimpleModel();
      model.setAttribute(Capability.ATTR_INSTANCES, ImmutableMap.<String, Set<String>>of("i2", ImmutableSet.of("base", "test")));
      assertTrue(predicate.apply(model));
   }

}


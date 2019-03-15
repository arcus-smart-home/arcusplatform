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

import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Predicate;
import com.iris.messages.capability.Capability;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;
import com.iris.model.predicate.Predicates;
import com.iris.util.IrisCollections;

/**
 * 
 */
public class TestTaggedWithPredicate extends Assert {
   Predicate<Model> predicate = Predicates.hasTag("location:Living Room");
   
   @Test
   public void testNullModel() throws Exception {
      assertFalse(predicate.apply(null));
   }

   @Test
   public void testNullTags() throws Exception {
      Model model = new SimpleModel();
      assertFalse(predicate.apply(model));
   }

   @Test
   public void testEmptyTags() throws Exception {
      Model model = new SimpleModel();
      model.setAttribute(Capability.ATTR_TAGS, Collections.emptySet());
      assertFalse(predicate.apply(model));
   }

   @Test
   public void testWrongTags() throws Exception {
      Model model = new SimpleModel();
      model.setAttribute(Capability.ATTR_TAGS, IrisCollections.setOf("tag1:value1", "tag2:value2", "tag3:value3"));
      assertFalse(predicate.apply(model));
   }

   @Test
   public void testMatchingTag() throws Exception {
      Model model = new SimpleModel();
      model.setAttribute(Capability.ATTR_TAGS, IrisCollections.setOf("test:tag", "location:Living Room"));
      assertTrue(predicate.apply(model));
   }
}


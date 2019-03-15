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

import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Predicate;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;
import com.iris.model.predicate.Predicates;

/**
 * 
 */
public class TestDeviceTypePredicate extends Assert {
   Predicate<Model> predicate = Predicates.attributeEquals(DeviceCapability.ATTR_DEVTYPEHINT, "swit");
   
   @Test
   public void testNullModel() throws Exception {
      assertFalse(predicate.apply(null));
   }

   @Test
   public void testNoDevTypeHint() throws Exception {
      Model model = new SimpleModel();
      assertFalse(predicate.apply(model));
   }

   @Test
   public void testEmptyDevTypeHint() throws Exception {
      Model model = new SimpleModel();
      model.setAttribute(DeviceCapability.ATTR_DEVTYPEHINT, "");
      assertFalse(predicate.apply(model));
   }

   @Test
   public void testWrongDevTypeHint() throws Exception {
      Model model = new SimpleModel();
      model.setAttribute(DeviceCapability.ATTR_DEVTYPEHINT, "button");
      assertFalse(predicate.apply(model));
   }

   @Test
   public void testMatch() throws Exception {
      Model model = new SimpleModel();
      model.setAttribute(DeviceCapability.ATTR_DEVTYPEHINT, "swit");
      assertTrue(predicate.apply(model));
   }

}


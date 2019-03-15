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
package com.iris.io.json;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * 
 */
public class TestJSON {

   @Test
   public void testGenericDeserialize() {
      assertEquals(null, JSON.fromJson("null", Object.class));
      assertEquals(false, JSON.fromJson("false", Object.class));
      assertEquals(true, JSON.fromJson("true", Object.class));
      assertEquals(10L, ((Long) JSON.fromJson("10", Object.class)).longValue());
      assertEquals(10.0D, ((Double) JSON.fromJson("10.0", Object.class)).doubleValue(), .1);
      assertEquals("string", JSON.fromJson("'string'", Object.class));
      assertEquals(ImmutableMap.of(), JSON.fromJson("{}", Object.class));
      assertEquals(ImmutableList.of(), (List<?>) JSON.fromJson("[]", Object.class));
   }
}


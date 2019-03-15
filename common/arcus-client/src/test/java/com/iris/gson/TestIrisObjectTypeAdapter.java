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
package com.iris.gson;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class TestIrisObjectTypeAdapter {
	Gson gson;
	
	@Before
	public void setUp() {
		IrisObjectTypeAdapterFactory.install();
		gson = 
			new GsonBuilder()
				.registerTypeAdapterFactory(new IrisObjectTypeAdapterFactory())
				.create();
	}
	
	@Test
	public void testWrite() {
		assertEquals("null", gson.toJson(null, Object.class));
		assertEquals("1", gson.toJson(1, Object.class));
		assertEquals("1.0", gson.toJson(1.0, Object.class));
		assertEquals("true", gson.toJson(true, Object.class));
		assertEquals("\"string\"", gson.toJson("string", Object.class));
		assertEquals("[]", gson.toJson(ImmutableSet.of(), Object.class));
		assertEquals("{}", gson.toJson(ImmutableMap.of(), Object.class));
		assertEquals("[1]", gson.toJson(ImmutableSet.of(1), Object.class));
		assertEquals("{\"nested\":[\"string\"]}", gson.toJson(ImmutableMap.of("nested", ImmutableSet.of("string")), Object.class));
	}
}


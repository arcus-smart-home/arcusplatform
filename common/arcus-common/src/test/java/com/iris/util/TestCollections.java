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
package com.iris.util;

import java.util.Collections;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

public class TestCollections {

	@Test
	public void testAddedElements() {
		Set<String> orig_diff = IrisCollections.setOf("a", "b", "c", "d");
		Set<String> orig_same = IrisCollections.setOf("c", "d", "e", "f");
		Set<String> updated = IrisCollections.setOf("c", "d", "e", "f");
		
		Set<String> added = IrisCollections.addedElements(updated, orig_diff);
		System.out.println("Added Elements: " + added);
		Assert.assertEquals(IrisCollections.setOf("e", "f"), added);
		
		added = IrisCollections.addedElements(updated, null);
		System.out.println("Added Elements: " + added);
		Assert.assertEquals(IrisCollections.setOf("c", "d", "e", "f"), added);
		
		added = IrisCollections.addedElements(null, orig_diff);
		System.out.println("Added Elements: " + added);
		Assert.assertEquals(Collections.emptySet(), added);
		
		added = IrisCollections.addedElements(updated, orig_same);
		System.out.println("Added Elements: " + added);
		Assert.assertEquals(Collections.emptySet(), added);
	}
	
	@Test
	public void testRemovedElements() {
		Set<String> orig_diff = IrisCollections.setOf("a", "b", "c", "d");
		Set<String> orig_same = IrisCollections.setOf("c", "d", "e", "f");
		Set<String> updated = IrisCollections.setOf("c", "d", "e", "f");
		
		Set<String> removed = IrisCollections.removedElements(updated, orig_diff);
		System.out.println("Removed Elements: " + removed);
		Assert.assertEquals(IrisCollections.setOf("a", "b"), removed);
		
		removed = IrisCollections.removedElements(updated, null);
		System.out.println("Removed Elements: " + removed);
		Assert.assertEquals(Collections.emptySet(), removed);
		
		removed = IrisCollections.removedElements(null, orig_diff);
		System.out.println("Removed Elements: " + removed);
		Assert.assertEquals(IrisCollections.setOf("a", "b", "c", "d"), removed);
		
		removed = IrisCollections.removedElements(updated, orig_same);
		System.out.println("Removed Elements: " + removed);
		Assert.assertEquals(Collections.emptySet(), removed);
	}
}


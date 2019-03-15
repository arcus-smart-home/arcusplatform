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
package com.iris.detector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestProfanityDetector {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testDetected() {
		ProfanityDetector detector = ProfanityDetector.INSTANCE;

		String input = "5qw2ea55bip";
		DetectorResult result = detector.detect(input);
		assertTrue(result.isFound());
		assertEquals("a55", result.getMatch());


		input = "a552exlbip";
		result = detector.detect(input);
		assertTrue(result.isFound());
		assertEquals("a55", result.getMatch());

		input = "5qw2exla55";
		result = detector.detect(input);
		assertTrue(result.isFound());
		assertEquals("a55", result.getMatch());

		input = "vcnuts4rwp";
		result = detector.detect(input);
		assertTrue(result.isFound());
		assertEquals("cnut", result.getMatch());

		input = "";
		result = detector.detect(input);
		assertFalse(result.isFound());
		assertNull(result.getMatch());

	}

	@Test
	public void testNotDetected() {
		ProfanityDetector detector = ProfanityDetector.INSTANCE;

		String input = "1fzayi5ihu";
		DetectorResult result = detector.detect(input);

		assertFalse(result.isFound());
		assertNull(result.getMatch());

		input = "vy9o5m4dyy";
		result = detector.detect(input);

		assertFalse(result.isFound());
		assertNull(result.getMatch());

	}

}


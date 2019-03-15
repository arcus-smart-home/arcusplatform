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

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class TestTimeUUIDComparator {
	List<UUID> sortedDesc1 = ImmutableList.of(
			UUID.fromString("f8f08790-6253-11e7-bcbf-1dac6a0459be"),
			UUID.fromString("e6dace80-624e-11e7-817e-c579d31d5c2d"),
			UUID.fromString("8db489c0-61ba-11e7-97d6-61a604589c2b"),
			UUID.fromString("aef1e700-61a0-11e7-b485-690cf7687347")
	);
	List<UUID> sortedDesc2 = ImmutableList.of(
			UUID.fromString("4088fed0-625d-11e7-82f5-dd24d03fe2d1"),
			UUID.fromString("2524f8b0-625d-11e7-a409-259a7314a4fb"),
			UUID.fromString("8f2e74c0-6258-11e7-a3fa-315fe03ab1c7"),
			UUID.fromString("f8f08790-6253-11e7-bcbf-1dac6a0459be"),
			UUID.fromString("aac36830-6253-11e7-92eb-592479e987e8"),
			UUID.fromString("2b5c3400-6253-11e7-96e8-21929e07fb29"),
			UUID.fromString("f65a65a0-624e-11e7-afdd-25deea686745"),
			UUID.fromString("e6dace80-624e-11e7-817e-c579d31d5c2d"),
			UUID.fromString("00cd9cf0-6245-11e7-ae7b-65fe32c72dd0"),
			UUID.fromString("fb4746a0-6244-11e7-b53c-f98cfabaa3c8"),
			UUID.fromString("eb1cc980-6244-11e7-989a-b9cbf7bde7fa"),
			UUID.fromString("3c1e3bd0-61c7-11e7-aece-45bea9184e49"),
			UUID.fromString("92deeb20-61bf-11e7-b60b-3d6d78e5ce7d"),
			UUID.fromString("8f7c3820-61bf-11e7-a330-09bfbfba48fb"),
			UUID.fromString("49da98c0-61bf-11e7-8c6e-29122d5a4601"),
			UUID.fromString("48908c90-61bf-11e7-b783-5d0ca1dcfbbd"),
			UUID.fromString("8db489c0-61ba-11e7-97d6-61a604589c2b"),
			UUID.fromString("a259f290-61b8-11e7-bdc0-c5da9ebd3db4"),
			UUID.fromString("e8240d10-61ae-11e7-813a-91b937244320"),
			UUID.fromString("f9076f60-61ad-11e7-a285-55f84a2f0939"),
			UUID.fromString("f4e69c80-61ad-11e7-9f15-79c466074549"),
			UUID.fromString("ef0de070-61ad-11e7-9cfb-696cc42235ff"),
			UUID.fromString("bd0717b0-61ab-11e7-b696-ddcac9a0703c"),
			UUID.fromString("db778990-61a2-11e7-a8f5-35a85664fbf3"),
			UUID.fromString("8bbc1e80-61a1-11e7-9c69-9578964e14c1"),
			UUID.fromString("aef1e700-61a0-11e7-b485-690cf7687347"),
			UUID.fromString("abaf8e40-6195-11e7-b8bf-81bc0313ef0f"),
			UUID.fromString("2bee1b30-618c-11e7-951d-fdfc10e028ee"),
			UUID.fromString("dd89e640-618b-11e7-8cfb-19fe950572bc"),
			UUID.fromString("67c7dc40-6187-11e7-b22c-25d6a5d770d6"),
			UUID.fromString("4d7f8f60-6013-11e7-9144-65a75593ae60"),
			UUID.fromString("0af57090-6010-11e7-a56e-a9760f173ca3"),
			UUID.fromString("c48c8990-600f-11e7-82da-99674f01c88d"),
			UUID.fromString("6e19a5c0-600f-11e7-bfcb-e1aaa7c6fa3c"),
			UUID.fromString("30f32820-600e-11e7-bebf-a564a672a680"),
			UUID.fromString("222d4410-600e-11e7-844b-617c573f1d08"),
			UUID.fromString("1a8c6140-6005-11e7-9e3a-c5f261b6d017"),
			UUID.fromString("0b8136d0-6005-11e7-9dd0-650fe95084d1"),
			UUID.fromString("eb5ce650-6000-11e7-8660-4d7b97f383a5"),
			UUID.fromString("14a65100-6000-11e7-b073-ad091ec2f9dc"),
			UUID.fromString("2b229cb0-5ffe-11e7-84c0-69c004716e8e"),
			UUID.fromString("fa6c84f0-5ffd-11e7-9aa3-810bf3b50fc9"),
			UUID.fromString("e7515080-5ffd-11e7-9b14-052f8cbbffe6"),
			UUID.fromString("da189c20-5ffd-11e7-a8d2-b52dd3414e52"),
			UUID.fromString("cf178840-5ffd-11e7-b99b-65cb50755a2c"),
			UUID.fromString("a9765a80-5ffd-11e7-8d01-7d7bbf047d9e"),
			UUID.fromString("92804490-5ff7-11e7-afa4-b1bf55b6d878"),
			UUID.fromString("098c4d00-5ff7-11e7-a694-91e6d89e358d")
	);

	private void shuffleSortAndAssert(List<UUID> expected, Comparator<UUID> c) {
		// perfectly in order
		{
			List<UUID> actual = new ArrayList<>(expected);
			Collections.sort(actual, c);
			assertEquals(expected, actual);
		}
		// perfectly out of order
		{
			List<UUID> actual = new ArrayList<>(expected);
			Collections.reverse(actual);
			Collections.sort(actual, c);
			assertEquals(expected, actual);
		}
		// randomness
		{
			for(int i=0; i<5; i++) {
				List<UUID> actual = new ArrayList<>(expected);
				Collections.shuffle(actual);
				Collections.sort(actual, c);
				assertEquals(expected, actual);
			}
		}
	}
	
	@Test
	public void testSortDesc() {
		shuffleSortAndAssert(sortedDesc1, IrisUUID.descTimeUUIDComparator());
		shuffleSortAndAssert(sortedDesc2, IrisUUID.descTimeUUIDComparator());
	}

	@Test
	public void testSortDescWithNull() {
		List<UUID> expected;
		
		expected = new ArrayList<>(sortedDesc1);
		expected.add(null);
		shuffleSortAndAssert(expected, IrisUUID.descTimeUUIDComparator());
		
		expected = new ArrayList<>(sortedDesc1);
		expected.add(null);
		shuffleSortAndAssert(expected, IrisUUID.descTimeUUIDComparator());
	}

	@Test
	public void testSortAsc() {
		List<UUID> expected;
		
		expected = new ArrayList<>(sortedDesc1);
		Collections.reverse(expected);
		shuffleSortAndAssert(expected, IrisUUID.ascTimeUUIDComparator());
		
		expected = new ArrayList<>(sortedDesc1);
		Collections.reverse(expected);
		shuffleSortAndAssert(expected, IrisUUID.ascTimeUUIDComparator());
	}

	@Test
	public void testSortAscWithNull() {
		List<UUID> expected;
		
		expected = new ArrayList<>(sortedDesc1);
		Collections.reverse(expected);
		expected.add(null);
		shuffleSortAndAssert(expected, IrisUUID.ascTimeUUIDComparator());
		
		expected = new ArrayList<>(sortedDesc1);
		Collections.reverse(expected);
		expected.add(null);
		shuffleSortAndAssert(expected, IrisUUID.ascTimeUUIDComparator());
	}

}


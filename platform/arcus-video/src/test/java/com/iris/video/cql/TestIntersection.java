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
package com.iris.video.cql;

import static com.iris.video.cql.UuidAssert.assertTimeUUIDEquals;
import static com.iris.video.cql.UuidAssert.timeUUID;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.iris.video.VideoUtil;

public class TestIntersection {

	private List<UUID> intersectionOf(Iterable<UUID> it1, Iterable<UUID> it2) {
		Iterator<UUID> result = VideoUtil.intersection(it1.iterator(), it2.iterator());
		return Arrays.asList(Iterators.toArray(result, UUID.class));
	}
	
	@Test
	public void testLeftDisjointIntersection() {
		List<UUID> actual = intersectionOf(ImmutableList.of(timeUUID(0x3001), timeUUID(0x2002), timeUUID(0x1003)), ImmutableList.of());
		assertTimeUUIDEquals(ImmutableList.of(), actual);
	}

	@Test
	public void testRightDisjointIntersection() {
		List<UUID> actual = intersectionOf(ImmutableList.of(), ImmutableList.of(timeUUID(0x3001), timeUUID(0x2002), timeUUID(0x1003)));
		assertTimeUUIDEquals(ImmutableList.of(), actual);
	}

	@Test
	public void testLeftSmallerDisjointIntersection() {
		List<UUID> actual = intersectionOf(ImmutableList.of(timeUUID(0x3003), timeUUID(0x2004), timeUUID(0x1005)), ImmutableList.of(timeUUID(0x6001), timeUUID(0x5002), timeUUID(0x4003)));
		assertTimeUUIDEquals(ImmutableList.of(), actual);
	}

	@Test
	public void testRightSmallerDisjointIntersection() {
		List<UUID> actual = intersectionOf(ImmutableList.of(timeUUID(0x6001), timeUUID(0x5002), timeUUID(0x4003)), ImmutableList.of(timeUUID(0x3004), timeUUID(0x2005), timeUUID(0x1006)));
		assertTimeUUIDEquals(ImmutableList.of(), actual);
	}

	@Test
	public void testStrideDisjointIntersection() {
		List<UUID> actual = intersectionOf(ImmutableList.of(timeUUID(0x5002), timeUUID(0x3004), timeUUID(0x1006)), ImmutableList.of(timeUUID(0x6001), timeUUID(0x4003), timeUUID(0x2005)));
		assertTimeUUIDEquals(ImmutableList.of(), actual);
	}

	@Test
	public void testExactIntersection() {
		List<UUID> unity = ImmutableList.of(timeUUID(0x3001), timeUUID(0x2002), timeUUID(0x1003));
		List<UUID> actual = intersectionOf(unity, unity);
		assertTimeUUIDEquals(unity, actual);
	}

	@Test
	public void testLeftIntersection() {
		List<UUID> everything = ImmutableList.of(timeUUID(0x3001), timeUUID(0x2002), timeUUID(0x1003));
		for(int i=0; i<2; i++) {
			List<UUID> expected = ImmutableList.of(everything.get(i));
			List<UUID> actual = intersectionOf(expected, everything);
			assertTimeUUIDEquals(expected, actual);
		}
	}

	@Test
	public void testRightIntersection() {
		List<UUID> everything = ImmutableList.of(timeUUID(0x3001), timeUUID(0x2002), timeUUID(0x1003));
		for(int i=0; i<3; i++) {
			List<UUID> expected = ImmutableList.of(everything.get(i));
			List<UUID> actual = intersectionOf(everything, expected);
			assertTimeUUIDEquals(expected, actual);
		}
	}

}


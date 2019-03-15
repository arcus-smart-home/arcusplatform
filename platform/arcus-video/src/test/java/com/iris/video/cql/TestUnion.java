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

public class TestUnion {

	private List<UUID> unionOf(Iterable<UUID> it1, Iterable<UUID> it2) {
		Iterator<UUID> result = VideoUtil.union(it1.iterator(), it2.iterator());
		return Arrays.asList(Iterators.toArray(result, UUID.class));
	}
	
	@Test
	public void testLeftDisjointUnion() {
		List<UUID> expected = ImmutableList.of(timeUUID(3), timeUUID(2), timeUUID(1));
		List<UUID> actual = unionOf(expected, ImmutableList.of());
		assertTimeUUIDEquals(expected, actual);
	}

	@Test
	public void testRightDisjointUnion() {
		List<UUID> expected = ImmutableList.of(timeUUID(3), timeUUID(2), timeUUID(1));
		List<UUID> actual = unionOf(ImmutableList.of(), expected);
		assertTimeUUIDEquals(expected, actual);
	}

	@Test
	public void testLeftSmallerDisjointUnion() {
		List<UUID> expected = ImmutableList.of(timeUUID(6), timeUUID(5), timeUUID(4), timeUUID(3), timeUUID(2), timeUUID(1));
		List<UUID> actual = unionOf(ImmutableList.of(timeUUID(3), timeUUID(2), timeUUID(1)), ImmutableList.of(timeUUID(6), timeUUID(5), timeUUID(4)));
		assertTimeUUIDEquals(expected, actual);
	}

	@Test
	public void testRightSmallerDisjointUnion() {
		List<UUID> expected = ImmutableList.of(timeUUID(6), timeUUID(5), timeUUID(4), timeUUID(3), timeUUID(2), timeUUID(1));
		List<UUID> actual = unionOf(ImmutableList.of(timeUUID(6), timeUUID(5), timeUUID(4)), ImmutableList.of(timeUUID(3), timeUUID(2), timeUUID(1)));
		assertTimeUUIDEquals(expected, actual);
	}

	@Test
	public void testStrideDisjointUnion() {
		List<UUID> expected = ImmutableList.of(timeUUID(6), timeUUID(5), timeUUID(4), timeUUID(3), timeUUID(2), timeUUID(1));
		List<UUID> actual = unionOf(ImmutableList.of(timeUUID(5), timeUUID(3), timeUUID(1)), ImmutableList.of(timeUUID(6), timeUUID(4), timeUUID(2)));
		assertTimeUUIDEquals(expected, actual);
	}

	@Test
	public void testExactUnion() {
		List<UUID> unity = ImmutableList.of(timeUUID(3), timeUUID(2), timeUUID(1));
		List<UUID> actual = unionOf(unity, unity);
		assertTimeUUIDEquals(unity, actual);
	}

	@Test
	public void testLeftUnion() {
		List<UUID> everything = ImmutableList.of(timeUUID(3), timeUUID(2), timeUUID(1));
		for(int i=0; i<3; i++) {
			List<UUID> smaller = ImmutableList.of(everything.get(0));
			List<UUID> actual = unionOf(smaller, everything);
			assertTimeUUIDEquals(everything, actual);
		}
	}

	@Test
	public void testRightUnion() {
		List<UUID> everything = ImmutableList.of(timeUUID(3), timeUUID(2), timeUUID(1));
		for(int i=0; i<3; i++) {
			List<UUID> smaller = ImmutableList.of(everything.get(0));
			List<UUID> actual = unionOf(everything, smaller);
			assertTimeUUIDEquals(everything, actual);
		}
	}

}


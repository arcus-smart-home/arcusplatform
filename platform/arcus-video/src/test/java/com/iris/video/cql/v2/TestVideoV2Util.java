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
package com.iris.video.cql.v2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import com.iris.util.IrisUUID;
import com.iris.video.VideoRecordingSize;
import com.iris.video.cql.v2.CassandraVideoV2Dao.UnionIterator;
import com.iris.video.recording.ConstantVideoTtlResolver;

public class TestVideoV2Util {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testCreateExpirationIdFromTTL1Day() {
		//UUID recordingId = UUIDs.timeBased();
		UUID recordingId = IrisUUID.timeUUID();
		//1 day TTL
		int ttlInSeconds = (int) (TimeUnit.DAYS.toMillis(1) /1000);
		System.out.println(new Date(IrisUUID.timeof(recordingId)));
		UUID expirationId = VideoV2Util.createExpirationIdFromTTL(recordingId, ttlInSeconds);
		System.out.println(new Date(IrisUUID.timeof(expirationId)));
		
		long actualTtl = VideoV2Util.createActualTTL(recordingId, expirationId);
		assertEquals(ttlInSeconds, actualTtl);
		
	}

	@Test
	public void testCreateExpirationIdFromTTL30Day() {
		UUID recordingId = IrisUUID.timeUUID();
		//30 day TTL
		long ttlInSeconds = ConstantVideoTtlResolver.getDefaultTtlInSeconds();
		//long ttlInSeconds = (TimeUnit.DAYS.toMillis(30) /1000);
		System.out.println(new Date(IrisUUID.timeof(recordingId)));
		UUID expirationId = VideoV2Util.createExpirationIdFromTTL(recordingId, ttlInSeconds);
		System.out.println(new Date(IrisUUID.timeof(expirationId)));
		
		long actualTtl = VideoV2Util.createActualTTL(recordingId, expirationId);
		assertEquals(ttlInSeconds, actualTtl);
		
		UUID expirationId2 = VideoV2Util.createExpirationIdFromTTL(recordingId, ttlInSeconds);
		long actualTtl2 = VideoV2Util.createActualTTL(recordingId, expirationId2);
		//assertEquals(expirationId, expirationId2);
		assertEquals(actualTtl, actualTtl2);
	}
	
	@Test
	public void testCreateExpirationFromTTL30Day() {
		UUID recordingId = IrisUUID.timeUUID();
		//30 day TTL
		long ttlInSeconds = ConstantVideoTtlResolver.getDefaultTtlInSeconds();
		//long ttlInSeconds = (TimeUnit.DAYS.toMillis(30) /1000);
		System.out.println(new Date(IrisUUID.timeof(recordingId)));
		long expirationId = VideoV2Util.createExpirationFromTTL(recordingId, ttlInSeconds);
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(expirationId);
		assertEquals(0, c.get(Calendar.MINUTE));  //expiration time should be the same as purge time, so it should be at the beginning of each hour.
		assertEquals(0, c.get(Calendar.SECOND));
		assertEquals(0, c.get(Calendar.MILLISECOND));
		System.out.println(new Date(expirationId));
		
		long actualTtl = VideoV2Util.createActualTTL(recordingId, expirationId);
		assertTrue(actualTtl>=ttlInSeconds);
		
		
	}
	
	@Test
	public void testFormatDate() throws Exception {
		Date dt1 = new Date();
		long dt1Str = VideoV2Util.formatDate(dt1);
		System.out.println(dt1);
		
		Date dt2 = new Date(dt1Str);
		assertEquals(dt1, dt2);
	}
	
	@Test
	public void testRecordingIdUnionIterator() {
		Calendar c = Calendar.getInstance();
		c.set(Calendar.MINUTE, 59);
		Date d1 = c.getTime();
		c.add(Calendar.MINUTE, -20);
		Date d2 = c.getTime();
		c.add(Calendar.MINUTE, 1);
		Date d3 = c.getTime();
		c.add(Calendar.MINUTE, 5);
		Date d4 = c.getTime();
		c.add(Calendar.MINUTE, 5);
		Date d5 = c.getTime();
		c.add(Calendar.MINUTE, 5);
		List<Date> dateList = ImmutableList.<Date>of(d1, d5, d4, d3, d2);
		System.out.println(dateList);

		List<VideoRecordingSize> expectedList = dateList.stream().map(d -> new VideoRecordingSize(IrisUUID.timeUUID(d), true)).collect(Collectors.toList());
		
		System.out.println("****Expected List - ");
		expectedList.forEach(r -> {
			System.out.println(r + " - "+new Date(IrisUUID.timeof(r.getRecordingId())));
		});
		
		ImmutableList<VideoRecordingSize> list1 = ImmutableList.<VideoRecordingSize>of(
				new VideoRecordingSize(expectedList.get(0).getRecordingId(), true),
				new VideoRecordingSize(expectedList.get(1).getRecordingId(), false),
				new VideoRecordingSize(expectedList.get(3).getRecordingId(), false),
				new VideoRecordingSize(expectedList.get(4).getRecordingId(), true)
				);
		
		ImmutableList<VideoRecordingSize> list2 = ImmutableList.<VideoRecordingSize>of(
				new VideoRecordingSize(expectedList.get(0).getRecordingId(), false),
				new VideoRecordingSize(expectedList.get(1).getRecordingId(), true),
				new VideoRecordingSize(expectedList.get(2).getRecordingId(), true),
				new VideoRecordingSize(expectedList.get(3).getRecordingId(), false),
				new VideoRecordingSize(expectedList.get(4).getRecordingId(), false)
				);
		ImmutableList<VideoRecordingSize> list3 = ImmutableList.<VideoRecordingSize>of(
				new VideoRecordingSize(expectedList.get(1).getRecordingId(), true),
				new VideoRecordingSize(expectedList.get(2).getRecordingId(), true),
				new VideoRecordingSize(expectedList.get(3).getRecordingId(), true)
				);
		List<PeekingIterator<VideoRecordingSize>> itList = new ArrayList<>();
		itList.add(Iterators.peekingIterator(list1.iterator()));
		itList.add(Iterators.peekingIterator(list2.iterator()));
		itList.add(Iterators.peekingIterator(list3.iterator()));
		UnionIterator unionIt = new UnionIterator(itList);
		List<VideoRecordingSize> actualList = new ArrayList<>();
		while(unionIt.hasNext()) {
			actualList.add(unionIt.next());
		}
		System.out.println("****Actual List - ");
		actualList.forEach(r -> {
			System.out.println(r + " - "+new Date(IrisUUID.timeof(r.getRecordingId())));
		});
		assertEquals(expectedList.size(), actualList.size());
		for(int i=0; i<actualList.size(); i++) {
			assertEquals(expectedList.get(i), actualList.get(i));
		}
	}
	
	
	@Test
	public void testUUIDComparator() throws Exception {
		//IrisUUID.descTimeUUIDComparator();
		Calendar c = Calendar.getInstance();
		c.set(Calendar.MINUTE, 59);
		Date d1 = c.getTime();
		UUID o1 = IrisUUID.timeUUID(d1);
		c.add(Calendar.MINUTE, -20);
		Date d2 = c.getTime();  //earlier
		UUID o2 = IrisUUID.timeUUID(d2);
		System.out.println(d1);
		System.out.println(d2);
		assertTrue(d1.after(d2));
		
		assertTrue(IrisUUID.descTimeUUIDComparator().compare(o1, o2) < 0);
		//System.out.println(IrisUUID.descTimeUUIDComparator().compare(o1, o2));
		
	}
	
	@Test
	public void testConvertUUIDToDate() throws Exception {
		String timeUUIDStr = "253d1800-ab59-11e8-a221-e131ca7e19ba";
		UUID id = IrisUUID.fromString(timeUUIDStr);
		long t = IrisUUID.timeof(id);
		System.out.println(new Date(t));
		
		
	}
	
	@Test
	public void testLongToDate() throws Exception {
		long d1 = 1538715600000l;
		System.out.println(new Date(d1));
		
		long d2 = 1538708414043l;
		System.out.println(new Date(d2));
	}

	

}


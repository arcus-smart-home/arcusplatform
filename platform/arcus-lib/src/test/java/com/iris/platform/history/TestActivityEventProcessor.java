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
package com.iris.platform.history;

import static com.google.common.collect.ImmutableSet.of;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.iris.messages.model.Fixtures;
import com.iris.messages.type.ActivityInterval;

public class TestActivityEventProcessor {
	private ActivityEventProcessor processor;
	
	private UUID placeId = UUID.randomUUID();
	private String device1 = Fixtures.createDeviceAddress().getRepresentation();
	private String device2 = Fixtures.createDeviceAddress().getRepresentation();
	private String device3 = Fixtures.createDeviceAddress().getRepresentation();
	
	protected ActivityEvent event(long ts, String... activeDevices) {
		return event(ts, new HashSet<>(Arrays.asList(activeDevices)), of());
	}
	
	protected ActivityEvent event(long ts, Set<String> activeDevices, Set<String> inactiveDevices) {
		ActivityEvent event = new ActivityEvent();
		event.setPlaceId(placeId);
		event.setTimestamp(new Date(ts));
		event.setActiveDevices(activeDevices);
		event.setInactiveDevices(inactiveDevices);
		return event;
	}
	
	@Before
	public void init() {
		processor = new ActivityEventProcessor(
				new Date(1000),
				new Date(10000),
				1000
		);
	}
	
	@Test
	public void testEmpty() {
		List<ActivityInterval> intervals = processor.consume(ImmutableList.of());
		assertEquals(0, intervals.size());
	}
	
	@Test
	public void testConsecutiveActivations() {
		List<ActivityEvent> events = Arrays.asList(
				event(9010, device1, device2, device3), // everything activated
				event(8100, device1, device2),
				event(7300, device1),
				event(500) // nothing activated
		);
		
		{
			List<ActivityInterval> intervals = processor.consume(events);
			assertEquals(4, intervals.size());
			int idx = 0;
			assertInterval(intervals, idx++, 1000, of());
			assertInterval(intervals, idx++, 7000, of(device1));
			assertInterval(intervals, idx++, 8000, of(device1, device2));
			assertInterval(intervals, idx++, 9000, of(device1, device2, device3));
		}
		{
			processor = new ActivityEventProcessor(processor.getStartTime(), processor.getEndTime(), processor.getBucketSizeMs(), of(device1));
			List<ActivityInterval> intervals = processor.consume(events);
			assertEquals(2, intervals.size());
			int idx = 0;
			assertInterval(intervals, idx++, 1000, of());
			assertInterval(intervals, idx++, 7000, of(device1));
		}
		{
			processor = new ActivityEventProcessor(processor.getStartTime(), processor.getEndTime(), processor.getBucketSizeMs(), of(device3));
			List<ActivityInterval> intervals = processor.consume(events);
			assertEquals(2, intervals.size());
			int idx = 0;
			assertInterval(intervals, idx++, 1000, of());
			assertInterval(intervals, idx++, 9000, of(device3));
		}
		{
			processor = new ActivityEventProcessor(processor.getStartTime(), processor.getEndTime(), processor.getBucketSizeMs(), of(device1, device3));
			List<ActivityInterval> intervals = processor.consume(events);
			assertEquals(3, intervals.size());
			int idx = 0;
			assertInterval(intervals, idx++, 1000, of());
			assertInterval(intervals, idx++, 7000, of(device1));
			assertInterval(intervals, idx++, 9000, of(device1, device3));
		}
		{
			processor = new ActivityEventProcessor(processor.getStartTime(), processor.getEndTime(), processor.getBucketSizeMs(), of(device1, device2, device3));
			List<ActivityInterval> intervals = processor.consume(events);
			assertEquals(4, intervals.size());
			int idx = 0;
			assertInterval(intervals, idx++, 1000, of());
			assertInterval(intervals, idx++, 7000, of(device1));
			assertInterval(intervals, idx++, 8000, of(device1, device2));
			assertInterval(intervals, idx++, 9000, of(device1, device2, device3));
		}
	}

	@Test
	public void testSparseActivations() {
		List<ActivityEvent> events = Arrays.asList(
				event(6400, device1, device2, device3), // everything activated
				event(4200, device1, device2),
				event(2100, device1),
				event(500) // nothing activated
		);
		
		List<ActivityInterval> intervals = processor.consume(events);
		assertEquals(4, intervals.size());
		int idx = 0;
		assertInterval(intervals, idx++, 1000, of());
		assertInterval(intervals, idx++, 2000, of(device1));
		assertInterval(intervals, idx++, 4000, of(device1, device2));
		assertInterval(intervals, idx++, 6000, of(device1, device2, device3));
	}

	@Test
	public void testDenseActivations() {
		List<ActivityEvent> events = Arrays.asList(
				event(3300, device1, device2, device3), // everything activated
				event(3200, device1, device2),
				event(3100, device1),
				event(500) // nothing activated
		);
		
		List<ActivityInterval> intervals = processor.consume(events);
		assertEquals(2, intervals.size());
		int idx = 0;
		assertInterval(intervals, idx++, 1000, of());
		assertInterval(intervals, idx++, 3000, of(device1, device2, device3));
	}

	@Test
	public void testConsecutiveDeactivations() {
		List<ActivityEvent> events = Arrays.asList(
				event(8010, of(), of(device1, device2, device3)), // everything deactivated
				event(7100, of(device1), of(device2, device3)),
				event(6300, of(device1, device2), of(device3)),
				event(500, device1, device2, device3) // everything activated
		);
		
		List<ActivityInterval> intervals = processor.consume(events);
		assertEquals(4, intervals.size());
		int idx = 0;
		assertInterval(intervals, idx++, 1000, of(device1, device2, device3));
		assertInterval(intervals, idx++, 7000, of(device1, device2), of(device3));
		assertInterval(intervals, idx++, 8000, of(device1), of(device2));
		assertInterval(intervals, idx++, 9000, of(), of(device1));
	}

	@Test
	public void testSparseDeactivations() {
		List<ActivityEvent> events = Arrays.asList(
				event(6400, of(), of(device1, device2, device3)), // everything deactivated
				event(4200, of(device1), of(device2, device3)),
				event(2100, of(device1, device2), of(device3)),
				event(500, device1, device2, device3) // everything activated
		);
		
		List<ActivityInterval> intervals = processor.consume(events);
		assertEquals(4, intervals.size());
		int idx = 0;
		assertInterval(intervals, idx++, 1000, of(device1, device2, device3));
		assertInterval(intervals, idx++, 3000, of(device1, device2), of(device3));
		assertInterval(intervals, idx++, 5000, of(device1), of(device2));
		assertInterval(intervals, idx++, 7000, of(), of(device1));
	}

	@Test
	public void testDenseDeactivations() {
		List<ActivityEvent> events = Arrays.asList(
				event(3300, of(), of(device1, device2, device3)), // everything inactive
				event(3200, of(device1), of(device2, device3)),
				event(3100, of(device1, device2), of(device3)),
				event(500, device1, device2, device3) // everything activate
		);
		
		List<ActivityInterval> intervals = processor.consume(events);
		assertEquals(2, intervals.size());
		int idx = 0;
		assertInterval(intervals, idx++, 1000, of(device1, device2, device3));
		assertInterval(intervals, idx++, 4000, of(), of(device1, device2, device3));
	}

	@Test
	public void testConsecutiveFlappingDeactivations() {
		List<ActivityEvent> events = Arrays.asList(
				event(2400, of(), of(device1)),
				event(2200, of(device1), of()),
				event(1600, of(), of(device1)),
				event(1100, of(device1), of()),
				event(500) // nothing activated
		);
		
		List<ActivityInterval> intervals = processor.consume(events);
		assertEquals(2, intervals.size());
		int idx = 0;
		assertInterval(intervals, idx++, 1000, of(device1));
		assertInterval(intervals, idx++, 3000, of(), of(device1));
	}

	@Test
	public void testDenseFlappingDeactivations() {
		List<ActivityEvent> events = Arrays.asList(
				event(3300, of(device1), of(device2)),
				event(3200, of(device2), of(device1)),
				event(3100, device1, device2),
				event(500) // nothing activated
		);
		
		List<ActivityInterval> intervals = processor.consume(events);
		assertEquals(3, intervals.size());
		int idx = 0;
		assertInterval(intervals, idx++, 1000, of());
		assertInterval(intervals, idx++, 3000, of(device1, device2));
		assertInterval(intervals, idx++, 4000, of(device1), of(device2));
	}

	@Test
	public void testBoundaryDeactivation() {
		List<ActivityEvent> events = Arrays.asList(
				event(9100, of(), of(device1)),
				event(500, device1)
		);
		
		List<ActivityInterval> intervals = processor.consume(events);
		assertEquals(1, intervals.size());
		int idx = 0;
		assertInterval(intervals, idx++, 1000, of(device1));
	}
	
	@Test
	public void testDeletedAcrossWindow() {
		List<ActivityEvent> events = Arrays.asList(
				event(2200), // it just disappears
				event(500, device1)
		);
		
		List<ActivityInterval> intervals = processor.consume(events);
		assertEquals(2, intervals.size());
		int idx = 0;
		assertInterval(intervals, idx++, 1000, of(device1));
		assertInterval(intervals, idx++, 3000, of(), of(device1));
	}

	@Test
	public void testDeactivateAcrossWindow() {
		List<ActivityEvent> events = Arrays.asList(
				event(2200, of(), of(device1)),
				event(500, device1)
		);
		
		List<ActivityInterval> intervals = processor.consume(events);
		assertEquals(2, intervals.size());
		int idx = 0;
		assertInterval(intervals, idx++, 1000, of(device1));
		assertInterval(intervals, idx++, 3000, of(), of(device1));
	}

	@Test
	public void testDeactivateBeforeWindow() {
		List<ActivityEvent> events = Arrays.asList(
				event(500, of(device1, device2), of(device2))
		);
		
		List<ActivityInterval> intervals = processor.consume(events);
		assertEquals(1, intervals.size());
		int idx = 0;
		assertInterval(intervals, idx++, 1000, of(device1));
	}

	private void assertInterval(List<ActivityInterval> intervals, int index, long ts, Set<String> active) {
		assertInterval(intervals, index, ts, active, of());
	}

	private void assertInterval(List<ActivityInterval> intervals, int index, long ts, Set<String> active, Set<String> deactivated) {
		ActivityInterval interval = intervals.get(index);
		assertEquals(ts, interval.getStart().getTime());
		assertEquals(Sets.union(active, deactivated), interval.getDevices().keySet());
		for(String device: active) {
			assertEquals(ActivityInterval.DEVICES_ACTIVATED, interval.getDevices().get(device));
		}
		for(String device: deactivated) {
			assertEquals(ActivityInterval.DEVICES_DEACTIVATED, interval.getDevices().get(device));
		}
	}

}



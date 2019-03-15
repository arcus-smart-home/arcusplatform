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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.iris.messages.type.ActivityInterval;

public class ActivityEventProcessor {
	private final Date startTime;
	private final Date endTime;
	private final Set<String> filter;
	private final int bucketSizeMs;
	
	public ActivityEventProcessor(Date startTime, Date endTime, int bucketSizeMs) {
		this(startTime, endTime, bucketSizeMs, null);
	}
	
	public ActivityEventProcessor(Date startTime, Date endTime, int bucketSizeMs, Set<String> filter) {
		this.startTime = startTime;
		this.endTime = endTime;
		this.bucketSizeMs = bucketSizeMs;
		this.filter = filter == null || filter.isEmpty() ? null : ImmutableSet.copyOf(filter);
	}
	
	public Date getStartTime() {
		return startTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public Set<String> getFilter() {
		return filter;
	}

	public int getBucketSizeMs() {
		return bucketSizeMs;
	}

	public List<ActivityInterval> consume(Iterable<ActivityEvent> events) {
		long lastBucket = toBucket(endTime);
		ActivityEventBucket consumer = new ActivityEventBucket(lastBucket);
		for(ActivityEvent event: events) {
			event = filter(event);
			consumer = consumer.update(event);
		}
		return toIntervals(consumer);
	}
	
	private ActivityEvent filter(ActivityEvent event) {
		if(filter == null) {
			return event;
		}
		ActivityEvent copy = new ActivityEvent();
		copy.setPlaceId(event.getPlaceId());
		copy.setTimestamp(event.getTimestamp());
		if(!event.getActiveDevices().isEmpty()) {
			Set<String> filtered = new HashSet<>(event.getActiveDevices());
			filtered.retainAll(filter);
			copy.setActiveDevices(filtered);
		}
		if(!event.getInactivateDevices().isEmpty()) {
			Set<String> filtered = new HashSet<>(event.getInactivateDevices());
			filtered.retainAll(filter);
			copy.setInactiveDevices(filtered);
		}
		return copy;
	}

	private ActivityEventBucket snapToStartTime(ActivityEventBucket consumer) {
		long startBucket = toBucket(startTime);
		if(consumer.bucket < startBucket) {
			// fast forward
			while(consumer.next != null && consumer.next.bucket < startBucket) {
				consumer = consumer.next;
			}
			if(consumer.next != null && consumer.next.bucket == startBucket) {
				Set<String> deactivatedDevices = consumer.calculateDeactivations(consumer.activeDevices);
				if(!deactivatedDevices.isEmpty()) {
					consumer.deactivateInNextBucket(deactivatedDevices);
				}
				consumer = consumer.next;
			}
			else {
				// don't create false deactivations, this is historical
				consumer.activeDevices.removeAll(consumer.inactiveDevices);
				consumer.bucket = startBucket;
			}
		}
		else if(
				consumer.bucket > startBucket &&
				!consumer.isLast() // don't try to rewind the last bucket
		) {
			// rewind
			consumer = new ActivityEventBucket(startBucket, consumer);
			consumer.activeDevices.addAll(consumer.next.activeDevices);
		}
		// else we're at a perfect match already
		return consumer;
	}
	
	private long toBucket(Date timestamp) {
		return timestamp.getTime() / bucketSizeMs;
	}

	private Date toDate(long bucket) {
		return new Date(bucket * bucketSizeMs);
	}
	
	private List<ActivityInterval> toIntervals(ActivityEventBucket consumer) {
		consumer = snapToStartTime(consumer);
		
		Set<String> lastActiveDevices = ImmutableSet.of();
		List<ActivityInterval> intervals = new ArrayList<>();
		// the first entry is a dummy entry
		while(consumer.next != null) {
			Set<String> deactivatedDevices = consumer.calculateDeactivations(lastActiveDevices);
			Set<String> currentActiveDevices = new HashSet<>(consumer.activeDevices);
			if(!deactivatedDevices.isEmpty()) {
				consumer.deactivateInNextBucket(deactivatedDevices);
			}
			
			if(
					consumer.deactivatedDevices.isEmpty() && 
					lastActiveDevices != ImmutableSet.<String>of() && 
					lastActiveDevices.equals(consumer.activeDevices)
			) {
				// this is a dummy entry, all its changes are deactivations deferred ot the next interval
			}
			else {
				ActivityInterval interval = consumer.toInterval();
				intervals.add(interval);
			}

			lastActiveDevices = currentActiveDevices;
			consumer = consumer.next;
		}
		return intervals;
	}
	
	private class ActivityEventBucket {
		private long bucket;
		private boolean firstMerge;
		@Nullable
		private ActivityEventBucket next;
		private final Set<String> activeDevices;
		private final Set<String> inactiveDevices;
		private final Set<String> deactivatedDevices; // FIXME store deactivated in the DB!
		
		private ActivityEventBucket(long bucket) {
			this(bucket, null);
		}
		
		private ActivityEventBucket(long bucket, ActivityEventBucket next) {
			this.bucket = bucket;
			this.firstMerge = true;
			this.next = next;
			this.activeDevices = new HashSet<>();
			this.inactiveDevices = new HashSet<>();
			this.deactivatedDevices = new HashSet<>();
		}
		
		public boolean isLast() {
			return next == null;
		}
		
		public boolean deactivateInNextBucket(Set<String> toDeactivate) {
			this.activeDevices.addAll(toDeactivate);
			if(this.next == null) {
				// don't create future buckets
				return false;
			}
			else if(this.next.bucket != this.bucket + 1) {
				ActivityEventBucket consumer = new ActivityEventBucket(bucket + 1, this.next);
				this.next = consumer;
				consumer.activeDevices.addAll(this.activeDevices);
				consumer.activeDevices.removeAll(toDeactivate);
				consumer.inactiveDevices.addAll(this.inactiveDevices);
				consumer.deactivatedDevices.addAll(toDeactivate);
				return true;
			}
			else {
				this.next.deactivatedDevices.addAll(toDeactivate);
				// remove devices that are re-activated in the next window
				this.next.deactivatedDevices.removeAll(this.next.activeDevices);
				return false;
			}
		}
		
		public ActivityEventBucket update(ActivityEvent event) {
			long destinationBucket = event.getTimestamp().getTime() / bucketSizeMs;
			if(destinationBucket >= bucket) {
				return merge(event);
			}
			else {
				return new ActivityEventBucket(destinationBucket, this).merge(event);
			}
		}
		
		public ActivityEventBucket merge(ActivityEvent event) {
			if(this.firstMerge) {
				this.firstMerge = false;
				// track what was inactive at the end of the bucket
				this.inactiveDevices.addAll(event.getInactivateDevices()); 
			}
			activeDevices.addAll(event.getActiveDevices());
			return this;
		}
		
		public Set<String> calculateDeactivations(Set<String> nextActive) {
			if(inactiveDevices.isEmpty() && activeDevices.containsAll(nextActive)) {
				return ImmutableSet.of();
			}
			
			HashSet<String> deactivatedDevices = new HashSet<>(activeDevices);
			deactivatedDevices.retainAll(inactiveDevices);
			deactivatedDevices.addAll(Sets.difference(nextActive, activeDevices));
			deactivatedDevices.removeAll(this.deactivatedDevices);
			return deactivatedDevices;
		}
		
		public ActivityInterval toInterval() {
			ActivityInterval interval = new ActivityInterval();
			interval.setStart(toDate(bucket));
			interval.setDevices(toDeviceActivity());
			return interval;
		}
		
		public Map<String, String> toDeviceActivity() {
			if(activeDevices.isEmpty() && deactivatedDevices.isEmpty()) {
				return ImmutableMap.of();
			}
			
			Map<String, String> activity = new HashMap<>(2 * (activeDevices.size() + deactivatedDevices.size()));
			for(String active: activeDevices) {
				activity.put(active, ActivityInterval.DEVICES_ACTIVATED);
			}
			for(String deactivated: deactivatedDevices) {
				activity.put(deactivated, ActivityInterval.DEVICES_DEACTIVATED);
			}
			return activity;
		}

		@Override
		public String toString() {
			return "ActivityEventConsumer [bucket=" + bucket + ", activeDevices=" + activeDevices + ", inactiveDevices="
					+ inactiveDevices + ", deactivatedDevices=" + deactivatedDevices + "]";
		}
		
	}
	
}


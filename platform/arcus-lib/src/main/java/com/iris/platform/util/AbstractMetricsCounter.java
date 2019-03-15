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
package com.iris.platform.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This generic class can be extended to keep track of counts of all the enum values
 * @author daniellepatrow
 *
 * @param <T>
 */
public class AbstractMetricsCounter<T extends Enum<T>> {
	private Map<T, AtomicInteger> counterMap;
	private final Class<T> clazz;
	
	public AbstractMetricsCounter(Class<T> clazz) {
		this.clazz = clazz;
		initMap();
	}
	
	private void initMap() {
		Map<T, AtomicInteger> newMap = new HashMap<>();
		T[] allValues = clazz.getEnumConstants();
		for(int i=0; i<allValues.length; i++) {
			newMap.put(allValues[i], new AtomicInteger(0));
		}
		counterMap = newMap;
	}
	
	public void clearAll() {
		initMap();
	}

	public int getTotal() {
		AtomicInteger total = new AtomicInteger(0);
		counterMap.values().forEach(c -> {
			total.addAndGet(c.get());
		});
		return total.get();
	}
	
	public int getCount(T t) {
		return counterMap.get(t).get();
	}
	
	public int incrementAndGet(T t) {		
		return counterMap.get(t).incrementAndGet();
	}
	
	public int incrementAndGet(int num, T t) {	
		return counterMap.get(t).addAndGet(num);
	}
	
	public int merge(AbstractMetricsCounter<T> that) {
		AtomicInteger total = new AtomicInteger(0);
		if(that != null) {
			counterMap.entrySet().forEach(curEntry -> {
				total.addAndGet(curEntry.getValue().addAndGet(that.getCount(curEntry.getKey()))) ;
			});
		}
		return total.get();
	}
}


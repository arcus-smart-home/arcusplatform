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
package com.iris.core.dao.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;
import com.google.inject.Singleton;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;
import com.iris.metrics.tag.TaggingMetric;

@Singleton
public class DaoMetrics {

	private static final IrisMetricSet METRICS = IrisMetrics.metrics("dao");

	private static final String TAG_NAME = "op";

	private static final TaggingMetric<Timer> readTimer = METRICS.taggingTimer("read.time");
	private static final TaggingMetric<Timer> insertTimer = METRICS.taggingTimer("insert.time");
	private static final TaggingMetric<Timer> updateTimer = METRICS.taggingTimer("update.time");
	private static final TaggingMetric<Timer> upsertTimer = METRICS.taggingTimer("upsert.time");
	private static final TaggingMetric<Timer > deleteTimer = METRICS.taggingTimer("delete.time");

	public static Timer readTimer(Class<?> dao, String method) {
		return readTimer.tag(TAG_NAME, tagValue(dao, method));
	}

	public static Timer insertTimer(Class<?> dao, String method) {
		return insertTimer.tag(TAG_NAME, tagValue(dao, method));
	}

	public static Timer updateTimer(Class<?> dao, String method) {
		return updateTimer.tag(TAG_NAME, tagValue(dao, method));
	}

	public static Timer upsertTimer(Class<?> dao, String method) {
		return upsertTimer.tag(TAG_NAME, tagValue(dao, method));
	}

	public static Timer deleteTimer(Class<?> dao, String method) {
		return deleteTimer.tag(TAG_NAME, tagValue(dao, method));
	}

	public static Histogram histogram(Class<?> dao, String name) {
		return METRICS.histogram(normalizeDaoName(dao) + "." + name.toLowerCase());
	}

	public static Counter counter(Class<?> dao, String name) {
		return METRICS.counter(normalizeDaoName(dao) + "." + name.toLowerCase());
	}

	private static String tagValue(Class<?> dao, String method) {
		return normalizeDaoName(dao)+ "." + method.toLowerCase();
	}

	private static String normalizeDaoName(Class<?> dao) {
		String name = dao.getSimpleName().toLowerCase();

		return name
				.replaceAll("impl", "")
				.replaceAll("cassandra", "");
	}

	private DaoMetrics() {
	}
}


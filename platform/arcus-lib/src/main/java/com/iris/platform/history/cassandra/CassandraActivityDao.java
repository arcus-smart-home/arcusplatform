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
package com.iris.platform.history.cassandra;

import java.util.Date;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.core.dao.cassandra.CassandraQueryBuilder;
import com.iris.core.dao.metrics.DaoMetrics;
import com.iris.platform.history.ActivityEvent;
import com.iris.platform.history.HistoryActivityDAO;
import com.iris.platform.history.HistoryAppenderConfig;
import com.iris.platform.history.HistoryAppenderDAO;

@Singleton
public class CassandraActivityDao implements HistoryActivityDAO {
   private static final Timer activitySystemLogTimer = DaoMetrics.insertTimer(HistoryAppenderDAO.class, "activity.subsystem");
   private static final Timer activitySystemReadTimer = DaoMetrics.readTimer(HistoryAppenderDAO.class, "activity.subsystem");
   
	public static final String TABLE_NAME = "histlog_care_activity";
	public static final int COLUMN_COUNT = 3;

	public static final class Columns {
		public static final String PLACE_ID  = "placeId";
		public static final String TIME = "time";
		public static final String ACTIVE_DEVICES = "activeDevices";
		public static final String DEACTIVATED_DEVICES = "deactivatedDevices";
	}

	private final Session session;
	private final PreparedStatement upsert;
	private final PreparedStatement listByRange;
	
	private final int bucketSizeMs;
	private final long rowTtlMs;
	
	@Inject
	public CassandraActivityDao(
			@Named(CassandraHistory.NAME) Session session, 
			HistoryAppenderConfig config
	) {
		this.bucketSizeMs = (int) TimeUnit.SECONDS.toMillis(config.getActivityBucketSizeSec());
		this.rowTtlMs = TimeUnit.HOURS.toMillis(config.getActivitySubsysTtlHours());
		this.session = session;
		this.upsert = 
				CassandraQueryBuilder
					.update(TABLE_NAME)
					.set(
							// anything that was active in this window counts as active
							Columns.ACTIVE_DEVICES + " = " + Columns.ACTIVE_DEVICES + " + ?, " +
							// only things that are inactive *at the end of the window* count as inactive
							Columns.DEACTIVATED_DEVICES + " = ?"
					)
					.addWhereColumnEquals(Columns.PLACE_ID)
					.addWhereColumnEquals(Columns.TIME)
					.withTtlSec(TimeUnit.HOURS.toSeconds(config.getActivitySubsysTtlHours()))
					.usingTimestamp()
					.prepare(session);
		
		this.listByRange =
				CassandraQueryBuilder
					.select(TABLE_NAME)
					.addColumns(Columns.PLACE_ID, Columns.TIME, Columns.ACTIVE_DEVICES, Columns.DEACTIVATED_DEVICES)
					.where(Columns.PLACE_ID + " = ? AND " + Columns.TIME + " >= ? AND " + Columns.TIME + " < ?")
					.withConsistencyLevel(ConsistencyLevel.LOCAL_ONE)
					.prepare(session);

	}

	@Override
	public void append(ActivityEvent event) {
		try(Context c = activitySystemLogTimer.time()) {
			Date timeBucket = bucket(event.getTimestamp());
			BoundStatement bs = upsert.bind(
					event.getTimestamp().getTime() * 1000,
					event.getActiveDevices(),
					event.getInactivateDevices(),
					event.getPlaceId(),
					timeBucket
			);
			session.execute( bs );
		}
	}

	@Override
	public Iterable<ActivityEvent> stream(UUID placeId, Date startTime, Date endTime) {
		final Date startBucket = new Date(bucket(startTime).getTime());
		final Date endBucket = new Date(bucket(endTime).getTime() + bucketSizeMs);
		return new Iterable<ActivityEvent>() {
			@Override
			public Iterator<ActivityEvent> iterator() {
				return new Iterator<ActivityEvent>() {
					final Context c = activitySystemReadTimer.time();   
					Iterator<Row> it = session.execute(listByRange.bind(placeId, startBucket, endBucket)).iterator();
					boolean needsOneMore = true;
					Row oneMore = null;
					
					@Override
					public boolean hasNext() {
						if(it.hasNext()) {
							return true;
						}
						tryOneMore();
						return oneMore != null;
					}

					@Override
					public ActivityEvent next() {
						if(it.hasNext()) {
							ActivityEvent event = transform( it.next() );
							if(event.getTimestamp().equals(startBucket)) {
								// if we've got a sample in the last bucket, then we don't need to
								// go back any farther
								needsOneMore = false;
								c.stop();
							}
							return event;
						}
						tryOneMore(); 
						
						if(oneMore == null) {
							throw new NoSuchElementException();
						}
						ActivityEvent event = transform(oneMore);
						oneMore = null;
						return event;
					}
					
					private void tryOneMore() {
						if(!needsOneMore) {
							return;
						}
						
						needsOneMore = false;
						BoundStatement bs = listByRange.bind(placeId, new Date(System.currentTimeMillis() - rowTtlMs), startBucket);
						bs.setFetchSize(1);
						oneMore = session.execute( bs ).one();
						c.stop();
					}
				};
			}
		};
	}
	
	protected ActivityEvent transform(Row row) {
		ActivityEvent event = new ActivityEvent();
		event.setTimestamp(row.getTimestamp(Columns.TIME));
		event.setPlaceId(row.getUUID(Columns.PLACE_ID));
		event.setActiveDevices(row.getSet(Columns.ACTIVE_DEVICES, String.class));
		event.setInactiveDevices(row.getSet(Columns.DEACTIVATED_DEVICES, String.class));
		return event;
	}

	private Date bucket(Date timestamp) {
		if(this.bucketSizeMs == 0) {
			return timestamp;
		}
		return new Date( (timestamp.getTime() / this.bucketSizeMs) * this.bucketSizeMs );
	}

}


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

import java.util.Date;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.cassandra.CassandraQueryBuilder;
import com.iris.video.PlacePurgeRecord;
import com.iris.video.PlacePurgeRecord.PurgeMode;
import com.iris.video.VideoDaoConfig;
import com.iris.video.cql.VideoTable;

@Singleton
public class PlacePurgeRecordingTable extends VideoTable {


	/**
	 * CREATE TABLE place_purge_recording (
         deletetime timestamp,
         placeid uuid,
         mode text,
         PRIMARY KEY ((deletetime),placeid)
      ) WITH COMPACT STORAGE
        AND CLUSTERING ORDER BY (placeid ASC);
	 */
	
	public static final String TABLE_NAME = "place_purge_recording";
	private static final Logger logger = LoggerFactory.getLogger(PlacePurgeRecordingTable.class);
	
	public static final String COL_PLACEID = "placeid";
	public static final String COL_DELETE_TIME = "deletetime";
	public static final String COL_MODE = "mode";
	private static final String[] COLUMNS = {COL_DELETE_TIME, COL_PLACEID, COL_MODE};
	
	protected final PreparedStatement insert;
	protected final PreparedStatement deleteByDeleteTime;
	protected final PreparedStatement selectByDeleteTime;
	
	@Inject
	public PlacePurgeRecordingTable(String ts, Session session, VideoDaoConfig config) {
		super(ts, session);
		this.deleteByDeleteTime =
				CassandraQueryBuilder
					.delete(getTableName())
					.where(COL_DELETE_TIME + " = ?")
					.prepare(session);
		this.insert = 
				CassandraQueryBuilder
				.insert(getTableName())
				.addColumns(getTableColumns())
				.withTtlSec(config.getPlacePurgeRecordingTTLInSeconds())
				.prepare(session);
		this.selectByDeleteTime = CassandraQueryBuilder
				.select(getTableName())
				.addColumns(getTableColumns())
				.where(COL_DELETE_TIME + " = ?")
				.prepare(session);
	}	
	
	@Override
	public String getTable() {
		return TABLE_NAME;
	}

	protected String[] getTableColumns() {
		return COLUMNS;
	}
	
	public BoundStatement insert(Date deleteTime, UUID placeId, PurgeMode mode) {
		return insert.bind(deleteTime, placeId, mode.name());
	}
	
	public BoundStatement selectBy(Date deleteTime) {
		return selectByDeleteTime.bind(deleteTime);
	}
	
	public BoundStatement deleteBy(Date deleteTime) {
		return deleteByDeleteTime.bind(deleteTime);
	}
	
	public PlacePurgeRecord buildEntity(Row row) {
		PurgeMode mode = PurgeMode.ALL;
		if(!row.isNull(COL_MODE)) {
			mode = PurgeMode.valueOf(row.getString(COL_MODE));
		}
		return new PlacePurgeRecord(row.getUUID(COL_PLACEID), row.getDate(COL_DELETE_TIME), mode);
		
	}
}


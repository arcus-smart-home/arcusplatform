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

import java.nio.ByteBuffer;
import java.util.UUID;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.core.cql.Statement;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.video.cql.RecordingTableField;

import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.*;

@Singleton
public class RecordingV2Table extends AbstractRecordingV2Table {
	/**
	 * CREATE TABLE recording_v2 (
			recordingid timeuuid,
			expiration timestamp,
            ts double,
            bo bigint,
            bl blob,
            PRIMARY KEY (recordingid,expiration, ts,bo)
         )
         WITH CLUSTERING ORDER BY (expiration DESC, ts ASC, bo ASC)
	 */
	public static final String TABLE_NAME = "recording_v2";
   public static final String COL_EXPIRATION = "expiration";
   private static final String[] COLUMNS = {COL_TS, COL_BO, COL_BL, COL_RECORDINGID, COL_EXPIRATION};


	@Inject
	public RecordingV2Table(String ts, CqlSession session) {
		super(ts, session);
	}

	@Override
	protected String[] getTableColumns() {
		return COLUMNS;
	}


	public Statement<?> insertField(UUID recordingId, long expiration, long actualTtlInSeconds, RecordingTableField ref, ByteBuffer value) {
		return insertIFrame(recordingId, expiration, actualTtlInSeconds, ref.ts(), ref.bo(), value);
	}

	public Statement<?> insertIFrame(UUID recordingId, long expiration, long actualTtlInSeconds, double ts, long bo, ByteBuffer value) {
		SimpleStatement insert = insertInto(getTableSpace(), TABLE_NAME)
			.value(COLUMNS[0], literal(ts))
			.value(COLUMNS[1], literal(bo))
			.value(COLUMNS[2], literal(value))
			.value(COLUMNS[3], literal(recordingId))
			.value(COLUMNS[4], literal(expiration))
			.usingTtl((int)actualTtlInSeconds)
			.build();
		return insert;
	}

	public Statement<?> insertIFrame(UUID recordingId, long ttlInSeconds, double ts, long bo, ByteBuffer value) {
		long expiration = VideoV2Util.createExpirationFromTTL(recordingId, ttlInSeconds);
		return insertIFrame(recordingId, expiration, VideoV2Util.createActualTTL(recordingId, expiration), ts, bo, value);
	}

	@Override
	public String getTable() {
		return TABLE_NAME;
	}

}

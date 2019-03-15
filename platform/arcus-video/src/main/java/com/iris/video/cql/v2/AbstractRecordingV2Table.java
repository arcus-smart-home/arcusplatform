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

import java.util.UUID;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.policies.DowngradingConsistencyRetryPolicy;
import com.datastax.driver.core.policies.LoggingRetryPolicy;
import com.google.inject.Inject;
import com.iris.core.dao.cassandra.CassandraQueryBuilder;
import com.iris.video.cql.VideoTable;

public abstract class AbstractRecordingV2Table extends VideoTable{
	
   public static final String COL_RECORDINGID = "recordingid";
   public static final String COL_TS = "ts";
   public static final String COL_BO = "bo";
   public static final String COL_BL = "bl";
   
	protected final PreparedStatement selectByRecordingId;
	protected final PreparedStatement deleteRecording;

	@Inject
	public AbstractRecordingV2Table(String ts, Session session) {
		super(ts, session);
		this.selectByRecordingId = CassandraQueryBuilder
					.select(getTableName())
					.addColumns(getTableColumns())
					.addWhereColumnEquals(COL_RECORDINGID)
					.prepare(session);
				
		this.deleteRecording =
				CassandraQueryBuilder
					.delete(getTableName())
					.addWhereColumnEquals(COL_RECORDINGID)
					.withRetryPolicy(new LoggingRetryPolicy(DowngradingConsistencyRetryPolicy.INSTANCE))
					.prepare(session);
	}
	protected abstract String[] getTableColumns() ;
	
	public BoundStatement select(UUID recordingId) {
		return selectByRecordingId.bind(recordingId);
	}	

	public BoundStatement deleteRecording(UUID recordingId) {
		return deleteRecording.bind(recordingId);
	}
	
	

}


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

import java.util.Date;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.google.common.collect.Iterators;
import com.google.inject.Inject;
import com.iris.core.dao.cassandra.CassandraQueryBuilder;
import com.iris.util.IrisUUID;

public abstract class AbstractPurgeRecordingTable extends VideoTable{
	
	public static final String COL_DELETETIME = "deletetime";
	public static final String COL_PARTITIONID = "partitionid";
	public static final String COL_RECORDINGID = "recordingid";
	public static final String COL_PLACEID = "placeid";
	public static final Date METADATA_DATE = new Date(0);
	public static final long METADATA_UUID_RANDOM = 0x6221E031CA7E19BAL;
	
	protected final PreparedStatement insert;
	protected final PreparedStatement deleteByTimeAndPartitionAndRecordingId;
	protected final PreparedStatement select;
	protected final Session session;
	protected final PreparedStatement deleteByTimeAndPartition;
	
	//Abstract methods
	protected abstract String[] getTableColumns();
	protected abstract PurgeRecord buildEntity(Row row) ;
	
	
	
	@Inject
	public AbstractPurgeRecordingTable(String ts, Session session) {
		super(ts, session);
		this.session = session;
		select = CassandraQueryBuilder
				.select(getTableName())
				.addColumns(getTableColumns())
				.addWhereColumnEquals(COL_DELETETIME)
				.addWhereColumnEquals(COL_PARTITIONID)
				.prepare(session);
		insert = 
				CassandraQueryBuilder
					.insert(getTableName())
					.addColumns(getTableColumns())
					.prepare(session);
		deleteByTimeAndPartitionAndRecordingId = 
				CassandraQueryBuilder
					.delete(getTableName())
					.addWhereColumnEquals(COL_DELETETIME)
					.addWhereColumnEquals(COL_PARTITIONID)
					.addWhereColumnEquals(COL_RECORDINGID)
					.prepare(session);
		deleteByTimeAndPartition = 
				CassandraQueryBuilder
					.delete(getTableName())
					.addWhereColumnEquals(COL_DELETETIME)
					.addWhereColumnEquals(COL_PARTITIONID)
					.prepare(session);
	}
	
	public BoundStatement selectPurgeableRows(int partitionId) {
		return select.bind(METADATA_DATE, partitionId);
	}
	
	public BoundStatement selectByDeleteTimeAndPartition(Date purgeTime, int partitionId) {
		return select.bind(purgeTime, partitionId);
	}	
	
	public Stream<PurgeRecord> streamSelectByDeleteTimeAndPartition(Date purgeTime, int partitionId) {
   	Iterator<Row> rows = session.execute(selectByDeleteTimeAndPartition(purgeTime, partitionId)).iterator();
   	Iterator<PurgeRecord> result = Iterators.transform(rows, (row) -> buildEntity(row));
      Spliterator<PurgeRecord> stream = Spliterators.spliteratorUnknownSize(result, Spliterator.IMMUTABLE | Spliterator.NONNULL);
      return StreamSupport.stream(stream, false);
	}	
	
	
	public BoundStatement deletePurgeEntry(Date purgeTime, int partitionId, UUID recordingId) {
		return deleteByTimeAndPartitionAndRecordingId.bind(purgeTime, partitionId, recordingId);
	}
		
	
	public Statement delete(Date purgeTime, int partitionId) throws Exception {
      UUID purgeTimeUuid = getPurgeTimeUUID(purgeTime);
      BatchStatement stmt = new BatchStatement();
      stmt.add(deleteByTimeAndPartition.bind(purgeTime, partitionId));
      stmt.add(deleteByTimeAndPartitionAndRecordingId.bind(METADATA_DATE, partitionId, purgeTimeUuid));

      return stmt;
   }   
	
	public UUID getRecordingId(Row row) {
		return row.getUUID(COL_RECORDINGID);
	}
	
	public UUID getPlaceId(Row row) {
		return row.getUUID(COL_PLACEID);
	}
	
	protected UUID getPurgeTimeUUID(Date purgeTime) {
		return IrisUUID.timeUUID(purgeTime, METADATA_UUID_RANDOM);
	}
	
	public static final class PurgeRecord {
		public final Date purgeTime;
		public final int partition;
		public final UUID placeId;
		public final UUID recordingId;
		public final String storage;
		public final boolean hasStorage;
		public final boolean purgePreview;
		
		public PurgeRecord(Date purgeTime, int partitionId, UUID recordingId, UUID placeId, String storage, boolean hasStorage, boolean purgePreview) {
			this.purgeTime = purgeTime;
			this.partition = partitionId;
			this.placeId = placeId;
			this.recordingId = recordingId;
			this.storage = storage;		
			this.hasStorage = hasStorage;
			this.purgePreview = purgePreview;
		}		
	}
}


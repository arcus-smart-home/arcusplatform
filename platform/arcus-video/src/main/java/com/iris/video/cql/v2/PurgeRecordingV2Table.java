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

import org.apache.commons.lang3.StringUtils;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.video.cql.AbstractPurgeRecordingTable;

@Singleton
public class PurgeRecordingV2Table extends AbstractPurgeRecordingTable {
	/**
	 * CREATE TABLE purge_recordings_v2 (
            deletetime timestamp,
            partitionid int,
            recordingid timeuuid,
            placeid uuid,
            storage text,
            PRIMARY KEY ((deletetime,partitionid),recordingid,placeid)
         ) WITH COMPACT STORAGE
           AND CLUSTERING ORDER BY (recordingid ASC);
	 */
	public static final String TABLE_NAME = "purge_recordings_v2";

	/**
	 * purge_recordings_v2 table needs a field to indicate if preview needs to be purged from the PreviewStorage.  However 
	 * I am not able to add a new field to the table because it is a COMPACT table.  We will be using "storage" field.  Going 
	 * forward, if no preview needs to be purged, the "storage" field will still contain the location of the video file as before.  
	 * Otherwise, it will be "$storage|P".  i.e
	 * "https://devirisvideo1.blob.core.windows.net/devrecording/1d6ba83f-6370-41a1-8ef4-64f1d49fb5ba/29b255d0-0f00-11e8-b952-cd56e3453fc3|P"
	 */
	public static final String COL_STORAGE = "storage";
	public static final String STORAGE_DELIMITER = "|";
	public static final String PURGE_PREVIEW = "P";	
	private static final String PURGE_PREVIEW_SUFFIX=STORAGE_DELIMITER + PURGE_PREVIEW;
	
	private static final String[] COLUMNS = {COL_DELETETIME,COL_PARTITIONID,COL_RECORDINGID,COL_PLACEID,COL_STORAGE};
	
	
//   public static final String PRGREC_INSERT = "INSERT INTO " + PURGE_RECORDINGS_TABLE + "(deletetime,partitionid,recordingid,placeid) VALUES (?,?,?,?)";
//   public static final String PRGREC_LIST = "SELECT * FROM " + PURGE_RECORDINGS_TABLE + " WHERE deletetime=? AND partitionid=?";
//   public static final String PRGREC_DELETE = "DELETE FROM " + PURGE_RECORDINGS_TABLE + " WHERE deletetime=? AND partitionid=?";
//   public static final String PRGREC_TRIM = "DELETE FROM " + PURGE_RECORDINGS_TABLE + " WHERE deletetime=? AND partitionid=? AND recordingid=?";
	
	@Inject
	public PurgeRecordingV2Table(String ts, Session session) {
		super(ts, session);		
	}	
	
	public BoundStatement insertPurgeEntry(Date purgeTime, int partitionId, UUID recordingId, UUID placeId, String storage, boolean purgePreview) {
		if(purgePreview) {
			return insert.bind(purgeTime, partitionId, recordingId, placeId, storage+PURGE_PREVIEW_SUFFIX);
		}else{
			return insert.bind(purgeTime, partitionId, recordingId, placeId, storage);
		}
	}
	
	public BoundStatement insertPurgeAt(Date purgeTime, int partitionId) {
		UUID purgeTimeUuid = getPurgeTimeUUID(purgeTime);
		return insert.bind(METADATA_DATE, partitionId, purgeTimeUuid, purgeTimeUuid, "");
	}


	@Override
	protected String[] getTableColumns() {
		return COLUMNS;
	}

	@Override
	protected PurgeRecord buildEntity(Row row) {
		String storageStr = row.getString(COL_STORAGE);
		boolean purgePreview = false;
		if(StringUtils.isNotBlank(storageStr) && storageStr.endsWith(PURGE_PREVIEW_SUFFIX)) {
			storageStr = storageStr.substring(0, storageStr.length()-2);
			purgePreview = true;
		}
		return new PurgeRecord(row.getDate(COL_DELETETIME), row.getInt(COL_PARTITIONID), row.getUUID(COL_RECORDINGID), row.getUUID(COL_PLACEID), storageStr, true, purgePreview);
	}

	@Override
	public String getTable() {
		return TABLE_NAME;
	}
	
}


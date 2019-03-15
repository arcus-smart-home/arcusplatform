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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.policies.DowngradingConsistencyRetryPolicy;
import com.datastax.driver.core.policies.LoggingRetryPolicy;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.cassandra.CassandraQueryBuilder;
import com.iris.video.cql.MetadataAttribute;
import com.iris.video.cql.VideoConstants;

/**
 * CREATE TABLE recording_metadata_v2_favorite (
            recordingid timeuuid,
            field text,
            value text,
            PRIMARY KEY ((recordingid), field) 
         )
         WITH COMPACT STORAGE
         AND CLUSTERING ORDER BY (field ASC);
 *
 */
@Singleton
public class VideoMetadataV2FavoriteTable extends AbstractVideoMetadataV2Table {
	private static final Logger logger = LoggerFactory.getLogger(VideoMetadataV2FavoriteTable.class);
	
	public static final String TABLE_NAME = "recording_metadata_v2_favorite";

	public static final String COL_EXPIRATION = "expiration";
	private static final String[] COLUMNS = {COL_RECORDINGID, COL_FIELD, COL_VALUE};		

	protected final PreparedStatement deleteField;
	protected final PreparedStatement insertField;
	
	@Inject
	public VideoMetadataV2FavoriteTable(String ts, Session session) {
		super(ts, session);
		this.deleteField =
				CassandraQueryBuilder
					.delete(getTableName())
					.addWhereColumnEquals(COL_RECORDINGID)
					.addWhereColumnEquals(COL_FIELD)
					.prepare(session);
		this.insertField =
				CassandraQueryBuilder
					.insert(getTableName())
					.addColumns(COLUMNS)
					.withRetryPolicy(new LoggingRetryPolicy(DowngradingConsistencyRetryPolicy.INSTANCE))
					.prepare(session);
	}
	
	@Override
	protected String[] getTableColumns() {
		return COLUMNS;
	}
	

	public BoundStatement deleteField(UUID recordingId, MetadataAttribute attribute) {
		return deleteField.bind(recordingId, attribute.name().toLowerCase());
	}

	public Statement insertTag(UUID recordingId, String tag) {
		return insertField(recordingId, VideoConstants.ATTR_TAG_PREFIX + tag, "null");
	}
	
	
	public Statement insertField(UUID recordingId, MetadataAttribute attribute, String value) {
		return insertField.bind(recordingId, attribute.name().toLowerCase(), value);
		
	}
	
	public Statement insertField(UUID recordingId, String attributeName, String value) {
		return insertField.bind(recordingId, attributeName, value);
		
	}

	public BoundStatement deleteTag(UUID recordingId, String tag) {
		return deleteField.bind(recordingId, VideoConstants.ATTR_TAG_PREFIX + tag);
	}

	@Override
	public String getTable() {
		return TABLE_NAME;
	}

}


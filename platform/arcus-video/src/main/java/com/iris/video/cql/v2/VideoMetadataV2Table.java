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
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.cassandra.CassandraQueryBuilder;
import com.iris.video.cql.MetadataAttribute;
import com.iris.video.cql.VideoConstants;

/**
 * CREATE TABLE recording_metadata_v2 (
            recordingid timeuuid,
            expiration timestamp,
            field text,
            value text,
            PRIMARY KEY ((recordingid), expiration, field) 
         )
 *
 */
@Singleton
public class VideoMetadataV2Table extends AbstractVideoMetadataV2Table {
	private static final Logger logger = LoggerFactory.getLogger(VideoMetadataV2Table.class);
	
	public static final String TABLE_NAME = "recording_metadata_v2";

	public static final String COL_EXPIRATION = "expiration";
	private static final String[] COLUMNS = {COL_RECORDINGID, COL_EXPIRATION, COL_FIELD, COL_VALUE};		

	protected final PreparedStatement deleteField;
	
	@Inject
	public VideoMetadataV2Table(String ts, Session session) {
		super(ts, session);
		this.deleteField =
				CassandraQueryBuilder
					.delete(getTableName())
					.addWhereColumnEquals(COL_RECORDINGID)
					.addWhereColumnEquals(COL_EXPIRATION)
					.addWhereColumnEquals(COL_FIELD)
					.prepare(session);
	}
	
	@Override
	protected String[] getTableColumns() {
		return COLUMNS;
	}

	public BoundStatement deleteField(UUID recordingId, long expiration, MetadataAttribute attribute) {
		return deleteField.bind(recordingId, expiration, attribute.name().toLowerCase());
	}

	public Statement insertTag(UUID recordingId, long expiration, long actualTtlInSeconds, String tag) {
		return doInsertField(recordingId, expiration, actualTtlInSeconds, VideoConstants.ATTR_TAG_PREFIX + tag, "null");
	}
	
	
	public Statement insertField(UUID recordingId, long expiration, long actualTtlInSeconds, MetadataAttribute attribute, String value) {
		Statement insert = QueryBuilder.insertInto(getTableSpace(), TABLE_NAME).using(QueryBuilder.ttl((int)actualTtlInSeconds))
				.values(COLUMNS, new Object[]{recordingId, expiration, attribute.name().toLowerCase(), value})
				.setRetryPolicy(new LoggingRetryPolicy(DowngradingConsistencyRetryPolicy.INSTANCE))			
				;
		return insert;		
	}
	

	private Statement doInsertField(UUID recordingId, long expiration, long actualTtlInSeconds, String attributeName, String value) {
		Statement insert = QueryBuilder.insertInto(getTableSpace(), TABLE_NAME).using(QueryBuilder.ttl((int)actualTtlInSeconds))
				.values(COLUMNS, new Object[]{recordingId, expiration, attributeName, value})
				.setRetryPolicy(new LoggingRetryPolicy(DowngradingConsistencyRetryPolicy.INSTANCE))			
				;
		return insert;		
	}

	public BoundStatement deleteTag(UUID recordingId, String tag) {
		return deleteField.bind(recordingId, VideoConstants.ATTR_TAG_PREFIX + tag);
	}

	@Override
	public String getTable() {
		return TABLE_NAME;
	}

}


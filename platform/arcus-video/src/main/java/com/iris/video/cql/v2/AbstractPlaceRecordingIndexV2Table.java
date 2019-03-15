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
import java.util.function.Function;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.google.inject.Inject;
import com.iris.core.dao.cassandra.CassandraQueryBuilder;
import com.iris.video.VideoRecordingSize;
import com.iris.video.cql.VideoTable;


public abstract class AbstractPlaceRecordingIndexV2Table extends VideoTable{
			
	public static final String COL_PLACEID = "placeid";
	public static final String COL_FIELD = "field";
	public static final String COL_VALUE = "value";
	public static final String COL_RECORDINGID = "recordingid";
	public static final String COL_SIZE = "size";	
	public static final String COL_SUM_SIZE = "totalsize";
	public static final String COL_COUNT = "count";
	
	public enum Field {
		CAMERA("cam"),
		DELETED("del"),
		TAG("tag"),
		TYPE("type");
		
		final String id;
		Field(String id) {
			this.id = id;
		}
	}
	
	public enum Type {
		STREAM("S"),
		RECORDING("R");

		final String id;
		Type(String id) {
			this.id = id;
		}
	}
	
	protected final PreparedStatement selectRecordingId;
	protected final PreparedStatement selectRecordingIdAndSizeAsc;
	protected final PreparedStatement selectRecordingSizeSum;
	protected final PreparedStatement deleteIndex;
	protected final PreparedStatement countByFieldValue;
	
	@Inject
	public AbstractPlaceRecordingIndexV2Table(String tableSpace, Session session) {
		super(tableSpace, session);
		this.selectRecordingId =
				CassandraQueryBuilder
					.select(getTableName())
					.addColumn(COL_RECORDINGID)
					.where(String.format(
							"%s = ? AND %s = ? AND %s = ? AND %s <= ? AND %s > ?",
							COL_PLACEID,
							COL_FIELD,
							COL_VALUE,
							COL_RECORDINGID,
							COL_RECORDINGID
					))
					.prepare(session);
		this.selectRecordingIdAndSizeAsc =
				CassandraQueryBuilder
					.select(getTableName())
					.addColumns(COL_RECORDINGID, COL_SIZE)
					.where(String.format(
							"%s = ? AND %s = ? AND %s = ? ORDER BY %s ASC",
							COL_PLACEID,
							COL_FIELD,
							COL_VALUE,
							COL_VALUE
					))
					.prepare(session);
		this.selectRecordingSizeSum =
				CassandraQueryBuilder
					.select(getTableName())
					.addColumn(String.format("SUM(%s) as %s", COL_SIZE, COL_SUM_SIZE))
					.addWhereColumnEquals(COL_PLACEID)
					.addWhereColumnEquals(COL_FIELD)
					.addWhereColumnEquals(COL_VALUE)
					.prepare(session);
		
		this.deleteIndex =
				CassandraQueryBuilder
					.delete(getTableName())
					.addWhereColumnEquals(COL_PLACEID)
					.addWhereColumnEquals(COL_FIELD)
					.addWhereColumnEquals(COL_VALUE)
					.addWhereColumnEquals(COL_RECORDINGID)
					.prepare(session);
		
		this.countByFieldValue =
				CassandraQueryBuilder
					.select(getTableName())
					.addColumn(String.format("COUNT(%s) as %s", COL_PLACEID, COL_COUNT))
					.where(String.format(
							"%s = ? AND %s = ? AND %s = ? ",
							COL_PLACEID,
							COL_FIELD,
							COL_VALUE
					))
					.prepare(session);
	}
	
	protected abstract String[] getTableColumns() ;
	protected abstract boolean isFavoriteTable();

	public BoundStatement selectIdsByField(UUID placeId, Field field, String value, UUID newest, UUID oldest, int sizeHint) {
		BoundStatement bs = selectRecordingId.bind(placeId, field.id, value, newest, oldest);
		bs.setFetchSize(sizeHint);
		return bs;
	}
	
	public BoundStatement selectIdsByCamera(UUID placeId, String cameraId, UUID newest, UUID oldest, int sizeHint) {
		return selectIdsByField(placeId, Field.CAMERA, cameraId, newest, oldest, sizeHint);
	}

	public BoundStatement selectIdsByTag(UUID placeId, String tag, UUID newest, UUID oldest, int sizeHint) {
		return selectIdsByField(placeId, Field.TAG, tag, newest, oldest, sizeHint);
	}

	public BoundStatement selectIdsByType(UUID placeId, Type type, UUID newest, UUID oldest, int sizeHint) {
		return selectIdsByField(placeId, Field.TYPE, type.id, newest, oldest, sizeHint);
	}

	public BoundStatement selectIdsByDeleted(UUID placeId, UUID newest, UUID oldest, int sizeHint) {
		return selectIdsByField(placeId, Field.DELETED, "", newest, oldest, sizeHint);
	}

	public BoundStatement selectIdsByFieldAsc(UUID placeId, Field field, String value) {
		return selectRecordingIdAndSizeAsc.bind(placeId, field.id, value);
	}
	
	public BoundStatement selectIdsByTagAsc(UUID placeId, String tag) {
		return selectIdsByFieldAsc(placeId, Field.TAG, tag);
	}

	public BoundStatement selectRecordingSizeAsc(UUID placeId, Type type) {
		return selectIdsByFieldAsc(placeId, Field.TYPE, type.id);
	}

	public BoundStatement selectRecordingSizeSum(UUID placeId) {
		return selectRecordingSizeSum.bind(placeId, Field.TYPE.id, Type.RECORDING.id);
	}
		
	
	public BoundStatement deleteDeleted(UUID placeId, UUID recordingId) {
		return deleteIndex.bind(placeId, Field.DELETED.id, "", recordingId);
	}
	
	public BoundStatement deleteCamera(UUID placeId, UUID recordingId, UUID cameraId) {
		return deleteIndex.bind(placeId, Field.CAMERA.id, cameraId.toString(), recordingId);
	}

	
	public BoundStatement deleteTag(UUID placeId, UUID recordingId, String tag) {
		return deleteIndex.bind(placeId, Field.TAG.id, tag, recordingId);
	}

	public BoundStatement deleteVideo(UUID placeId, UUID recordingId, Type type) {
		return deleteIndex.bind(placeId, Field.TYPE.id, type.id, recordingId);
	}
	
	public UUID getRecordingId(Row row) {
		return row.getUUID(COL_RECORDINGID);
	}
	
	
	public VideoRecordingSize getRecordingIdAndSizeAndFavorite(Row row) {
		return new VideoRecordingSize(getRecordingId(row), row.getLong(COL_SIZE), isFavoriteTable());
	}
	
	public VideoRecordingSize getRecordingIdAndFavorite(Row row) {
		return new VideoRecordingSize(getRecordingId(row), VideoRecordingSize.SIZE_UNKNOWN, isFavoriteTable());
	}
	
	
	public VideoRecordingSize materializeVideoRecordingSize(Row row) {
		return new VideoRecordingSize(row.getUUID(COL_RECORDINGID), row.getLong(COL_SIZE), isFavoriteTable());
	}
	
	

	public boolean isCompleteRecording(Row row) {
		return !row.isNull(COL_SIZE);
	}
	
	public BoundStatement countByField(UUID placeId, Field field, String value) {
		return countByFieldValue.bind(placeId, field.id, value);
	}
	
	public static class GetRecordingIdFunction implements Function<Row, UUID> {
		@Override
		public UUID apply(Row r) {
			return r.getUUID(COL_RECORDINGID);
		}
		
	}

}


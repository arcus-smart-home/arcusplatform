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
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.core.cql.Statement;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import static com.datastax.oss.driver.api.querybuilder.QueryBuilder.*;

/**
 * CREATE TABLE place_recording_index_v2_favorite (
            placeid uuid,
            field text,
            value text,
            recordingid timeuuid,
            size bigint,
            PRIMARY KEY ((placeid, field), value, recordingid)
         )
         WITH CLUSTERING ORDER BY (value DESC, recordingid DESC);
 *
 */
@Singleton
public class PlaceRecordingIndexV2FavoriteTable extends AbstractPlaceRecordingIndexV2Table {
	public static final String TABLE_NAME = "place_recording_index_v2_favorite";

	private static final String[] COLUMNS = {COL_PLACEID, COL_FIELD, COL_VALUE, COL_RECORDINGID, COL_SIZE};

	@Inject
	public PlaceRecordingIndexV2FavoriteTable(String ts, CqlSession session) {
		super(ts, session);

	}



	private Statement<?> doInsert(UUID placeId, UUID recordingId, String fieldName, String value, Long size) {
		SimpleStatement insert = insertInto(getTableSpace(), TABLE_NAME)
				.value(COLUMNS[0], literal(placeId))
				.value(COLUMNS[1], literal(fieldName))
				.value(COLUMNS[2], literal(value))
				.value(COLUMNS[3], literal(recordingId))
				.value(COLUMNS[4], literal(size))
				.build();
		return insert;
	}

	public Statement<?> insertDeleted(UUID placeId, UUID recordingId) {
		return doInsert(placeId, recordingId, Field.DELETED.id, "", null);

	}

	public Statement<?> insertCamera(UUID placeId, UUID recordingId, String cameraId) {
		return doInsert(placeId, recordingId, Field.CAMERA.id, cameraId, null);
	}

	public Statement<?> insertTag(UUID placeId, UUID recordingId, String tag) {
		return doInsert(placeId, recordingId, Field.TAG.id, tag, null);
	}


	public Statement<?> insertVideo(UUID placeId, UUID recordingId, Type type) {
		return doInsert(placeId, recordingId, Field.TYPE.id, type.id, null);
	}

	public Statement<?> insertRecording(UUID placeId, UUID recordingId, long size) {
		return doInsert(placeId, recordingId, Field.TYPE.id, Type.RECORDING.id, size);

	}



	@Override
	protected String[] getTableColumns() {
		return COLUMNS;
	}



	@Override
	protected boolean isFavoriteTable() {
		return true;
	}

	@Override
	public String getTable() {
		return TABLE_NAME;
	}

}

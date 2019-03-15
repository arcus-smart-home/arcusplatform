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
package com.iris.video.purge.dao;

import java.util.Date;
import java.util.UUID;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.Nullable;

import com.datastax.driver.core.ResultSet;
import com.iris.video.VideoMetadata;
import com.iris.video.cql.AbstractPurgeRecordingTable.PurgeRecord;

public interface VideoPurgeDao {

	@Nullable
	VideoMetadata getMetadata(UUID placeId, UUID recordingId);

	@Nullable
	String getStorageLocation(UUID recordingId);

	ResultSet listPurgeableRows(int partitionId) throws Exception;
	

	ResultSet deletePurgeableRow(Date time, int partitionId) throws Exception;

	Stream<PurgeRecord> listPurgeableRecordings(Date time, int partitionId) throws Exception;

	void purge(VideoMetadata metadata) throws Exception;

	void purge(UUID placeId, UUID recordingId) throws Exception;

}


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
package com.iris.video.purge.dao.v2;

import static com.iris.video.purge.VideoPurgeTaskMetrics.DELETE_METADATA_FAIL;
import static com.iris.video.purge.VideoPurgeTaskMetrics.DELETE_METADATA_SUCCESS;
import static com.iris.video.purge.VideoPurgeTaskMetrics.DELETE_RECORDING_NEW_FAIL;
import static com.iris.video.purge.VideoPurgeTaskMetrics.DELETE_RECORDING_NEW_SUCCESS;
import static com.iris.video.purge.VideoPurgeTaskMetrics.DELETE_RECORDING_OLD_FAIL;
import static com.iris.video.purge.VideoPurgeTaskMetrics.DELETE_RECORDING_OLD_SUCCESS;
import static com.iris.video.purge.VideoPurgeTaskMetrics.LIST_METADATA_FAIL;
import static com.iris.video.purge.VideoPurgeTaskMetrics.LIST_METADATA_SUCCESS;
import static com.iris.video.purge.VideoPurgeTaskMetrics.LIST_PURGED_FAIL;
import static com.iris.video.purge.VideoPurgeTaskMetrics.LIST_PURGED_SUCCESS;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.Nullable;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.video.VideoDao;
import com.iris.video.VideoMetadata;
import com.iris.video.cql.AbstractPurgeRecordingTable.PurgeRecord;
import com.iris.video.cql.Table;
import com.iris.video.cql.v2.PurgeRecordingV2Table;
import com.iris.video.purge.VideoPurgeTaskConfig;
import com.iris.video.purge.dao.VideoPurgeDao;

@Singleton
public class CassandraVideoPurgeV2Dao implements VideoPurgeDao {
   private final Session session;


   private final PurgeRecordingV2Table purgeTable;
   private final VideoDao videoDao;
   private final VideoPurgeTaskConfig config;
   
   @Inject
   public CassandraVideoPurgeV2Dao(VideoPurgeTaskConfig config, Session session, VideoDao videoDao) {
      this.session = session;
      this.config = config;
      this.purgeTable = Table.get(session, config.getTableSpace(), PurgeRecordingV2Table.class);
      this.videoDao = videoDao;
   }
   
   /* (non-Javadoc)
	 * @see com.iris.video.purge.dao.VideoPurgeDao#getMetadata(java.util.UUID, java.util.UUID)
	 */
   @Override
	@Nullable
   public VideoMetadata getMetadata(UUID placeId, UUID recordingId) {
   	throw new IllegalAccessError("getMetadata() is not supported for CassandraVideoPurgeV2Dao");
   }

   /* (non-Javadoc)
	 * @see com.iris.video.purge.dao.VideoPurgeDao#getStorageLocation(java.util.UUID)
	 */
   @Override
	@Nullable
   public String getStorageLocation(UUID recordingId) {
      throw new IllegalAccessError("getStorageLocation() is not supported for CassandraVideoPurgeV2Dao");
   }

   /* (non-Javadoc)
	 * @see com.iris.video.purge.dao.VideoPurgeDao#listPurgeableRows(int)
	 */
   @Override
	public ResultSet listPurgeableRows(int partitionId) throws Exception {
      long startTime = System.nanoTime();

      try {
      	BoundStatement stmt = purgeTable.selectPurgeableRows(partitionId);
         ResultSet result = session.execute(stmt);
         LIST_METADATA_SUCCESS.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
         return result;
      } catch (Exception ex) {
         LIST_METADATA_FAIL.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
         throw ex;
      }
   }

   /* (non-Javadoc)
	 * @see com.iris.video.purge.dao.VideoPurgeDao#deletePurgeableRow(java.util.Date, int)
	 */
   @Override
	public ResultSet deletePurgeableRow(Date time, int partitionId) throws Exception {
      long startTime = System.nanoTime();

      try {
         ResultSet result = session.execute(purgeTable.delete(time, partitionId));
         DELETE_METADATA_SUCCESS.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
         return result;
      } catch (Exception ex) {
         DELETE_METADATA_FAIL.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
         throw ex;
      }
   }

   /* (non-Javadoc)
	 * @see com.iris.video.purge.dao.VideoPurgeDao#listPurgeableRecordings(java.util.Date, int)
	 */
   @Override
	public Stream<PurgeRecord> listPurgeableRecordings(Date time, int partitionId) throws Exception {
      long startTime = System.nanoTime();
      boolean success = false;
      try {
         return purgeTable.streamSelectByDeleteTimeAndPartition(time, partitionId);
      } catch (Exception ex) {         
         throw ex;
      } finally{
      	if(success) {
      		LIST_PURGED_SUCCESS.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
      	}else{
      		LIST_PURGED_FAIL.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
      	}
      }
   }

   /* (non-Javadoc)
	 * @see com.iris.video.purge.dao.VideoPurgeDao#purge(com.iris.video.VideoMetadata)
	 */
   @Override
	public void purge(VideoMetadata metadata) throws Exception {
      long startTime = System.nanoTime();
      try {
         videoDao.purge(metadata);
         DELETE_RECORDING_NEW_SUCCESS.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
      } catch (Exception ex) {
         DELETE_RECORDING_NEW_FAIL.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
         throw ex;
      }
   }

   /* (non-Javadoc)
	 * @see com.iris.video.purge.dao.VideoPurgeDao#purge(java.util.UUID, java.util.UUID)
	 */
   @Override
	public void purge(UUID placeId, UUID recordingId) throws Exception {
      long startTime = System.nanoTime();
      try {
         videoDao.purge(placeId, recordingId);
         DELETE_RECORDING_OLD_SUCCESS.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
      } catch (Exception ex) {
         DELETE_RECORDING_OLD_FAIL.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
         throw ex;
      }
   }

}


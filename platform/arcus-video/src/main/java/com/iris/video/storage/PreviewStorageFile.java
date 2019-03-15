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
package com.iris.video.storage;

import static com.iris.video.VideoMetrics.VIDEO_PREVIEW_FILE_READ_FAIL;
import static com.iris.video.VideoMetrics.VIDEO_PREVIEW_FILE_READ_SUCCESS;
import static com.iris.video.VideoMetrics.VIDEO_PREVIEW_FILE_WRITE_FAIL;
import static com.iris.video.VideoMetrics.VIDEO_PREVIEW_FILE_WRITE_SUCCESS;
import static com.iris.video.VideoMetrics.VIDEO_PREVIEW_FILE_DELETE_SUCCESS;
import static com.iris.video.VideoMetrics.VIDEO_PREVIEW_FILE_DELETE_FAIL;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;

public class PreviewStorageFile implements PreviewStorage {
   private final File baseDir;

   public PreviewStorageFile(File baseDir) {
      this.baseDir = baseDir;
   }

   @Override
   public void write(String id, byte[] image) throws IOException {
      write(id, image, 0, TimeUnit.MILLISECONDS);
   }

   @Override
   public void write(String id, byte[] image, long ttl, TimeUnit unit) throws IOException {
      long startTime = System.nanoTime();

      try {
         File snapshot = new File(baseDir, id);
         try(FileOutputStream fos = new FileOutputStream(snapshot)) {
            fos.write(image);
            fos.flush();
         }

         VIDEO_PREVIEW_FILE_WRITE_SUCCESS.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
      } catch (Exception ex) {
         VIDEO_PREVIEW_FILE_WRITE_FAIL.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
         throw ex;
      }
   }

   @Override
   public byte[] read(String id) throws IOException {
      long startTime = System.nanoTime();

      try {
         File snapshot = new File(baseDir, id);
         if(!snapshot.exists()) {
            return null;
         }

         byte[] results = FileUtils.readFileToByteArray(snapshot);

         VIDEO_PREVIEW_FILE_READ_SUCCESS.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
         return results;
      } catch (Exception ex) {
         VIDEO_PREVIEW_FILE_READ_FAIL.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
         throw ex;
      }
   }

	@Override
	public void delete(String id) throws Exception {
		long startTime = System.nanoTime();

      try {
         File snapshot = new File(baseDir, id);
         if(snapshot.exists()) {
         	snapshot.deleteOnExit();
            VIDEO_PREVIEW_FILE_DELETE_SUCCESS.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
         }
      } catch (Exception ex) {
         VIDEO_PREVIEW_FILE_DELETE_FAIL.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
         throw ex;
      }
		
	}

}


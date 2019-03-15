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

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

public class PreviewStorageNull implements PreviewStorage {
   public PreviewStorageNull() {
   }

   @PreDestroy
   public void destroy() {
   }

   @Override
   public void write(String id, byte[] image) throws IOException {
   }

   @Override
   public void write(String id, byte[] image, long ttl, TimeUnit unit) throws IOException {
   }

   @Override
   public byte[] read(String id) throws IOException {
      return null;
   }

	@Override
	public void delete(String id) throws Exception {		
	}
}


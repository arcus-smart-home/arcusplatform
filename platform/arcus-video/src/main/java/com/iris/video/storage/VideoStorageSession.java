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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;

import com.iris.messages.address.Address;
import com.iris.video.VideoUtil;

public interface VideoStorageSession {
   UUID getRecordingId();
   UUID getCameraId();
   UUID getAccountId();
   UUID getPlaceId();
   long getRecordingTtlInSeconds();

   @Nullable
   UUID getPersonId();
   
   @Nullable
   default Address getActor() { return VideoUtil.getActorFromPersonId(getPlaceId(), getPersonId()); }

   String location() throws Exception;
   OutputStream output() throws Exception;
   InputStream input() throws Exception;
   void read(byte[] buf, long offset, long bytes, int bufferOffset) throws Exception;
}


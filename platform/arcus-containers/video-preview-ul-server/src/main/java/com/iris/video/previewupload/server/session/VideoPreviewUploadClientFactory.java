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
package com.iris.video.previewupload.server.session;

import com.google.inject.Singleton;
import com.iris.bridge.server.client.Client;
import com.iris.bridge.server.client.ClientFactory;

@Singleton
public class VideoPreviewUploadClientFactory implements ClientFactory {
   @Override
   public Client create() {
      return null;
   }

   @Override
   public Client load(String principal) {
      return null;
   }
}


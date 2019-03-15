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
package com.iris.common.subsystem.cameras;

import java.util.UUID;

import com.iris.messages.capability.CameraCapability;
import com.iris.messages.capability.RecordingCapability;
import com.iris.messages.model.test.ModelFixtures;
import com.iris.util.IrisUUID;
import com.iris.util.IrisCollections.MapBuilder;

public class CamerasFixtures extends ModelFixtures {

   public static DeviceBuilder buildCamera() {
      return ModelFixtures
            .buildDeviceAttributes(CameraCapability.NAMESPACE);
   }

   public static MapBuilder<String, Object> buildStream() {
      UUID recordingId = IrisUUID.timeUUID();
      return
         ModelFixtures
            .buildServiceAttributes(recordingId, RecordingCapability.NAMESPACE)
            .put(RecordingCapability.ATTR_ACCOUNTID, UUID.randomUUID())
            .put(RecordingCapability.ATTR_PLACEID, UUID.randomUUID())
            .put(RecordingCapability.ATTR_CAMERAID, UUID.randomUUID())
            .put(RecordingCapability.ATTR_FRAMERATE, 5.0)
            .put(RecordingCapability.ATTR_BANDWIDTH, 128000)
            .put(RecordingCapability.ATTR_HEIGHT, 480)
            .put(RecordingCapability.ATTR_WIDTH, 640)
            .put(RecordingCapability.ATTR_PRECAPTURE, 8.0)
            .put(RecordingCapability.ATTR_TIMESTAMP, IrisUUID.timeof(recordingId))
            .put(RecordingCapability.ATTR_TYPE, RecordingCapability.TYPE_STREAM);
   }

   public static MapBuilder<String, Object> buildRecording() {
      return buildStream().put(RecordingCapability.ATTR_TYPE, RecordingCapability.TYPE_RECORDING);
   }

}


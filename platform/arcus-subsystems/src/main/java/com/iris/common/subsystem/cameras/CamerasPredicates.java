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

import com.google.common.base.Predicate;
import com.iris.messages.capability.CameraCapability;
import com.iris.messages.capability.RecordingCapability;
import com.iris.messages.model.Model;
import com.iris.model.predicate.Predicates;

class CamerasPredicates {

   private CamerasPredicates() {
   }

   static final Predicate<Model> IS_CAMERA = Predicates.isA(CameraCapability.NAMESPACE);
   static final Predicate<Model> IS_RECORDING = Predicates.isA(RecordingCapability.NAMESPACE);

   static final Predicate<Model> IS_OFFLINE = Predicates.isDeviceOffline();
}


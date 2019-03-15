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

import static com.iris.messages.errors.Errors.invalidRequest;
import static java.lang.String.format;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.SubsystemUtils;
import com.iris.messages.capability.SubsystemCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.model.Model;
import com.iris.messages.model.ServiceLevel;
import com.iris.messages.model.subs.CamerasSubsystemModel;

class CamerasContextAdapter {

   private static final int MAX_SIMULTANEOUS_STREAMS_DEFAULT = 2;
   private static final int MAX_SIMULTANEOUS_STREAMS_MIN = 1;
   private static final int MAX_SIMULTANEOUS_STREAMS_MAX = 6;

   private final SubsystemContext<CamerasSubsystemModel> context;

   CamerasContextAdapter(SubsystemContext<CamerasSubsystemModel> context) {
      this.context = context;
   }

   Logger logger() {
      return context.logger();
   }

   void clear() {
      context.model().setAvailable(Boolean.FALSE);
      context.model().setRecordingEnabled(Boolean.TRUE);
      context.model().setCameras(ImmutableSet.<String>of());
      context.model().setOfflineCameras(ImmutableSet.<String>of());
      context.model().setState(SubsystemCapability.STATE_ACTIVE);
      context.model().setWarnings(ImmutableMap.<String,String>of());
   }

   Iterable<Model> getCameras() {
      return Iterables.filter(context.models().getModels(), CamerasPredicates.IS_CAMERA);
   }

   void updateAvailable() {
      context.model().setAvailable(context.model().getCameras().size() > 0);
   }   

   void initMaxSimultaneousStreams()
   {
      if (context.model().getMaxSimultaneousStreams() == null)
      {
         context.model().setMaxSimultaneousStreams(MAX_SIMULTANEOUS_STREAMS_DEFAULT);
      }
   }

   void validateMaxSimultaneousStreams(int value)
   {
      if (value < MAX_SIMULTANEOUS_STREAMS_MIN || value > MAX_SIMULTANEOUS_STREAMS_MAX)
      {
         String message = format("maxSimultaneousStreams [%d] is not between [%d] and [%d] inclusive",
            value, MAX_SIMULTANEOUS_STREAMS_MIN, MAX_SIMULTANEOUS_STREAMS_MAX);

         throw new ErrorEventException(invalidRequest(message));
      }
   }

   void updateWarnings() {
      Map<String, String> warnings = new HashMap<>();
      for(Model camera: getCameras()) {
         String warning = getWarning(camera);
         if(warning != null) {
            warnings.put(camera.getAddress().getRepresentation(), warning);
         }
      }
      context.model().setWarnings(Collections.unmodifiableMap(warnings));
   }
   
   void updateWarning(Model m, String warning) {
      String address = m.getAddress().getRepresentation();
      Map<String,String> warnings = new HashMap<>(context.model().getWarnings());
      String curWarning = warnings.get(address);
      if(!StringUtils.equals(warning, curWarning)) {
         if(warning == null) {
            warnings.remove(address);
         } else {
            warnings.put(address, warning);
         }

         context.model().setWarnings(Collections.unmodifiableMap(warnings));
      }
   }

   void removeWarning(Model m) {
      Map<String,String> warnings = new HashMap<>(context.model().getWarnings());
      if(warnings.remove(m.getAddress().getRepresentation()) != null) {
         context.model().setWarnings(Collections.unmodifiableMap(warnings));
      }
   }

   String getWarning(Model m) {
      if(CamerasPredicates.IS_OFFLINE.apply(m)) {
         return CamerasSubsystem.WARN_OFFLINE;
      }
      return null;
   }

}


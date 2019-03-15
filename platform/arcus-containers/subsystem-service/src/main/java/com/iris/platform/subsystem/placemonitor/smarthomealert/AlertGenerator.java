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
package com.iris.platform.subsystem.placemonitor.smarthomealert;

import org.eclipse.jdt.annotation.Nullable;

import com.iris.common.subsystem.SubsystemContext;
import com.iris.messages.PlatformMessage;
import com.iris.messages.model.Model;
import com.iris.messages.model.subs.PlaceMonitorSubsystemModel;

public abstract class AlertGenerator {

   public void onStarted(SubsystemContext<PlaceMonitorSubsystemModel> context, AlertScratchPad scratch) {
      // no op hook
   }

   public final void onMessageReceived(SubsystemContext<PlaceMonitorSubsystemModel> context, PlatformMessage msg, AlertScratchPad scratch) {
      if(!isInterestedInMessage(msg)) {
         return;
      }
      Model m = modelForMessage(context, msg);
      if(m == null) {
         context.logger().info("{} ignoring interesting message because no model could be found", getClass().getSimpleName());
         return;
      }

      handleMessage(context, scratch, msg, m);
   }

   protected boolean isInterestedInMessage(PlatformMessage msg) {
      return false;
   }

   protected void handleMessage(SubsystemContext<PlaceMonitorSubsystemModel> context, AlertScratchPad scratch, PlatformMessage msg, Model m) {
      // no op hook
   }

   @Nullable
   protected Model modelForMessage(SubsystemContext<PlaceMonitorSubsystemModel> context, PlatformMessage msg) {
      return null;
   }

   public final void onModelChanged(SubsystemContext<PlaceMonitorSubsystemModel> context, String attribute, Model model, AlertScratchPad scratch) {
      if(!isInterestedInAttributeChange(attribute)) {
         return;
      }
      handleModelChanged(context, model, scratch);
   }

   protected boolean isInterestedInAttributeChange(String attribute) {
      return false;
   }

   protected void handleModelChanged(SubsystemContext<PlaceMonitorSubsystemModel> context, Model model, AlertScratchPad scratchPad) {
   }
}


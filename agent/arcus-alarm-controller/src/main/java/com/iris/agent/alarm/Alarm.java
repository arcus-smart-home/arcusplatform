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
package com.iris.agent.alarm;

import java.util.Map;

import com.iris.messages.address.Address;

public interface Alarm {
   boolean canHandleEvent(AlarmEvents.Event event);
   void handleEvent(AlarmEvents.Event event);

   void clearTriggers();

   void markNeedsReporting();
   void clearNeedsReporting();
   boolean isReportingNeeded();

   String getAlertState();
   boolean isSilent();
   void updateReportAttributes(Map<String,Object> attrs);

   void reloaded(Map<String,String> state);
   String getName();

   void afterProcessReflexDevices();
   void onDeviceOnline(Address device);
   void onDeviceOffline(Address device);

   void onVerified();

   int getPriority();
}


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
package com.iris.common.subsystem.care.behavior.evaluators;

import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.care.behavior.CareBehaviorTypeWrapper;
import com.iris.common.subsystem.care.behavior.WeeklyTimeWindow;
import com.iris.messages.event.ModelChangedEvent;
import com.iris.messages.event.ModelRemovedEvent;
import com.iris.messages.event.ScheduledEvent;
import com.iris.messages.model.subs.CareSubsystemModel;

public interface BehaviorEvaluator {

   List<WeeklyTimeWindow> getWeeklyTimeWindows();
   WeeklyTimeWindow getNextTimeWindow(Date asOfDate,TimeZone tz); //If currently in a time window it will return that
   CareBehaviorTypeWrapper getCareBehavior();
   String getBehaviorId();

   void onModelChange(ModelChangedEvent event,SubsystemContext<CareSubsystemModel>context);
   void onModelRemove(ModelRemovedEvent event,SubsystemContext<CareSubsystemModel>context);
   void onWindowStart(WeeklyTimeWindow window,SubsystemContext<CareSubsystemModel>context);
   void onWindowEnd(WeeklyTimeWindow window,SubsystemContext<CareSubsystemModel>context);
   void onAlarmCleared(SubsystemContext<CareSubsystemModel>context);
   void onTimeout(ScheduledEvent event,SubsystemContext<CareSubsystemModel>context);
   void onStart(SubsystemContext<CareSubsystemModel>context);
   void onRemoved(SubsystemContext<CareSubsystemModel>context);
   void onAlarmModeChange(SubsystemContext<CareSubsystemModel>context);
   void validateConfig(SubsystemContext<CareSubsystemModel>context);

}


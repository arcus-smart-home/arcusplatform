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
package com.iris.common.subsystem.alarm.co;

import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.alarm.generic.AlertState;
import com.iris.messages.model.subs.SubsystemModel;

public class COAlertState extends AlertState
{
   private static final COAlertState INSTANCE = new COAlertState();

   public static COAlertState instance() {
      return INSTANCE;
   }

   private COAlertState() {
   }


   @Override
   public String onEnter(SubsystemContext<? extends SubsystemModel> context, String name)
   {          
      return super.onEnter(context, name);
   }   
}


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
package com.iris.common.subsystem.care.behavior;

import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.time.TimeOfDay;
import com.iris.messages.model.Model;
import com.iris.messages.model.subs.CareSubsystemModel;
import com.iris.util.TypeMarker;

public class BehaviorUtil {

   public static boolean removeStringFromSet(String value,String attribute,Model model){
      Set<String>stringSet=model.getAttribute(TypeMarker.setOf(String.class), attribute).or(ImmutableSet.<String>of());
      Set<String>newSet=new HashSet<>(stringSet);
      boolean removed = newSet.remove(value);
      if(removed){
         model.setAttribute(attribute, newSet);
      }
      return removed;
   }
   public static boolean addStringToSet(String string, String attribute, Model model) {
      Set<String> devices = model.getAttribute(TypeMarker.setOf(String.class), attribute).get();
      if(devices.contains(string)) {
         return false;
      }
      Set<String> newDevices = new HashSet<>(devices);
      newDevices.add(string);
      model.setAttribute(attribute, newDevices);
      return true;
   }

   public static CareBehaviorTypeWrapper getBehaviorFromContext(String id,SubsystemContext<CareSubsystemModel> context){
      Map<String, Object> behaviorInfo = context.getVariable(BehaviorMonitor.BEHAVIOR_KEY.create(id)).as(TypeMarker.mapOf(String.class, Object.class));
      return behaviorInfo == null ? null : new CareBehaviorTypeWrapper(behaviorInfo);
   }
   
   public static List<Map<String, Object>> convertListOfType(List<? extends Object> typeList) {
      List<Map<String, Object>> mapList = new ArrayList<Map<String, Object>>();
      for (Object object : typeList){
         try{
            Method m = object.getClass().getMethod("toMap");
            Map<String, Object> type = (Map<String, Object>) m.invoke(object);
            mapList.add(type);

         }catch (Exception e){
            throw new RuntimeException(e);
         }
      }
      return mapList;
   }
   public static Date nextDailyOccurence(Calendar subsystemCal, String time){
      Preconditions.checkArgument(time!=null, "Expecting time in format 'HH:mm:ss' but was " + time);
      try{
         TimeOfDay tod = TimeOfDay.fromString(time);
         return tod.next(subsystemCal).getTime();
      }
      catch(Exception e){
         throw new IllegalArgumentException("Invalid time format for " + time);
      }
   }
}


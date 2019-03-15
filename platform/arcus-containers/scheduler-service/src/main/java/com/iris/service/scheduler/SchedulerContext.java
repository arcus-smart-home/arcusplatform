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
package com.iris.service.scheduler;

import java.util.Calendar;
import java.util.TimeZone;

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.iris.common.sunrise.GeoLocation;
import com.iris.common.sunrise.SunriseSunsetCalc;
import com.iris.common.sunrise.SunriseSunsetInfo;
import com.iris.messages.model.Model;
import com.iris.platform.model.ModelEntity;

public class SchedulerContext {
   private SunriseSunsetCalc calculator;
   private ModelEntity scheduler;
   private TimeZone tz;
   @Nullable private GeoLocation location; 
   
   public SchedulerContext() {
      // TODO Auto-generated constructor stub
   }

   public SunriseSunsetInfo calculateSunriseSunsetForDay(Calendar day) {
      Preconditions.checkArgument(location != null, "Must specify a location to calculate sunrise / sunset");
      return calculator.calculateSunriseSunset(day, location);
   }
   
   /**
    * @return the calculator
    */
   public SunriseSunsetCalc getCalculator() {
      return calculator;
   }

   /**
    * @param calculator the calculator to set
    */
   public void setCalculator(SunriseSunsetCalc calculator) {
      this.calculator = calculator;
   }

   /**
    * @return the scheduler
    */
   public ModelEntity getScheduler() {
      return scheduler;
   }

   /**
    * @param scheduler the scheduler to set
    */
   public void setScheduler(ModelEntity scheduler) {
      this.scheduler = scheduler;
   }

   /**
    * @return the tz
    */
   public TimeZone getTimeZone() {
      return tz;
   }

   /**
    * @param tz the tz to set
    */
   public void setTimeZone(TimeZone tz) {
      this.tz = tz;
   }

   /**
    * @return the location
    */
   public GeoLocation getLocation() {
      return location;
   }

   /**
    * @param location the location to set
    */
   public void setLocation(GeoLocation location) {
      this.location = location;
   }

}


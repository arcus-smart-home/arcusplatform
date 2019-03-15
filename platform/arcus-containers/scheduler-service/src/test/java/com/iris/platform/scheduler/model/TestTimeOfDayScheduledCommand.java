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
package com.iris.platform.scheduler.model;

import static org.junit.Assert.fail;

import java.util.UUID;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.type.TimeOfDayCommand;
import com.iris.service.scheduler.model.TimeOfDayScheduledCommand;

public class TestTimeOfDayScheduledCommand {

   @Test
   public void testLegacyAbsoluteCommandIsValidOnAdd() {
      TimeOfDayScheduledCommand command = TimeOfDayScheduledCommand.fromMap(ImmutableMap.of(
            TimeOfDayCommand.ATTR_SCHEDULEID, "scheduleId",
            TimeOfDayCommand.ATTR_TIME, "11:00:00",
            TimeOfDayCommand.ATTR_DAYS, ImmutableSet.of("MON", "TUE", "WED"),
            TimeOfDayCommand.ATTR_ATTRIBUTES, ImmutableMap.of()
      ));
      command.validate();
   }

   @Test
   public void testModernAbsoluteCommandIsValidOnAdd() {
      TimeOfDayScheduledCommand command = TimeOfDayScheduledCommand.fromMap(
            ImmutableMap.<String, Object>builder()
               .put(TimeOfDayCommand.ATTR_SCHEDULEID, "scheduleId")
               .put(TimeOfDayCommand.ATTR_MODE, TimeOfDayCommand.MODE_ABSOLUTE)
               .put(TimeOfDayCommand.ATTR_TIME, "11:00:00")
               .put(TimeOfDayCommand.ATTR_OFFSETMINUTES, 0)
               .put(TimeOfDayCommand.ATTR_DAYS, ImmutableSet.of("MON", "TUE", "WED"))
               .put(TimeOfDayCommand.ATTR_ATTRIBUTES, ImmutableMap.of())
               .build()
      );
      command.validate();
   }

   @Test
   public void testLegacyAbsoluteCommandIsValidOnUpdate() {
      TimeOfDayScheduledCommand command = TimeOfDayScheduledCommand.fromMap(ImmutableMap.of(
            TimeOfDayCommand.ATTR_ID, UUID.randomUUID().toString(),
            TimeOfDayCommand.ATTR_SCHEDULEID, "scheduleId",
            TimeOfDayCommand.ATTR_TIME, "11:00:00",
            TimeOfDayCommand.ATTR_DAYS, ImmutableSet.of("MON", "TUE", "WED"),
            TimeOfDayCommand.ATTR_ATTRIBUTES, ImmutableMap.of()
      ));
      command.validate();
   }

   @Test
   public void testModernAbsoluteCommandIsValidOnUpdate() {
      TimeOfDayScheduledCommand command = TimeOfDayScheduledCommand.fromMap(
         ImmutableMap.<String,Object>builder()
            .put(TimeOfDayCommand.ATTR_ID, UUID.randomUUID().toString())
            .put(TimeOfDayCommand.ATTR_SCHEDULEID, "scheduleId")
            .put(TimeOfDayCommand.ATTR_MODE, TimeOfDayCommand.MODE_ABSOLUTE)
            .put(TimeOfDayCommand.ATTR_TIME, "11:00:00")
            .put(TimeOfDayCommand.ATTR_OFFSETMINUTES, 0)
            .put(TimeOfDayCommand.ATTR_DAYS, ImmutableSet.of("MON", "TUE", "WED"))
            .put(TimeOfDayCommand.ATTR_ATTRIBUTES, ImmutableMap.of())
            .build()
      );
      command.validate();
   }

   @Test
   public void testTimeAndOffsetSpecified() {
      TimeOfDayScheduledCommand command = TimeOfDayScheduledCommand.fromMap(
            ImmutableMap.<String,Object>builder()
               .put(TimeOfDayCommand.ATTR_ID, UUID.randomUUID().toString())
               .put(TimeOfDayCommand.ATTR_SCHEDULEID, "scheduleId")
               .put(TimeOfDayCommand.ATTR_MODE, TimeOfDayCommand.MODE_ABSOLUTE)
               .put(TimeOfDayCommand.ATTR_TIME, "11:00:00")
               .put(TimeOfDayCommand.ATTR_OFFSETMINUTES, 10)
               .put(TimeOfDayCommand.ATTR_DAYS, ImmutableSet.of("MON", "TUE", "WED"))
               .put(TimeOfDayCommand.ATTR_ATTRIBUTES, ImmutableMap.of())
               .build()
      );
      try {
         command.validate();
         fail("Incorrectly passed validation");
      }
      catch(ErrorEventException e) {
         // expected
      }
   }

   @Test
   public void testOffsetSpecifiedWithNoMode() {
      TimeOfDayScheduledCommand command = TimeOfDayScheduledCommand.fromMap(
            ImmutableMap.<String,Object>builder()
               .put(TimeOfDayCommand.ATTR_ID, UUID.randomUUID().toString())
               .put(TimeOfDayCommand.ATTR_SCHEDULEID, "scheduleId")
               .put(TimeOfDayCommand.ATTR_OFFSETMINUTES, 10)
               .put(TimeOfDayCommand.ATTR_DAYS, ImmutableSet.of("MON", "TUE", "WED"))
               .put(TimeOfDayCommand.ATTR_ATTRIBUTES, ImmutableMap.of())
               .build()
      );
      try {
         command.validate();
         fail("Incorrectly passed validation");
      }
      catch(ErrorEventException e) {
         // expected
      }
   }

   @Test
   public void testTimeSpecifiedWitRelativeMode() {
      TimeOfDayScheduledCommand command = TimeOfDayScheduledCommand.fromMap(
            ImmutableMap.<String,Object>builder()
               .put(TimeOfDayCommand.ATTR_ID, UUID.randomUUID().toString())
               .put(TimeOfDayCommand.ATTR_SCHEDULEID, "scheduleId")
               .put(TimeOfDayCommand.ATTR_MODE, TimeOfDayCommand.MODE_SUNRISE)
               .put(TimeOfDayCommand.ATTR_TIME, "11:00:00")
               .put(TimeOfDayCommand.ATTR_OFFSETMINUTES, 10)
               .put(TimeOfDayCommand.ATTR_DAYS, ImmutableSet.of("MON", "TUE", "WED"))
               .put(TimeOfDayCommand.ATTR_ATTRIBUTES, ImmutableMap.of())
               .build()
      );
      try {
         command.validate();
         fail("Incorrectly passed validation");
      }
      catch(ErrorEventException e) {
         // expected
      }
   }

}


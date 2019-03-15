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
package com.iris.common.subsystem.lawnngarden.model.schedules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import com.iris.common.subsystem.lawnngarden.model.operations.OperationSequence;
import com.iris.common.subsystem.lawnngarden.model.operations.PendingClearOperation;
import com.iris.common.subsystem.lawnngarden.model.operations.PendingOperation;
import com.iris.common.subsystem.lawnngarden.model.operations.PendingOperation.State;
import com.iris.common.subsystem.lawnngarden.model.operations.PendingSetOperation;
import com.iris.common.subsystem.lawnngarden.model.schedules.Schedule.Status;
import com.iris.common.subsystem.lawnngarden.model.schedules.ScheduleEvent.EventStatus;
import com.iris.common.subsystem.lawnngarden.model.schedules.Transition.TransitionStatus;

public class ScheduleModelTestUtil {

   public static Schedule<?, ?> completeAll(Schedule<?, ?> schedule, List<OperationSequence> operations) {
      for(OperationSequence sequence : operations) {
         PendingOperation op = sequence.next();
         while(op != null) {
            op.setState(State.SUCCESSFUL);
            schedule = schedule.updateTransitionState(op);
            op = sequence.next();
         }
      }
      return schedule;
   }

   public static void assertAllApplied(Schedule<?, ?> schedule) {
      assertEquals(Status.APPLIED, schedule.status());
      for(ScheduleEvent<?> evt : schedule.events()) {
         assertEquals(EventStatus.APPLIED, evt.status());
         for(Transition transition : evt.transitions()) {
            assertEquals(TransitionStatus.APPLIED, transition.status());
         }
      }
   }

   public static class ExpectZoneOpCounts {
      public int pendingOpCount;
      public int transitionCount;
      public ExpectZoneOpCounts(int pendingOpCount, int transitionCount) {
         this.pendingOpCount = pendingOpCount;
         this.transitionCount = transitionCount;
      }
   }

   public static void assertClearOperations(List<OperationSequence> operations, int zones) {
      assertEquals(1, operations.size());
      for(OperationSequence operation : operations) {
         OperationSequence copy = OperationSequence.builder(operation).build();
         List<PendingOperation> pendingOperations = copy.operations();
         assertEquals(pendingOperations.size(), zones);
         for(PendingOperation op : pendingOperations) {
            assertTrue(op instanceof PendingClearOperation);
         }
      }
   }

   public static void assertOperations(List<OperationSequence> operations, int seqCount, Map<String,ExpectZoneOpCounts> expectations) {
      assertEquals(seqCount, operations.size());
      for(OperationSequence operation : operations) {
         if(operation.zone() != null) {
            assertEquals(expectations.get(operation.zone()).pendingOpCount, operation.operations().size());
         }

         int transitionsTotal = 0;
         for(PendingOperation op : operation.operations()) {
            if(op instanceof PendingSetOperation) {
               transitionsTotal += ((List) op.attributes().get("transitions")).size();
            }
         }

         if(operation.zone() != null) {
            assertEquals(expectations.get(operation.zone()).transitionCount, transitionsTotal);
         }
      }
   }

   public static void assertEventsInState(Schedule<?, ?> schedule, EventStatus status, int count) {
      int eventsMatching = 0;
      for(ScheduleEvent<?> evt : schedule.events()) {
         if(evt.status() == status) {
            eventsMatching++;
         }
      }

      assertEquals(count, eventsMatching);
   }

   public static void assertAllEventsInState(Schedule<?, ?> schedule, EventStatus status) {
      for(ScheduleEvent<?> evt : schedule.events()) {
         assertEquals(status, evt.status());
      }
   }

   public static void assertAllTransitionsInState(ScheduleEvent<?> evt, TransitionStatus status) {
      for(Transition transition : evt.transitions()) {
         assertEquals(status, transition.status());
      }
   }

}


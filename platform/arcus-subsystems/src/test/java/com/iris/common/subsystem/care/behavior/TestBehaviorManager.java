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

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.CareSubsystemCapability;
import com.iris.messages.capability.CareSubsystemCapability.BehaviorActionEvent;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.type.CareBehavior;
import com.iris.messages.type.CareBehaviorInactivity;
import com.iris.messages.type.CareBehaviorTemplate;
import com.iris.messages.type.TimeWindow;
import com.iris.util.TypeMarker;

public class TestBehaviorManager extends BaseCareBehaviorTest {
   private BehaviorManager manager;
   private CareBehaviorInactivity existingBehavior;

   @Before
   public void init() {
      manager = new BehaviorManager();
      context.model().setBehaviors(ImmutableSet.<String> of());
      context.model().setActiveBehaviors(ImmutableSet.<String> of());
      context.model().setAlarmMode(CareSubsystemCapability.ALARMMODE_ON);
      existingBehavior = BehaviorFixtures.createInactivtyCareBehavior();
      manager.bind(context);
   }

   @Test
   public void testAddBehavior(){
      CareBehavior behavior = BehaviorFixtures.createCareBehavior();
      behavior.setTimeWindows(null);
      String behaviorId=manager.addBehavior(new CareBehaviorTypeWrapper(behavior.toMap()), context);
      assertNotNull(behaviorId);
      assertTrue(context.model().getBehaviors().contains(behaviorId));
      Map<String,Object>behavMap=context.getVariable(BehaviorMonitor.BEHAVIOR_KEY.create(behaviorId)).as(TypeMarker.mapOf(String.class, Object.class));
      CareBehavior behaviorConfig = new CareBehavior(behavMap);
      CareBehaviorTypeWrapper tw=manager.listCareBehaviors(context).get(0);
      assertEquals("behavior should be stored in context var",behaviorId,behaviorConfig.getId());
      try{
         String id2=manager.addBehavior(new CareBehaviorTypeWrapper(behavior.toMap()), context);
         assertTrue("should have thrown exception",false);
      }
      catch(Exception e){
      }

      MessageBody event = BehaviorActionEvent.builder()
            .withBehaviorId(behaviorId)
            .withBehaviorAction(BehaviorActionEvent.BEHAVIORACTION_ADDED)
            .withBehaviorName(behavior.getName()).build();
      assertEquals(event.getAttributes(), broadcasts.getValue().getAttributes());
      
   }
   
   @Test()
   public void testAddBehaviorDuplicateWindow(){
      TimeWindow window = BehaviorFixtures.createTimeWindow("MONDAY",3600,"08:00:00");
      TimeWindow window2 = BehaviorFixtures.createTimeWindow("MONDAY",3600,"08:00:00");
      expectingError(ImmutableList.of(window,window2));
   }
   @Test()
   public void testAddBehaviorOverlappingWindow(){
      TimeWindow window = BehaviorFixtures.createTimeWindow("SUNDAY",3600,"08:00:00");
      TimeWindow window2 = BehaviorFixtures.createTimeWindow("SUNDAY",3600,"08:59:59");
      expectingError(ImmutableList.of(window,window2));
   }
   
   @Test
   public void testAddBehaviorProperWindows(){
      TimeWindow window1 = BehaviorFixtures.createTimeWindow("SUNDAY",3600,"08:00:00");
      //TimeWindow window1 = BehaviorFixtures.createTimeWindow("SUNDAY",86400,"08:00:00");  DST problem
      TimeWindow window2 = BehaviorFixtures.createTimeWindow("MONDAY",3600,"08:00:00");
      TimeWindow window3 = BehaviorFixtures.createTimeWindow("MONDAY",3600,"09:00:00");
      TimeWindow window4 = BehaviorFixtures.createTimeWindow("FRIDAY",3600,"09:00:00");
      TimeWindow window5 = BehaviorFixtures.createTimeWindow("TUESDAY",3600,"09:00:00");

      expectingPass(ImmutableList.of(window1,window2,window3,window4,window5));
   }
   
   @Test
   public void testAddBehaviorOverlappingWindowSepearteDays(){
      TimeWindow window = BehaviorFixtures.createTimeWindow("MONDAY",3600,"08:00:00");
      TimeWindow window2 = BehaviorFixtures.createTimeWindow("TUESDAY",3600,"08:00:00");
      expectingPass(ImmutableList.of(window,window2));
   }

   private void expectingError(List<TimeWindow>windows){
      assertDuplicateWindows(windows, true);
   }
   
   private void expectingPass(List<TimeWindow>windows){
      assertDuplicateWindows(windows, false);
   }
   
   private void assertDuplicateWindows(List<TimeWindow>windows, boolean expectException){
      CareBehaviorInactivity behavior = BehaviorFixtures.createInactivtyCareBehavior();
      behavior.setTimeWindows(BehaviorUtil.convertListOfType(windows));
      try{
         manager.addBehavior(new CareBehaviorTypeWrapper(behavior.toMap()), context);
         if(expectException){
            fail("Should have failed");
         }
      }
      catch(ErrorEventException ex){
         if(!expectException){
            fail("Should not have thrown exception");
         }
         assertEquals("care.duplicate_windows", ex.getCode());

      }
   }
   
   @Test
   public void testRemoveBehavior() {
      String behaviorId = manager.addBehavior(new CareBehaviorTypeWrapper(existingBehavior.toMap()), context);
      boolean result = manager.removeBehavior(behaviorId, context);
      assertTrue(result);
      assertFalse(context.model().getBehaviors().contains(behaviorId));
      assertNull(context.getVariable(BehaviorMonitor.BEHAVIOR_KEY.create(behaviorId)).as(Object.class));
      MessageBody event = BehaviorActionEvent.builder()
            .withBehaviorId(behaviorId)
            .withBehaviorAction(BehaviorActionEvent.BEHAVIORACTION_DELETED)
            .withBehaviorName(existingBehavior.getName()).build();
      assertEquals(event.getAttributes(), broadcasts.getValues().get(1).getAttributes());

   }

   @Test
   public void testUpdateBehavior() {
      String behaviorId = manager.addBehavior(new CareBehaviorTypeWrapper(existingBehavior.toMap()), context);
      existingBehavior.setId(behaviorId);
      existingBehavior.setTimeWindows(ImmutableList.of(BehaviorFixtures.createTimeWindow().toMap()));
      CareBehaviorTypeWrapper lookup = manager.getBehaviorFromContext(behaviorId, context);
      CareBehaviorInactivity inactivity = new CareBehaviorInactivity(lookup.getAttributes());
      inactivity.setDurationSecs(60);
      manager.updateBehavior(new CareBehaviorTypeWrapper(inactivity.toMap()), context);
      assertTrue(context.model().getBehaviors().contains(behaviorId));
      CareBehaviorTypeWrapper careBehavior = manager.getBehaviorFromContext(behaviorId, context);
      assertEquals(60l,careBehavior.getAttributes().get(CareBehaviorInactivity.ATTR_DURATIONSECS));
      List<Map<String, Object>> tws = careBehavior.getTimeWindows();
      // this is ugly but something isn't right about the types with the
      // variable support until you get them back into the type obejcts
      assertEquals(new TimeWindow(existingBehavior.getTimeWindows().get(0)).toMap(), new TimeWindow(tws.get(0)).toMap());
      
      MessageBody event = BehaviorActionEvent.builder()
            .withBehaviorId(behaviorId)
            .withBehaviorAction(BehaviorActionEvent.BEHAVIORACTION_MODIFIED)
            .withBehaviorName(existingBehavior.getName()).build();
      assertEquals(event.getAttributes(), broadcasts.getValues().get(1).getAttributes());

   }

   @Test
   public void testListBehaviors() {
      String behaviorId = manager.addBehavior(new CareBehaviorTypeWrapper(existingBehavior.toMap()), context);
      existingBehavior.setId(behaviorId);
      List<CareBehaviorTypeWrapper> behaviors = manager.listCareBehaviors(context);
      assertEquals(1, behaviors.size());
      // not sure why these aren't equal
      // assertEquals(existingBehavior.toMap(),behaviors.get(0).toMap());
   }

   @Test
   public void testListBehaviorTemplates() {
      List<CareBehaviorTemplate> templates = manager.listCareBehaviorTemplates(context);
      assertTrue(templates.size() > 0);
   }

}


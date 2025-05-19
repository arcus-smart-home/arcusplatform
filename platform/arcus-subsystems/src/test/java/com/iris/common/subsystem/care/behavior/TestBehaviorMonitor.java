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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.common.subsystem.SubsystemUtils;
import com.iris.common.time.DayOfWeek;
import com.iris.common.time.TimeOfDay;
import com.iris.messages.address.Address;
import com.iris.messages.capability.CareSubsystemCapability;
import com.iris.messages.event.ScheduledEvent;
import com.iris.messages.type.CareBehavior;
import com.iris.messages.type.CareBehaviorInactivity;
import com.iris.messages.type.TimeWindow;

public class TestBehaviorMonitor extends BaseCareBehaviorTest{
   private BehaviorMonitor monitor;
   private CareBehavior careBehavior;
   private Calendar currentMoment;
   private TimeZone TZ=TimeZone.getTimeZone("UTC"); 
   private UUID behaviorId;
   
   final Capture<String> nameRef = EasyMock.newCapture(CaptureType.LAST);
   final Capture<Object> valueRef = EasyMock.newCapture(CaptureType.LAST);
   
   private Date currentWindowStart;
   private TimeWindow currentTimeWindow;
   private TimeWindow previousTimeWindow;
   private TimeWindow futureTimeWindow;

   private CareBehaviorInactivity currentBehavior;
   
   @Before
   public void init(){
      TZ=context.getLocalTime().getTimeZone();
      model.setAlarmMode(CareSubsystemCapability.ALARMMODE_ON);
      monitor=new BehaviorMonitor();
      behaviorId=UUID.randomUUID();
      context.model().setBehaviors(ImmutableSet.<String>of());
      context.model().setActiveBehaviors(ImmutableSet.<String>of());
      careBehavior=EasyMock.createNiceMock(CareBehavior.class);
      
      currentMoment = context.getLocalTime();
      
      EasyMock.expect(careBehavior.getId()).andReturn(behaviorId.toString()).anyTimes();
      EasyMock.expect(careBehavior.toMap()).andReturn(ImmutableMap.<String,Object>of()).anyTimes();
      
      Date previousWindowStart=adjustDate(currentMoment.getTime(),-4,TimeUnit.HOURS);
      currentWindowStart=adjustDate(currentMoment.getTime(),-1,TimeUnit.HOURS);
      Date futureWindowStart=adjustDate(currentMoment.getTime(),4,TimeUnit.HOURS);
      Date futureWindowStartNextDate=adjustDate(currentMoment.getTime(),24,TimeUnit.HOURS);

      previousTimeWindow = createRelativeTimeWindow(previousWindowStart,2,TimeUnit.HOURS);
      currentTimeWindow = createRelativeTimeWindow(currentWindowStart,2,TimeUnit.HOURS);
      futureTimeWindow = createRelativeTimeWindow(futureWindowStart,2,TimeUnit.HOURS);
      TimeWindow futureTimeWindowNextDay = createRelativeTimeWindow(futureWindowStartNextDate,2,TimeUnit.HOURS);
      
      currentBehavior=addBehaviorToContext(ImmutableList.of(previousTimeWindow,currentTimeWindow,futureTimeWindow,futureTimeWindowNextDay));

   }

   @Test
   public void testBind(){
      monitor.bind(context);
      assertTimeoutExists(BehaviorMonitor.WINDOW_START_KEY.create(currentBehavior.getId()), currentWindowStart);
   }
   
   @Test
   public void testBindInVisitMode(){
      context.model().setAlarmMode(CareSubsystemCapability.ALARMMODE_VISIT);
      monitor.bind(context);
      assertNoTimeoutExists(BehaviorMonitor.WINDOW_START_KEY.create(currentBehavior.getId()));
   }

   @Test
   public void testCleanup(){
      monitor.bind(context);
      scheduleTimeout(currentWindowStart,BehaviorMonitor.WINDOW_END_KEY.create(currentBehavior.getId()));
      scheduleTimeout(currentWindowStart,BehaviorMonitor.ALERT_KEY.create(currentBehavior.getId()));

      BehaviorMonitor.clearBehaviorTimeouts(currentBehavior.getId(), context);
      
      Date timeoutDate = SubsystemUtils.getTimeout(context,BehaviorMonitor.WINDOW_START_KEY.create(currentBehavior.getId())).orNull();
      Date endTimeout = SubsystemUtils.getTimeout(context,BehaviorMonitor.WINDOW_END_KEY.create(currentBehavior.getId())).orNull();
      Date alertTimeout = SubsystemUtils.getTimeout(context,BehaviorMonitor.ALERT_KEY.create(currentBehavior.getId())).orNull();

      assertNull(timeoutDate);
      assertNull(endTimeout);
      assertNull(alertTimeout);
      
   }
   @Test
   public void testSwitchModeToOFF(){
      monitor.bind(context);

      ScheduledEvent event = scheduleTimeout(currentWindowStart,BehaviorMonitor.WINDOW_START_KEY.create(currentBehavior.getId()));
      monitor.handleTimeout(event, context);
      
      monitor.changeMode(CareSubsystemCapability.ALARMMODE_VISIT,context);
      
      CareBehaviorTypeWrapper tw = BehaviorUtil.getBehaviorFromContext(currentBehavior.getId(), context);
      assertFalse(tw.getActive());
      assertEquals(new HashSet<String>(),context.model().getActiveBehaviors());
      assertNull(SubsystemUtils.getTimeout(context,BehaviorMonitor.WINDOW_END_KEY.create(currentBehavior.getId())).orNull());
      assertNull(SubsystemUtils.getTimeout(context,BehaviorMonitor.WINDOW_START_KEY.create(currentBehavior.getId())).orNull());
      assertNull(SubsystemUtils.getTimeout(context,BehaviorMonitor.ALERT_KEY.create(currentBehavior.getId())).orNull());
      
   }
   
   @Test
   public void testSwitchModeToON(){
      context.model().setBehaviors(ImmutableSet.<String>of(currentBehavior.getId()));

      monitor.bind(context);
      monitor.changeMode(CareSubsystemCapability.ALARMMODE_ON,context);
      CareBehaviorTypeWrapper tw = BehaviorUtil.getBehaviorFromContext(currentBehavior.getId(), context);
      assertNotNull(SubsystemUtils.getTimeout(context,BehaviorMonitor.WINDOW_START_KEY.create(currentBehavior.getId())).orNull());
   }
   
   @Test
   public void testClear(){
      currentBehavior.setEnabled(false);
      updateCurrentBehaviorInContext();
      context.model().setBehaviors(ImmutableSet.<String>of(currentBehavior.getId()));
      monitor.bind(context);
      assertNull(SubsystemUtils.getTimeout(context,BehaviorMonitor.WINDOW_START_KEY.create(currentBehavior.getId())).orNull());
      monitor.careAlarmCleared(context);
      assertNull(SubsystemUtils.getTimeout(context,BehaviorMonitor.WINDOW_START_KEY.create(currentBehavior.getId())).orNull());
   }
   private void updateCurrentBehaviorInContext(){
      context.setVariable(BehaviorMonitor.BEHAVIOR_KEY.create(currentBehavior.getId()), currentBehavior.toMap());
   }
   @Test 
   public void testOnWindowStartTimeout(){
      ScheduledEvent event = scheduleTimeout(currentWindowStart,BehaviorMonitor.WINDOW_START_KEY.create(currentBehavior.getId()));

      monitor.handleTimeout(event, context);
      
      Date expectedEndDate = WeeklyTimeWindow.calculateEndDate(new Date(event.getScheduledTimestamp()), currentTimeWindow);
      assertTimeoutExists(BehaviorMonitor.WINDOW_END_KEY.create(currentBehavior.getId()), expectedEndDate);
      assertTrue(context.model().getActiveBehaviors().contains(currentBehavior.getId()));
      
      CareBehaviorTypeWrapper tw = BehaviorUtil.getBehaviorFromContext(currentBehavior.getId(), context);
      assertEquals(new Date(event.getScheduledTimestamp()),tw.getLastActivated());
      //assertTrue(tw.getEnabled());
      assertTrue(tw.getActive());

   }
   @Test
   public void testOnWindowEndTimeout(){
      currentWindowStart = adjustDate(currentWindowStart, 4, TimeUnit.HOURS);
      ScheduledEvent event = scheduleTimeout(currentWindowStart,BehaviorMonitor.WINDOW_END_KEY.create(currentBehavior.getId()));
      BehaviorUtil.addStringToSet(currentBehavior.getId(), CareSubsystemCapability.ATTR_ACTIVEBEHAVIORS, context.model());
      monitor.handleTimeout(event, context);
      
      Date expectedStartDate = new WeeklyTimeWindow(futureTimeWindow).nextWeekStartDate(currentMoment.getTime(), currentMoment.getTimeZone());
      assertTimeoutExists(BehaviorMonitor.WINDOW_START_KEY.create(currentBehavior.getId()), expectedStartDate);
      assertFalse(context.model().getActiveBehaviors().contains(currentBehavior.getId()));

      CareBehaviorTypeWrapper tw = BehaviorUtil.getBehaviorFromContext(currentBehavior.getId(), context);
      assertFalse(tw.getActive());

   }
   
   @Test
   public void testChange(){
      currentWindowStart = adjustDate(currentWindowStart, 4, TimeUnit.HOURS);
      ScheduledEvent event = scheduleTimeout(currentWindowStart,BehaviorMonitor.WINDOW_END_KEY.create(currentBehavior.getId()));
      BehaviorUtil.addStringToSet(currentBehavior.getId(), CareSubsystemCapability.ATTR_ACTIVEBEHAVIORS, context.model());
      monitor.handleTimeout(event, context);
      
      Date expectedStartDate = new WeeklyTimeWindow(futureTimeWindow).nextWeekStartDate(currentMoment.getTime(), currentMoment.getTimeZone());
      assertTimeoutExists(BehaviorMonitor.WINDOW_START_KEY.create(currentBehavior.getId()), expectedStartDate);
      assertFalse(context.model().getActiveBehaviors().contains(currentBehavior.getId()));

      CareBehaviorTypeWrapper tw = BehaviorUtil.getBehaviorFromContext(currentBehavior.getId(), context);
      assertFalse(tw.getActive());
   }

   
   private ScheduledEvent scheduleTimeout(Date timeout, String key){
      Date timeoutDate = SubsystemUtils.setTimeout(timeout,context,key);
      ScheduledEvent event = new ScheduledEvent(Address.fromString("SERV:subscare:"),timeoutDate.getTime());
      return event;
   }
   private CareBehaviorInactivity addBehaviorToContext(List<TimeWindow> timeWindows){
      List<Map<String,Object>>windows = new ArrayList<Map<String,Object>>();
      for(TimeWindow window:timeWindows){
         windows.add(window.toMap());
      }
      CareBehaviorInactivity behavior = behaviorFixture(windows);
      context.setVariable(BehaviorMonitor.BEHAVIOR_KEY.create(behavior.getId()), behavior.toMap());
      context.model().setBehaviors(ImmutableSet.of(behavior.getId()));
      return behavior;
   }
   
   private TimeWindow createRelativeTimeWindow(Date relativeToDate, int duration, TimeUnit durationUnit){
      Calendar now = Calendar.getInstance(TZ);
      now.setTime(relativeToDate);
      DayOfWeek day = DayOfWeek.from(now);
      TimeOfDay time = new TimeOfDay(now);
      TimeWindow tw = new TimeWindow();
      tw.setDay(day.name());
      tw.setDurationSecs((int)TimeUnit.SECONDS.convert(duration, durationUnit));
      tw.setStartTime(time.toString());
      return tw;
   }
   
   private Date adjustDate(Date date,int adjust,TimeUnit adjustUnit){
      Calendar now = Calendar.getInstance(TZ);
      now.setTime(date);
      now.add(Calendar.MILLISECOND, (int)TimeUnit.MILLISECONDS.convert(adjust, adjustUnit));
      return now.getTime();
   }
   
   private CareBehaviorInactivity behaviorFixture(List<Map<String,Object>>timeWindows){
      CareBehaviorInactivity behavior = new CareBehaviorInactivity();
      behavior.setId(behaviorId.toString());
      behavior.setType(CareBehavior.TYPE_INACTIVITY);
      behavior.setDurationSecs(30*60);
      behavior.setDevices(ImmutableSet.<String>of());
      behavior.setTimeWindows(timeWindows);
      behavior.setEnabled(true);
      return behavior;
   }
   
   private void assertTimeoutExists(String name,Date fireTime){
      Date timeoutDate = SubsystemUtils.getTimeout(context,name).orNull();
      assertNotNull("expecting timeout for " +name,timeoutDate);
      assertEquals("execting timeout for name " + name,fireTime.getTime(),timeoutDate.getTime());
   }
   private void assertNoTimeoutExists(String name){
      Date timeoutDate = SubsystemUtils.getTimeout(context,name).orNull();
      assertNull("expecting timeout for " +name,timeoutDate);
   }
   
}


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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import com.iris.common.subsystem.SubsystemTestCase;
import com.iris.common.subsystem.SubsystemUtils;
import com.iris.common.subsystem.care.CareSubsystem;
import com.iris.common.subsystem.care.behavior.evaluators.BehaviorEvaluator;
import com.iris.common.subsystem.event.SubsystemLifecycleEvent;
import com.iris.common.time.DayOfWeek;
import com.iris.common.time.TimeOfDay;
import com.iris.messages.capability.AccountCapability;
import com.iris.messages.capability.CareSubsystemCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.capability.SubsystemCapability;
import com.iris.messages.event.Listener;
import com.iris.messages.event.ModelChangedEvent;
import com.iris.messages.event.ModelEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.ServiceLevel;
import com.iris.messages.model.SimpleModel;
import com.iris.messages.model.subs.CareSubsystemModel;
import com.iris.messages.model.test.ModelFixtures;
import com.iris.messages.type.TimeWindow;

public class BaseCareBehaviorTest extends SubsystemTestCase<CareSubsystemModel> {

   protected SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
   protected TimeZone CST = TimeZone.getTimeZone("CST");
   protected TimeZone UTC = TimeZone.getTimeZone("UTC");
   protected TimeZone TEST_TZ = UTC;
   protected CareSubsystem careSS;
   protected Model owner = null;

   protected void start() {
      placeModel.setAttribute(PlaceCapability.ATTR_SERVICELEVEL, "PREMIUM");
      
      careSS = new CareSubsystem();
      careSS.onEvent(SubsystemLifecycleEvent.added(context.model().getAddress()), context);
      careSS.onEvent(SubsystemLifecycleEvent.started(context.model().getAddress()), context);
      store.addListener(new Listener<ModelEvent>() {
         @Override
         public void onEvent(ModelEvent event) {
            careSS.onEvent(event, context);
         }
      });      
      careSS.onStarted(context);
      
      owner = addModel(ModelFixtures.createPersonAttributes());
            
      placeModel.setAttribute(PlaceCapability.ATTR_SERVICELEVEL, ServiceLevel.PREMIUM.name());
      addModel(placeModel.toMap());

      accountModel.setAttribute(AccountCapability.ATTR_OWNER, owner.getId());
      addModel(accountModel.toMap());
      careSS.onStarted(context);
   }

   @Override
   protected CareSubsystemModel createSubsystemModel() {
      Map<String, Object> attributes = ModelFixtures.createServiceAttributes(SubsystemCapability.NAMESPACE, CareSubsystemCapability.NAMESPACE);
      return new CareSubsystemModel(new SimpleModel(attributes));
   }
   
   protected ModelChangedEvent createModelChangedEvent(Model model,String attr,String value){
      model.setAttribute(attr, value);
      store.updateModel(model.getAddress(), model.toMap());
      ModelChangedEvent event = ModelChangedEvent.create(model.getAddress(), attr, value, null);
      return event;
   }
   protected ModelChangedEvent createModelChangedEvent(Model model,Map<String,Object>attrs,String changedAttr){
      Object fromValue = model.getAttribute(changedAttr);
      model.update(attrs);
      store.updateModel(model.getAddress(), model.toMap());
      ModelChangedEvent event = ModelChangedEvent.create(model.getAddress(), changedAttr, attrs.get(changedAttr), fromValue);
      return event;
   }
   
   protected void assertBehaviorAlertTimeoutScheduled(BehaviorEvaluator evaluator,Date expectedTimeout){
      Date actualTimeout = SubsystemUtils.getTimeout(context, BehaviorMonitor.ALERT_KEY.create(evaluator.getBehaviorId())).orNull();
      assertNotNull("should have timeout", actualTimeout);
      assertEquals("expecting alert to be scheduled for behavior " + evaluator.getBehaviorId(), expectedTimeout, actualTimeout);
   }
   protected void assertExistsInLastTriggeredDevices(String... addresses){
      //find out what to do with these.
      //assertNotNull("last alert triggers should not be null",context.model().getLastAlertTriggers());
      //for(String address:addresses){
      //   assertTrue("last alert triggers should contain " +address,context.model().getLastAlertTriggers().containsKey(address));
      //}
   }
   protected Date simulateDateWithCST(String strDate) {
      strDate += "-0600";
      Date testDate;
      try{
         testDate = df.parse(strDate);
         return testDate;

      }catch (ParseException e){
         throw new RuntimeException("bad datat format, expecting " + df.toPattern(), e);
      }
   }
   protected TimeWindow createTimeWindow(DayOfWeek dow,TimeOfDay tod,int duration){
      TimeWindow tw = new TimeWindow();
      tw.setDay(dow.name());
      tw.setStartTime(tod.toString());
      tw.setDurationSecs(duration);
      return tw;
   }
   protected Date secondsFromContextTime(int seconds){
      return new Date(context.getLocalTime().getTime().getTime() + (seconds*1000));
   }
   protected String futureTimeDay(Calendar cal,int adjustMillis){
      cal.add(Calendar.MILLISECOND, adjustMillis);
      return String.format("%02d:%02d:%02d", cal.get(Calendar.HOUR_OF_DAY),cal.get(Calendar.MINUTE),cal.get(Calendar.SECOND));
   }
   
}


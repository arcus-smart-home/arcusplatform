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
package com.iris.platform.subsystem.placemonitor;

import static com.iris.messages.capability.NotificationCapability.NotifyRequest.PRIORITY_LOW;
import static com.iris.messages.capability.NotificationCapability.NotifyRequest.PRIORITY_MEDIUM;
import static com.iris.platform.subsystem.placemonitor.PlaceMonitorNotifications.MSG_KEY_DEVICE_BATTERY_LOW;
import static com.iris.platform.subsystem.placemonitor.PlaceMonitorNotifications.MSG_KEY_DEVICE_BATTERY_RECHARGEABLE_CRITICAL;
import static com.iris.platform.subsystem.placemonitor.PlaceMonitorNotifications.MSG_KEY_DEVICE_BATTERY_RECHARGEABLE_DEAD;
import static com.iris.platform.subsystem.placemonitor.PlaceMonitorNotifications.MSG_KEY_DEVICE_BATTERY_RECHARGEABLE_FULL;
import static com.iris.platform.subsystem.placemonitor.PlaceMonitorNotifications.MSG_KEY_DEVICE_BATTERY_RECHARGEABLE_LOW;
import static com.iris.platform.subsystem.placemonitor.PlaceMonitorNotifications.MSG_KEY_DEVICE_BATTERY_RECHARGEABLE_VERYLOW;
import static org.apache.commons.lang3.reflect.FieldUtils.writeField;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.iris.common.subsystem.Subsystem;
import com.iris.common.subsystem.SubsystemTestCase;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.capability.NotificationCapability.NotifyRequest;
import com.iris.messages.capability.PlaceMonitorSubsystemCapability;
import com.iris.messages.capability.SubsystemCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;
import com.iris.messages.model.dev.DeviceModel;
import com.iris.messages.model.dev.DevicePowerModel;
import com.iris.messages.model.serv.AccountModel;
import com.iris.messages.model.subs.PlaceMonitorSubsystemModel;
import com.iris.messages.model.test.ModelFixtures;
import com.iris.platform.subsystem.placemonitor.offlinenotifications.AbstractBatteryNotificationsHandler;
import com.iris.platform.subsystem.placemonitor.offlinenotifications.CriticalBatteryNotificationsHandler;
import com.iris.platform.subsystem.placemonitor.offlinenotifications.DeadBatteryNotificationsHandler;
import com.iris.platform.subsystem.placemonitor.offlinenotifications.FullBatteryNotificationsHandler;
import com.iris.platform.subsystem.placemonitor.offlinenotifications.LowBatteryNotificationsHandler;
import com.iris.platform.subsystem.placemonitor.offlinenotifications.VeryLowBatteryNotificationsHandler;

/**
 * <pre>
 * Non-rechargeable test cases:
 * - testFullNoSendNoClear:       100, 95
 * - testLowAndClear:              10, 25
 * - testVeryLowNoSendNoClear:      7, 10
 * - testCriticalNoSendNoClear:     5,  7
 * - testDeadNoSendNoClear:         1,  5
 * - testSlowDrainThenNew:        100, 95, 11, 10, 7, 5, 1, 100
 * - testFastDrainThenNew:        100, 1, 100
 * - testFastDrainThenOldThenNew: 100, 1, 24, 100
 * - testLowOverride: Uses productId "highbattery" to override a notification level via &lt;deviceOverride>
 * - testLowDisabled: Uses productId "nosend" to disable a notification level via &lt;deviceOverride>
 * 
 * Rechargeable test cases:
 * - testRechargeableFullAndClear:              100, 95
 * - testRechargeableLowAndClear:                10, 25
 * - testRechargeableVeryLowAndClear:             7, 10
 * - testRechargeableCriticalAndClear:            5,  7
 * - testRechargeableDeadAndClear:                1,  5
 * - testRechargeableSlowDrainFastRecharge:     100, 95, 11, 10, 7, 5, 1, 100
 * - testRechargeableFastDrainModerateRecharge: 100, 1, 50, 100
 * - testRechargeableFastDrainSlowRecharge:     100, 1, 5, 7, 10, 25, 95, 100
 * </pre>
 * 
 * @author Dan Ignat
 */
public class TestBatteryNotifications extends SubsystemTestCase<PlaceMonitorSubsystemModel>
{
   private List<AbstractBatteryNotificationsHandler> handlers;

   private Model device;

   @Before
   public void init() throws IllegalAccessException
   {
      handlers = ImmutableList.of(
         new FullBatteryNotificationsHandler(),
         new LowBatteryNotificationsHandler(),
         new VeryLowBatteryNotificationsHandler(),
         new CriticalBatteryNotificationsHandler(),
         new DeadBatteryNotificationsHandler());

      PlaceMonitorNotifications notifier = new PlaceMonitorNotifications(null);

      for (AbstractBatteryNotificationsHandler handler : handlers)
      {
         writeField(handler, "notificationThresholdsConfigPath", "classpath:/conf/notification-thresholds-config.xml",
            true);
         writeField(handler, "notifier", notifier, true);
         handler.onStarted(context);
         handler.init();
      }

      device = new SimpleModel(ModelFixtures.buildDeviceAttributes(DeviceConnectionCapability.NAMESPACE).create());
      device.setAttribute(DeviceCapability.ATTR_NAME, "Test Device");

      Model owner = store.addModel(ModelFixtures.createPersonAttributes());
      AccountModel.setOwner(accountModel, owner.getId());
   }

   @Override
   protected PlaceMonitorSubsystemModel createSubsystemModel()
   {
      Map<String, Object> attributes = ModelFixtures.createServiceAttributes(
         SubsystemCapability.NAMESPACE, PlaceMonitorSubsystemCapability.NAMESPACE);

      return new PlaceMonitorSubsystemModel(new SimpleModel(attributes));
   }

   @Override
   protected Subsystem<PlaceMonitorSubsystemModel> subsystem()
   {
      return null;
   }

   @Test
   public void testFullNoSendNoClear()
   {
      testBatteryNotifications(
         false,
         null,
         TestStep.builder()
            .withBatteryLevel(100)
            .withExpectedNotificationLevelsMarkedSent()
            .withExpectedNotificationMsgKey()
            .withExpectedNotificationPriorities()
            .build(),
         TestStep.builder()
            .withBatteryLevel(95)
            .withExpectedNotificationLevelsMarkedSent()
            .withExpectedNotificationMsgKey()
            .withExpectedNotificationPriorities()
            .build());
   }

   @Test
   public void testLowAndClear()
   {
      testBatteryNotifications(
         false,
         null,
         TestStep.builder()
            .withBatteryLevel(10)
            .withExpectedNotificationLevelsMarkedSent(BatteryNotificationLevel.LOW)
            .withExpectedNotificationMsgKey(MSG_KEY_DEVICE_BATTERY_LOW)
            .withExpectedNotificationPriorities(PRIORITY_LOW)
            .build(),
         TestStep.builder()
            .withBatteryLevel(25)
            .withExpectedNotificationLevelsMarkedSent()
            .withExpectedNotificationMsgKey()
            .withExpectedNotificationPriorities()
            .build());
   }

   @Test
   public void testVeryLowNoSendNoClear()
   {
      testBatteryNotifications(
         false,
         null,
         TestStep.builder()
            .withBatteryLevel(7)
            .withExpectedNotificationLevelsMarkedSent(BatteryNotificationLevel.LOW)
            .withExpectedNotificationMsgKey(MSG_KEY_DEVICE_BATTERY_LOW)
            .withExpectedNotificationPriorities(PRIORITY_LOW)
            .build(),
         TestStep.builder()
            .withBatteryLevel(10)
            .withExpectedNotificationLevelsMarkedSent(BatteryNotificationLevel.LOW)
            .withExpectedNotificationMsgKey()
            .withExpectedNotificationPriorities()
            .build());
   }

   @Test
   public void testCriticalNoSendNoClear()
   {
      testBatteryNotifications(
         false,
         null,
         TestStep.builder()
            .withBatteryLevel(5)
            .withExpectedNotificationLevelsMarkedSent(BatteryNotificationLevel.LOW)
            .withExpectedNotificationMsgKey(MSG_KEY_DEVICE_BATTERY_LOW)
            .withExpectedNotificationPriorities(PRIORITY_LOW)
            .build(),
         TestStep.builder()
            .withBatteryLevel(7)
            .withExpectedNotificationLevelsMarkedSent(BatteryNotificationLevel.LOW)
            .withExpectedNotificationMsgKey()
            .withExpectedNotificationPriorities()
            .build());
   }

   @Test
   public void testDeadNoSendNoClear()
   {
      testBatteryNotifications(
         false,
         null,
         TestStep.builder()
            .withBatteryLevel(1)
            .withExpectedNotificationLevelsMarkedSent(BatteryNotificationLevel.LOW)
            .withExpectedNotificationMsgKey(MSG_KEY_DEVICE_BATTERY_LOW)
            .withExpectedNotificationPriorities(PRIORITY_LOW)
            .build(),
         TestStep.builder()
            .withBatteryLevel(5)
            .withExpectedNotificationLevelsMarkedSent(BatteryNotificationLevel.LOW)
            .withExpectedNotificationMsgKey()
            .withExpectedNotificationPriorities()
            .build());
   }

   @Test
   public void testSlowDrainThenNew()
   {
      testBatteryNotifications(
         false,
         null,
         TestStep.builder()
            .withBatteryLevel(100)
            .withExpectedNotificationLevelsMarkedSent()
            .withExpectedNotificationMsgKey()
            .withExpectedNotificationPriorities()
            .build(),
         TestStep.builder()
            .withBatteryLevel(95)
            .withExpectedNotificationLevelsMarkedSent()
            .withExpectedNotificationMsgKey()
            .withExpectedNotificationPriorities()
            .build(),
         TestStep.builder()
            .withBatteryLevel(11)
            .withExpectedNotificationLevelsMarkedSent()
            .withExpectedNotificationMsgKey()
            .withExpectedNotificationPriorities()
            .build(),
         TestStep.builder()
            .withBatteryLevel(10)
            .withExpectedNotificationLevelsMarkedSent(BatteryNotificationLevel.LOW)
            .withExpectedNotificationMsgKey(MSG_KEY_DEVICE_BATTERY_LOW)
            .withExpectedNotificationPriorities(PRIORITY_LOW)
            .build(),
         TestStep.builder()
            .withBatteryLevel(7)
            .withExpectedNotificationLevelsMarkedSent(BatteryNotificationLevel.LOW)
            .withExpectedNotificationMsgKey()
            .withExpectedNotificationPriorities()
            .build(),
         TestStep.builder()
            .withBatteryLevel(5)
            .withExpectedNotificationLevelsMarkedSent(BatteryNotificationLevel.LOW)
            .withExpectedNotificationMsgKey()
            .withExpectedNotificationPriorities()
            .build(),
         TestStep.builder()
            .withBatteryLevel(1)
            .withExpectedNotificationLevelsMarkedSent(BatteryNotificationLevel.LOW)
            .withExpectedNotificationMsgKey()
            .withExpectedNotificationPriorities()
            .build(),
         TestStep.builder()
            .withBatteryLevel(100)
            .withExpectedNotificationLevelsMarkedSent()
            .withExpectedNotificationMsgKey()
            .withExpectedNotificationPriorities()
            .build());
   }

   @Test
   public void testFastDrainThenNew()
   {
      testBatteryNotifications(
         false,
         null,
         TestStep.builder()
            .withBatteryLevel(100)
            .withExpectedNotificationLevelsMarkedSent()
            .withExpectedNotificationMsgKey()
            .withExpectedNotificationPriorities()
            .build(),
         TestStep.builder()
            .withBatteryLevel(1)
            .withExpectedNotificationLevelsMarkedSent(BatteryNotificationLevel.LOW)
            .withExpectedNotificationMsgKey(MSG_KEY_DEVICE_BATTERY_LOW)
            .withExpectedNotificationPriorities(PRIORITY_LOW)
            .build(),
         TestStep.builder()
            .withBatteryLevel(100)
            .withExpectedNotificationLevelsMarkedSent()
            .withExpectedNotificationMsgKey()
            .withExpectedNotificationPriorities()
            .build());
   }

   @Test
   public void testFastDrainThenOldThenNew()
   {
      testBatteryNotifications(
         false,
         null,
         TestStep.builder()
            .withBatteryLevel(100)
            .withExpectedNotificationLevelsMarkedSent()
            .withExpectedNotificationMsgKey()
            .withExpectedNotificationPriorities()
            .build(),
         TestStep.builder()
            .withBatteryLevel(1)
            .withExpectedNotificationLevelsMarkedSent(BatteryNotificationLevel.LOW)
            .withExpectedNotificationMsgKey(MSG_KEY_DEVICE_BATTERY_LOW)
            .withExpectedNotificationPriorities(PRIORITY_LOW)
            .build(),
         TestStep.builder()
            .withBatteryLevel(24)
            .withExpectedNotificationLevelsMarkedSent(BatteryNotificationLevel.LOW)
            .withExpectedNotificationMsgKey()
            .withExpectedNotificationPriorities()
            .build(),
         TestStep.builder()
            .withBatteryLevel(100)
            .withExpectedNotificationLevelsMarkedSent()
            .withExpectedNotificationMsgKey()
            .withExpectedNotificationPriorities()
            .build());
   }

   @Test
   public void testLowOverride()
   {
      testBatteryNotifications(
         false,
         "highbattery",
         TestStep.builder()
            .withBatteryLevel(60)
            .withExpectedNotificationLevelsMarkedSent(BatteryNotificationLevel.LOW)
            .withExpectedNotificationMsgKey(MSG_KEY_DEVICE_BATTERY_LOW)
            .withExpectedNotificationPriorities(PRIORITY_LOW)
            .build(),
         TestStep.builder()
            .withBatteryLevel(90)
            .withExpectedNotificationLevelsMarkedSent()
            .withExpectedNotificationMsgKey()
            .withExpectedNotificationPriorities()
            .build());
   }

   @Test
   public void testLowDisabled()
   {
      testBatteryNotifications(
         false,
         "nosend",
         TestStep.builder()
            .withBatteryLevel(-10)
            .withExpectedNotificationLevelsMarkedSent()
            .withExpectedNotificationMsgKey()
            .withExpectedNotificationPriorities()
            .build());
   }

   @Test
   public void testRechargeableFullAndClear()
   {
      testBatteryNotifications(
         true,
         null,
         TestStep.builder()
            .withBatteryLevel(100)
            .withExpectedNotificationLevelsMarkedSent(BatteryNotificationLevel.FULL)
            .withExpectedNotificationMsgKey(MSG_KEY_DEVICE_BATTERY_RECHARGEABLE_FULL)
            .withExpectedNotificationPriorities(PRIORITY_MEDIUM, PRIORITY_LOW)
            .build(),
         TestStep.builder()
            .withBatteryLevel(95)
            .withExpectedNotificationLevelsMarkedSent()
            .withExpectedNotificationMsgKey()
            .withExpectedNotificationPriorities()
            .build());
   }

   @Test
   public void testRechargeableLowAndClear()
   {
      testBatteryNotifications(
         true,
         null,
         TestStep.builder()
            .withBatteryLevel(10)
            .withExpectedNotificationLevelsMarkedSent(BatteryNotificationLevel.LOW)
            .withExpectedNotificationMsgKey(MSG_KEY_DEVICE_BATTERY_RECHARGEABLE_LOW)
            .withExpectedNotificationPriorities(PRIORITY_MEDIUM, PRIORITY_LOW)
            .build(),
         TestStep.builder()
            .withBatteryLevel(25)
            .withExpectedNotificationLevelsMarkedSent()
            .withExpectedNotificationMsgKey()
            .withExpectedNotificationPriorities()
            .build());
   }

   @Test
   public void testRechargeableVeryLowAndClear()
   {
      testBatteryNotifications(
         true,
         null,
         TestStep.builder()
            .withBatteryLevel(7)
            .withExpectedNotificationLevelsMarkedSent(BatteryNotificationLevel.LOW, BatteryNotificationLevel.VERY_LOW)
            .withExpectedNotificationMsgKey(MSG_KEY_DEVICE_BATTERY_RECHARGEABLE_VERYLOW)
            .withExpectedNotificationPriorities(PRIORITY_MEDIUM, PRIORITY_LOW)
            .build(),
         TestStep.builder()
            .withBatteryLevel(10)
            .withExpectedNotificationLevelsMarkedSent(BatteryNotificationLevel.LOW)
            .withExpectedNotificationMsgKey()
            .withExpectedNotificationPriorities()
            .build());
   }

   @Test
   public void testRechargeableCriticalAndClear()
   {
      testBatteryNotifications(
         true,
         null,
         TestStep.builder()
            .withBatteryLevel(5)
            .withExpectedNotificationLevelsMarkedSent(BatteryNotificationLevel.LOW, BatteryNotificationLevel.VERY_LOW,
               BatteryNotificationLevel.CRITICAL)
            .withExpectedNotificationMsgKey(MSG_KEY_DEVICE_BATTERY_RECHARGEABLE_CRITICAL)
            .withExpectedNotificationPriorities(PRIORITY_MEDIUM, PRIORITY_LOW)
            .build(),
         TestStep.builder()
            .withBatteryLevel(7)
            .withExpectedNotificationLevelsMarkedSent(BatteryNotificationLevel.LOW, BatteryNotificationLevel.VERY_LOW)
            .withExpectedNotificationMsgKey()
            .withExpectedNotificationPriorities()
            .build());
   }

   @Test
   public void testRechargeableDeadAndClear()
   {
      testBatteryNotifications(
         true,
         null,
         TestStep.builder()
            .withBatteryLevel(1)
            .withExpectedNotificationLevelsMarkedSent(BatteryNotificationLevel.LOW, BatteryNotificationLevel.VERY_LOW,
               BatteryNotificationLevel.CRITICAL, BatteryNotificationLevel.DEAD)
            .withExpectedNotificationMsgKey(MSG_KEY_DEVICE_BATTERY_RECHARGEABLE_DEAD)
            .withExpectedNotificationPriorities(PRIORITY_MEDIUM, PRIORITY_LOW)
            .build(),
         TestStep.builder()
            .withBatteryLevel(5)
            .withExpectedNotificationLevelsMarkedSent(BatteryNotificationLevel.LOW, BatteryNotificationLevel.VERY_LOW,
               BatteryNotificationLevel.CRITICAL)
            .withExpectedNotificationMsgKey()
            .withExpectedNotificationPriorities()
            .build());
   }

   @Test
   public void testRechargeableSlowDrainFastRecharge()
   {
      testBatteryNotifications(
         true,
         null,
         TestStep.builder()
            .withBatteryLevel(100)
            .withExpectedNotificationLevelsMarkedSent(BatteryNotificationLevel.FULL)
            .withExpectedNotificationMsgKey(MSG_KEY_DEVICE_BATTERY_RECHARGEABLE_FULL)
            .withExpectedNotificationPriorities(PRIORITY_MEDIUM, PRIORITY_LOW)
            .build(),
         TestStep.builder()
            .withBatteryLevel(95)
            .withExpectedNotificationLevelsMarkedSent()
            .withExpectedNotificationMsgKey()
            .withExpectedNotificationPriorities()
            .build(),
         TestStep.builder()
            .withBatteryLevel(11)
            .withExpectedNotificationLevelsMarkedSent()
            .withExpectedNotificationMsgKey()
            .withExpectedNotificationPriorities()
            .build(),
         TestStep.builder()
            .withBatteryLevel(10)
            .withExpectedNotificationLevelsMarkedSent(BatteryNotificationLevel.LOW)
            .withExpectedNotificationMsgKey(MSG_KEY_DEVICE_BATTERY_RECHARGEABLE_LOW)
            .withExpectedNotificationPriorities(PRIORITY_MEDIUM, PRIORITY_LOW)
            .build(),
         TestStep.builder()
            .withBatteryLevel(7)
            .withExpectedNotificationLevelsMarkedSent(BatteryNotificationLevel.LOW, BatteryNotificationLevel.VERY_LOW)
            .withExpectedNotificationMsgKey(MSG_KEY_DEVICE_BATTERY_RECHARGEABLE_VERYLOW)
            .withExpectedNotificationPriorities(PRIORITY_MEDIUM, PRIORITY_LOW)
            .build(),
         TestStep.builder()
            .withBatteryLevel(5)
            .withExpectedNotificationLevelsMarkedSent(BatteryNotificationLevel.LOW, BatteryNotificationLevel.VERY_LOW,
               BatteryNotificationLevel.CRITICAL)
            .withExpectedNotificationMsgKey(MSG_KEY_DEVICE_BATTERY_RECHARGEABLE_CRITICAL)
            .withExpectedNotificationPriorities(PRIORITY_MEDIUM, PRIORITY_LOW)
            .build(),
         TestStep.builder()
            .withBatteryLevel(1)
            .withExpectedNotificationLevelsMarkedSent(
               BatteryNotificationLevel.LOW, BatteryNotificationLevel.VERY_LOW,
               BatteryNotificationLevel.CRITICAL, BatteryNotificationLevel.DEAD)
            .withExpectedNotificationMsgKey(MSG_KEY_DEVICE_BATTERY_RECHARGEABLE_DEAD)
            .withExpectedNotificationPriorities(PRIORITY_MEDIUM, PRIORITY_LOW)
            .build(),
         TestStep.builder()
            .withBatteryLevel(100)
            .withExpectedNotificationLevelsMarkedSent(BatteryNotificationLevel.FULL)
            .withExpectedNotificationMsgKey(MSG_KEY_DEVICE_BATTERY_RECHARGEABLE_FULL)
            .withExpectedNotificationPriorities(PRIORITY_MEDIUM, PRIORITY_LOW)
            .build());
   }

   @Test
   public void testRechargeableFastDrainModerateRecharge()
   {
      testBatteryNotifications(
         true,
         null,
         TestStep.builder()
            .withBatteryLevel(100)
            .withExpectedNotificationLevelsMarkedSent(BatteryNotificationLevel.FULL)
            .withExpectedNotificationMsgKey(MSG_KEY_DEVICE_BATTERY_RECHARGEABLE_FULL)
            .withExpectedNotificationPriorities(PRIORITY_MEDIUM, PRIORITY_LOW)
            .build(),
         TestStep.builder()
            .withBatteryLevel(1)
            .withExpectedNotificationLevelsMarkedSent(
               BatteryNotificationLevel.LOW, BatteryNotificationLevel.VERY_LOW,
               BatteryNotificationLevel.CRITICAL, BatteryNotificationLevel.DEAD)
            .withExpectedNotificationMsgKey(MSG_KEY_DEVICE_BATTERY_RECHARGEABLE_DEAD)
            .withExpectedNotificationPriorities(PRIORITY_MEDIUM, PRIORITY_LOW)
            .build(),
         TestStep.builder()
            .withBatteryLevel(50)
            .withExpectedNotificationLevelsMarkedSent()
            .withExpectedNotificationMsgKey()
            .withExpectedNotificationPriorities()
            .build(),
         TestStep.builder()
            .withBatteryLevel(100)
            .withExpectedNotificationLevelsMarkedSent(BatteryNotificationLevel.FULL)
            .withExpectedNotificationMsgKey(MSG_KEY_DEVICE_BATTERY_RECHARGEABLE_FULL)
            .withExpectedNotificationPriorities(PRIORITY_MEDIUM, PRIORITY_LOW)
            .build());
   }

   @Test
   public void testRechargeableFastDrainSlowRecharge()
   {
      testBatteryNotifications(
         true,
         null,
         TestStep.builder()
            .withBatteryLevel(100)
            .withExpectedNotificationLevelsMarkedSent(BatteryNotificationLevel.FULL)
            .withExpectedNotificationMsgKey(MSG_KEY_DEVICE_BATTERY_RECHARGEABLE_FULL)
            .withExpectedNotificationPriorities(PRIORITY_MEDIUM, PRIORITY_LOW)
            .build(),
         TestStep.builder()
            .withBatteryLevel(1)
            .withExpectedNotificationLevelsMarkedSent(
               BatteryNotificationLevel.LOW, BatteryNotificationLevel.VERY_LOW,
               BatteryNotificationLevel.CRITICAL, BatteryNotificationLevel.DEAD)
            .withExpectedNotificationMsgKey(MSG_KEY_DEVICE_BATTERY_RECHARGEABLE_DEAD)
            .withExpectedNotificationPriorities(PRIORITY_MEDIUM, PRIORITY_LOW)
            .build(),
         TestStep.builder()
            .withBatteryLevel(5)
            .withExpectedNotificationLevelsMarkedSent(BatteryNotificationLevel.LOW, BatteryNotificationLevel.VERY_LOW,
               BatteryNotificationLevel.CRITICAL)
            .withExpectedNotificationMsgKey()
            .withExpectedNotificationPriorities()
            .build(),
         TestStep.builder()
            .withBatteryLevel(7)
            .withExpectedNotificationLevelsMarkedSent(BatteryNotificationLevel.LOW, BatteryNotificationLevel.VERY_LOW)
            .withExpectedNotificationMsgKey()
            .withExpectedNotificationPriorities()
            .build(),
         TestStep.builder()
            .withBatteryLevel(10)
            .withExpectedNotificationLevelsMarkedSent(BatteryNotificationLevel.LOW)
            .withExpectedNotificationMsgKey()
            .withExpectedNotificationPriorities()
            .build(),
         TestStep.builder()
            .withBatteryLevel(25)
            .withExpectedNotificationLevelsMarkedSent()
            .withExpectedNotificationMsgKey()
            .withExpectedNotificationPriorities()
            .build(),
         TestStep.builder()
            .withBatteryLevel(95)
            .withExpectedNotificationLevelsMarkedSent()
            .withExpectedNotificationMsgKey()
            .withExpectedNotificationPriorities()
            .build(),
         TestStep.builder()
            .withBatteryLevel(100)
            .withExpectedNotificationLevelsMarkedSent(BatteryNotificationLevel.FULL)
            .withExpectedNotificationMsgKey(MSG_KEY_DEVICE_BATTERY_RECHARGEABLE_FULL)
            .withExpectedNotificationPriorities(PRIORITY_MEDIUM, PRIORITY_LOW)
            .build());
   }

   private void testBatteryNotifications(boolean rechargeable, String productId, TestStep... testSteps)
   {
      DevicePowerModel.setRechargeable(device, rechargeable);

      if (productId != null)
      {
         DeviceModel.setProductId(device, productId);
      }

      for (TestStep testStep : testSteps)
      {
         DevicePowerModel.setBattery(device, testStep.batteryLevel);

         handlers.forEach(handler -> handler.onDeviceBatteryChange(device, context));

         assertNotificationRequests(testStep);

         assertNotificationLevelsMarkedSent(testStep);
      }
   }

   private void assertNotificationRequests(TestStep testStep)
   {
      List<MessageBody> notificationRequests = requests.getValues();

      int expectedNotificationRequestCount = (testStep.expectedNotificationMsgKey == null ? 0 : 1) *
         ArrayUtils.getLength(testStep.expectedNotificationPriorities);

      assertThat(notificationRequests, hasSize(expectedNotificationRequestCount));

      for (int i = 0; i < notificationRequests.size(); i++)
      {
         MessageBody notificationRequest = notificationRequests.get(i);

         assertThat(notificationRequest.getMessageType(), equalTo(NotifyRequest.NAME));

         if (testStep.expectedNotificationMsgKey != null)
         {
            assertThat(NotifyRequest.getMsgKey(notificationRequest),   equalTo(testStep.expectedNotificationMsgKey));
            assertThat(NotifyRequest.getPriority(notificationRequest), equalTo(testStep.expectedNotificationPriorities[i]));
         }
      }

      requests.reset();
   }

   private void assertNotificationLevelsMarkedSent(TestStep testStep)
   {
      for (BatteryNotificationLevel batteryNotificationLevel : BatteryNotificationLevel.values())
      {
         Map<String, Date> sentNotificationsDeviceMap;
         switch (batteryNotificationLevel)
         {
            case FULL:
               sentNotificationsDeviceMap = context.model().getFullBatteryNotificationSent();
               break;
            case LOW:
               sentNotificationsDeviceMap = context.model().getLowBatteryNotificationSent();
               break;
            case VERY_LOW:
               sentNotificationsDeviceMap = context.model().getVeryLowBatteryNotificationSent();
               break;
            case CRITICAL:
               sentNotificationsDeviceMap = context.model().getCriticalBatteryNotificationSent();
               break;
            case DEAD:
               sentNotificationsDeviceMap = context.model().getDeadBatteryNotificationSent();
               break;
            default:
               throw new IllegalArgumentException(batteryNotificationLevel.name());
         }

         boolean expectedMarkedSent =
            testStep.expectedNotificationLevelsMarkedSent != null &&
            testStep.expectedNotificationLevelsMarkedSent.contains(batteryNotificationLevel);

         assertThat(batteryNotificationLevel + " should've been " + (expectedMarkedSent ? "marked sent" : "cleared"),
            sentNotificationsDeviceMap.containsKey(device.getAddress().getRepresentation()),
            equalTo(expectedMarkedSent));
      }
   }

   private enum BatteryNotificationLevel
   {
      FULL,
      LOW,
      VERY_LOW,
      CRITICAL,
      DEAD
   }

   private static class TestStep
   {
      public static Builder builder()
      {
         return new Builder();
      }

      private final Integer batteryLevel;
      private final ImmutableSet<BatteryNotificationLevel> expectedNotificationLevelsMarkedSent;
      private final String expectedNotificationMsgKey;
      private final String[] expectedNotificationPriorities;

      private TestStep(int batteryLevel, ImmutableSet<BatteryNotificationLevel> expectedNotificationLevelsMarkedSent,
         String expectedNotificationMsgKey, String[] expectedNotificationPriorities)
      {
         this.batteryLevel = batteryLevel;
         this.expectedNotificationLevelsMarkedSent = expectedNotificationLevelsMarkedSent;
         this.expectedNotificationMsgKey = expectedNotificationMsgKey;
         this.expectedNotificationPriorities = expectedNotificationPriorities;
      }

      public static class Builder
      {
         private Integer batteryLevel;
         private ImmutableSet<BatteryNotificationLevel> expectedNotificationLevelsMarkedSent;
         private String expectedNotificationMsgKey;
         private String[] expectedNotificationPriorities;

         public Builder withBatteryLevel(int batteryLevel)
         {
            this.batteryLevel = batteryLevel;
            return this;
         }

         public Builder withExpectedNotificationLevelsMarkedSent(
            BatteryNotificationLevel... expectedNotificationLevelsMarkedSent)
         {
            this.expectedNotificationLevelsMarkedSent = ImmutableSet.copyOf(expectedNotificationLevelsMarkedSent);
            return this;
         }

         public Builder withExpectedNotificationMsgKey()
         {
            return withExpectedNotificationMsgKey(null);
         }

         public Builder withExpectedNotificationMsgKey(String expectedNotificationMsgKey)
         {
            this.expectedNotificationMsgKey = expectedNotificationMsgKey;
            return this;
         }

         public Builder withExpectedNotificationPriorities(String... expectedNotificationPriorities)
         {
            this.expectedNotificationPriorities = expectedNotificationPriorities;
            return this;
         }

         public TestStep build()
         {
            return new TestStep(batteryLevel, expectedNotificationLevelsMarkedSent, expectedNotificationMsgKey,
               expectedNotificationPriorities);
         }
      }
   }
}


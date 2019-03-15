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

import static com.iris.util.TimeUtil.toFriendlyDurationSince;
import static org.apache.commons.lang3.reflect.FieldUtils.writeField;

import java.text.DateFormat;
import java.util.Date;
import java.util.Map;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.iris.common.subsystem.Subsystem;
import com.iris.common.subsystem.SubsystemTestCase;
import com.iris.core.template.TemplateService;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.capability.HubConnectionCapability;
import com.iris.messages.capability.NotificationCapability;
import com.iris.messages.capability.PlaceMonitorSubsystemCapability;
import com.iris.messages.capability.SubsystemCapability;
import com.iris.messages.event.ScheduledEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.SimpleModel;
import com.iris.messages.model.dev.DeviceConnectionModel;
import com.iris.messages.model.serv.AccountModel;
import com.iris.messages.model.subs.PlaceMonitorSubsystemModel;
import com.iris.messages.model.test.ModelFixtures;
import com.iris.platform.subsystem.placemonitor.offlinenotifications.OfflineNotificationsHandler;

public class TestPlaceMonitorOfflineNotifications extends SubsystemTestCase<PlaceMonitorSubsystemModel> {
   private static final String DFLT_NAME = "New Device";
   
   private PlaceMonitorSubsystem subsystem = null;
   OfflineNotificationsHandler handler=null;
   
   Model devconnDevice =null;
   Model owner = null;
   Model hub = null;

   TemplateService templateService = EasyMock.createMock(TemplateService.class);

   
   @Override
   protected PlaceMonitorSubsystemModel createSubsystemModel() {
      Map<String, Object> attributes = ModelFixtures.createServiceAttributes(SubsystemCapability.NAMESPACE, PlaceMonitorSubsystemCapability.NAMESPACE);
      return new PlaceMonitorSubsystemModel(new SimpleModel(attributes));
   }

   @Override
   protected Subsystem<PlaceMonitorSubsystemModel> subsystem() {
      return subsystem;
   }

   protected void start() throws Exception {
      init(subsystem);
      store.addModel(devconnDevice.toMap());
      replay();
   }
   
   @Before
   public void init() throws IllegalAccessException {
      subsystem = new PlaceMonitorSubsystem(ImmutableMap.<String, PlaceMonitorHandler>of(),new PlaceMonitorNotifications(null), templateService);
      devconnDevice = new SimpleModel(ModelFixtures.buildDeviceAttributes(DeviceConnectionCapability.NAMESPACE).create());
      devconnDevice.setAttribute(DeviceCapability.ATTR_NAME, "Test Device");
      
      handler = new OfflineNotificationsHandler(new PlaceMonitorNotifications(null));
      writeField(handler, "notificationThresholdsConfigPath", "classpath:/conf/notification-thresholds-config.xml", true);
      
      handler.init();
      subsystem.handlers=ImmutableMap.<String, PlaceMonitorHandler>of(OfflineNotificationsHandler.class.getName(),handler);
      
      owner = store.addModel(ModelFixtures.createPersonAttributes());
      AccountModel.setOwner(accountModel, owner.getId());
      hub = store.addModel(ModelFixtures.createHubAttributes());
   }

   @Test
   public void testOnDeviceOfflinePastThresholdWithNullName() throws Exception {
      long offlinetime = setDeviceOfflineSecs(93600); // 26 hours with a 24 hour threshold
      devconnDevice.setAttribute(DeviceCapability.ATTR_NAME, null);
      start();
      
      handler.onConnectivityChange(devconnDevice, context);
      MessageBody notification = requests.getValue();
      Map<String,String>msgParams=NotificationCapability.NotifyRequest.getMsgParams(notification);
      assertEquals(DFLT_NAME,msgParams.get("deviceName"));
      assertOfflineTime(offlinetime, msgParams);
   }  

   private void assertOfflineTime(long expectedTime, Map<String,String>msgParams) throws Exception {
      String actualOfflineTime = msgParams.get("offlineTime");
      DateFormat df = DateFormat.getDateTimeInstance();
//      df.setTimeZone(placeTimezone);
      Date t = df.parse(actualOfflineTime);
      assertEquals(expectedTime/1000, t.getTime()/1000);
   }

   @Test
   public void testOnDeviceOfflinePastThreshold() throws Exception {
      long offlinetime = setDeviceOfflineSecs(93600); // 26 hours with a 24 hour threshold
      start();
      
      handler.onConnectivityChange(devconnDevice, context);
      assertTrue("should contain offline device address",model.getOfflineNotificationSent().containsKey(devconnDevice.getAttribute(Capability.ATTR_ADDRESS)));

      MessageBody notification = requests.getValue();
      assertEquals(NotificationCapability.NotifyRequest.NAME,notification.getMessageType());
      assertEquals(owner.getId(),notification.getAttributes().get(NotificationCapability.NotifyRequest.ATTR_PERSONID));
      assertEquals(PlaceMonitorNotifications.MSG_KEY_DEVICE_OFFLINE,NotificationCapability.NotifyRequest.getMsgKey(notification));
      
      Map<String,String>msgParams=NotificationCapability.NotifyRequest.getMsgParams(notification);
      assertEquals(devconnDevice.getAttribute(DeviceCapability.ATTR_NAME),msgParams.get("deviceName"));
      assertEquals(devconnDevice.getAttribute(DeviceCapability.ATTR_DEVTYPEHINT),msgParams.get("deviceType"));
      assertEquals(toFriendlyDurationSince((Date)devconnDevice.getAttribute(DeviceConnectionCapability.ATTR_LASTCHANGE)),msgParams.get("offlineDuration"));
      assertOfflineTime(offlinetime, msgParams);
      MessageBody event = broadcasts.getValue();
      assertEquals(devconnDevice.getAttribute(DeviceConnectionCapability.ATTR_LASTCHANGE),((Date)event.getAttributes().get(PlaceMonitorSubsystemCapability.DeviceOfflineEvent.ATTR_LASTONLINETIME)));
      assertEquals(devconnDevice.getAddress().getRepresentation(),event.getAttributes().get(PlaceMonitorSubsystemCapability.DeviceOfflineEvent.ATTR_DEVICEADDRESS));
   }
   
   @Test
   public void testOnStartedOfflineDevices() throws Exception {
      setDeviceOfflineSecs(93600); // 26 hours with a 24 hour threshold
      start();
      handler.onStarted(context);
      assertTrue(PlaceMonitorSubsystemModel.getOfflineNotificationSent(model).containsKey(devconnDevice.getAddress().getRepresentation()));
   }
   
   @Test
   public void testOnDeviceReturnOnline() throws Exception {
      setDeviceOfflineSecs(93600); // 26 hours with a 24 hour threshold
      start();
      handler.onConnectivityChange(devconnDevice, context);

      setDeviceOnline();
      handler.onConnectivityChange(devconnDevice, context);
      assertFalse("should not contain offline device that is back online",model.getOfflineNotificationSent().containsKey(devconnDevice.getAttribute(Capability.ATTR_ADDRESS)));
      MessageBody event = broadcasts.getValues().get(1);
      assertEquals(devconnDevice.getAddress().getRepresentation(),event.getAttributes().get(PlaceMonitorSubsystemCapability.DeviceOnlineEvent.ATTR_DEVICEADDRESS));
      assertEquals(DeviceConnectionModel.getLastchange(devconnDevice),event.getAttributes().get(PlaceMonitorSubsystemCapability.DeviceOnlineEvent.ATTR_ONLINETIME));
 
      MessageBody notification = requests.getValues().get(1);
      assertEquals(NotificationCapability.NotifyRequest.NAME,notification.getMessageType());
      assertEquals(owner.getId(),notification.getAttributes().get(NotificationCapability.NotifyRequest.ATTR_PERSONID));
      assertEquals(PlaceMonitorNotifications.MSG_KEY_DEVICE_ONLINE,NotificationCapability.NotifyRequest.getMsgKey(notification));
   }   
   
   @Test
   public void testOnDeviceOfflineUnderThreshold() throws Exception {
      setDeviceOfflineSecs(21600); //offline 6 hours with a 24 hour threshhold
      start();
      handler.onConnectivityChange(devconnDevice, context);
      Date timeoutDate = context.getVariable(OfflineNotificationsHandler.OFFLINE_CHECK_TIMEOUT_KEY).as(Date.class);
      assertTrue("Should have a timeout greater ",(timeoutDate.getTime()-System.currentTimeMillis())>21500*00);

      fireScheduledEvent(OfflineNotificationsHandler.OFFLINE_CHECK_TIMEOUT_KEY);
      assertFalse("should not contain offline device address",model.getOfflineNotificationSent().containsKey(devconnDevice.getAttribute(Capability.ATTR_ADDRESS)));
      
      setDeviceOnline();
      handler.onConnectivityChange(devconnDevice, context);
      timeoutDate = context.getVariable(OfflineNotificationsHandler.OFFLINE_CHECK_TIMEOUT_KEY).as(Date.class);
      assertTrue("Should have the timeout cancelled ",timeoutDate==null);
      
   }
   
   @Test
   public void testDeviceOfflineThresholdOverride() throws Exception {
      devconnDevice.setAttribute(DeviceCapability.ATTR_PRODUCTID, "xyz");
      setDeviceOfflineSecs(300); //5 minutes with a 60 second
      start();
      handler.onConnectivityChange(devconnDevice, context);
      fireScheduledEvent(OfflineNotificationsHandler.OFFLINE_CHECK_TIMEOUT_KEY);
      assertTrue("should not contain overidden offline device address",model.getOfflineNotificationSent().containsKey(devconnDevice.getAttribute(Capability.ATTR_ADDRESS)));
   }
   
   @Test
   public void testDeviceOfflineWitholdNotification() throws Exception {
      devconnDevice.setAttribute(DeviceCapability.ATTR_PRODUCTID, "nosend");
      setDeviceOfflineSecs(300); //5 minutes with a 60 second
      start();
      handler.onConnectivityChange(devconnDevice, context);
      fireScheduledEvent(OfflineNotificationsHandler.OFFLINE_CHECK_TIMEOUT_KEY);
      assertFalse("should contain overidded offline device address",model.getOfflineNotificationSent().containsKey(devconnDevice.getAttribute(Capability.ATTR_ADDRESS)));
   }
   
   @Test //need hub lastchange time
   public void testHubOffline() throws Exception {
      start();
      setHubOfflineSecs(4000000);
      handler.onHubConnectivityChange(hub,context);
      fireScheduledEvent(OfflineNotificationsHandler.OFFLINE_CHECK_TIMEOUT_KEY);
      assertTrue("should contain an offline hub address",model.getOfflineNotificationSent().containsKey(hub.getAddress().getRepresentation()));
   }

   @Test
   public void testPrune() throws Exception {
      store.addModel(devconnDevice.toMap());
      Model device2 = new SimpleModel(ModelFixtures.buildDeviceAttributes(DeviceConnectionCapability.NAMESPACE).create());
      store.addModel(device2.toMap());
      start();

      model.setOfflineNotificationSent(ImmutableMap.<String, Date>of(devconnDevice.getAddress().getRepresentation(), new Date(),device2.getAddress().getRepresentation(), new Date()));
      store.removeModel(devconnDevice.getAddress());
      
      assertFalse("should not contain removed device",model.getOfflineNotificationSent().containsKey(devconnDevice.getAddress().getRepresentation()));
      assertTrue("should contain non removed device",model.getOfflineNotificationSent().containsKey(device2.getAddress().getRepresentation()));

   }   
   
   private long setDeviceOfflineSecs(long timeInSecs){
      devconnDevice.setAttribute(DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_OFFLINE);
      long offlinetime = System.currentTimeMillis()-timeInSecs*1000;
      devconnDevice.setAttribute(DeviceConnectionCapability.ATTR_LASTCHANGE, offlinetime);
      store.updateModel(devconnDevice.getAddress(), devconnDevice.toMap());
      return offlinetime;
   }
   
   private void setHubOfflineSecs(long timeInSecs){
      hub.setAttribute(HubConnectionCapability.ATTR_STATE, HubConnectionCapability.STATE_OFFLINE);
      hub.setAttribute(HubConnectionCapability.ATTR_LASTCHANGE, System.currentTimeMillis()-timeInSecs*1000);
      store.updateModel(hub.getAddress(), hub.toMap());
   }
   
   private void setDeviceOnline(){
      devconnDevice.setAttribute(DeviceConnectionCapability.ATTR_STATE, DeviceConnectionCapability.STATE_ONLINE);
      devconnDevice.setAttribute(DeviceConnectionCapability.ATTR_LASTCHANGE, System.currentTimeMillis());
      store.updateModel(devconnDevice.getAddress(), devconnDevice.toMap());
   }
   private ScheduledEvent fireScheduledEvent(String eventKey){
      Date date = new Date();
      context.setVariable(eventKey, new Date(System.currentTimeMillis()+1000));
      ScheduledEvent event = new ScheduledEvent(model.getAddress(), date.getTime());
      handler.onScheduledEvent(event, context);
      return event;
   }
   
}


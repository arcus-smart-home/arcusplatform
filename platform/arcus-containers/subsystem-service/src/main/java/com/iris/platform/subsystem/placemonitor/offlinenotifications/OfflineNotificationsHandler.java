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
package com.iris.platform.subsystem.placemonitor.offlinenotifications;

import static com.iris.messages.capability.HubAlarmCapability.ATTR_LASTDISARMEDTIME;
import static com.iris.messages.capability.HubAlarmCapability.STATE_ACTIVE;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.PostConstruct;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.SubsystemUtils;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AlarmIncidentCapability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.capability.HubConnectionCapability;
import com.iris.messages.capability.PlaceMonitorSubsystemCapability;
import com.iris.messages.event.ModelReportEvent;
import com.iris.messages.event.ModelReportEvent.ValueChange;
import com.iris.messages.event.ScheduledEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.hub.HubConnectionModel;
import com.iris.messages.model.serv.HubAlarmModel;
import com.iris.messages.model.subs.PlaceMonitorSubsystemModel;
import com.iris.platform.subsystem.placemonitor.BasePlaceMonitorHandler;
import com.iris.platform.subsystem.placemonitor.PlaceMonitorNotifications;
import com.iris.platform.subsystem.placemonitor.config.WatchedJAXBConfigFileReference;

@Singleton
public class OfflineNotificationsHandler extends BasePlaceMonitorHandler {
   private static final int TIMEOUT_PADDING = 5000;  //Make sure schedule the timeout for after the grace period
   private static final int MIN_TIMEOUT_PERIOD = 10000;

   public final static String OFFLINE_CHECK_TIMEOUT_KEY = "offline.check.timeout";

   private PlaceMonitorNotifications notifier;

   @Inject(optional = true)
   @Named("notification.thresholds.config.path")
   private String notificationThresholdsConfigPath = "conf/notification-thresholds-config.xml";

   private AtomicReference<NotificationThresholdsConfig> notificationThresholdsConfig;
   
   @Inject
   public OfflineNotificationsHandler(PlaceMonitorNotifications notifier) {
      this.notifier = notifier;
   }
   
   @PostConstruct
   public void init() {
      notificationThresholdsConfig = new WatchedJAXBConfigFileReference<NotificationThresholdsConfig>(notificationThresholdsConfigPath, NotificationThresholdsConfig.class).getReference();
   }
   
   @Override
   public void onDeviceRemoved(Model model, SubsystemContext<PlaceMonitorSubsystemModel> context) {
      pruneAddressDateMap(context, PlaceMonitorSubsystemCapability.ATTR_OFFLINENOTIFICATIONSENT);
   }

   @Override
   public void onStarted(SubsystemContext<PlaceMonitorSubsystemModel> context) {
      setIfNull(context.model(), PlaceMonitorSubsystemCapability.ATTR_OFFLINENOTIFICATIONSENT, ImmutableMap.<String, Date>of());
      checkOfflineDevices(context);
   }

   @Override
   public void onScheduledEvent(ScheduledEvent event, SubsystemContext<PlaceMonitorSubsystemModel> context) {

      if (SubsystemUtils.isMatchingTimeout(event, context, OFFLINE_CHECK_TIMEOUT_KEY)){
         SubsystemUtils.clearTimeout(context, OFFLINE_CHECK_TIMEOUT_KEY);
         checkOfflineDevices(context);
         return;
      }
   }

   @Override
   public void onConnectivityChange(Model model, SubsystemContext<PlaceMonitorSubsystemModel> context) {
      if (model.getAttribute(DeviceConnectionCapability.ATTR_STATE).equals(DeviceConnectionCapability.STATE_ONLINE)){
         onOnlineDevice(model, context);
      }
      else{
         onOfflineDevice(model, context);
      }
   }

   private void onOfflineDevice(Model device, SubsystemContext<PlaceMonitorSubsystemModel> context) {
      context.logger().debug("Device Offline [{}]", device);
      checkOfflineDevices(context);
   }



   private void onOnlineDevice(Model device, SubsystemContext<PlaceMonitorSubsystemModel> context) {
      context.logger().debug("Device Online [{}]", device);
      handleOnlineDevice(new ConnectionCapableDevice(device), context);
   }

   private void checkOfflineDevices(SubsystemContext<PlaceMonitorSubsystemModel> context) {
      Long nextCheck = Long.MAX_VALUE;
      List<ConnectionCapableDevice>models=new ArrayList<ConnectionCapableDevice>();
      
      for(Model dev:context.models().getModelsByType(DeviceCapability.NAMESPACE)){
         if(dev.supports(DeviceConnectionCapability.NAMESPACE)){
            models.add(new ConnectionCapableDevice(dev));
         }
      }      
      
      for(Model hub:context.models().getModelsByType(HubCapability.NAMESPACE)){
         models.add(new ConnectionCapableDevice(hub));
      }
      
      for (ConnectionCapableDevice device : models){
         if(device.getState() == null || device.getLastChange()==null){
            continue;
         }
         if (device.isOffline()){
            handleOfflineDevice(device, context);
            if(!hasAlreadySentNotification(device, context)){
               nextCheck = Math.min(nextCheck, millisUntilOfflineNotification(device));
            }
         }
      }
      SubsystemUtils.clearTimeout(context, OFFLINE_CHECK_TIMEOUT_KEY);
      if(nextCheck<=0){
         context.logger().debug("found a negative nextcheck. withholding check of {}",nextCheck);
      }
      else if(!nextCheck.equals(Long.MAX_VALUE)){
         context.logger().debug("offline devices encountered. scheduling an offline notifcation check at {} ({} ms)",new Date(System.currentTimeMillis()+nextCheck),nextCheck);
         SubsystemUtils.setTimeout(Math.max(MIN_TIMEOUT_PERIOD,nextCheck+TIMEOUT_PADDING), context, OFFLINE_CHECK_TIMEOUT_KEY);
      }else{
         context.logger().debug("no devices were found offline. all timeouts have been cancelled and none will be rescheduled.");
      }
   }

   @Override
   public void onHubConnectivityChange(Model hub, SubsystemContext<PlaceMonitorSubsystemModel> context) {
      context.logger().debug("hub connectivity change - {}", hub);
      String state = HubConnectionModel.getState(hub);
      switch(state){
         case HubConnectionCapability.STATE_OFFLINE:
            checkOfflineDevices(context);
            break;
         case HubConnectionCapability.STATE_ONLINE:
            String hubAlarmState = HubAlarmModel.getState(hub);
            // Older hubs that don't have hubalarm:state are counted non-ACTIVE
            if (hubAlarmState == null || !hubAlarmState.equals(STATE_ACTIVE))
            {
               handleOnlineDevice(new ConnectionCapableDevice(hub), context);
            }
            break;
         default:
            break;
      }
   }
   
   @Override
   public void onHubReportEvent(ModelReportEvent event, Model hub, SubsystemContext<PlaceMonitorSubsystemModel> context)
   {
      Boolean reconnectReport = HubAlarmModel.getReconnectReport(hub);

      if (reconnectReport != null && reconnectReport == true)
      {
         context.logger().debug("hub report event - {} for hub {}", event, hub);

         Map<String, ValueChange> changes = event.getChanges();

         if (!shouldIgnoreReport(changes))
         {
            Address offlineDisarmedBy = getOfflineDisarmedBy(changes, hub);
            Address offlineIncident = getOfflineIncident(changes, hub);

            handleOnlineDevice(new ConnectionCapableDevice(hub, offlineDisarmedBy, offlineIncident), context);
         }
      }
   }
   
   /*
    * Seems like there are currently two separate reports sent by the hub with hubalarm:reconnectReport == true.  The
    * first one contains entirely non-hubalarm attributes, and should be ignored so that we don't emit more than one
    * HubOnlineEvent (which could in turn cause duplicate notifications and history entries).
    */
   private boolean shouldIgnoreReport(Map<String, ValueChange> changes)
   {
      return changes.keySet().stream().anyMatch(key -> !key.startsWith(HubAlarmModel.NAMESPACE + ":"));
   }
   
   private Address getOfflineDisarmedBy(Map<String, ValueChange> changes, Model hub)
   {
      /*
       * hubalarm:lastDisarmedTime is always present in changes if there was an offline disarm, whereas
       * hubalarm:lastDisarmedBy is only present if there was an offline disarm *and* the person who did it is different
       * from that of the previous disarm.  (The hub Model contains the most up-to-date view of the attributes, including
       * the new values in changes.)
       */
      String offlineDisarmedByString = changes.containsKey(ATTR_LASTDISARMEDTIME) ?
         HubAlarmModel.getLastDisarmedBy(hub) : null;

      Address offlineDisarmedBy = !isEmpty(offlineDisarmedByString) ?
         Address.fromString(offlineDisarmedByString) : null;

      return offlineDisarmedBy;
   }
   
   private Address getOfflineIncident(Map<String, ValueChange> changes, Model hub)
   {
      /*
       * Getting hubalarm:currentIncident from the hub Model means we will get any current incident regardless of
       * whether it started before or after the hub went offline (because the hub Model contains the most up-to-date
       * view of the attributes, including the new values in changes).
       */
      String offlineIncidentIdString = HubAlarmModel.getCurrentIncident(hub);

      Address offlineIncident = !isEmpty(offlineIncidentIdString) ?
         Address.platformService(UUID.fromString(offlineIncidentIdString), AlarmIncidentCapability.NAMESPACE) : null;

      return offlineIncident;
   }
   
   public void handleOnlineDevice(ConnectionCapableDevice device,SubsystemContext<PlaceMonitorSubsystemModel> context){

      if (removeFromSentList(device.getAddress(), context))
      {
         notifyOwnerOnline(device, context);

         if (device.isHub())
         {
            broadcastHubOnlineEvent(device, context);
         }
         else
         {
            broadcastDeviceOnlineEvent(device, context);
         }
      }
      else
      {
         // If no notification was sent but there was an offline disarm, broadcast HubOnlineEvent anyway
         if (device.isHub() && device.getHubOfflineDisarmedBy() != null)
         {
            broadcastHubOnlineEvent(device, context);
         }
      }

      checkOfflineDevices(context);
   }
   
   public void handleOfflineDevice(ConnectionCapableDevice device,SubsystemContext<PlaceMonitorSubsystemModel> context){
      if(shouldNotifyOwner(device, context)){
         notifyOwnerOffline(device, context);
         addAddressAndTimeToMap(context.model(), PlaceMonitorSubsystemCapability.ATTR_OFFLINENOTIFICATIONSENT, device.getAddress(), new Date());
         if (device.isHub())
         {
            broadcastHubOfflineEvent(device, context);
         }
         else
         {
            broadcastDeviceOfflineEvent(device, context);
         }
      }
   }
   
   private void notifyOwnerOffline(ConnectionCapableDevice device, SubsystemContext<PlaceMonitorSubsystemModel> context){
      
      if(device.isHub()){
         notifier.sendHubOffline(device.model(), device.getLastChange(), context);
      }
      else{
         notifier.sendDeviceOffline(device.model(), device.getLastChange(), context);
      }
   }
   
   private void notifyOwnerOnline(ConnectionCapableDevice device, SubsystemContext<PlaceMonitorSubsystemModel> context){
      if(device.isHub()){
         notifier.sendHubOnline(device.model(), device.getLastChange(), device.getHubOfflineDisarmedBy(), device.getHubOfflineIncident(), context);
      }
      else{
         notifier.sendDeviceOnline(device.model(), device.getLastChange(), context);
      }
   }
   
   private boolean hasAlreadySentNotification(ConnectionCapableDevice device,SubsystemContext<PlaceMonitorSubsystemModel> context){
      if(context.model().getOfflineNotificationSent().containsKey(device.getAddress().getRepresentation())){
         return true;
      }
      return false;
   }
   private boolean shouldNotifyOwner(ConnectionCapableDevice device,SubsystemContext<PlaceMonitorSubsystemModel> context){
	  boolean retValue = false;
	  long thresholdMs = findDeviceWaitTimeMilli(device);

      if(hasAlreadySentNotification(device,context) || thresholdMs<=0){
    	  retValue = false;
      }else {
    	  long offlineMs=System.currentTimeMillis()-device.getLastChange().getTime();
          context.logger().debug("device has been offline for {} ms with a threshold of {} ms",offlineMs,thresholdMs);
          if(offlineMs > thresholdMs){
        	  retValue = true;
          }
      }
      
      return retValue;
   }
   
   private long millisUntilOfflineNotification(ConnectionCapableDevice  device){
      long offlineMs=System.currentTimeMillis()-device.getLastChange().getTime();
      long thresholdMs = findDeviceWaitTimeMilli(device);
      return thresholdMs-offlineMs;
   }
   
   private long findDeviceWaitTimeMilli(ConnectionCapableDevice device){
      long defaultDeviceWaitTime=3600;
      
      if(device.isHub()){
         defaultDeviceWaitTime = notificationThresholdsConfig.get().getHubOfflineTimeoutSec();
      }
      else{
         defaultDeviceWaitTime = notificationThresholdsConfig.get().getDeviceOfflineTimeoutSec(device.getProductId());
      }
      return defaultDeviceWaitTime*1000;
   }
   
   private boolean removeFromSentList(Address address,SubsystemContext<PlaceMonitorSubsystemModel> context){
      return removeAddressFromAddressDateMap(context.model(), PlaceMonitorSubsystemCapability.ATTR_OFFLINENOTIFICATIONSENT, address);
   }
   
   private void broadcastHubOfflineEvent(ConnectionCapableDevice device, SubsystemContext<PlaceMonitorSubsystemModel> context){
      MessageBody body = PlaceMonitorSubsystemCapability.HubOfflineEvent.builder()
         .withHubAddress(device.getAddress().getRepresentation())
         .withLastOnlineTime(device.getLastChange())
         .build();
      context.broadcast(body);
   }
   
   private void broadcastHubOnlineEvent(ConnectionCapableDevice device, SubsystemContext<PlaceMonitorSubsystemModel> context){
      MessageBody body = PlaceMonitorSubsystemCapability.HubOnlineEvent.builder()
         .withHubAddress(device.getAddress().getRepresentation())
         .withOnlineTime(device.getLastChange())
         .withDisarmedBy(device.getHubOfflineDisarmedBy() == null ? null : device.getHubOfflineDisarmedBy().toString())
         .withOfflineIncident(device.getHubOfflineIncident() == null ? null : device.getHubOfflineIncident().toString())
         .build();
      context.broadcast(body);
   }
   
   private void broadcastDeviceOfflineEvent(ConnectionCapableDevice device, SubsystemContext<PlaceMonitorSubsystemModel> context){
      MessageBody body = PlaceMonitorSubsystemCapability.DeviceOfflineEvent.builder()
         .withDeviceAddress(device.getAddress().getRepresentation())
         .withLastOnlineTime(device.getLastChange())
         .build();
      context.broadcast(body);
   }
   
   private void broadcastDeviceOnlineEvent(ConnectionCapableDevice device, SubsystemContext<PlaceMonitorSubsystemModel> context){
      MessageBody body = PlaceMonitorSubsystemCapability.DeviceOnlineEvent.builder()
         .withDeviceAddress(device.getAddress().getRepresentation())
         .withOnlineTime(device.getLastChange())
         .build();
      context.broadcast(body);
   }
}


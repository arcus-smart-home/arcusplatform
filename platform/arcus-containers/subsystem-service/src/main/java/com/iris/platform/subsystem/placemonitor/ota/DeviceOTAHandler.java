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
package com.iris.platform.subsystem.placemonitor.ota;

import java.util.Optional;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.SubsystemUtils;
import com.iris.firmware.ota.DeviceOTAFirmwareResolver;
import com.iris.firmware.ota.DeviceOTAFirmwareResponse;
import com.iris.messages.address.Address;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.DeviceOtaCapability;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.capability.PlaceMonitorSubsystemCapability;
import com.iris.messages.event.ScheduledEvent;
import com.iris.messages.listener.annotation.OnValueChanged;
import com.iris.messages.model.Model;
import com.iris.messages.model.dev.DeviceModel;
import com.iris.messages.model.dev.DeviceOtaModel;
import com.iris.messages.model.serv.PlaceModel;
import com.iris.messages.model.subs.PlaceMonitorSubsystemModel;
import com.iris.model.query.expression.ExpressionCompiler;
import com.iris.platform.subsystem.placemonitor.BasePlaceMonitorHandler;

@Singleton
public class DeviceOTAHandler extends BasePlaceMonitorHandler {

   public static final String FIRMWARE_RETRY_TIMEOUT_KEY = "FIRMWARE_RETRY_TIMEOUT_KEY";
   public final static String QUERY_FAILING_OTA = String.format(  "base:caps contains '%s' AND devota:status == '%s'",
                                                                  DeviceOtaCapability.NAMESPACE,DeviceOtaCapability.STATUS_FAILED);
   static final Predicate<Model> HAS_FAILED_OTA = ExpressionCompiler.compile(QUERY_FAILING_OTA);

   protected DeviceOTAFirmwareResolver otaResolver;

   @Inject
   public DeviceOTAHandler(DeviceOTAFirmwareResolver otaResolver) {
      this.otaResolver = otaResolver;
   }

   @Override
   public void onAdded(SubsystemContext<PlaceMonitorSubsystemModel> context) {
      context.model().setUpdatedDevices(ImmutableSet.<String>of());
   }

   @OnValueChanged(attributes = { DeviceOtaCapability.ATTR_CURRENTVERSION })
   public void onVersionChange(Model device, SubsystemContext<PlaceMonitorSubsystemModel> context) {
      context.logger().debug("OTA reporting version [{}]", device);
      if(!context.model().getUpdatedDevices().contains(device.getAddress().getRepresentation())){
         provisionDevice(device, context);
      }
   }
   
   @OnValueChanged(attributes = { DeviceOtaCapability.ATTR_STATUS})
   public void onStatusChange(Model device, SubsystemContext<PlaceMonitorSubsystemModel> context) {
      context.logger().debug("OTA status changin [{}]", device);
      if(DeviceOtaCapability.STATUS_FAILED.equals(DeviceOtaModel.getStatus(device))){
         DeviceOTAFirmwareResponse fwInfo = getFirmwareInfo(context,device);
         if(DeviceOtaModel.getRetryCount(device)==null){
            context.logger().warn("no retry support in driver for device [{}]", device);
            return;
         }
         int retryCount = DeviceOtaModel.getRetryCount(device);
         if(retryCount<fwInfo.getRetryAttempts()){
            removeAddressFromSet(device.getAddress().getRepresentation(),PlaceMonitorSubsystemCapability.ATTR_UPDATEDDEVICES,context.model());
            scheduleNextUpgrade(device, context);
         }
         else{
            context.logger().warn("MAX retries [{}] reached for OTA uprade of device [{}]", retryCount, device);
         }
        
      }
   }   

   private void scheduleNextUpgrade(Model device, SubsystemContext<PlaceMonitorSubsystemModel> context){
      int retryCount = DeviceOtaModel.getRetryCount(device,0)+1;
      DeviceOTAFirmwareResponse fwInfo = getFirmwareInfo(context,device);
      long nextTimeout = (fwInfo.getRetryIntervalMinutes()*60000)*retryCount;

      //TODO: why do backoffs seem broken?
      //long nextTimeout = Backoffs.linear()
      //      .attempt(retryCount)
      //      .delay(fwInfo.getRetryIntervalMinutes(), TimeUnit.MINUTES)
      //      .random(0, TimeUnit.SECONDS)
      //      .initial(0, TimeUnit.SECONDS)
      //      .build()
      //      .getNextDelay(TimeUnit.MILLISECONDS);
      context.logger().debug("scheduleding next retry for [{}]", nextTimeout);
      SubsystemUtils.setTimeout(nextTimeout, context,FIRMWARE_RETRY_TIMEOUT_KEY);
   }
   
   
   @Override
   public void onScheduledEvent(ScheduledEvent event, SubsystemContext<PlaceMonitorSubsystemModel> context) {
      if(SubsystemUtils.isMatchingTimeout(event, context, FIRMWARE_RETRY_TIMEOUT_KEY)){
         context.logger().debug("retry firmware timeout reached");
         for(Model failedDevice:context.models().getModels(HAS_FAILED_OTA)){
            DeviceOTAFirmwareResponse fwInfo = getFirmwareInfo(context,failedDevice);
            int retryCount = DeviceOtaModel.getRetryCount(failedDevice,0);
            //TODO: Check if there are multiple simultaneous failing, we need to check to make sure this is the right one for the timeout.
            if(retryCount<fwInfo.getRetryAttempts()){
               provisionDevice(failedDevice, context);   
            }
         }
      }
   }

   @Override
   public void onStarted(SubsystemContext<PlaceMonitorSubsystemModel> context) {
      setIfNull(context.model(), PlaceMonitorSubsystemCapability.ATTR_UPDATEDDEVICES, ImmutableSet.<String>of());
      pruneAddressSetForRemovedDevices(context, PlaceMonitorSubsystemCapability.ATTR_UPDATEDDEVICES);
      Set<String>provisionedDevices = context.model().getUpdatedDevices();
      for(Model model:context.models().getModelsByType(DeviceCapability.NAMESPACE)){
         if(!provisionedDevices.contains(model.getAddress().getRepresentation())){
            provisionDevice(model,context);
         }
      }
   }

   public void onDeviceAdded(Model model,SubsystemContext<PlaceMonitorSubsystemModel> context){
      provisionDevice(model, context);
   }
   
   public void onDeviceRemoved(Model model,SubsystemContext<PlaceMonitorSubsystemModel> context){
      removeAddressFromSet(model.getAddress().getRepresentation(), PlaceMonitorSubsystemCapability.ATTR_UPDATEDDEVICES, context.model());
   }
   
   public void provisionDevice(Model model,SubsystemContext<PlaceMonitorSubsystemModel> context) {
      if(issueMinimumFirmwareVersion(model,context))
      {
         addAddressToSet(model.getAddress().getRepresentation(), PlaceMonitorSubsystemCapability.ATTR_UPDATEDDEVICES, context.model());
      }
   }
   
   private boolean issueMinimumFirmwareVersion(Model model,SubsystemContext<PlaceMonitorSubsystemModel> context){
      boolean issued = false;
      Model place = getPlace(context);
      String currentVersion = DeviceOtaModel.getCurrentVersion(model);
      String productId = DeviceModel.getProductId(model);

      if(!model.supports(DeviceOtaCapability.NAMESPACE) ||  currentVersion==null || productId==null){
         context.logger().debug("DeviceOTA or driver not reporting firmware version {}",model);
         return false;
      }
      
      DeviceOTAFirmwareResponse resp = otaResolver.resolve(Optional.ofNullable(PlaceModel.getPopulation(place)), productId, currentVersion);
      if(resp.isUpgrade()){
         provisionNewFirmware(model.getAddress(), resp, context);
         issued=true;
      }
      return issued;
   }
   private Model getPlace(SubsystemContext<PlaceMonitorSubsystemModel> context){
      return context.models().getModelByAddress(Address.platformService(context.getPlaceId(),PlaceCapability.NAMESPACE));
   }
   
   private String getPopulation(SubsystemContext<PlaceMonitorSubsystemModel> context){
      return PlaceModel.getPopulation(getPlace(context));
   }
   
   private DeviceOTAFirmwareResponse getFirmwareInfo(SubsystemContext<PlaceMonitorSubsystemModel> context,Model model){
      String currentVersion = DeviceOtaModel.getCurrentVersion(model);
      String productId = DeviceModel.getProductId(model);
      DeviceOTAFirmwareResponse resp = otaResolver.resolve(Optional.ofNullable(getPopulation(context)), productId, currentVersion);
      return resp;
   }
   
   private void provisionNewFirmware(Address deviceAddress, DeviceOTAFirmwareResponse otaInfo,SubsystemContext<PlaceMonitorSubsystemModel> context){
      context.request(deviceAddress, 
            DeviceOtaCapability.FirmwareUpdateRequest.builder()
            .withUrl(otaInfo.getTargetImage())
            .withPriority(DeviceOtaCapability.FirmwareUpdateRequest.PRIORITY_URGENT)
            .withMd5(otaInfo.getMd5())
            .build());
   }

}


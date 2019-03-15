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

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.messages.address.Address;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.event.ModelReportEvent;
import com.iris.messages.event.ScheduledEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.subs.PlaceMonitorSubsystemModel;
import com.iris.util.TypeMarker;

public class BasePlaceMonitorHandler implements PlaceMonitorHandler {

   @Override
   public void onDeviceBatteryChange(Model hub, SubsystemContext<PlaceMonitorSubsystemModel> context) {
      //noopp
   }

   @Override
   public void onAdded(SubsystemContext<PlaceMonitorSubsystemModel> context) {
      // no-op
   }

   @Override
   public void onStarted(SubsystemContext<PlaceMonitorSubsystemModel> context) {
      // no-op
   }

   @Override
   public void onDeviceAdded(Model model, SubsystemContext<PlaceMonitorSubsystemModel> context) {
      // no-op
   }
   
   @Override
   public void onDeviceRemoved(Model model,SubsystemContext<PlaceMonitorSubsystemModel> context){
      //no-op
   }
   
   
   @Override
   public void onScheduledEvent(ScheduledEvent event, SubsystemContext<PlaceMonitorSubsystemModel> context) {
      //no-op
   }

   @Override
   public void onConnectivityChange(Model model, SubsystemContext<PlaceMonitorSubsystemModel> context) {
      //no-op
      
   }
   @Override
   public void onHubConnectivityChange(Model hub,SubsystemContext<PlaceMonitorSubsystemModel> context){
      //no-op
   }
   @Override
   public void onHubReportEvent(ModelReportEvent event, Model hub, SubsystemContext<PlaceMonitorSubsystemModel> context) {
      //no-op
   }

   protected boolean setIfNull(Model model, String attribute, Object value) {
      Object currentValue = model.getAttribute(attribute);
      if(currentValue == null) {
         model.setAttribute(attribute, value);
         return true;
      }
      else {
         return false;
      }
   }
   protected boolean addAddressToSet(String address, String attribute, Model model) {
      Set<String> devices = model.getAttribute(TypeMarker.setOf(String.class), attribute).get();
      // skip a copy if we can
      if(devices.contains(address)) {
         return false;
      }
      
      Set<String> newDevices = new HashSet<>(devices);
      newDevices.add(address);
      model.setAttribute(attribute, newDevices);
      return true;
   }
   
   protected boolean removeAddressFromSet(String address, String attribute, Model model) {
      Set<String> devices = model.getAttribute(TypeMarker.setOf(String.class), attribute).get();
      if(!devices.contains(address)) {
         return false;
      }
      
      Set<String> newDevices = new HashSet<>(devices);
      newDevices.remove(address);
      model.setAttribute(attribute, newDevices);
      return true;
   }
   
   protected void addAddressAndTimeToMap(PlaceMonitorSubsystemModel model,String attr,Address address,Date time){
      Map<String,Date>map=model.getAttribute(TypeMarker.mapOf(String.class, Date.class), attr).orNull();
      if(map==null){
         throw new RuntimeException("address time map not found or not initialized");
      }
      Map<String,Date>newMap=new HashMap<String, Date>(map);
      newMap.put(address.getRepresentation(), time);
      model.setAttribute(attr, newMap);
   }
   protected boolean removeAddressFromAddressDateMap(PlaceMonitorSubsystemModel model,String attr,Address address){
      Map<String,Date>map=getAddressDateMapAttribute(model, attr);
      Map<String,Date>newMap=new HashMap<String, Date>(map);
      Date value = newMap.remove(address.getRepresentation());
      model.setAttribute(attr, newMap);
      return value!=null?true:false;
   }
   //will prune the list for all devices that were removed after a notification was sent
   protected void pruneAddressDateMap(SubsystemContext<PlaceMonitorSubsystemModel> context, String attr){
      Map<String,Date>map=getAddressDateMapAttribute(context.model(), attr);
      Map<String,Date>prunedList=new HashMap<String,Date>(map);
      Map<String,Date>devices=new HashMap<String, Date>();
      for(Model device:context.models().getModelsByType(DeviceCapability.NAMESPACE)){
         devices.put(device.getAddress().getRepresentation(), new Date());
      }
      for(String key:map.keySet()){
         if(!devices.containsKey(key)){
            prunedList.remove(key);
         }
      }
      context.model().setAttribute(attr, prunedList);
   }
   protected void pruneAddressSetForRemovedDevices(SubsystemContext<PlaceMonitorSubsystemModel> context, String attr){
      Set<String>addresses=context.model().getAttribute(TypeMarker.setOf(String.class),attr).or(ImmutableSet.of());
      Set<String>newAddresses=new HashSet<String>(addresses);
      boolean modified = false;
      for(String address:addresses){
         if(context.models().getModelByAddress(Address.fromString(address))==null){
            newAddresses.remove(address);
            modified=true;
         }
      }
      if(modified){
         context.model().setAttribute(attr, newAddresses);
      }
   }
   
   private Map<String,Date>getAddressDateMapAttribute(PlaceMonitorSubsystemModel model, String attr){
      Map<String,Date>map=model.getAttribute(TypeMarker.mapOf(String.class, Date.class), attr).orNull();
      if(map==null){
         throw new RuntimeException("address time map not found or not initialized");
      }
      return map;
   }
   
   

}


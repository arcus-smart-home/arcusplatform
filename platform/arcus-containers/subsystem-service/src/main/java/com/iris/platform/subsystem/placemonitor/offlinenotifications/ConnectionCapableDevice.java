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

import java.util.Date;

import com.iris.messages.address.Address;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.capability.HubConnectionCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.dev.DeviceConnectionModel;
import com.iris.messages.model.dev.DeviceModel;
import com.iris.messages.model.hub.HubConnectionModel;
import com.iris.messages.model.hub.HubModel;

public class ConnectionCapableDevice {

   private Model model;
   private Address hubOfflineDisarmedBy;
   private Address hubOfflineIncident;
   
   public ConnectionCapableDevice(Model model) {
      this.model = model;
   }
   
   public ConnectionCapableDevice(Model model, Address hubOfflineDisarmedBy, Address hubOfflineIncident) {
      this.model = model;
      this.hubOfflineDisarmedBy = hubOfflineDisarmedBy;
      this.hubOfflineIncident = hubOfflineIncident;
   }
   
   public Address getAddress() {
      return model.getAddress();
   }

   public Date getLastChange() {
      if(isHub()){
         return HubConnectionModel.getLastchange(model, null);
      }
      return DeviceConnectionModel.getLastchange(model,null);
   }

   public String getName() {
      if(isHub()){
         return HubModel.getName(model);
      }
      return DeviceModel.getName(model);
   }
   
   public String getState() {
      if(isHub()){
         return HubConnectionModel.getState(model);
      }
      return DeviceConnectionModel.getState(model);
   }
   
   public Model model(){
      return model;
   }
   
   public boolean isHub(){
      return model.supports(HubCapability.NAMESPACE);
   }
   
   public boolean isDevice(){
      return model.supports(DeviceCapability.NAMESPACE);
   }
   
   public boolean isConnectionCapable(){
      return model.supports(DeviceConnectionCapability.NAMESPACE) || model.supports(HubConnectionCapability.NAMESPACE);
   }
   
   public String getProductId(){
      if(isDevice()){
         return DeviceConnectionModel.getProductId(model);
      }
      return null;
   }
   public boolean isOffline(){
      return "OFFLINE".equals(getState());
   }   
   
   public Address getHubOfflineDisarmedBy()
   {
      return hubOfflineDisarmedBy;
   }

   public Address getHubOfflineIncident()
   {
      return hubOfflineIncident;
   }
}


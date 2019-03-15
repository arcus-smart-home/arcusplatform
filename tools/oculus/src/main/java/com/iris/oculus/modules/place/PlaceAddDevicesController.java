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
/**
 * 
 */
package com.iris.oculus.modules.place;

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.iris.client.ClientRequest;
import com.iris.client.IrisClient;
import com.iris.client.IrisClientFactory;
import com.iris.client.capability.Place;
import com.iris.client.event.ListenerRegistration;
import com.iris.client.model.DeviceModel;
import com.iris.client.model.ModelAddedEvent;
import com.iris.client.model.ModelChangedEvent;
import com.iris.client.model.ModelDeletedEvent;
import com.iris.client.model.PlaceModel;
import com.iris.client.model.PlaceMonitorSubsystemModel;
import com.iris.client.model.Store;
import com.iris.client.model.SubsystemModel;
import com.iris.messages.address.PlatformServiceAddress;
import com.iris.oculus.Oculus;
import com.iris.oculus.util.CallbackRegistration;

/**
 * 
 */
@Singleton
public class PlaceAddDevicesController {
   private IrisClient client;
   private Store<DeviceModel> devices;
   private Store<SubsystemModel> subStore;
   private Store<PlaceModel> places;
   private Optional<PlaceModel> place;
   
   private CallbackRegistration<Callback> callbacks = new CallbackRegistration<>(Callback.class);
   
   private int pairingTimeoutMs = 300000;
   private boolean pairing = false;
   
   @Inject
   public PlaceAddDevicesController(IrisClient client) {
   
      this.places=IrisClientFactory.getStore(PlaceModel.class);
      places.addListener(ModelAddedEvent.class, (event) -> onPlaceAdded(event));
      places.addListener(ModelDeletedEvent.class, (event) -> onPlaceRemoved(event));
      
      this.subStore = IrisClientFactory.getStore(SubsystemModel.class);
      this.subStore.addListener(ModelChangedEvent.class, (event) -> {
         onPlaceMonPairingChanged(event);
      });

      this.client = client;
      this.devices = IrisClientFactory.getStore(DeviceModel.class);
      this.devices.addListener(ModelAddedEvent.class, (event) -> onDeviceAdded(event));
   }

   protected void onPlaceAdded(ModelAddedEvent event) {
      place = Optional.of((PlaceModel) event.getModel());
   }
   protected void onPlaceRemoved(ModelDeletedEvent event) {
      place = Optional.empty();
   } 
   
   protected void onPlaceMonPairingChanged(ModelChangedEvent event) {
      Map<String,Object>changes=event.getChangedAttributes();
      if(changes.containsKey(PlaceMonitorSubsystemModel.ATTR_PAIRINGSTATE)){
         if(PlaceMonitorSubsystemModel.PAIRINGSTATE_PAIRING.equals(changes.get(PlaceMonitorSubsystemModel.ATTR_PAIRINGSTATE))){
            pairing = true;
            callbacks.delegate().showPairing();   
         }
         else{
            pairing = false;
            callbacks.delegate().hidePairing();   
         }
      }
   }   
   
   protected void onDeviceAdded(ModelAddedEvent event) {
      if(pairing) {
         callbacks.delegate().deviceAdded((DeviceModel) event.getModel());
      }
   }
   
   public ListenerRegistration addCallback(Callback callback) {
      return callbacks.register(callback);
   }
   public void startAddingDevices() {
      ClientRequest req = new ClientRequest();
      req.setAddress(PlatformServiceAddress.platformService(place.get().getId(), Place.NAMESPACE).getRepresentation());
      req.setCommand(Place.StartAddingDevicesRequest.NAME);
      req.setTimeoutMs(30000);
      req.setAttribute(Place.StartAddingDevicesRequest.ATTR_TIME, (long)pairingTimeoutMs);
      this.client.request(req).onFailure((error) -> Oculus.showError("Unable to start Add Device Mode", error));  
   }
   
   public void stopAddingDevices() {
       ClientRequest req = new ClientRequest();
       req.setAddress(PlatformServiceAddress.platformService(place.get().getId(), Place.NAMESPACE).getRepresentation());
       req.setCommand(Place.StopAddingDevicesRequest.NAME);
       req.setTimeoutMs(30000);
       this.client.request(req); 
     }
   
   public interface Callback {
      
      public void showPairing();
      
      public void hidePairing();
      
      public void deviceAdded(DeviceModel model);
   }
}


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
package com.iris.common.subsystem.cameras;

import static com.iris.messages.capability.CamerasSubsystemCapability.ATTR_MAXSIMULTANEOUSSTREAMS;
import static com.iris.messages.capability.CamerasSubsystemCapability.TYPE_MAXSIMULTANEOUSSTREAMS;

import java.util.UUID;

import com.google.common.base.Predicates;
import com.google.inject.Singleton;
import com.iris.annotation.Version;
import com.iris.common.subsystem.BaseSubsystem;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.annotation.Subsystem;
import com.iris.common.subsystem.util.AddressesAttributeBinder;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.CamerasSubsystemCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.RecordingCapability;
import com.iris.messages.event.ScheduledEvent;
import com.iris.messages.listener.annotation.OnMessage;
import com.iris.messages.listener.annotation.OnScheduledEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.subs.CamerasSubsystemModel;

@Singleton
@Subsystem(CamerasSubsystemModel.class)
@Version(2)
public class CamerasSubsystem extends BaseSubsystem<CamerasSubsystemModel> {

   public static final String WARN_OFFLINE = "warning.offline";
   public static final String RECORDING_ADDRESS = "SERV:" + RecordingCapability.NAMESPACE + ":*";
   
   public static Address cameraIdToAddress(String cameraId) {
      return Address.platformService(UUID.fromString(cameraId), RecordingCapability.NAMESPACE);
   }

   private final AddressesAttributeBinder<CamerasSubsystemModel> cameras = 
         new AddressesAttributeBinder<CamerasSubsystemModel>(CamerasPredicates.IS_CAMERA, CamerasSubsystemCapability.ATTR_CAMERAS) {
            @Override
            protected void afterAdded(SubsystemContext<CamerasSubsystemModel> context, Model added) {
               CamerasContextAdapter adapter = new CamerasContextAdapter(context);
               adapter.updateAvailable();
               adapter.updateWarnings();
               CameraStatusAdapter.get(context, added.getAddress()).onCameraAdded(added);
               adapter.logger().info("A new camera device was added {}", added.getAddress());
            }
            
            @Override
            protected void afterRemoved(SubsystemContext<CamerasSubsystemModel> context, Address address) {
               CamerasContextAdapter adapter = new CamerasContextAdapter(context);
               adapter.updateAvailable();
               adapter.updateWarnings();
               CameraStatusAdapter.get(context, address).onCameraRemoved();
               adapter.logger().info("A camera device was removed {}", address);
            }
         };
   private final AddressesAttributeBinder<CamerasSubsystemModel> offlineCameras = 
         new AddressesAttributeBinder<CamerasSubsystemModel>(
               Predicates.and(CamerasPredicates.IS_CAMERA, CamerasPredicates.IS_OFFLINE), 
               CamerasSubsystemCapability.ATTR_OFFLINECAMERAS
         ) {
            @Override
            protected void afterAdded(SubsystemContext<CamerasSubsystemModel> context, Model added) {
               CamerasContextAdapter adapter = new CamerasContextAdapter(context);
               adapter.updateWarnings();
               adapter.logger().info("Camera went offline {}", added.getAddress());
               CameraStatusAdapter.get(context, added.getAddress()).onDisconnected();
            }
            
            @Override
            protected void afterRemoved(SubsystemContext<CamerasSubsystemModel> context, Address address) {
               CamerasContextAdapter adapter = new CamerasContextAdapter(context);
               adapter.updateWarnings();
               adapter.logger().info("Camera came online {}", address);
               CameraStatusAdapter.get(context, address).onConnected();
            }
      
         };

   @Override
   protected void onAdded(SubsystemContext<CamerasSubsystemModel> context) {
      super.onAdded(context);
      CamerasContextAdapter adapter = new CamerasContextAdapter(context);
      adapter.clear();
   }

   @Override
   protected void onStarted(SubsystemContext<CamerasSubsystemModel> context) {
      super.onStarted(context);
      CamerasContextAdapter adapter = new CamerasContextAdapter(context);
      cameras.bind(context);
      offlineCameras.bind(context);
      adapter.updateAvailable();
      adapter.initMaxSimultaneousStreams();
      adapter.updateWarnings();
      //Ideally RecordingEnabled should only be set to true once rather than every time startup is called.
      //There is no easy way of enabling it once for existing CameraSubsystem with Basic service level.
      context.model().setRecordingEnabled(Boolean.TRUE);
      
      for(String camera: context.model().getCameras()) {
         try {
            CameraStatusAdapter.get(context, Address.fromString(camera)).onSubsystemStarted();
         }
         catch(Exception e) {
            context.logger().warn("Error setting up camera status for camera [{}]", camera, e);
         }
      }
   }

   @Override
   protected void setAttribute(String name, Object value, SubsystemContext<CamerasSubsystemModel> context)
   {
      switch (name)
      {
         case ATTR_MAXSIMULTANEOUSSTREAMS:
            CamerasContextAdapter adapter = new CamerasContextAdapter(context);
            adapter.validateMaxSimultaneousStreams((Integer) TYPE_MAXSIMULTANEOUSSTREAMS.coerce(value));
            break;
      }

      super.setAttribute(name, value, context);
   }  

   // NOTE - have to use OnMessage instead of targetted model events because
   //        recordings are not stored in the model cache
   @OnMessage(from=RECORDING_ADDRESS, types=Capability.EVENT_ADDED)
   public void onVideoAdded(SubsystemContext<CamerasSubsystemModel> context, PlatformMessage message) {
      CameraStatusAdapter.onVideoAdded(context, message);
   }
   
   @OnMessage(from=RECORDING_ADDRESS, types=Capability.EVENT_VALUE_CHANGE)
   public void onVideoValueChange(SubsystemContext<CamerasSubsystemModel> context, PlatformMessage message) {
      CameraStatusAdapter.onVideoValueChange(context, message);
   }
   
   @OnMessage(from=RECORDING_ADDRESS, types=Capability.EVENT_DELETED)
   public void onVideoDeleted(SubsystemContext<CamerasSubsystemModel> context, PlatformMessage message) {
      CameraStatusAdapter.onVideoDeleted(context, message);
   }
   
   @OnScheduledEvent
   public void onTimeout(SubsystemContext<CamerasSubsystemModel> context, ScheduledEvent event) {
      CameraStatusAdapter.onTimeout(context, event);
   }

}


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
package com.iris.common.subsystem.alarm;

import com.google.common.base.Predicate;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.SubsystemUtils;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.model.Model;
import com.iris.messages.model.ServiceLevel;
import com.iris.messages.model.subs.AlarmSubsystemModel;
import com.iris.messages.service.VideoService;
import com.iris.model.query.expression.ExpressionCompiler;

public class RecordOnSecurityAdapter
{
   public static final String QUERY_CAMERA_DEVICE = "base:caps contains 'camera'";
   public static final Predicate<Model> IS_CAMERA_DEVICE = ExpressionCompiler.compile(QUERY_CAMERA_DEVICE);

   protected static final Integer RECORDING_DURATION_DEFAULT = 300;  //5 min
   protected static final Boolean RECORD_ON_SECURITY_DEFAULT = Boolean.TRUE;
   
   private final SubsystemContext<? extends AlarmSubsystemModel> context;

   public RecordOnSecurityAdapter(SubsystemContext<? extends AlarmSubsystemModel> context) {
      this.context = context;
   }
   
   public boolean isRecordingSupported() {
       if(isRecordingSupportedByServiceLevel()) {
          //check for # of cameras
          Iterable<Model> cameras = context.models().getModels(IS_CAMERA_DEVICE);
          if(cameras != null && cameras.iterator().hasNext()) {
             return true;
          }
       }
       return false;
   }
   
   public void sendRecordMessageIfNecessary()
   {
      AlarmSubsystemModel alarmSubsystem = context.model();
      context.setActor(Address.fromString(alarmSubsystem.getCurrentIncident()));
      try {
         if(alarmSubsystem.getRecordingSupported(false) && 
            alarmSubsystem.getRecordOnSecurity(RECORD_ON_SECURITY_DEFAULT)) {
            //Send record message to each camera
            Iterable<Model> cameras = context.models().getModels(IS_CAMERA_DEVICE);
            for(Model curCamera : cameras) {
               sendRecordMessageFor(curCamera);
            }
         }
      }
      finally {
         context.setActor(null);
      }
      
   }  

   public boolean isRecordingSupportedByServiceLevel() {       
      String curServiceLevel = SubsystemUtils.getServiceLevel(context);
      if(ServiceLevel.isPremiumOrPromon(curServiceLevel)) {
         return true;         
      }else {
         return false;
      }
   }
   

   private void sendRecordMessageFor(Model curCamera)
   {
      MessageBody payload =
         VideoService.StartRecordingRequest
            .builder()
            .withAccountId(context.getAccountId().toString())
            .withCameraAddress(curCamera.getAddress().getRepresentation())
            .withDuration(context.model().getRecordingDurationSec(RECORDING_DURATION_DEFAULT))
            .withPlaceId(context.getPlaceId().toString())
            .withStream(false)
            .build();
      context.request(Address.platformService(context.getPlaceId(), VideoService.NAMESPACE), payload);
   }
   
}


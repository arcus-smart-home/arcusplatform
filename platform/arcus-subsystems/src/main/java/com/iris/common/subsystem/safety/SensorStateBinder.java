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
package com.iris.common.subsystem.safety;

import java.util.HashMap;
import java.util.Map;

import com.iris.common.subsystem.SubsystemContext;
import com.iris.messages.capability.CarbonMonoxideCapability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.capability.LeakGasCapability;
import com.iris.messages.capability.LeakH2OCapability;
import com.iris.messages.capability.SmokeCapability;
import com.iris.messages.event.Listener;
import com.iris.messages.event.ModelAddedEvent;
import com.iris.messages.event.ModelChangedEvent;
import com.iris.messages.event.ModelEvent;
import com.iris.messages.event.ModelRemovedEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.dev.CarbonMonoxideModel;
import com.iris.messages.model.dev.DeviceConnectionModel;
import com.iris.messages.model.dev.LeakGasModel;
import com.iris.messages.model.dev.LeakH2OModel;
import com.iris.messages.model.dev.SmokeModel;
import com.iris.messages.model.subs.SafetySubsystemModel;
import com.iris.util.Subscription;

public class SensorStateBinder {

   public static final String SENSOR_TYPE_SMOKE = "SMOKE";
   public static final String SENSOR_TYPE_CO = "CO";
   public static final String SENSOR_TYPE_WATER = "WATER";
   public static final String SENSOR_TYPE_GAS = "GAS";

   public static final String SENSOR_STATUS_NONE      = "NONE";
   public static final String SENSOR_STATUS_SAFE      = "SAFE";
   public static final String SENSOR_STATUS_OFFLINE   = "OFFLINE";
   public static final String SENSOR_STATUS_DETECTED  = "DETECTED";
   
   public Subscription bind(final SubsystemContext<SafetySubsystemModel> context) {
   	refreshSensors(context);
   	return context.models().addListener(new Listener<ModelEvent>() {
			@Override
			public void onEvent(ModelEvent event) {
				if(event instanceof ModelChangedEvent) {
					String attributeName = ((ModelChangedEvent) event).getAttributeName();
					if(
							CarbonMonoxideCapability.ATTR_CO.equals(attributeName) ||
							LeakGasCapability.ATTR_STATE.equals(attributeName) ||
							LeakH2OCapability.ATTR_STATE.equals(attributeName) ||
							SmokeCapability.ATTR_SMOKE.equals(attributeName) ||
							DeviceConnectionCapability.ATTR_STATE.equals(attributeName)
					) {
						refreshSensors(context);
					}
				}
				if(
						(event instanceof ModelAddedEvent || event instanceof ModelRemovedEvent) &&
						DeviceCapability.NAMESPACE.equals(event.getAddress().getGroup())
				) {
					refreshSensors(context);
				}
			}
		});
   }
   
	private Map<String,String> initSensorTypes() {
      Map<String, String> sensorTypes = new HashMap<>(4);
      sensorTypes.put(SENSOR_TYPE_SMOKE, SENSOR_STATUS_NONE);
      sensorTypes.put(SENSOR_TYPE_CO, SENSOR_STATUS_NONE);
      sensorTypes.put(SENSOR_TYPE_WATER, SENSOR_STATUS_NONE);
      sensorTypes.put(SENSOR_TYPE_GAS, SENSOR_STATUS_NONE);
      return sensorTypes;
   }

   protected void refreshSensors(SubsystemContext<SafetySubsystemModel> context) {
      Map<String,String> newSensorState = initSensorTypes();
      for(Model m : context.models().getModels()) {
         updateSensor(m, newSensorState);
      }

      context.model().setSensorState(newSensorState);
   }

   protected boolean updateSensor(Model model, Map<String, String> sensorTypes) {
      boolean updated = false;
      if(model.supports(SmokeCapability.NAMESPACE)) {
         updated |= updateSensors(
               SENSOR_TYPE_SMOKE,
               SmokeModel.isSmokeDETECTED(model),
               DeviceConnectionModel.isStateONLINE(model),
               sensorTypes
         );
      }
      if(model.supports(CarbonMonoxideCapability.NAMESPACE)) {
         updated |= updateSensors(
               SENSOR_TYPE_CO,
               CarbonMonoxideModel.isCoDETECTED(model),
               DeviceConnectionModel.isStateONLINE(model),
               sensorTypes
         );
      }
      if(model.supports(LeakH2OCapability.NAMESPACE)) {
         updated |= updateSensors(
               SENSOR_TYPE_WATER,
               LeakH2OModel.isStateLEAK(model),
               DeviceConnectionModel.isStateONLINE(model),
               sensorTypes
         );
      }
      if(model.supports(LeakGasCapability.NAMESPACE)) {
         updated |= updateSensors(
               SENSOR_TYPE_GAS,
               LeakGasModel.isStateLEAK(model),
               DeviceConnectionModel.isStateONLINE(model),
               sensorTypes
         );
      }
      return updated;
   }

   protected boolean updateSensors(
         String sensorType,
         boolean triggered,
         boolean online,
         Map<String, String> sensorTypes
   ) {
      String state = sensorTypes.get(sensorType);
      if(SENSOR_STATUS_DETECTED.equals(state)) {
         return false;
      }

      if(triggered) {
         sensorTypes.put(sensorType, SENSOR_STATUS_DETECTED);
         return true;
      }

      if(SENSOR_STATUS_OFFLINE.equals(state)) {
         return false;
      }

      if(!online) {
         sensorTypes.put(sensorType, SENSOR_STATUS_OFFLINE);
         return true;
      }

      if(SENSOR_STATUS_SAFE.equals(state)) {
         return false;
      }

      sensorTypes.put(sensorType, SENSOR_STATUS_SAFE);
      return true;
   }

}


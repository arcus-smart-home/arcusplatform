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
package com.iris.common.subsystem.water;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.inject.Singleton;
import com.iris.annotation.Version;
import com.iris.common.subsystem.BaseSubsystem;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.annotation.Subsystem;
import com.iris.common.subsystem.util.AddressesAttributeBinder;
import com.iris.common.subsystem.util.NotificationsUtil;
import com.iris.common.subsystem.util.NotificationsUtil.ContinuousWaterUse;
import com.iris.common.subsystem.util.NotificationsUtil.ExcessiveWaterUse;
import com.iris.common.subsystem.util.NotificationsUtil.WaterSoftenerLowSalt;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.capability.NotificationCapability.NotifyRequest;
import com.iris.messages.capability.WaterHeaterCapability;
import com.iris.messages.capability.WaterSoftenerCapability;
import com.iris.messages.capability.WaterSubsystemCapability;
import com.iris.messages.capability.WaterSubsystemCapability.ContinuousWaterUseEvent;
import com.iris.messages.capability.WaterSubsystemCapability.ExcessiveWaterUseEvent;
import com.iris.messages.capability.WaterSubsystemCapability.LowSaltEvent;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.model.Model;
import com.iris.messages.model.dev.DeviceModel;
import com.iris.messages.model.dev.EcowaterWaterSoftenerModel;
import com.iris.messages.model.dev.WaterSoftenerModel;
import com.iris.messages.model.subs.WaterSubsystemModel;

/**
 * 
 */
@Singleton
@Subsystem(WaterSubsystemModel.class)
@Version(1)
public class WaterSubsystem extends BaseSubsystem<WaterSubsystemModel> {	

	private static enum EventType { ADDED, REMOVED, INIT };
	private static enum DeviceType { 
		HEATER (WaterSubsystemPredicates.IS_WATERHEATER), 
		SOFTENER(WaterSubsystemPredicates.IS_WATERSOFTENER);
		
		
		protected Predicate<Model> predicate;
		DeviceType(Predicate<Model> pred) {
			this.predicate = pred;
		}
		
		public Predicate<Model> getPredicate() {
			return this.predicate;
		}		
	
	};
		
	private final AddressesAttributeBinder<WaterSubsystemModel> closedValves = new AddressesAttributeBinder<WaterSubsystemModel>(
			Predicates.and(WaterSubsystemPredicates.IS_VALVE, WaterSubsystemPredicates.IS_CLOSED_VALVE), 
            WaterSubsystemCapability.ATTR_CLOSEDWATERVALVES) {
		@Override
		protected void afterAdded(
				SubsystemContext<WaterSubsystemModel> context, Model added) {
			context.logger().info("A closed valve device was added to the closedValves list [{}]",
					added.getAddress());
			
		}

		@Override
		protected void afterRemoved(
				SubsystemContext<WaterSubsystemModel> context, Address address) {
			context.logger().info("A closed valve device was removed from the closedValves list [{}]", address);			
		}
		
	};
	
	private final AddressesAttributeBinder<WaterSubsystemModel> waterDevices = new AddressesAttributeBinder<WaterSubsystemModel>(
			WaterSubsystemPredicates.QUERY_WATER_DEVICES, WaterSubsystemCapability.ATTR_WATERDEVICES) {
		@Override
		protected void afterAdded(
				SubsystemContext<WaterSubsystemModel> context, Model added) {
			context.logger().info("A new water device was added {}", added.getAddress());
			if(added.supports(WaterHeaterCapability.NAMESPACE)) {
				syncPrimaryDevices(context, added, EventType.ADDED, DeviceType.HEATER);
			}
			if(added.supports(WaterSoftenerCapability.NAMESPACE)) {
				syncPrimaryDevices(context, added, EventType.ADDED, DeviceType.SOFTENER);
			}			
			updateAvailable(context);
		}

		@Override
		protected void afterRemoved(
				SubsystemContext<WaterSubsystemModel> context, Address address) {
			context.logger().info("A water device was removed {}", address);	
			syncPrimaryDevicesAfterDeleted(context, address, DeviceType.HEATER);
			syncPrimaryDevicesAfterDeleted(context, address, DeviceType.SOFTENER);
			updateAvailable(context);
		}
	};
	
	private final AddressesAttributeBinder<WaterSubsystemModel> continuousWaterUseDevices = new AddressesAttributeBinder<WaterSubsystemModel>(
	   Predicates.and( WaterSubsystemPredicates.IS_ECOWATER, WaterSubsystemPredicates.IS_CONTINUOUS_WATER_USE), WaterSubsystemCapability.ATTR_CONTINUOUSWATERUSEDEVICES) {
	   
	   @Override
      protected void afterAdded(
            SubsystemContext<WaterSubsystemModel> context, Model added) {
         context.logger().debug("A new continuous water use device was added {}", added.getAddress());
         emitContinuousWaterUse(context, added);
         sendNotificationForContinuousWaterUse(context, added);
      }         
	   
	};
	
	private final AddressesAttributeBinder<WaterSubsystemModel> excessiveWaterUseDevices = new AddressesAttributeBinder<WaterSubsystemModel>(
      Predicates.and( WaterSubsystemPredicates.IS_ECOWATER, WaterSubsystemPredicates.IS_EXCESSIVE_WATER_USE), WaterSubsystemCapability.ATTR_EXCESSIVEWATERUSEDEVICES) {
      
      @Override
      protected void afterAdded(
            SubsystemContext<WaterSubsystemModel> context, Model added) {
         context.logger().debug("A new excessive water use device was added {}", added.getAddress());
         emitExessiveWaterUse(context, added);
         sendNotificationForExcessiveWaterUse(context, added);
      }
      
   };
   
   private final AddressesAttributeBinder<WaterSubsystemModel> lowSaltDevices = new AddressesAttributeBinder<WaterSubsystemModel>(
      Predicates.and( WaterSubsystemPredicates.IS_WATERSOFTENER, WaterSubsystemPredicates.IS_LOW_SALT), WaterSubsystemCapability.ATTR_LOWSALTDEVICES) {
      
      @Override
      protected void afterAdded(
            SubsystemContext<WaterSubsystemModel> context, Model added) {
         context.logger().debug("A new low salt device was added {}", added.getAddress());
         emitLowSalt(context, added);
         sendNotificationForLowSalt(context, added);
      }
      
   };

	@Override
	protected void onAdded(SubsystemContext<WaterSubsystemModel> context) {
		super.onAdded(context);
	}

	@Override
	protected void onStarted(SubsystemContext<WaterSubsystemModel> context) {
		super.onStarted(context);
		initializeAttributes(context);
		waterDevices.bind(context);
		closedValves.bind(context);
		continuousWaterUseDevices.bind(context);
		excessiveWaterUseDevices.bind(context);
		lowSaltDevices.bind(context);
		syncPrimaryDevices(context, null, EventType.INIT, DeviceType.HEATER);
		syncPrimaryDevices(context, null, EventType.INIT, DeviceType.SOFTENER);
		updateAvailable(context);
	}
	
	
	
	private void initializeAttributes(SubsystemContext<WaterSubsystemModel> context) {
		WaterSubsystemModel model = context.model();
		setIfNull(model, WaterSubsystemCapability.ATTR_CLOSEDWATERVALVES, ImmutableSet.<String>of());
		setIfNull(model, WaterSubsystemCapability.ATTR_WATERDEVICES, ImmutableSet.<String>of());
	    setIfNull(model, WaterSubsystemCapability.ATTR_PRIMARYWATERHEATER, "");
	    setIfNull(model, WaterSubsystemCapability.ATTR_PRIMARYWATERSOFTENER, "");
	    
	}

	@Override
	protected void setAttribute(String name, Object value,
			SubsystemContext<WaterSubsystemModel> context)
			throws ErrorEventException {
		String address = (String) value;
		if(WaterSubsystemCapability.ATTR_PRIMARYWATERHEATER.equals(name)) {
			Model model = context.models().getModelByAddress(Address.fromString(address));
			if(model == null || !model.supports(WaterHeaterCapability.NAMESPACE)) {
				throw new ErrorEventException(WaterSubsystemErrors.notWaterHeater(address));
			}
	    }else if(WaterSubsystemCapability.ATTR_PRIMARYWATERSOFTENER.equals(name)) {
	    	Model model = context.models().getModelByAddress(Address.fromString(address));
			if(model == null || !model.supports(WaterSoftenerCapability.NAMESPACE)) {
				throw new ErrorEventException(WaterSubsystemErrors.notWaterSoftener(address));
			}
	    }
		super.setAttribute(name, value, context);
	}
	
	private void syncPrimaryDevices(SubsystemContext<WaterSubsystemModel> context, Model model, EventType event, DeviceType deviceType) {
		if(EventType.ADDED == event || EventType.INIT == event) {
			//Set primary water heater/softener if the current primary value is empty
			syncPrimaryDevice(context, model, deviceType);
		}
	}
	
	private void syncPrimaryDevicesAfterDeleted(SubsystemContext<WaterSubsystemModel> context, Address address, DeviceType deviceType) {
		String curPrimary = getCurrentPrimaryValue(context, deviceType);		
		if(curPrimary.equals(address.getRepresentation())) {
			if(deviceType == DeviceType.HEATER) {
				context.model().setPrimaryWaterHeater("");
			}else {
				context.model().setPrimaryWaterSoftener("");
			}
			
		}
		//Try to find another water heater/softener to be the primary since the current primary is removed.
		syncPrimaryDevice(context, null, deviceType);
	}
	
	private String getCurrentPrimaryValue(SubsystemContext<WaterSubsystemModel> context, DeviceType deviceType) {
		String curPrimary = null;
		if(deviceType == DeviceType.HEATER) {
			curPrimary = context.model().getPrimaryWaterHeater("");
		}else{
			curPrimary = context.model().getPrimaryWaterSoftener("");
		}
		return curPrimary;
	}
	
	
	/**
	 * Set PrimaryWaterHeater in the WaterSubsystemModel only if currently there is no value set.  If the given model is null, it will 
	 * iterate through all the models and find the first water heater to be set as the primary.
	 * @param context
	 * @param model
	 * @param deviceType 
	 * @return
	 */
	private boolean syncPrimaryDevice(SubsystemContext<WaterSubsystemModel> context, Model model, DeviceType deviceType) {
		String curPrimary = getCurrentPrimaryValue(context, deviceType);
		if(StringUtils.isEmpty(curPrimary)) {
			Model curModel = model;
			if(curModel == null) {
				//Find a first device of this type in the model
				curModel = Iterables.getFirst(context.models().getModels(deviceType.getPredicate()), null);
			}
			if(curModel != null) {
				if(DeviceType.HEATER == deviceType && curModel.supports(WaterHeaterCapability.NAMESPACE)) {
					context.model().setPrimaryWaterHeater(curModel.getAddress().getRepresentation());
					return true;
				}else if(DeviceType.SOFTENER == deviceType && curModel.supports(WaterSoftenerCapability.NAMESPACE)) {
					context.model().setPrimaryWaterSoftener(curModel.getAddress().getRepresentation());
					return true;
				}
			}
		}
		return false;
	}
	
	private void updateAvailable(SubsystemContext<WaterSubsystemModel> context) {
      boolean isAvailable =
            context.model().getWaterDevices().size() > 0 ;
      context.model().setAvailable(isAvailable);
   }
		
	
	private void emitContinuousWaterUse(SubsystemContext<WaterSubsystemModel> context, Model model)
   {
	   MessageBody body = ContinuousWaterUseEvent.builder()
         .withDurationSec(EcowaterWaterSoftenerModel.getContinuousDuration(model, 0))
         .withFlowRate(EcowaterWaterSoftenerModel.getContinuousRate(model, 0.0))
         .withSensor(model.getAddress().getRepresentation())
         .build();

	   context.broadcast(body);      
   }
	
	private void emitExessiveWaterUse(SubsystemContext<WaterSubsystemModel> context, Model model)
   {
      MessageBody body = ExcessiveWaterUseEvent.builder()
         .withSensor(model.getAddress().getRepresentation())
         .build();

      context.broadcast(body);      
   }
	
   private void emitLowSalt(SubsystemContext<WaterSubsystemModel> context, Model model)
   {
      MessageBody body = LowSaltEvent.builder()
         .withSensor(model.getAddress().getRepresentation())
         .build();

      context.broadcast(body);
   }
   
	private void sendNotificationForContinuousWaterUse(SubsystemContext<WaterSubsystemModel> context, Model added)
   {
	   String ownerAddress = NotificationsUtil.getAccountOwnerAddress(context);
	   Map<String, String> parameters = ImmutableMap.<String, String>of(ContinuousWaterUse.PARAM_DEVICE_NAME, DeviceModel.getName(added, ""),
	         ContinuousWaterUse.PARAM_CONTINUOUS_RATE, EcowaterWaterSoftenerModel.getContinuousRate(added, 0.0).toString(),
	         ContinuousWaterUse.PARAM_CONTINUOUS_DURATION, (Long.valueOf(TimeUnit.SECONDS.toMinutes(EcowaterWaterSoftenerModel.getContinuousDuration(added, 0)))).toString());
	   
	   NotificationsUtil.sendNotification(context, ContinuousWaterUse.KEY, ownerAddress, NotifyRequest.PRIORITY_MEDIUM, parameters);
	   NotificationsUtil.sendNotification(context, ContinuousWaterUse.KEY, ownerAddress, NotifyRequest.PRIORITY_LOW, parameters);
      
   }   
	
	private void sendNotificationForExcessiveWaterUse(SubsystemContext<WaterSubsystemModel> context, Model added)
   {
	   String ownerAddress = NotificationsUtil.getAccountOwnerAddress(context);
      Map<String, String> parameters = ImmutableMap.<String, String>of(ExcessiveWaterUse.PARAM_DEVICE_NAME, DeviceModel.getName(added, ""));
      
      NotificationsUtil.sendNotification(context, ExcessiveWaterUse.KEY, ownerAddress, NotifyRequest.PRIORITY_MEDIUM, parameters);
      NotificationsUtil.sendNotification(context, ExcessiveWaterUse.KEY, ownerAddress, NotifyRequest.PRIORITY_LOW, parameters);
      
   }
	
	private void sendNotificationForLowSalt(SubsystemContext<WaterSubsystemModel> context, Model added)
   {
	   String ownerAddress = NotificationsUtil.getAccountOwnerAddress(context);
      Map<String, String> parameters = ImmutableMap.<String, String>of(
         WaterSoftenerLowSalt.PARAM_DEVICE_NAME, DeviceModel.getName(added, ""),
         WaterSoftenerLowSalt.PARAM_CURRENT_SALT_LEVEL, WaterSoftenerModel.getCurrentSaltLevel(added, 0).toString());
      
      NotificationsUtil.sendNotification(context, WaterSoftenerLowSalt.KEY, ownerAddress, NotifyRequest.PRIORITY_MEDIUM, parameters);
      NotificationsUtil.sendNotification(context, WaterSoftenerLowSalt.KEY, ownerAddress, NotifyRequest.PRIORITY_LOW, parameters);
   }
   
}


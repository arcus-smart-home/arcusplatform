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

import java.util.Date;

import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.util.OrderedAddressesAttributeBinder;
import com.iris.messages.address.Address;
import com.iris.messages.capability.HaloCapability;
import com.iris.messages.capability.SafetySubsystemCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.subs.SafetySubsystemModel;
import com.iris.model.predicate.Predicates;
import com.iris.util.Subscription;

public class PreSmokeAddressBinder extends OrderedAddressesAttributeBinder<SafetySubsystemModel> {

	public PreSmokeAddressBinder() {
		super(
				Predicates.attributeEquals(HaloCapability.ATTR_DEVICESTATE, HaloCapability.DEVICESTATE_PRE_SMOKE), 
      		SafetySubsystemCapability.ATTR_SMOKEPREALERTDEVICES
      );
	}

   @Override
	public Subscription bind(SubsystemContext<SafetySubsystemModel> context) {
		Subscription s = super.bind(context);
		syncSmokePreAlert(context);
		return s;
	}

	@Override
   protected void afterAdded(SubsystemContext<SafetySubsystemModel> context, Model added) {
      context.logger().info("A prealerting device was added {}", added.getAddress());
      syncSmokePreAlert(context);
   }
   
   @Override
   protected void afterRemoved(SubsystemContext<SafetySubsystemModel> context, Address address) {
      context.logger().info("A prealerting device was removed {}", address);
      syncSmokePreAlert(context);
   }

   private void syncSmokePreAlert(SubsystemContext<SafetySubsystemModel> context) {
      if (context.model().getSmokePreAlertDevices().isEmpty()) {
      	context.model().setSmokePreAlert(SafetySubsystemCapability.SMOKEPREALERT_READY);
      } 
      else {
      	context.model().setSmokePreAlert(SafetySubsystemCapability.SMOKEPREALERT_ALERT);
      	context.model().setLastSmokePreAlertTime(new Date());
      }      
   }
   
}


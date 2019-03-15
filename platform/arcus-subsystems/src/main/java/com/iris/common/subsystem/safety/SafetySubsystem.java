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

import com.google.inject.Inject;
import com.iris.capability.definition.DefinitionRegistry;
import com.iris.common.subsystem.Subsystem;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.event.SubsystemStartedEvent;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.capability.SafetySubsystemCapability;
import com.iris.messages.capability.SubsystemCapability;
import com.iris.messages.event.AddressableEvent;
import com.iris.messages.event.ModelChangedEvent;
import com.iris.messages.model.subs.SafetySubsystemModel;
import com.iris.model.Version;

public class SafetySubsystem implements Subsystem<SafetySubsystemModel> {
	private final SafetySubsystemV1 v1 = new SafetySubsystemV1();
	private final SafetySubsystemV2 v2 = new SafetySubsystemV2();
	
	// note the binders are extracted to here because its hard to add / remove these listeners
	// at runtime and they behavior is the same across V1 & V2
	private final PreSmokeAddressBinder preSmokeBinder = new PreSmokeAddressBinder();
	private final SensorStateBinder sensorStateBinder = new SensorStateBinder();
	private final WaterValvesAddressBinder waterValvesBinder = new WaterValvesAddressBinder();

	@Override
	public String getName() {
		return SafetySubsystemCapability.NAME;
	}

	@Override
	public String getNamespace() {
		return SafetySubsystemCapability.NAMESPACE;
	}

	@Override
	public Class<SafetySubsystemModel> getType() {
		return SafetySubsystemModel.class;
	}

	@Override
	public Version getVersion() {
		return Version.fromRepresentation("2");
	}

   @Inject
   public void setDefinitionRegistry(DefinitionRegistry registry) {
      this.v1.setDefinitionRegistry(registry);
      this.v2.setDefinitionRegistry(registry);
   }
   
	@Override
	public void onEvent(AddressableEvent event, SubsystemContext<SafetySubsystemModel> context) {
		if(
				event instanceof ModelChangedEvent && 
				AlarmSubsystemCapability.NAMESPACE.equals(event.getAddress().getGroup()) &&
				SubsystemCapability.ATTR_STATE.equals(((ModelChangedEvent) event).getAttributeName())
		) {
			if(SubsystemCapability.STATE_ACTIVE.equals(((ModelChangedEvent) event).getAttributeValue())) {
				upgrade(context);
			}
			else {
				downgrade(context);
			}
		}
		else {
			delegate(context).onEvent(event, context);
			if(event instanceof SubsystemStartedEvent) {
				preSmokeBinder.bind(context);
				sensorStateBinder.bind(context);
				waterValvesBinder.bind(context);
			}
		}
	}
	
	private void upgrade(SubsystemContext<SafetySubsystemModel> context) {
		v1.onStopped(context);
		v2.onStarted(context);
	}
	
	private void downgrade(SubsystemContext<SafetySubsystemModel> context) {
		v2.onStopped(context);
		v1.onStarted(context);
	}

	private Subsystem<SafetySubsystemModel> delegate(SubsystemContext<SafetySubsystemModel> context) {
		String alarmSubsystemState = (String) context.models().getAttributeValue(Address.platformService(context.getPlaceId(), AlarmSubsystemCapability.NAMESPACE), SubsystemCapability.ATTR_STATE);
		boolean isV2 = SubsystemCapability.STATE_ACTIVE.equals(alarmSubsystemState);
		return isV2 ? v2 : v1;
	}

}


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
package com.iris.common.subsystem.security;

import com.google.inject.Inject;
import com.iris.capability.definition.DefinitionRegistry;
import com.iris.common.subsystem.Subsystem;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.event.SubsystemStartedEvent;
import com.iris.messages.address.Address;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.capability.SecuritySubsystemCapability;
import com.iris.messages.capability.SubsystemCapability;
import com.iris.messages.event.AddressableEvent;
import com.iris.messages.event.ModelChangedEvent;
import com.iris.messages.model.subs.SecuritySubsystemModel;
import com.iris.model.Version;

public class SecuritySubsystem implements Subsystem<SecuritySubsystemModel> {
	private final SecuritySubsystemV1 v1 = new SecuritySubsystemV1();
	private final SecuritySubsystemV2 v2 = new SecuritySubsystemV2();
	
	// note the binders are extracted to here because its hard to add / remove these listeners
	// at runtime and they behavior is the same across V1 & V2
	private final KeypadBinder keypadBinder = new KeypadBinder();

	@Override
	public String getName() {
		return SecuritySubsystemCapability.NAME;
	}

	@Override
	public String getNamespace() {
		return SecuritySubsystemCapability.NAMESPACE;
	}

	@Override
	public Class<SecuritySubsystemModel> getType() {
		return SecuritySubsystemModel.class;
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
	public void onEvent(AddressableEvent event, SubsystemContext<SecuritySubsystemModel> context) {
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
			if(event instanceof SubsystemStartedEvent) {
				keypadBinder.bind(context);
			}
			delegate(context).onEvent(event, context);
		}
	}
	
	private void upgrade(SubsystemContext<SecuritySubsystemModel> context) {
		v1.onStopped(context);
		v2.onStarted(context);
	}
	
	private void downgrade(SubsystemContext<SecuritySubsystemModel> context) {
		v2.onStopped(context);
		v1.onStarted(context);
	}

	private Subsystem<SecuritySubsystemModel> delegate(SubsystemContext<SecuritySubsystemModel> context) {
		String alarmSubsystemState = (String) context.models().getAttributeValue(Address.platformService(context.getPlaceId(), AlarmSubsystemCapability.NAMESPACE), SubsystemCapability.ATTR_STATE);
		boolean isV2 = SubsystemCapability.STATE_ACTIVE.equals(alarmSubsystemState);
		return isV2 ? v2 : v1;
	}

}


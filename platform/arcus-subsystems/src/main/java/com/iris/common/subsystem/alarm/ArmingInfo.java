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

import java.util.Date;

import org.eclipse.jdt.annotation.Nullable;

import com.iris.common.subsystem.SubsystemContext;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.model.subs.AlarmSubsystemModel;

/**
 * Holds information about who / what armed the alarm until the 
 * arming period completes.
 * @author tweidlin
 */
public class ArmingInfo {
	private static final String VAR_ARMINGINFO = "armingInfo";
	
	@Nullable
	public static ArmingInfo get(SubsystemContext<?> context) {
		return context.getVariable(VAR_ARMINGINFO).as(ArmingInfo.class);
	}
	
	public static ArmingInfo save(SubsystemContext<?> context, PlatformMessage armingRequest) {
		ArmingInfo info = new ArmingInfo();
		info.setBy(armingRequest.getActor());
		info.setFrom(armingRequest.getSource());
		context.setVariable(VAR_ARMINGINFO, info);
		return info;
	}
	
	@Nullable
	public static ArmingInfo clear(SubsystemContext<?> context) {
		ArmingInfo info = get(context);
		doClear(context);
		return info;
	}
	
	@Nullable
	public static ArmingInfo disarmed(SubsystemContext<? extends AlarmSubsystemModel> context) {
      ArmingInfo info = get(context);
      if(info != null) {
         AlarmSubsystemModel alarmSubSystem = context.model();
         alarmSubSystem.setLastDisarmedTime(new Date());
         alarmSubSystem.setLastDisarmedBy(info.getByRepresentation());
         alarmSubSystem.setLastDisarmedFrom(info.getFromRepresentation());
         doClear(context);
      }
      return info;
   }	
	
	private Address from;
	private Address by;
	
	@Nullable
	public Address getFrom() {
		return from;
	}
	
	@Nullable
	public String getFromRepresentation() {
		return from != null ? from.getRepresentation() : null;
	}
	
	public void setFrom(Address from) {
		this.from = from;
	}
	
	@Nullable
	public Address getBy() {
		return by;
	}
	
	@Nullable
	public String getByRepresentation() {
		return by != null ? by.getRepresentation() : null;
	}
	
	public void setBy(Address by) {
		this.by = by;
	}
		

	@Override
	public String toString() {
		return "ArmingContext [from=" + from + ", by=" + by + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((by == null) ? 0 : by.hashCode());
		result = prime * result + ((from == null) ? 0 : from.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ArmingInfo other = (ArmingInfo) obj;
		if (by == null) {
			if (other.by != null)
				return false;
		} else if (!by.equals(other.by))
			return false;
		if (from == null) {
			if (other.from != null)
				return false;
		} else if (!from.equals(other.from))
			return false;
		return true;
	}
	
	private static void doClear(SubsystemContext<?> context) {
	   context.setVariable(VAR_ARMINGINFO, null);
	}
	
}


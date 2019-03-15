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
package com.iris.agent.alarm.lights;


public class AlarmLEDKey {
	private String name;
	private boolean isBattery;

	private AlarmLEDKey(String name, boolean isBattery) {
		this.name = name;
		this.isBattery = isBattery;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (isBattery ? 1231 : 1237);
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		AlarmLEDKey other = (AlarmLEDKey) obj;
		if (isBattery != other.isBattery)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	private static Builder BUILDER = new Builder();
	
	public static Builder builder() {
		return BUILDER;
	}
	
	public static class Builder {
		private String name = "";
		private boolean isBattery = false;
		
		public Builder() {
			
		}

		public Builder isBattery(boolean isBattery) {
			this.isBattery = isBattery;
			return this;
		}
		
		public Builder onBattery() {
			isBattery = true;
			return this;
		}
		
		public Builder notBattery() {
			isBattery = false;
			return this;
		}
		
		public Builder withAlarm(String alarm) {
			this.name = alarm;
			return this;
		}
		
		public AlarmLEDKey build() {
			return new AlarmLEDKey(name,isBattery);
		}
		
	}
	
	

}


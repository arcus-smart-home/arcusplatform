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

import com.iris.agent.hal.LEDState;

public class AlarmLEDValue {
	private LEDState	state;
	private int 		duration;
	
	private AlarmLEDValue(LEDState state, int duration) {
		this.state = state;
		this.duration = duration;
	}
	
	public LEDState getState() {
		return state;
	}

	public int getDuration() {
		return duration;
	}

	private static Builder BUILDER = new Builder();
	
	public static Builder builder() {
		return BUILDER;
	}

	public static class Builder {
		private LEDState state = LEDState.UNKNOWN;
		private int duration = 0;
	
		public Builder() {			
		}
	
		public Builder withState(LEDState state) {
			this.state = state;
			return this;
		}
		
		public Builder withDuration(int duration) {
			this.duration = duration;
			return this;
		}
		
		public AlarmLEDValue build() {
			return new AlarmLEDValue(state,duration);
		}
	}
	
}


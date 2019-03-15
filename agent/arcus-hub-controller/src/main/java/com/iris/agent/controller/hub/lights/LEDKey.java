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
package com.iris.agent.controller.hub.lights;

import com.iris.agent.hal.Model;

public class LEDKey {
    
    // These pulled from the original updateLEDState in HubController.
    private final boolean isBackupConnection;
    private final boolean isBattery;
    private final boolean isAuthorized;
    private final boolean pairing;
    private final boolean connected;
    private final boolean isV2;
    
    private LEDKey(boolean isBackupConnection, boolean isBattery, boolean isAuthorized, boolean pairing, boolean connected, boolean isV2) {
        this.isBackupConnection = isBackupConnection;
        this.isBattery          = isBattery;
        this.isAuthorized       = isAuthorized;
        this.pairing            = pairing;
        this.connected          = connected;
        this.isV2 				= isV2;
    }

    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (connected ? 1231 : 1237);
		result = prime * result + (isAuthorized ? 1231 : 1237);
		result = prime * result + (isBackupConnection ? 1231 : 1237);
		result = prime * result + (isBattery ? 1231 : 1237);
		result = prime * result + (isV2 ? 1231 : 1237);
		result = prime * result + (pairing ? 1231 : 1237);
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
		LEDKey other = (LEDKey) obj;
		if (connected != other.connected)
			return false;
		if (isAuthorized != other.isAuthorized)
			return false;
		if (isBackupConnection != other.isBackupConnection)
			return false;
		if (isBattery != other.isBattery)
			return false;
		if (isV2 != other.isV2)
			return false;
		if (pairing != other.pairing)
			return false;
		return true;
	}

	private static final Builder BUILDER = new Builder();
    
    public static Builder builder() {
        return BUILDER;
    }
    
    public static class Builder {
        private boolean isBackupConnection = false;
        private boolean isBattery = false;
        private boolean isAuthorized = false;
        private boolean pairing = false;
        private boolean connected = false;
        private boolean isV2 = true;
        
        public Builder() {
            
        }
        
        public Builder isBackup(boolean backup) {
            this.isBackupConnection = backup;
            return this;
        }
        public Builder isOnBackup() {
            this.isBackupConnection = true;
            return this;
        }

        public Builder isNotOnBackup() {
            this.isBackupConnection = false;
            return this;
        }
        
        public Builder isBattery(boolean battery) {
            this.isBattery = battery;
            return this;
        }
        public Builder isOnBattery() {
            this.isBattery = true;
            return this;
        }
        public Builder isNotOnBattery() {
            this.isBattery = false;
            return this;
        }

        public Builder isAuthorized(boolean authorized) {
            this.isAuthorized = authorized;
            return this;
        }
        public Builder isAuthorized() {
            this.isAuthorized = true;
            return this;
        }
        public Builder isNotAuthorized() {
            this.isAuthorized = false;
            return this;
        }
        
        public Builder isPairing(boolean pairing) {
            this.pairing = pairing;
            return this;
        }
        public Builder isPairing() {
            this.pairing = true;
            return this;
        }
        public Builder isNotPairing() {
            this.pairing = false;
            return this;
        }
                
        public Builder isConnected(boolean connected) {
            this.connected = connected;
            return this;
        }
        public Builder isConnected() {
            this.connected = true;
            return this;
        }       
        public Builder isDisconnected() {
            this.connected = false;
            return this;
        }

        public Builder withModel(String model) {
        	this.isV2 = Model.isV2(model);
        	return this;
        }
        
        public Builder isV2() {
        	this.isV2 = true;
        	return this;
        }
        
        public Builder isV3() {
        	this.isV2 = false;
        	return this;
        }
        
        public LEDKey build() {
            return new LEDKey(isBackupConnection,isBattery,isAuthorized,pairing,connected,isV2);
        }

        public Builder V2(boolean isBackupConnection, boolean isBattery, boolean isAuthorized, boolean pairing, boolean connected) {
            this.isBackupConnection = isBackupConnection;
            this.isBattery = isBattery;
            this.isAuthorized = isAuthorized;
            this.pairing = pairing;
            this.connected = connected;
            this.isV2 = true;
            return this;
        }

		public Builder V3(boolean isBackupConnection, boolean isBattery, boolean isAuthorized, boolean pairing, boolean connected) {
            this.isBackupConnection = isBackupConnection;
            this.isBattery = isBattery;
            this.isAuthorized = isAuthorized;
            this.pairing = pairing;
            this.connected = connected;
            this.isV2 = false;
			return this;
		}
    }
    
    
}


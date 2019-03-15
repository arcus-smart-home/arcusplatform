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
package com.iris.messages.model;

import java.util.Date;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.iris.messages.address.Address;
import com.iris.messages.model.BaseEntity;
import com.iris.messages.services.PlatformConstants;

public class HubRegistration extends BaseEntity<String, HubRegistration> {
	
	public enum RegistrationState {
		ONLINE,
		OFFLINE,
		DOWNLOADING,
		APPLYING,
		REGISTERED
	}

	private Date lastConnected;
	private RegistrationState state;
	private Date upgradeRequestTime;
	private String firmwareVersion;
	private String targetVersion;
	private String upgradeErrorCode;
	private String upgradeErrorMessage;
	private int downloadProgress;
	private Date upgradeErrorTime;
	
	@Override
	public String getType() {
		return PlatformConstants.SERVICE_HUB;
	}

	@Override
	public String getAddress() {
		return Address.hubAddress(getId()).getRepresentation();
	}

	@Override
	public Set<String> getCaps() {
		return ImmutableSet.<String> of();
	}

	public Date getLastConnected() {
		return lastConnected;
	}

	public void setLastConnected(Date lastConnected) {
		this.lastConnected = lastConnected;
	}

	public RegistrationState getState() {
		return state;
	}

	public void setState(RegistrationState state) {
		this.state = state;
	}

	public Date getUpgradeRequestTime() {
		return upgradeRequestTime;
	}

	public void setUpgradeRequestTime(Date upgradeRequestTime) {
		this.upgradeRequestTime = upgradeRequestTime;
	}

	public String getFirmwareVersion() {
		return firmwareVersion;
	}

	public void setFirmwareVersion(String firmwareVersion) {
		this.firmwareVersion = firmwareVersion;
	}

	public String getTargetVersion() {
		return targetVersion;
	}

	public void setTargetVersion(String targetVersion) {
		this.targetVersion = targetVersion;
	}

	public String getUpgradeErrorCode() {
		return upgradeErrorCode;
	}

	public void setUpgradeErrorCode(String upgradeErrorCode) {
		this.upgradeErrorCode = upgradeErrorCode;
	}

	public String getUpgradeErrorMessage() {
		return upgradeErrorMessage;
	}

	public void setUpgradeErrorMessage(String upgradeErrorMessage) {
		this.upgradeErrorMessage = upgradeErrorMessage;
	}
	
	public int getDownloadProgress() {
		return downloadProgress;
	}

	public void setDownloadProgress(int downloadProgress) {
		this.downloadProgress = downloadProgress;
	}
	
   public Date getUpgradeErrorTime()
   {
      return upgradeErrorTime;
   }

   public void setUpgradeErrorTime(Date upgradeErrorTime)
   {
      this.upgradeErrorTime = upgradeErrorTime;
   }

   @Override
   protected Object clone() throws CloneNotSupportedException {
   HubRegistration hubReg = (HubRegistration) super.clone();
     return hubReg;
   }
   
   @Override
   public String toString() {
      return "HubRegistration [hubId=" + getId() + ", created=" + getCreated()
            + ", modified=" + getModified() + ", state=" + getState() + ", lastConnected="
            + lastConnected + ", upgradeRequestTime=" + upgradeRequestTime + ", firmwareVersion="
            + firmwareVersion + ", targetVersion=" + targetVersion + ", downloadProgress=" + downloadProgress + ", upgradeErrorCode=" + upgradeErrorCode
            + ", upgradeErrorMessage=" + upgradeErrorMessage 
            + ", upgradeErrorTime=" + upgradeErrorTime + "]";
   }
   
   @Override
   public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result
            + ((state == null) ? 0 : state.hashCode());
      result = prime
            * result
            + ((lastConnected == null) ? 0 : lastConnected
                  .hashCode());
      result = prime * result
            + ((upgradeRequestTime == null) ? 0 : upgradeRequestTime.hashCode());
      result = prime * result
            + ((firmwareVersion == null) ? 0 : firmwareVersion.hashCode());
      result = prime
            * result
            + ((targetVersion == null) ? 0 : targetVersion.hashCode());
      result = prime * result
            + ((upgradeErrorCode == null) ? 0 : upgradeErrorCode.hashCode());
      result = prime * result
            + ((upgradeErrorMessage == null) ? 0 : upgradeErrorMessage.hashCode());
      result = prime * result + downloadProgress;
      result = prime * result
         + ((upgradeErrorTime == null) ? 0 : upgradeErrorTime.hashCode());
      return result;
   }

	@Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (!super.equals(obj))
         return false;
      if (getClass() != obj.getClass())
         return false;
      HubRegistration other = (HubRegistration) obj;
      if (state == null) {
         if (other.state != null)
            return false;
      } else if (!state.equals(other.state))
         return false;
      if (lastConnected == null) {
         if (other.lastConnected != null)
            return false;
      } else if (!lastConnected.equals(other.lastConnected))
         return false;
      if (upgradeRequestTime == null) {
         if (other.upgradeRequestTime != null)
            return false;
      } else if (!upgradeRequestTime.equals(other.upgradeRequestTime))
         return false;
      if (firmwareVersion == null) {
         if (other.firmwareVersion != null)
            return false;
      } else if (!firmwareVersion.equals(other.firmwareVersion))
         return false;
      if (targetVersion == null) {
         if (other.targetVersion != null)
            return false;
      } else if (!targetVersion.equals(other.targetVersion))
         return false;
      if (upgradeErrorCode == null) {
         if (other.upgradeErrorCode != null)
            return false;
      } else if (!upgradeErrorCode.equals(other.upgradeErrorCode))
         return false;
      if (upgradeErrorMessage == null) {
         if (other.upgradeErrorMessage != null)
            return false;
      } else if (!upgradeErrorMessage.equals(other.upgradeErrorMessage))
         return false;
      if(downloadProgress != other.downloadProgress) {
    	  return false;
      }
      if (upgradeErrorTime == null) {
         if (other.upgradeErrorTime != null)
            return false;
      } else if (!upgradeErrorTime.equals(other.upgradeErrorTime))
         return false;
      
      return true;
   }



	

}


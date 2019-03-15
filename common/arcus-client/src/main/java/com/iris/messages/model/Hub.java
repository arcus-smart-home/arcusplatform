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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.iris.messages.MessageConstants;
import com.iris.messages.services.PlatformConstants;

public class Hub extends BaseEntity<String, Hub> {

   // TODO - correct to reflect states described in wiki
   public static final String STATE_CREATED = "created";
   public static final String STATE_PROVISIONING = "provisioning";
   public static final String STATE_ACTIVE = "active";

   private UUID accountId;
   private UUID placeId;
   private Set<String> caps;

   private String name;

   private String vendor;
   private String model;
   private String serialNum;
   private String hardwareVer;
   private String macAddress;
   private String mfgInfo;

   private String firmwareGroup;

   private String osVer;
   private String agentVer;
   private String bootloaderVer;
   private String state;
   private String registrationState;

   private UUID lastDeviceAddRemove;
   private UUID lastReset;

   private boolean disallowCell = false;
   private String disallowCellReason;

   @Override
   public String getType() {
      return PlatformConstants.SERVICE_HUB;
   }

   @Override
   public String getAddress() {
      return MessageConstants.SERVICE + ":" + getId() + ":" + PlatformConstants.SERVICE_HUB;
   }

   /** @deprecated Use getAccount */
   @Deprecated
   public UUID getAccountId() {
      return accountId;
   }
   public UUID getAccount() { return getAccountId(); }

   /** @deprecated Use setAccount */
   @Deprecated
   public void setAccountId(UUID accountId) {
      this.accountId = accountId;
   }
   public void setAccount(UUID accountId) { setAccountId(accountId); }

   /** @deprecated Use getPlace */
   @Deprecated
   public UUID getPlaceId() {
      return placeId;
   }
   public UUID getPlace() { return getPlaceId(); }

   /** @deprecated Use setPlace */
   @Deprecated
   public void setPlaceId(UUID placeId) {
      this.placeId = placeId;
   }
   public void setPlace(UUID placeId) { setPlaceId(placeId); }

   @Override
   public Set<String> getCaps() {
      return caps;
   }

   public void setCaps(Set<String> caps) {
      this.caps = new HashSet<String>();
      if (caps != null)
         this.caps.addAll(caps);
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getVendor() {
      return vendor;
   }

   public void setVendor(String vendor) {
      this.vendor = vendor;
   }

   public String getModel() {
      return model;
   }

   public void setModel(String model) {
      this.model = model;
   }

   public String getSerialNum() {
      return serialNum;
   }

   public void setSerialNum(String serialNum) {
      this.serialNum = serialNum;
   }

   /** @deprecated Use getHardwarever */
   @Deprecated
   public String getHardwareVer() {
      return hardwareVer;
   }
   public String getHardwarever() { return getHardwareVer(); }

   /** @deprecated Use setHardwarever */
   @Deprecated
   public void setHardwareVer(String hardwareVer) {
      this.hardwareVer = hardwareVer;
   }
   public void setHardwarever(String hardwareVer) { setHardwareVer(hardwareVer); }

   /** @deprecated Use getMac */
   @Deprecated
   public String getMacAddress() {
      return macAddress;
   }
   public String getMac() { return getMacAddress(); }

   /** @deprecated Use setMac */
   @Deprecated
   public void setMacAddress(String macAddress) {
      this.macAddress = macAddress;
   }
   public void setMac(String macAddress) { setMacAddress(macAddress); }

   public String getMfgInfo() {
      return mfgInfo;
   }

   public void setMfgInfo(String mfgInfo) {
      this.mfgInfo = mfgInfo;
   }

   public String getFirmwareGroup() {
      return firmwareGroup;
   }

   public void setFirmwareGroup(String firmwareGroup) {
      this.firmwareGroup = firmwareGroup;
   }

   /** @deprecated Use getOsver */
   @Deprecated
   public String getOsVer() {
      return osVer;
   }
   public String getOsver() { return getOsVer(); }

   /** @deprecated Use setOsver */
   @Deprecated
   public void setOsVer(String osVer) {
      this.osVer = osVer;
   }
   public void setOsver(String osVer) { setOsVer(osVer); }

   /** @deprecated Use getAgentver */
   @Deprecated
   public String getAgentVer() {
      return agentVer;
   }
   public String getAgentver() { return getAgentVer(); }

   /** @deprecated Use setAgentver */
   @Deprecated
   public void setAgentVer(String agentVer) {
      this.agentVer = agentVer;
   }
   public void setAgentver(String agentVer) { setAgentVer(agentVer); }

   public String getBootloaderVer() {
      return bootloaderVer;
   }

   public void setBootloaderVer(String bootloaderVer) {
      this.bootloaderVer = bootloaderVer;
   }

   public String getState() {
      return state;
   }

   public void setState(String state) {
      this.state = state;
   }

   public String getRegistrationState() {
      return registrationState;
   }

   public void setRegistrationState(String registrationState) {
      this.registrationState = registrationState;
   }

   public boolean isDisallowCell() {
      return disallowCell;
   }

   public void setDisallowCell(boolean disallowCell) {
      this.disallowCell = disallowCell;
   }

   public String getDisallowCellReason() {
      return disallowCellReason;
   }

   public void setDisallowCellReason(String disallowCellReason) {
      this.disallowCellReason = disallowCellReason;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result
            + ((accountId == null) ? 0 : accountId.hashCode());
      result = prime * result + ((agentVer == null) ? 0 : agentVer.hashCode());
      result = prime * result
            + ((bootloaderVer == null) ? 0 : bootloaderVer.hashCode());
      result = prime * result + ((caps == null) ? 0 : caps.hashCode());
      result = prime * result + (disallowCell ? 1231 : 1237);
      result = prime
            * result
            + ((disallowCellReason == null) ? 0 : disallowCellReason.hashCode());
      result = prime * result
            + ((firmwareGroup == null) ? 0 : firmwareGroup.hashCode());
      result = prime * result
            + ((hardwareVer == null) ? 0 : hardwareVer.hashCode());
      result = prime
            * result
            + ((lastDeviceAddRemove == null) ? 0 : lastDeviceAddRemove
                  .hashCode());
      result = prime * result
            + ((lastReset == null) ? 0 : lastReset.hashCode());
      result = prime * result
            + ((macAddress == null) ? 0 : macAddress.hashCode());
      result = prime * result + ((mfgInfo == null) ? 0 : mfgInfo.hashCode());
      result = prime * result + ((model == null) ? 0 : model.hashCode());
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      result = prime * result + ((osVer == null) ? 0 : osVer.hashCode());
      result = prime * result + ((placeId == null) ? 0 : placeId.hashCode());
      result = prime * result
            + ((registrationState == null) ? 0 : registrationState.hashCode());
      result = prime * result
            + ((serialNum == null) ? 0 : serialNum.hashCode());
      result = prime * result + ((state == null) ? 0 : state.hashCode());
      result = prime * result + ((vendor == null) ? 0 : vendor.hashCode());
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
      Hub other = (Hub) obj;
      if (accountId == null) {
         if (other.accountId != null)
            return false;
      } else if (!accountId.equals(other.accountId))
         return false;
      if (agentVer == null) {
         if (other.agentVer != null)
            return false;
      } else if (!agentVer.equals(other.agentVer))
         return false;
      if (bootloaderVer == null) {
         if (other.bootloaderVer != null)
            return false;
      } else if (!bootloaderVer.equals(other.bootloaderVer))
         return false;
      if (caps == null) {
         if (other.caps != null)
            return false;
      } else if (!caps.equals(other.caps))
         return false;
      if (disallowCell != other.disallowCell)
         return false;
      if (disallowCellReason == null) {
         if (other.disallowCellReason != null)
            return false;
      } else if (!disallowCellReason.equals(other.disallowCellReason))
         return false;
      if (firmwareGroup == null) {
         if (other.firmwareGroup != null)
            return false;
      } else if (!firmwareGroup.equals(other.firmwareGroup))
         return false;
      if (hardwareVer == null) {
         if (other.hardwareVer != null)
            return false;
      } else if (!hardwareVer.equals(other.hardwareVer))
         return false;
      if (lastDeviceAddRemove == null) {
         if (other.lastDeviceAddRemove != null)
            return false;
      } else if (!lastDeviceAddRemove.equals(other.lastDeviceAddRemove))
         return false;
      if (lastReset == null) {
         if (other.lastReset != null)
            return false;
      } else if (!lastReset.equals(other.lastReset))
         return false;
      if (macAddress == null) {
         if (other.macAddress != null)
            return false;
      } else if (!macAddress.equals(other.macAddress))
         return false;
      if (mfgInfo == null) {
         if (other.mfgInfo != null)
            return false;
      } else if (!mfgInfo.equals(other.mfgInfo))
         return false;
      if (model == null) {
         if (other.model != null)
            return false;
      } else if (!model.equals(other.model))
         return false;
      if (name == null) {
         if (other.name != null)
            return false;
      } else if (!name.equals(other.name))
         return false;
      if (osVer == null) {
         if (other.osVer != null)
            return false;
      } else if (!osVer.equals(other.osVer))
         return false;
      if (placeId == null) {
         if (other.placeId != null)
            return false;
      } else if (!placeId.equals(other.placeId))
         return false;
      if (registrationState == null) {
         if (other.registrationState != null)
            return false;
      } else if (!registrationState.equals(other.registrationState))
         return false;
      if (serialNum == null) {
         if (other.serialNum != null)
            return false;
      } else if (!serialNum.equals(other.serialNum))
         return false;
      if (state == null) {
         if (other.state != null)
            return false;
      } else if (!state.equals(other.state))
         return false;
      if (vendor == null) {
         if (other.vendor != null)
            return false;
      } else if (!vendor.equals(other.vendor))
         return false;
      return true;
   }

   @Override
   public String toString() {
      return "Hub [accountId=" + accountId + ", placeId=" + placeId + ", caps=" + caps + ", name=" + name + ", vendor=" + vendor + ", model=" + model + ", serialNum=" + serialNum + ", hardwareVer=" + hardwareVer + ", macAddress=" + macAddress + ", mfgInfo=" + mfgInfo + ", firmwareGroup=" + firmwareGroup + ", osVer=" + osVer + ", agentVer=" + agentVer
            + ", bootloaderVer=" + bootloaderVer + ", state=" + state + ", registrationState=" + registrationState + ", lastDeviceAddRemove=" + lastDeviceAddRemove + ", lastReset=" + lastReset + ", disallowCell=" + disallowCell + ", disallowCellReason=" + disallowCellReason + "]";
   }

   @Override
   protected Object clone() throws CloneNotSupportedException {
      Hub hub = (Hub) super.clone();
      hub.caps = this.caps == null ? null : new HashSet<String>(caps);
      return hub;
   }

   @Override
   public Hub copy() {
      try {
         return (Hub) clone();
      } catch(CloneNotSupportedException cnse) {
         throw new RuntimeException(cnse);
      }
   }

   public UUID getLastDeviceAddRemove() {
      return lastDeviceAddRemove;
   }


   public void setLastDeviceAddRemove(UUID lastDeviceAddRemove) {
      this.lastDeviceAddRemove = lastDeviceAddRemove;
   }


   public UUID getLastReset() {
      return lastReset;
   }


   public void setLastReset(UUID lastReset) {
      this.lastReset = lastReset;
   }


}


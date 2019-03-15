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
package com.iris.protocol.ipcd;

import java.util.Collection;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableSet;
import com.iris.device.attributes.AttributeMap;
import com.iris.messages.address.ProtocolDeviceId;
import com.iris.messages.model.Copyable;
import com.iris.protocol.ipcd.message.model.Device;
import com.iris.protocol.ipcd.message.model.DeviceInfo;

public class IpcdDevice implements Copyable<IpcdDevice> {

   public static enum ConnState { ONLINE, OFFLINE };
   public static enum RegistrationState { UNREGISTERED, PENDING_DRIVER, REGISTERED };

   private String protocolAddress;
   private String driverAddress;
   private UUID accountId;
   private UUID placeId;
   private Date created;
   private Date modified;
   private Date lastConnected;

   private String vendor;
   private String model;
   private String sn;
   private String ipcdver;
   private String firmware;
   private String connection;
   private Set<String> actions;
   private Set<String> commands;
   private String v1DeviceId;

   private ConnState connState;
   private RegistrationState registrationState = RegistrationState.UNREGISTERED;

   /**
    * Update the IpcdDevice with a Device object from the Ipcd message model. If there
    * is no protocol address set, this will set the protocol address. If there is a protocol
    * address already set then it will only be set if the protocol address remains unchanged.
    *
    * @param device Ipcd device message model
    * @return true if the IpcdDevice was updated
    */
   public boolean updateWithDevice(Device device) {
      String newProtocolAddress = IpcdProtocol.ipcdAddress(device).getRepresentation();
      if (StringUtils.isEmpty(protocolAddress)) {
         setProtocolAddress(newProtocolAddress);
      }
      else if (!protocolAddress.equals(newProtocolAddress)) {
         return false;
      }
      setVendor(device.getVendor());
      setModel(device.getModel());
      setSn(device.getSn());
      setIpcdver(device.getIpcdver());
      return true;
   }

   public void updateWithDeviceInfo(DeviceInfo deviceInfo) {
      setFirmware(deviceInfo.getFwver());
      setConnection(deviceInfo.getConnection());
      setActions(deviceInfo.getActions());
      setCommands(deviceInfo.getCommands());
   }

   public void syncToIrisDevice(com.iris.messages.model.Device irisDevice) {
      if (!StringUtils.isEmpty(irisDevice.getProtocolAddress())) {
         setProtocolAddress(irisDevice.getProtocolAddress());
      }
      if (irisDevice.getAccount() != null) {
         setAccountId(irisDevice.getAccount());
      }
      if (irisDevice.getPlace() != null) {
         setPlaceId(irisDevice.getPlace());
      }
      if (!StringUtils.isEmpty(irisDevice.getAddress())) {
         setDriverAddress(irisDevice.getAddress());
      }

      // if there is an iris device, we must be registered
      setRegistrationState(RegistrationState.REGISTERED);
      AttributeMap attrs = irisDevice.getProtocolAttributes();
      // Let IpcdProtocol handle protocol attributes so all that protocol attribute handling
      // is in one place.
      IpcdProtocol.mergeProtocolAttributesIntoIpcdDevice(this, attrs);
   }

   public Device getDevice() {
      Device device = new Device();
      device.setVendor(vendor);
      device.setModel(model);
      device.setSn(sn);
      device.setIpcdver(ipcdver);
      return device;
   }

   public ProtocolDeviceId getProtocolId() {
      return IpcdProtocol.ipcdProtocolId(getDevice());
   }

   public String getProtocolAddress() {
      return protocolAddress;
   }

   public void setProtocolAddress(String protocolAddress) {
      this.protocolAddress = protocolAddress;
   }

   public String getDriverAddress() {
      return driverAddress;
   }

   public void setDriverAddress(String driverAddress) {
      this.driverAddress = driverAddress;
   }

   public UUID getAccountId() {
      return accountId;
   }

   public void setAccountId(UUID accountId) {
      this.accountId = accountId;
   }

   public UUID getPlaceId() {
      return placeId;
   }

   public void setPlaceId(UUID placeId) {
      this.placeId = placeId;
   }

   public Date getCreated() {
      return created;
   }

   public void setCreated(Date created) {
      this.created = created;
   }

   public Date getModified() {
      return modified;
   }

   public void setModified(Date modified) {
      this.modified = modified;
   }

   public Date getLastConnected() {
      return lastConnected;
   }

   public void setLastConnected(Date lastConnected) {
      this.lastConnected = lastConnected;
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

   public String getSn() {
      return sn;
   }

   public void setSn(String sn) {
      this.sn = sn;
   }

   public String getIpcdver() {
      return ipcdver;
   }

   public void setIpcdver(String ipcdver) {
      this.ipcdver = ipcdver;
   }

   public String getFirmware() {
      return firmware;
   }

   public void setFirmware(String firmware) {
      this.firmware = firmware;
   }

   public String getConnection() {
      return connection;
   }

   public void setConnection(String connection) {
      this.connection = connection;
   }

   public Set<String> getActions() {
      return actions;
   }

   public void setActions(Collection<String> actions) {
      this.actions = ImmutableSet.copyOf(actions);
   }

   public Set<String> getCommands() {
      return commands;
   }

   public void setCommands(Collection<String> commands) {
      this.commands = ImmutableSet.copyOf(commands);
   }

   public String getV1DeviceId() {
      return v1DeviceId;
   }

   public void setV1DeviceId(String v1DeviceId) {
      this.v1DeviceId = v1DeviceId;
   }

   public ConnState getConnState() {
      return connState;
   }

   public void setConnState(ConnState connState) {
      this.connState = connState;
   }

   public RegistrationState getRegistrationState() {
      return registrationState;
   }

   public void setRegistrationState(RegistrationState registrationState) {
      this.registrationState = registrationState;
   }

   @Override
   public IpcdDevice copy() {
      try {
         return (IpcdDevice)clone();
      } catch (CloneNotSupportedException e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((accountId == null) ? 0 : accountId.hashCode());
      result = prime * result + ((actions == null) ? 0 : actions.hashCode());
      result = prime * result + ((commands == null) ? 0 : commands.hashCode());
      result = prime * result + ((connState == null) ? 0 : connState.hashCode());
      result = prime * result + ((connection == null) ? 0 : connection.hashCode());
      result = prime * result + ((created == null) ? 0 : created.hashCode());
      result = prime * result + ((driverAddress == null) ? 0 : driverAddress.hashCode());
      result = prime * result + ((firmware == null) ? 0 : firmware.hashCode());
      result = prime * result + ((ipcdver == null) ? 0 : ipcdver.hashCode());
      result = prime * result + ((lastConnected == null) ? 0 : lastConnected.hashCode());
      result = prime * result + ((model == null) ? 0 : model.hashCode());
      result = prime * result + ((modified == null) ? 0 : modified.hashCode());
      result = prime * result + ((placeId == null) ? 0 : placeId.hashCode());
      result = prime * result + ((protocolAddress == null) ? 0 : protocolAddress.hashCode());
      result = prime * result + ((registrationState == null) ? 0 : registrationState.hashCode());
      result = prime * result + ((sn == null) ? 0 : sn.hashCode());
      result = prime * result + ((v1DeviceId == null) ? 0 : v1DeviceId.hashCode());
      result = prime * result + ((vendor == null) ? 0 : vendor.hashCode());
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
      IpcdDevice other = (IpcdDevice) obj;
      if (accountId == null) {
         if (other.accountId != null)
            return false;
      } else if (!accountId.equals(other.accountId))
         return false;
      if (actions == null) {
         if (other.actions != null)
            return false;
      } else if (!actions.equals(other.actions))
         return false;
      if (commands == null) {
         if (other.commands != null)
            return false;
      } else if (!commands.equals(other.commands))
         return false;
      if (connState != other.connState)
         return false;
      if (connection == null) {
         if (other.connection != null)
            return false;
      } else if (!connection.equals(other.connection))
         return false;
      if (created == null) {
         if (other.created != null)
            return false;
      } else if (!created.equals(other.created))
         return false;
      if (driverAddress == null) {
         if (other.driverAddress != null)
            return false;
      } else if (!driverAddress.equals(other.driverAddress))
         return false;
      if (firmware == null) {
         if (other.firmware != null)
            return false;
      } else if (!firmware.equals(other.firmware))
         return false;
      if (ipcdver == null) {
         if (other.ipcdver != null)
            return false;
      } else if (!ipcdver.equals(other.ipcdver))
         return false;
      if (lastConnected == null) {
         if (other.lastConnected != null)
            return false;
      } else if (!lastConnected.equals(other.lastConnected))
         return false;
      if (model == null) {
         if (other.model != null)
            return false;
      } else if (!model.equals(other.model))
         return false;
      if (modified == null) {
         if (other.modified != null)
            return false;
      } else if (!modified.equals(other.modified))
         return false;
      if (placeId == null) {
         if (other.placeId != null)
            return false;
      } else if (!placeId.equals(other.placeId))
         return false;
      if (protocolAddress == null) {
         if (other.protocolAddress != null)
            return false;
      } else if (!protocolAddress.equals(other.protocolAddress))
         return false;
      if (registrationState != other.registrationState)
         return false;
      if (sn == null) {
         if (other.sn != null)
            return false;
      } else if (!sn.equals(other.sn))
         return false;
      if (v1DeviceId == null) {
         if (other.v1DeviceId != null)
            return false;
      } else if (!v1DeviceId.equals(other.v1DeviceId))
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
      return "IpcdDevice [protocolAddress=" + protocolAddress + ", driverAddress=" + driverAddress + ", accountId="
            + accountId + ", placeId=" + placeId + ", created=" + created + ", modified=" + modified
            + ", lastConnected=" + lastConnected + ", vendor=" + vendor + ", model=" + model + ", sn=" + sn
            + ", ipcdver=" + ipcdver + ", firmware=" + firmware + ", connection=" + connection + ", actions=" + actions
            + ", commands=" + commands + ", v1DeviceId=" + v1DeviceId + ", connState=" + connState
            + ", registrationState=" + registrationState + "]";
   }
}


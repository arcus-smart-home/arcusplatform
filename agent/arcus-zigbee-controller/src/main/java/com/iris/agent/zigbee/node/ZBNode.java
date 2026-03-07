/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019 Arcus Project
 *
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

package com.iris.agent.zigbee.node;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.iris.agent.zigbee.events.ZBEventDispatcher;
import com.iris.agent.zigbee.events.ZBNodeGoneOfflineEvent;
import com.iris.agent.zigbee.events.ZBNodeGoneOnlineEvent;
import com.iris.messages.address.Address;
import com.iris.messages.address.ProtocolDeviceId;

public class ZBNode {

   private final long ieeeAddr;
   private int nwkAddr;
   private int parentAddr;
   private int state;

   // Node descriptor fields
   private int maximumIncomingTransferSize;
   private int maximumOutgoingTransferSize;
   private int nodeFlags;
   private int serverMask;
   private int manufacturerCode;
   private int descriptorCapability;
   private int maximumBufferSize;
   private int macCapabilityFlags;

   // Power descriptor
   private int powerDescriptor;

   // Device capability from Device Announce
   private int deviceCapability;

   // Basic cluster attributes (for driver matching)
   private String vendor;
   private String model;

   private boolean online;
   private int offlineTimeout;
   private int strikes;
   private long lastCall;

   private final ProtocolDeviceId deviceId;

   public ZBNode(long ieeeAddr, int nwkAddr, int parentAddr, int state,
                 int maximumIncomingTransferSize, int maximumOutgoingTransferSize,
                 int nodeFlags, int serverMask, int manufacturerCode,
                 int descriptorCapability, int maximumBufferSize, int macCapabilityFlags,
                 int powerDescriptor, int deviceCapability, boolean online, int offlineTimeout,
                 String vendor, String model) {
      this.ieeeAddr = ieeeAddr;
      this.nwkAddr = nwkAddr;
      this.parentAddr = parentAddr;
      this.state = state;
      this.maximumIncomingTransferSize = maximumIncomingTransferSize;
      this.maximumOutgoingTransferSize = maximumOutgoingTransferSize;
      this.nodeFlags = nodeFlags;
      this.serverMask = serverMask;
      this.manufacturerCode = manufacturerCode;
      this.descriptorCapability = descriptorCapability;
      this.maximumBufferSize = maximumBufferSize;
      this.macCapabilityFlags = macCapabilityFlags;
      this.powerDescriptor = powerDescriptor;
      this.deviceCapability = deviceCapability;
      this.online = online;
      this.offlineTimeout = offlineTimeout;
      this.vendor = vendor;
      this.model = model;
      this.strikes = 0;
      this.lastCall = 0;
      this.deviceId = computeDeviceId(ieeeAddr);
   }

   public static ZBNodeBuilder builder(long ieeeAddr) {
      return new ZBNodeBuilder(ieeeAddr);
   }

   public static ProtocolDeviceId computeDeviceId(long ieeeAddr) {
      ByteBuffer buf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
      buf.putLong(ieeeAddr);
      return ProtocolDeviceId.fromBytes(buf.array());
   }

   public Address getProtocolAddress() {
      return Address.hubProtocolAddress(
            com.iris.agent.hal.IrisHal.getHubId(), "ZIGB", deviceId);
   }

   public long getIeeeAddr() {
      return ieeeAddr;
   }

   public int getNwkAddr() {
      return nwkAddr;
   }

   public void setNwkAddr(int nwkAddr) {
      this.nwkAddr = nwkAddr;
   }

   public int getParentAddr() {
      return parentAddr;
   }

   public void setParentAddr(int parentAddr) {
      this.parentAddr = parentAddr;
   }

   public int getState() {
      return state;
   }

   public void setState(int state) {
      this.state = state;
   }

   public int getMaximumIncomingTransferSize() {
      return maximumIncomingTransferSize;
   }

   public void setMaximumIncomingTransferSize(int maximumIncomingTransferSize) {
      this.maximumIncomingTransferSize = maximumIncomingTransferSize;
   }

   public int getMaximumOutgoingTransferSize() {
      return maximumOutgoingTransferSize;
   }

   public void setMaximumOutgoingTransferSize(int maximumOutgoingTransferSize) {
      this.maximumOutgoingTransferSize = maximumOutgoingTransferSize;
   }

   public int getNodeFlags() {
      return nodeFlags;
   }

   public void setNodeFlags(int nodeFlags) {
      this.nodeFlags = nodeFlags;
   }

   public int getServerMask() {
      return serverMask;
   }

   public void setServerMask(int serverMask) {
      this.serverMask = serverMask;
   }

   public int getManufacturerCode() {
      return manufacturerCode;
   }

   public void setManufacturerCode(int manufacturerCode) {
      this.manufacturerCode = manufacturerCode;
   }

   public int getDescriptorCapability() {
      return descriptorCapability;
   }

   public void setDescriptorCapability(int descriptorCapability) {
      this.descriptorCapability = descriptorCapability;
   }

   public int getMaximumBufferSize() {
      return maximumBufferSize;
   }

   public void setMaximumBufferSize(int maximumBufferSize) {
      this.maximumBufferSize = maximumBufferSize;
   }

   public int getMacCapabilityFlags() {
      return macCapabilityFlags;
   }

   public void setMacCapabilityFlags(int macCapabilityFlags) {
      this.macCapabilityFlags = macCapabilityFlags;
   }

   public int getPowerDescriptor() {
      return powerDescriptor;
   }

   public int getDeviceCapability() {
      return deviceCapability;
   }

   public boolean isOnline() {
      return online;
   }

   public void setOnline(boolean online) {
      if (this.online != online) {
         this.online = online;
         if (online) {
            ZBEventDispatcher.INSTANCE.dispatch(new ZBNodeGoneOnlineEvent(ieeeAddr));
         } else {
            ZBEventDispatcher.INSTANCE.dispatch(new ZBNodeGoneOfflineEvent(ieeeAddr));
         }
      }
   }

   public int getOfflineTimeout() {
      return offlineTimeout;
   }

   public void setOfflineTimeout(int offlineTimeout) {
      this.offlineTimeout = offlineTimeout;
   }

   public int getStrikes() {
      return strikes;
   }

   public void setStrikes(int strikes) {
      this.strikes = strikes;
   }

   public long getLastCall() {
      return lastCall;
   }

   public void setLastCall(long lastCall) {
      this.lastCall = lastCall;
      this.strikes = 0;
   }

   public ProtocolDeviceId getDeviceId() {
      return deviceId;
   }

   /**
    * Checks if the device is a sleepy end-device based on MAC capability flags.
    * Bit 3 (0x08) indicates "Receiver on when idle" - if NOT set, device is sleepy.
    */
   public boolean isSleepyDevice() {
      return (macCapabilityFlags & 0x08) == 0;
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
}

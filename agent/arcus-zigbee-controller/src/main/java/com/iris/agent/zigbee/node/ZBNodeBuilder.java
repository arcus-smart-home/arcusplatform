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

public class ZBNodeBuilder {
   private final long ieeeAddr;
   private int nwkAddr = 0;
   private int parentAddr = 0;
   private int state = 0;
   private int maximumIncomingTransferSize = 0;
   private int maximumOutgoingTransferSize = 0;
   private int nodeFlags = 0;
   private int serverMask = 0;
   private int manufacturerCode = 0;
   private int descriptorCapability = 0;
   private int maximumBufferSize = 0;
   private int macCapabilityFlags = 0;
   private int powerDescriptor = 0;
   private int deviceCapability = 0;
   private boolean online = true;
   private int offlineTimeout = 0;
   private String vendor;
   private String model;

   public ZBNodeBuilder(long ieeeAddr) {
      this.ieeeAddr = ieeeAddr;
   }

   public ZBNodeBuilder setNwkAddr(int nwkAddr) {
      this.nwkAddr = nwkAddr;
      return this;
   }

   public ZBNodeBuilder setParentAddr(int parentAddr) {
      this.parentAddr = parentAddr;
      return this;
   }

   public ZBNodeBuilder setState(int state) {
      this.state = state;
      return this;
   }

   public ZBNodeBuilder setMaximumIncomingTransferSize(int maximumIncomingTransferSize) {
      this.maximumIncomingTransferSize = maximumIncomingTransferSize;
      return this;
   }

   public ZBNodeBuilder setMaximumOutgoingTransferSize(int maximumOutgoingTransferSize) {
      this.maximumOutgoingTransferSize = maximumOutgoingTransferSize;
      return this;
   }

   public ZBNodeBuilder setNodeFlags(int nodeFlags) {
      this.nodeFlags = nodeFlags;
      return this;
   }

   public ZBNodeBuilder setServerMask(int serverMask) {
      this.serverMask = serverMask;
      return this;
   }

   public ZBNodeBuilder setManufacturerCode(int manufacturerCode) {
      this.manufacturerCode = manufacturerCode;
      return this;
   }

   public ZBNodeBuilder setDescriptorCapability(int descriptorCapability) {
      this.descriptorCapability = descriptorCapability;
      return this;
   }

   public ZBNodeBuilder setMaximumBufferSize(int maximumBufferSize) {
      this.maximumBufferSize = maximumBufferSize;
      return this;
   }

   public ZBNodeBuilder setMacCapabilityFlags(int macCapabilityFlags) {
      this.macCapabilityFlags = macCapabilityFlags;
      return this;
   }

   public ZBNodeBuilder setPowerDescriptor(int powerDescriptor) {
      this.powerDescriptor = powerDescriptor;
      return this;
   }

   public ZBNodeBuilder setDeviceCapability(int deviceCapability) {
      this.deviceCapability = deviceCapability;
      return this;
   }

   public ZBNodeBuilder setOnline(boolean online) {
      this.online = online;
      return this;
   }

   public ZBNodeBuilder setOfflineTimeout(int offlineTimeout) {
      this.offlineTimeout = offlineTimeout;
      return this;
   }

   public ZBNodeBuilder setVendor(String vendor) {
      this.vendor = vendor;
      return this;
   }

   public ZBNodeBuilder setModel(String model) {
      this.model = model;
      return this;
   }

   public ZBNode build() {
      return new ZBNode(ieeeAddr, nwkAddr, parentAddr, state,
            maximumIncomingTransferSize, maximumOutgoingTransferSize,
            nodeFlags, serverMask, manufacturerCode,
            descriptorCapability, maximumBufferSize, macCapabilityFlags,
            powerDescriptor, deviceCapability, online, offlineTimeout,
            vendor, model);
   }
}

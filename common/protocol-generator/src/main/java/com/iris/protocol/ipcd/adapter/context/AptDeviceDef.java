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
package com.iris.protocol.ipcd.adapter.context;

public class AptDeviceDef {
   private AptDeviceValue vendor;
   private AptDeviceValue model;
   private AptDeviceValue sn;
   private AptDeviceValue ipcdver;
   private AptDeviceValue fwver;
   private AptDeviceValue connectURL;
   private AptDeviceValue connection;
   private AptDeviceValue actions;
   private AptDeviceValue commands;
   
   public AptDeviceValue getVendor() {
      return vendor;
   }
   public void setVendor(AptDeviceValue vendor) {
      this.vendor = vendor;
   }
   public AptDeviceValue getModel() {
      return model;
   }
   public void setModel(AptDeviceValue model) {
      this.model = model;
   }
   public AptDeviceValue getSn() {
      return sn;
   }
   public void setSn(AptDeviceValue sn) {
      this.sn = sn;
   }
   public AptDeviceValue getIpcdver() {
      return ipcdver;
   }
   public void setIpcdver(AptDeviceValue ipcdver) {
      this.ipcdver = ipcdver;
   }
   public AptDeviceValue getFwver() {
      return fwver;
   }
   public void setFwver(AptDeviceValue fwver) {
      this.fwver = fwver;
   }
   public AptDeviceValue getConnectURL() {
      return connectURL;
   }
   public void setConnectURL(AptDeviceValue connectURL) {
      this.connectURL = connectURL;
   }
   public AptDeviceValue getConnection() {
      return connection;
   }
   public void setConnection(AptDeviceValue connection) {
      this.connection = connection;
   }
   public AptDeviceValue getActions() {
      return actions;
   }
   public void setActions(AptDeviceValue actions) {
      this.actions = actions;
   }
   public AptDeviceValue getCommands() {
      return commands;
   }
   public void setCommands(AptDeviceValue commands) {
      this.commands = commands;
   }
   
   
}


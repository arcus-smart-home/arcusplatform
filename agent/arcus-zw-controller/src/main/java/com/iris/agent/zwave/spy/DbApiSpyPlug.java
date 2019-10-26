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
package com.iris.agent.zwave.spy;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.iris.agent.spy.SpyPlugIn;
import com.iris.agent.util.ByteUtils;
import com.iris.agent.zwave.db.KeyValuePair;
import com.iris.agent.zwave.db.ZWDao;
import com.iris.agent.zwave.node.ZWNode;

public class DbApiSpyPlug implements SpyPlugIn {

   @Override
   public Object apply(HttpServletRequest input) {
      List<KeyValuePair> configs = ZWDao.getConfig();
      List<ZWNode> nodes = ZWDao.getAllNodes();
      return new ZipDatabase(configs, nodes);  
   }

   @Override
   public boolean showLink() {
      return false;
   }

   @Override
   public String pageName() {
      return "getzipdb";
   }

   @Override
   public String title() {
      return "";
   }
   
   private static class ZipDatabase {
      private final List<KeyValuePair> configs;
      private final List<ZipNodeModel> nodes;
      
      ZipDatabase(List<KeyValuePair> configs, List<ZWNode> nodes) {
         this.configs = new ArrayList<>();
         for (KeyValuePair pair : configs) {
            if (pair.getKey().equals("hubzip:homeid")) {
               this.configs.add(new KeyValuePair(pair.getKey(), String.format("%08x", Integer.parseInt(pair.getValue()))));
            }
            else {
               this.configs.add(new KeyValuePair(pair.getKey(), pair.getValue()));
            }
         }
         
         this.nodes = new ArrayList<>();
         for (ZWNode node : nodes) {
            ZipNodeModel model = new ZipNodeModel();
            model.setNodeId(String.valueOf(node.getNodeId()));
            model.setBasicDeviceType(String.format("%02x", node.getBasicDeviceType()));
            model.setGenericDeviceType(String.format("%02x", node.getGenericDeviceType()));
            model.setSpecificDeviceType(String.format("%02x", node.getSpecificDeviceType()));
            model.setManufacturerId(String.format("%02x", node.getManufacturerId()));
            model.setProductId(String.format("%02x", node.getProductId()));
            model.setProductTypeId(String.format("%02x", node.getProductTypeId()));
            model.setOnline(node.isOnline() ? "TRUE" : "FALSE");
            model.setOfflineTimeout(String.valueOf(node.getOfflineTimeout()));
            model.setCmdClassSet(ByteUtils.byteArray2SpacedString(node.getCmdClassBytes()));
            this.nodes.add(model);
         }
      }

      @SuppressWarnings("unused")
      public List<KeyValuePair> getConfigs() {
         return configs;
      }

      @SuppressWarnings("unused")
      public List<ZipNodeModel> getNodes() {
         return nodes;
      }
   }
   
   @SuppressWarnings("unused")
   private static class ZipNodeModel {
      private String nodeId;
      private String basicDeviceType;
      private String genericDeviceType;
      private String specificDeviceType;
      private String manufacturerId;
      private String productTypeId;
      private String productId;
      private String online;
      private String offlineTimeout;
      private String cmdClassSet;
      
      public String getNodeId() {
         return nodeId;
      }
      public void setNodeId(String nodeId) {
         this.nodeId = nodeId;
      }
      public String getBasicDeviceType() {
         return basicDeviceType;
      }
      public void setBasicDeviceType(String basicDeviceType) {
         this.basicDeviceType = basicDeviceType;
      }
      public String getGenericDeviceType() {
         return genericDeviceType;
      }
      public void setGenericDeviceType(String genericDeviceType) {
         this.genericDeviceType = genericDeviceType;
      }
      public String getSpecificDeviceType() {
         return specificDeviceType;
      }
      public void setSpecificDeviceType(String specificDeviceType) {
         this.specificDeviceType = specificDeviceType;
      }
      public String getManufacturerId() {
         return manufacturerId;
      }
      public void setManufacturerId(String manufacturerId) {
         this.manufacturerId = manufacturerId;
      }
      public String getProductTypeId() {
         return productTypeId;
      }
      public void setProductTypeId(String productTypeId) {
         this.productTypeId = productTypeId;
      }
      public String getProductId() {
         return productId;
      }
      public void setProductId(String productId) {
         this.productId = productId;
      }
      public String getOnline() {
         return online;
      }
      public void setOnline(String online) {
         this.online = online;
      }
      public String getOfflineTimeout() {
         return offlineTimeout;
      }
      public void setOfflineTimeout(String offlineTimeout) {
         this.offlineTimeout = offlineTimeout;
      }
      public String getCmdClassSet() {
         return cmdClassSet;
      }
      public void setCmdClassSet(String cmdClassSet) {
         this.cmdClassSet = cmdClassSet;
      }
   }

}


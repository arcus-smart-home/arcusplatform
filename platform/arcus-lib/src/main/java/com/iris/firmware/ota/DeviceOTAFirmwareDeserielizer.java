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
package com.iris.firmware.ota;


import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.math.NumberUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.iris.io.xml.BaseJAXPDeserializer;

public class DeviceOTAFirmwareDeserielizer extends BaseJAXPDeserializer<DeviceOTAFirmware>{

   private static final String FIRMWARE_ELEMENT_NAME = "firmware";
   private static final String DEFAULTS_ELEMENT_NAME = "configDefaults";
   private static final String FROM_VERSIONS_NAME  = "from-versions";
   private static final String VERSION_NAME  = "version";
   private static final String ANY_NAMESPACE = "*";
   private static final String DEVICE_OTA_FIRMWARE_XSD = "classpath:/schema/ota/device-ota-firmware.xsd";

   public DeviceOTAFirmware fromJAXP(Document rootElement){
      DeviceOTAFirmware firmwares = new DeviceOTAFirmware();

      NodeList defaults = rootElement.getElementsByTagNameNS(ANY_NAMESPACE, DEFAULTS_ELEMENT_NAME);
      for(int i=0;i<defaults.getLength();i++){
         Element el = (Element)defaults.item(i);
         firmwares.setRetryAttemptsMax(Integer.parseInt(el.getAttribute("retryAttemptsMax")));
         firmwares.setRetryIntervalMins(Integer.parseInt(el.getAttribute("retryIntervalMins")));
      }
      
      NodeList firmwareElements = rootElement.getElementsByTagNameNS(ANY_NAMESPACE, FIRMWARE_ELEMENT_NAME);
      
      for(int i=0;i<firmwareElements.getLength();i++){
         Element el = (Element)firmwareElements.item(i);
         DeviceOTAFirmwareItem fw = mapDeviceOTAFirmware(el);
         firmwares.getFirmwares().add(fw);
      }
      return firmwares;
   }
   
   @Override
   public String schemaResource() {
      return DEVICE_OTA_FIRMWARE_XSD;
   }

   private DeviceOTAFirmwareItem mapDeviceOTAFirmware(Element element){      
      String population = element.getAttribute("populations");
      String productId = element.getAttribute("productId");
      String version = element.getAttribute("version");
      String path = element.getAttribute("path");
      String retryAttemptsMax = element.getAttribute("retryAttemptsMax");
      String retryIntervalMins = element.getAttribute("retryIntervalMins");
      String md5 = element.getAttribute("md5");
      List<DeviceOTAFirmwareFromVersion> fromVersions = new ArrayList<DeviceOTAFirmwareFromVersion>();
      
      Integer retry = null;
      Integer interval = null;
      
      if(retryAttemptsMax!=null && NumberUtils.isNumber(retryAttemptsMax)){
         retry=Integer.parseInt(retryAttemptsMax);
      }
      if(retryIntervalMins!=null && NumberUtils.isNumber(retryIntervalMins)){
         interval=Integer.parseInt(retryIntervalMins);
      }
      
      NodeList nodes = element.getElementsByTagNameNS(ANY_NAMESPACE,FROM_VERSIONS_NAME);
      
      for(int i=0;i<nodes.getLength();i++){
         Element el = (Element)nodes.item(i);
         mapFromVersions(el,fromVersions);
      }
      return new DeviceOTAFirmwareItem(population, productId, version, path,interval,retry, md5, fromVersions);
   }
   private void mapFromVersions(Element element,List<DeviceOTAFirmwareFromVersion> fromVersions ) {
      NodeList nodes = element.getElementsByTagNameNS(ANY_NAMESPACE,VERSION_NAME);
      
      for(int i=0;i<nodes.getLength();i++){
         Element el = (Element)nodes.item(i);
         DeviceOTAFirmwareFromVersion fromVersion = mapVersion(el);
         fromVersions.add(fromVersion);
      }
   }
   private DeviceOTAFirmwareFromVersion mapVersion(Element el) {
      String type = el.getAttribute("type");
      String value = el.getAttribute("match");      
      
      return new DeviceOTAFirmwareFromVersion(type,value);
   }
   
}


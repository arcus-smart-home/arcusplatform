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


import java.util.UUID;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.iris.io.xml.BaseJAXPDeserializer;

public class DeviceOTAPlaceDeserielizer extends BaseJAXPDeserializer<DeviceOTAPlaces>{

   private static final String PLACES_ELEMENT_NAME = "places";
   private static final String PLACE_ELEMENT_NAME = "place";
   private static final String PLACE_ID_NAME = "id";
   private static final String ANY_NAMESPACE = "*";
   private static final String DEVICE_OTA_PLACES_XSD = "classpath:/schema/ota/device-ota-places.xsd";

   public DeviceOTAPlaces fromJAXP(Document rootElement) {
      DeviceOTAPlaces places = new DeviceOTAPlaces();
            
      NodeList placesElement = rootElement.getElementsByTagNameNS(ANY_NAMESPACE, PLACES_ELEMENT_NAME);
      NodeList placeElements = ((Element) placesElement.item(0)).getElementsByTagNameNS(ANY_NAMESPACE, PLACE_ELEMENT_NAME);
      
      
      for(int i=0;i<placeElements.getLength();i++){
         Element el = (Element)placeElements.item(i);
         String id = el.getAttribute(PLACE_ID_NAME);
         UUID uuid = UUID.fromString(id);
         places.getPlaces().add(uuid);
      }
      return places;
   }
   
   @Override
   public String schemaResource() {
      return DEVICE_OTA_PLACES_XSD;
   }

   
}


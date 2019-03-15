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
package com.iris.platform.subsystem.placemonitor.smarthomealert;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.collect.ImmutableMap;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.messages.address.Address;
import com.iris.messages.capability.HubCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.dev.DeviceModel;
import com.iris.messages.model.subs.PlaceMonitorSubsystemModel;
import com.iris.messages.type.Population;
import com.iris.messages.type.SmartHomeAlert;
import com.iris.prodcat.ProductCatalogEntry;
import com.iris.prodcat.ProductCatalogManager;

public enum SmartHomeAlerts {
   ;

   public static final String TEMPLATE_NAME = "alert";

   public static final String CONTEXT_ATTR_DEVICENAME = "devicename";
   public static final String CONTEXT_ATTR_KEY = "key";
   public static final String CONTEXT_ATTR_TIME = "time";
   public static final String CONTEXT_ATTR_SEVERITY = "severity";
   public static final String CONTEXT_ATTR_DEVICEID = "deviceid";
   public static final String CONTEXT_ATTR_DEVICETYPE = "devicetype";
   public static final String CONTEXT_ATTR_DEVICEVENDOR = "devicevendor";
   public static final String CONTEXT_ATTR_PRODUCTCATALOGID = "productcatalogid";
   public static final String CONTEXT_ATTR_BATTERYTYPE = "batterytype";
   public static final String CONTEXT_ATTR_BATTERYNUMBER = "batterynumber";
   public static final String CONTEXT_ATTR_HUBID = "hubid";
   public static final String CONTEXT_ATTR_POWERSRC = "powersrc";

   public static SmartHomeAlert create(
      String alertType,
      String severity,
      Address subjectAddr,
      Map<String, Object> attributes,
      UUID placeId
   ) {
      return create(alertType, severity, subjectAddr.getRepresentation(), attributes, String.valueOf(placeId));
   }

   public static SmartHomeAlert create(
      String alertType,
      String severity,
      String subjectAddr,
      Map<String, Object> attributes,
      UUID placeId
   ) {
      return create(alertType, severity, subjectAddr, attributes, String.valueOf(placeId));
   }

   public static SmartHomeAlert create(
      String alertType,
      String severity,
      Address subjectAddr,
      Map<String, Object> attributes,
      String placeId
   ) {
      return create(alertType, severity, subjectAddr.getRepresentation(), attributes, placeId);
   }

   public static SmartHomeAlert create(
      String alertType,
      String severity,
      String subjectAddr,
      Map<String, Object> attributes,
      String placeId
   ) {
      SmartHomeAlert alert = new SmartHomeAlert();
      alert.setAlertkey(AlertKeys.key(alertType, subjectAddr));
      alert.setSeverity(severity);
      alert.setAlerttype(alertType);
      alert.setAttributes(attributes);
      alert.setCreated(new Date());
      alert.setSubjectaddr(subjectAddr);
      return alert;
   }

   public static Map<String, Object> baseDeviceAttribues(Model m, ProductCatalogManager productCat) {
      ProductCatalogEntry entry = productCat.getCatalog(Population.NAME_GENERAL).getProductById(DeviceModel.getProductId(m));
      String vendor = entry == null ? DeviceModel.getVendor(m, "Unknown") : entry.getVendor();
      String prodcatId = entry == null ? DeviceModel.getProductId(m, "") : entry.getId();
      return ImmutableMap.of(
         CONTEXT_ATTR_DEVICEID, m.getId(),
         CONTEXT_ATTR_DEVICENAME, DeviceModel.getName(m, ""),
         CONTEXT_ATTR_DEVICETYPE, DeviceModel.getDevtypehint(m),
         CONTEXT_ATTR_DEVICEVENDOR, vendor,
         CONTEXT_ATTR_PRODUCTCATALOGID, prodcatId
      );
   }
   
   @Nullable
   public static Model hubModel(SubsystemContext<PlaceMonitorSubsystemModel> context) {
      Iterable<Model> hubs = context.models().getModelsByType(HubCapability.NAMESPACE);
      Iterator<Model> ihubs = (hubs != null) ? hubs.iterator() : null;
      return (ihubs != null && ihubs.hasNext()) ? ihubs.next() : null;
   }
}


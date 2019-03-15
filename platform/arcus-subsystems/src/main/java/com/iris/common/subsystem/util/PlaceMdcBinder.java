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
package com.iris.common.subsystem.util;

import com.iris.messages.model.Model;
import com.iris.messages.model.serv.PlaceModel;
import com.iris.util.MdcContext;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Locale;

public class PlaceMdcBinder {

   private static final Logger LOGGER = LoggerFactory.getLogger(PlaceMdcBinder.class);

   private Model model;

   private PlaceMdcBinder() {}

   public PlaceMdcBinder(Model model) {
      this.model = model;
   }

   public void bind() {
      bindPlace();
      bindLocation();
      bindServiceLevel();
   }

   private void bindServiceLevel() {
      if (!needsBound(MdcContext.MDC_SERVICE_LEVEL)) return;

      String serviceLevel = PlaceModel.getServiceLevel(model);
      if (serviceLevel == null) {
         LOGGER.trace("Binding mdc service level skipped because serviceLevel was null.");
         return;
      }

      MDC.put(MdcContext.MDC_SERVICE_LEVEL, serviceLevel);
   }

   private void bindLocation() {
      if (!needsBound(MdcContext.MDC_LOCATION)) return;

      Double latitude = PlaceModel.getAddrLatitude(model);
      Double longitude = PlaceModel.getAddrLongitude(model);
      if (latitude == null || longitude == null) {
         LOGGER.trace("Binding mdc location skipped because latitude or longitude was null.");
         return;
      }

      String location = String.format(Locale.US, "%.5f, %.5f", latitude, longitude);
      MDC.put(MdcContext.MDC_LOCATION, location);

   }

   private void bindPlace() {
      if (!needsBound(MdcContext.MDC_PLACE)) return;

      MDC.put(MdcContext.MDC_PLACE, model.getId());
   }

   private boolean needsBound(String key) {
      String existing = MDC.get(key);
      if (StringUtils.trimToNull(existing) != null) {
         LOGGER.trace("MDC property {} already bound.", key);
         return false;
      }

      if (model == null) {
         LOGGER.debug("Binding mdc property {} skipped because model was null.", key);
         return false;
      }
      return true;
   }

}


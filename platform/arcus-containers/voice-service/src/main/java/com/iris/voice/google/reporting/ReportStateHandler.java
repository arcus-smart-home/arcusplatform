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
package com.iris.voice.google.reporting;

import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.iris.google.Predicates;
import com.iris.google.Transformers;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.ColorCapability;
import com.iris.messages.capability.ColorTemperatureCapability;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.capability.DimmerCapability;
import com.iris.messages.capability.DoorLockCapability;
import com.iris.messages.capability.FanCapability;
import com.iris.messages.capability.LightCapability;
import com.iris.messages.capability.SwitchCapability;
import com.iris.messages.capability.TemperatureCapability;
import com.iris.messages.capability.ThermostatCapability;
import com.iris.messages.model.Model;
import com.iris.prodcat.ProductCatalogEntry;
import com.iris.prodcat.ProductCatalogManager;
import com.iris.voice.VoiceUtil;
import com.iris.voice.context.VoiceContext;
import com.iris.voice.google.GoogleWhitelist;
import com.iris.voice.google.homegraph.HomeGraphAPI;
import com.iris.voice.proactive.ProactiveReportHandler;

public class ReportStateHandler implements ProactiveReportHandler {

   private static final Logger logger = LoggerFactory.getLogger(ReportStateHandler.class);

   // @formatter:off
   // These are the attributes that will trigger a ReportState if changed
   private static final Set<String> interestingAttributes = ImmutableSet.of(
         SwitchCapability.ATTR_STATE,
         DimmerCapability.ATTR_BRIGHTNESS,
         FanCapability.ATTR_SPEED,
         ColorCapability.ATTR_HUE,
         ColorCapability.ATTR_SATURATION,
         ColorTemperatureCapability.ATTR_COLORTEMP,
         DoorLockCapability.ATTR_LOCKSTATE,
         TemperatureCapability.ATTR_TEMPERATURE,
         ThermostatCapability.ATTR_COOLSETPOINT,
         ThermostatCapability.ATTR_HEATSETPOINT,
         ThermostatCapability.ATTR_HVACMODE,
         LightCapability.ATTR_COLORMODE,
         DeviceAdvancedCapability.ATTR_ERRORS
         // DeviceConnectionCapability.ATTR_STATE // New devices would trigger a ReportState before the Sync went out.  Better to let the SYNC code handle the ReportState for this attribute.

   );
   // @formatter:on

   private final GoogleWhitelist whitelist;
   private final ProductCatalogManager prodCat;
   private final HomeGraphAPI homegraph;

   public ReportStateHandler(HomeGraphAPI homegraph, ProductCatalogManager prodCat, GoogleWhitelist whitelist) {
      this.homegraph = homegraph;
      this.prodCat = prodCat;
      this.whitelist = whitelist;
   }

   @Override
   public boolean isInterestedIn(VoiceContext context, Model m, MessageBody body) {
      boolean whitelisted = whitelist.isWhitelisted(context.getPlaceId());
      ProductCatalogEntry entry = VoiceUtil.getProduct(prodCat, m);

      switch (body.getMessageType()) {
         //case Capability.EVENT_ADDED: // When adding and deleting devices, the ReportState needs to come after the SYNC response. (Note, this is different than the SYNC request)
         //case Capability.EVENT_DELETED:  // Report State is triggered in RequestHandler#handleSync
         case Capability.EVENT_VALUE_CHANGE:
            return hasInterestingReportStateAttribute(body) && Predicates.isSupportedModel(m, whitelisted, entry);
         default:
            return false;
      }
   }

   /**
    * Only send the Report State if an attribute Google is tracking has changed.  See {@link Transformers#modelToStateMap(Model, boolean, boolean, ProductCatalogEntry)} for reference.
    */
   private boolean hasInterestingReportStateAttribute(MessageBody body) {
      Map<String, Object> attributes = body.getAttributes();
      if (attributes.keySet().stream().noneMatch(interestingAttributes::contains)) {
         return false;
      }
      return true;
   }

   @Override
   public void report(VoiceContext context, Model m, MessageBody body) {           
      homegraph.sendReportState(context);
   }
}


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
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.google.Predicates;
import com.iris.messages.MessageBody;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.SceneCapability;
import com.iris.messages.model.Model;
import com.iris.messages.service.VoiceService;
import com.iris.prodcat.ProductCatalogEntry;
import com.iris.prodcat.ProductCatalogManager;
import com.iris.voice.VoiceUtil;
import com.iris.voice.context.VoiceContext;
import com.iris.voice.google.GoogleWhitelist;
import com.iris.voice.google.homegraph.HomeGraphAPI;
import com.iris.voice.proactive.ProactiveCreds;
import com.iris.voice.proactive.ProactiveReportHandler;

public class SyncRequestHandler implements ProactiveReportHandler {

   private static final Logger logger = LoggerFactory.getLogger(SyncRequestHandler.class);

   private final GoogleWhitelist whitelist;
   private final ProductCatalogManager prodCat;
   private final HomeGraphAPI homegraph;

   public SyncRequestHandler(HomeGraphAPI homegraph, ProductCatalogManager prodCat, GoogleWhitelist whitelist) {
      this.homegraph = homegraph;
      this.prodCat = prodCat;
      this.whitelist = whitelist;
   }

   @Override
   public boolean isInterestedIn(VoiceContext context, Model m, MessageBody body) {
      boolean whitelisted = whitelist.isWhitelisted(context.getPlaceId());
      ProductCatalogEntry entry = VoiceUtil.getProduct(prodCat, m);

      switch (body.getMessageType()) {
         case Capability.EVENT_ADDED:
         case Capability.EVENT_DELETED:
            return Predicates.isSupportedModel(m, whitelisted, entry);
         case Capability.EVENT_VALUE_CHANGE:
            // don't use is supported model for scenes in this case because the actions could have
            // changed such the scene may be unsupported
            if (m.supports(SceneCapability.NAMESPACE) && hasInterestingSyncAttribute(body)) {
               return true;
            }

            return hasInterestingSyncAttribute(body) && Predicates.isSupportedModel(m, whitelisted, entry);
         default:
            return false;
      }
   }

   private boolean hasInterestingSyncAttribute(MessageBody body) {
      Map<String, Object> attrs = body.getAttributes();
      return attrs.containsKey(DeviceCapability.ATTR_NAME) || attrs.containsKey(SceneCapability.ATTR_NAME) || attrs.containsKey(SceneCapability.ATTR_ACTIONS);
   }

   @Override
   public void report(VoiceContext context, Model m, MessageBody body) {
      Optional<ProactiveCreds> creds = context.getProactiveCreds(VoiceService.StartPlaceRequest.ASSISTANT_GOOGLE);
      if (!creds.isPresent()) {
         logger.debug("Ignoring Request Sync to Google for {}, reporting is not enabled", context.getPlaceId());
         return;
      }

      homegraph.requestSync(creds.get().getAccess());
   }
}


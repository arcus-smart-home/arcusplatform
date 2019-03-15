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

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.messages.MessageBody;
import com.iris.messages.model.Model;
import com.iris.prodcat.ProductCatalogManager;
import com.iris.voice.context.VoiceContext;
import com.iris.voice.google.GoogleWhitelist;
import com.iris.voice.google.homegraph.HomeGraphAPI;
import com.iris.voice.proactive.ProactiveReportHandler;

/**
 * Currently there can only be a single handler per 'assistant'.  This is the google handler.  It handles Sync Requests and Report State.
 * Those two API call have different criteria for isInterestedIn.  Rather than jumbling the logic all together, another set of handlers was created.
 * It's not ideal, but it is easier to read.
 */
@Singleton
public class GoogleProactiveReportHandler implements ProactiveReportHandler {

   private final GoogleWhitelist whitelist;
   private final ProductCatalogManager prodCat;
   private final HomeGraphAPI homegraph;
   private final Set<ProactiveReportHandler> handlers;

   @Inject
   public GoogleProactiveReportHandler(
      GoogleWhitelist whitelist,
      ProductCatalogManager prodCat,
      HomeGraphAPI homegraph
   ) {
      this.whitelist = whitelist;
      this.prodCat = prodCat;
      this.homegraph = homegraph;
      
      this.handlers = ImmutableSet.of(
            new ReportStateHandler(this.homegraph, this.prodCat, this.whitelist),
            new SyncRequestHandler(this.homegraph, this.prodCat, this.whitelist)
      );
   }

   @Override
   public boolean isInterestedIn(VoiceContext context, Model m, MessageBody body) {
      return this.handlers.stream().anyMatch(handler -> handler.isInterestedIn(context, m, body));
   }

   @Override
   public void report(VoiceContext context, Model m, MessageBody body) {
      this.handlers.stream().forEach(handler -> {
         if (handler.isInterestedIn(context, m, body)) {
            handler.report(context, m, body);
         }
      });
   }
}


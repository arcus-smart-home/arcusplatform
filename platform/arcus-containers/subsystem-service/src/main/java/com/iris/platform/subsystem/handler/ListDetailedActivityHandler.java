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
package com.iris.platform.subsystem.handler;

import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.Utils;
import com.iris.capability.attribute.transform.BeanAttributesTransformer;
import com.iris.capability.attribute.transform.BeanListTransformer;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.CareSubsystemCapability;
import com.iris.messages.capability.CareSubsystemCapability.ListDetailedActivityRequest;
import com.iris.messages.listener.annotation.Request;
import com.iris.messages.model.subs.CareSubsystemModel;
import com.iris.platform.PagedResults;
import com.iris.platform.history.HistoryLogDAO;
import com.iris.platform.history.HistoryLogDAO.ListEntriesQuery;
import com.iris.platform.history.HistoryLogEntry;
import com.iris.platform.history.HistoryLogEntryType;

@Singleton
public class ListDetailedActivityHandler {
   public static final int DFLT_LIMIT = 10;
   
   private final BeanListTransformer<HistoryLogEntry> transformer;
   private final HistoryLogDAO logDao;

   @Inject
   public ListDetailedActivityHandler(
         BeanAttributesTransformer<HistoryLogEntry> transformer,
         HistoryLogDAO logDao
   ) {
      this.transformer = new BeanListTransformer<>(transformer);
      this.logDao = logDao;
   }

   @Request(ListDetailedActivityRequest.NAME)
   public MessageBody handleRequest(SubsystemContext<CareSubsystemModel> context, PlatformMessage msg) {
      Utils.assertNotNull(context, "context is required to list subsystem activity log entries");

      MessageBody request = msg.getValue();
      String token = CareSubsystemCapability.ListDetailedActivityRequest.getToken(request);
      Integer limit = CareSubsystemCapability.ListDetailedActivityRequest.getLimit(request);
      if(limit == null) {
         limit = DFLT_LIMIT;
      }
      
      Set<String> devices = CareSubsystemCapability.ListDetailedActivityRequest.getDevices(request);
      if(devices == null || devices.isEmpty()) {
         devices = context.model().getCareDevices();
      }
      
      ListEntriesQuery query = new ListEntriesQuery();
      query.setType(HistoryLogEntryType.DETAILED_PLACE_LOG);
      query.setId(context.getPlaceId());
      query.setLimit(limit);
      query.setToken(token);
      query.setFilter(new CareFilter(devices));
      
      PagedResults<HistoryLogEntry> results = logDao.listEntriesByQuery(query);
      return
            CareSubsystemCapability.ListDetailedActivityResponse
               .builder()
               .withNextToken(results.getNextToken())
               .withResults(transformer.convertListToAttributes(results.getResults()))
               .build();
   }

   private static class CareFilter implements Predicate<HistoryLogEntry> {
      private static final Set<String> events =
            ImmutableSet.of(
                  "device.contact.opened",
                  "device.contact.closed",
                  "device.motion.detected",
                  "device.motion.none",
                  "device.motion.detected.inst",
                  "device.motion.none.inst"
            );
      private final Set<String> addresses;

      CareFilter(Set<String> addresses) {
         this.addresses = addresses;
      }
      
      @Override
      public boolean apply(HistoryLogEntry input) {
         return events.contains(input.getMessageKey()) && addresses.contains(input.getSubjectAddress());
      }
   }
}


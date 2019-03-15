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
package com.iris.driver.service.handler;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.iris.Utils;
import com.iris.capability.attribute.transform.BeanAttributesTransformer;
import com.iris.capability.attribute.transform.BeanListTransformer;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.DeviceDriverAddress;
import com.iris.messages.capability.DeviceCapability.ListHistoryEntriesRequest;
import com.iris.messages.capability.DeviceCapability.ListHistoryEntriesResponse;
import com.iris.messages.errors.NotFoundException;
import com.iris.messages.model.Device;
import com.iris.platform.PagedResults;
import com.iris.platform.history.HistoryLogDAO;
import com.iris.platform.history.HistoryLogDAO.ListEntriesQuery;
import com.iris.platform.history.HistoryLogEntry;
import com.iris.platform.history.HistoryLogEntryType;

public class ListHistoryEntriesHandler implements DriverServiceRequestHandler, ContextualRequestMessageHandler<Device> {
   public static final int DFLT_LIMIT = 10;
   
   private static final Logger logger = LoggerFactory.getLogger(ListHistoryEntriesHandler.class);
   
   private final BeanListTransformer<HistoryLogEntry> transformer;
   private final HistoryLogDAO logDao;
   private final DeviceDAO deviceDao;

   @Inject
   public ListHistoryEntriesHandler(
         BeanAttributesTransformer<HistoryLogEntry> transformer,
         HistoryLogDAO logDao,
         DeviceDAO deviceDao
   ) {
      this.transformer = new BeanListTransformer<>(transformer);
      this.logDao = logDao;
      this.deviceDao = deviceDao;
   }

   /* (non-Javadoc)
    * @see com.iris.core.platform.PlatformRequestMessageHandler#handleMessage(com.iris.messages.PlatformMessage)
    */
   @Override
   public MessageBody handleMessage(PlatformMessage message) throws Exception {
      return handleRequest(loadDevice(message), message);
   }

   @Override
   public String getMessageType() {
      return ListHistoryEntriesRequest.NAME;
   }

   @Override
   public MessageBody handleRequest(Device context, PlatformMessage msg) {
      Utils.assertNotNull(context, "Device is required to list history log entries");
      
      MessageBody request = msg.getValue();
      String token = ListHistoryEntriesRequest.getToken(request);
      Integer limit = ListHistoryEntriesRequest.getLimit(request);
      if(limit == null) {
         limit = DFLT_LIMIT;
      }
      
      ListEntriesQuery query = new ListEntriesQuery();
      query.setType(HistoryLogEntryType.DETAILED_DEVICE_LOG);
      query.setId(context.getId());
      query.setLimit(limit);
      query.setToken(token);
      
      PagedResults<HistoryLogEntry> results = logDao.listEntriesByQuery(query);
      return
            ListHistoryEntriesResponse
               .builder()
               .withNextToken(results.getNextToken())
               .withResults(transformer.convertListToAttributes(results.getResults()))
               .build();
   }

   protected Device loadDevice(PlatformMessage message) throws NotFoundException {
      Address destination = message.getDestination();
      UUID deviceId = ((DeviceDriverAddress) destination).getDeviceId();

      Device dev = deviceDao.findById(deviceId);
      if (dev == null) {
         logger.warn("Message sent to non-existent device [{}]", (deviceId));
         throw new NotFoundException(destination);
      }
      return dev;
   }


}


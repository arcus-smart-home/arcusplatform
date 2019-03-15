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
package com.iris.platform.services.account.handlers;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.Utils;
import com.iris.capability.attribute.transform.BeanAttributesTransformer;
import com.iris.core.dao.HubDAO;
import com.iris.core.platform.ContextualRequestMessageHandler;
import com.iris.messages.MessageBody;
import com.iris.messages.MessageConstants;
import com.iris.messages.PlatformMessage;
import com.iris.messages.model.Account;
import com.iris.messages.model.Hub;

@Singleton
public class ListHubsHandler implements ContextualRequestMessageHandler<Account> {

   private final HubDAO hubDao;
   private final BeanAttributesTransformer<Hub> hubTransformer;

   @Inject
   public ListHubsHandler(HubDAO hubDao, BeanAttributesTransformer<Hub> hubTransformer) {
      this.hubDao = hubDao;
      this.hubTransformer = hubTransformer;
   }

   @Override
   public String getMessageType() {
      return MessageConstants.MSG_LIST_HUBS;
   }

   @Override
   public MessageBody handleRequest(Account context, PlatformMessage msg) {
      Utils.assertNotNull(context, "The account is required");
      Set<String> hubIds = hubDao.findHubIdsByAccount(context.getId());
      List<Map<String,Object>> hubs = new LinkedList<>();
      for(String hubId : hubIds) {
         Hub hub = hubDao.findById(hubId);
         if(hub != null) {
            hubs.add(hubTransformer.transform(hub));
         }
      }

      Map<String,Object> response = new HashMap<>();
      response.put("hubs", hubs);
      return MessageBody.buildResponse(msg.getValue(), response);
   }
}


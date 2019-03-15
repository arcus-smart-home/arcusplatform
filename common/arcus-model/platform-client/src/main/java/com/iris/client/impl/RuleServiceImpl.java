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
/**
 *
 */
package com.iris.client.impl;

import java.util.Map;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.iris.client.ClientEvent;
import com.iris.client.ClientRequest;
import com.iris.client.IrisClient;
import com.iris.client.IrisClientFactory;
import com.iris.client.capability.Rule;
import com.iris.client.capability.RuleTemplate;
import com.iris.client.event.ClientFuture;
import com.iris.client.event.Futures;
import com.iris.client.service.RuleService;

/**
 *
 */
// TODO auto-generate these things
public class RuleServiceImpl implements RuleService {
   private IrisClient client;

   /**
    *
    */
   public RuleServiceImpl(IrisClient client) {
      this.client = client;
   }

   /* (non-Javadoc)
    * @see com.iris.client.service.Service#getName()
    */
   @Override
   public String getName() {
      return RuleService.NAME;
   }

   /* (non-Javadoc)
    * @see com.iris.client.service.Service#getAddress()
    */
   @Override
   public String getAddress() {
      return "SERV:" + RuleService.NAMESPACE + ":";
   }

   /* (non-Javadoc)
    * @see com.iris.client.service.RuleService#listRuleTemplates(java.lang.String)
    */
   @Override
   public ClientFuture<ListRuleTemplatesResponse> listRuleTemplates(String placeId) {
      ClientRequest request = buildRequest(
            RuleService.CMD_LISTRULETEMPLATES,
            ImmutableMap.<String, Object>of(ListRuleTemplatesRequest.ATTR_PLACEID, placeId)
      );
      ClientFuture<ClientEvent> result = client.request(request);
      return Futures.transform(result, new Function<ClientEvent, ListRuleTemplatesResponse>() {
         @Override
         public ListRuleTemplatesResponse apply(ClientEvent input) {
            ListRuleTemplatesResponse response = new ListRuleTemplatesResponse(input);
            IrisClientFactory.getModelCache().retainAll(RuleTemplate.NAMESPACE, response.getRuleTemplates());
            return response;
         }

      });
   }

   /* (non-Javadoc)
    * @see com.iris.client.service.RuleService#listRules(java.lang.String)
    */
   @Override
   public ClientFuture<ListRulesResponse> listRules(String placeId) {
      ClientRequest request = buildRequest(
            RuleService.CMD_LISTRULES,
            ImmutableMap.<String, Object>of(ListRulesRequest.ATTR_PLACEID, placeId)
      );
      ClientFuture<ClientEvent> result = client.request(request);
      return Futures.transform(result, new Function<ClientEvent, ListRulesResponse>() {
         @Override
         public ListRulesResponse apply(ClientEvent input) {
            ListRulesResponse response = new ListRulesResponse(input);
            IrisClientFactory.getModelCache().retainAll(Rule.NAMESPACE, response.getRules());
            return response;
         }

      });
   }

   @Override
   public ClientFuture<GetCategoriesResponse> getCategories(String placeId) {
      ClientRequest request = buildRequest(
            RuleService.CMD_GETCATEGORIES,
            ImmutableMap.<String,Object>of(GetCategoriesRequest.ATTR_PLACEID, placeId)
      );
      ClientFuture<ClientEvent> result = client.request(request);
      return Futures.transform(result, new Function<ClientEvent, GetCategoriesResponse>() {
         @Override
         public GetCategoriesResponse apply(ClientEvent input) {
            return new GetCategoriesResponse(input);
         }
      });
   }

   @Override
   public ClientFuture<GetRuleTemplatesByCategoryResponse> getRuleTemplatesByCategory(String placeId, String category) {
      ClientRequest request = buildRequest(
            RuleService.CMD_GETRULETEMPLATESBYCATEGORY,
            ImmutableMap.<String,Object>of(GetRuleTemplatesByCategoryRequest.ATTR_PLACEID, placeId,
                                           GetRuleTemplatesByCategoryRequest.ATTR_CATEGORY, category));
      ClientFuture<ClientEvent> result = client.request(request);
      return Futures.transform(result, new Function<ClientEvent, GetRuleTemplatesByCategoryResponse>() {
         @Override
         public GetRuleTemplatesByCategoryResponse apply(ClientEvent input) {
            GetRuleTemplatesByCategoryResponse response = new GetRuleTemplatesByCategoryResponse(input);
            IrisClientFactory.getModelCache().addOrUpdate(response.getRuleTemplates());
            return response;
         }
      });
   }

   // TODO move down to BaseServiceImpl
   protected ClientRequest buildRequest(String commandName, Map<String, Object> attributes) {
      ClientRequest request = new ClientRequest();
      request.setCommand(commandName);
      request.setAddress(getAddress());
      request.setAttributes(attributes);
      request.setRestfulRequest(false);
      request.setTimeoutMs(30000);
      return request;
   }
}


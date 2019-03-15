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
package com.iris.netty.server.message;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.upperCase;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;
import java.util.UUID;

import javax.inject.Singleton;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.iris.bridge.server.session.Session;
import com.iris.core.platform.AnalyticsMessageBus;
import com.iris.messages.ClientMessage;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.service.SessionService;
import com.iris.messages.service.SessionService.TagRequest;
import com.iris.messages.service.SessionService.TagResponse;
import com.iris.messages.service.SessionService.TaggedEvent;
import com.iris.population.PlacePopulationCacheManager;

@Singleton
public class TagHandler extends BaseClientRequestHandler
{
   private static final Logger tagLog = getLogger("tag");

   public static final String KEY_SERVICE_LEVEL = "service.level";

   private final IrisNettyMessageUtil messageUtil;
   private final AnalyticsMessageBus tagBus;
   private final PlacePopulationCacheManager populationCacheMgr;

   @Inject
   public TagHandler(IrisNettyMessageUtil messageUtil, AnalyticsMessageBus tagBus, PlacePopulationCacheManager populationCacheMgr)
   {
      super(directExecutor());

      this.messageUtil = messageUtil;
      this.tagBus = tagBus;
      this.populationCacheMgr = populationCacheMgr;
   }

   @Override
   public String getRequestType()
   {
      return TagRequest.NAME;
   }

   @Override
   protected MessageBody doHandle(ClientMessage request, Session session)
   {
      MessageBody requestBody = request.getPayload();

      String tagName = TagRequest.getName(requestBody);
      Map<String, String> tagContext = TagRequest.getContext(requestBody);

      tagLog.info("Tag {} : {}", tagName, tagContext);

      // These two will be null for UI tags before the user logs in
      String placeId = session.getActivePlace();
      UUID personId = session.getClient().getPrincipalId();

      String serviceLevel = tagContext.get(KEY_SERVICE_LEVEL);
      if (isBlank(serviceLevel))
      {
         serviceLevel = null;
      }

      MessageBody eventBody = TaggedEvent.builder()
         .withName(tagName)
         .withPlaceId(placeId)
         .withPersonId(personId == null ? null : personId.toString())
         .withSource(upperCase(session.getClientType()))
         .withVersion(session.getClientVersion())
         .withServiceLevel(serviceLevel)
         .build();

      Address clientAddress = Address.fromString(messageUtil.buildId(session.getClientToken().getRepresentation()));
      Address personAddress = personId == null ? null : Address.platformService(personId, PersonCapability.NAMESPACE);

      tagBus.send(PlatformMessage.buildEvent(eventBody, clientAddress)
         .withPlaceId(placeId)
         .withPopulation(populationCacheMgr.getPopulationByPlaceId(UUID.fromString(placeId)))
         .withActor(personAddress)
         .create());

      return TagResponse.instance();
   }

   @Override
   protected Address address()
   {
   	return SessionService.ADDRESS;
   }
}


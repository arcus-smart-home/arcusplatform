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
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;

import javax.inject.Singleton;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.iris.bridge.server.session.Session;
import com.iris.messages.ClientMessage;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.service.SessionService;
import com.iris.messages.service.SessionService.TagRequest;
import com.iris.messages.service.SessionService.TagResponse;

/**
 * Acknowledges the usage tags clients still send. They used to be published for the tag-service,
 * which has been removed. Nothing consumes them now, so they are discarded; the handler stays
 * only so that clients get an answer instead of an unsupported-message error.
 */
@Singleton
public class TagHandler extends BaseClientRequestHandler
{
   private static final Logger logger = getLogger(TagHandler.class);

   @Inject
   public TagHandler()
   {
      super(directExecutor());
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

      logger.debug("Tag {} : {}", tagName, tagContext);

      return TagResponse.instance();
   }

   @Override
   protected Address address()
   {
   	return SessionService.ADDRESS;
   }
}

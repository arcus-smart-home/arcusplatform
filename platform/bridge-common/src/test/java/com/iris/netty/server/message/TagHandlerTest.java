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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.iris.messages.ClientMessage;
import com.iris.messages.MessageBody;
import com.iris.messages.service.SessionService.TagRequest;
import com.iris.messages.service.SessionService.TagResponse;

public class TagHandlerTest
{
   // clients still send tags, so they must keep getting an answer rather than an unsupported-message error
   @Test
   public void testTagIsAcknowledged()
   {
      ClientMessage request = ClientMessage.builder()
         .withPayload(TagRequest.builder().withName("dashboard.viewed").withContext(ImmutableMap.of("source", "test")).build())
         .create();

      MessageBody response = new TagHandler().doHandle(request, null);

      assertEquals(TagResponse.instance(), response);
   }
}

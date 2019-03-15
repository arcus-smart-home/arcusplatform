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
package com.iris.alexa.shs;

import static org.junit.Assert.assertEquals;

import com.iris.alexa.message.AlexaMessage;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.service.AlexaService;

public enum ShsAssertions {
   ;

   public static void assertCommonResponseHeader(AlexaMessage req, AlexaMessage res, String respName, String payloadVer) {
      assertEquals(respName, res.getHeader().getName());
      assertEquals(req.getHeader().getNamespace(), res.getHeader().getNamespace());
      assertEquals(req.getHeader().getMessageId(), res.getHeader().getMessageId());
      assertEquals(req.getHeader().getCorrelationToken(), res.getHeader().getCorrelationToken());
      assertEquals(payloadVer, res.getHeader().getPayloadVersion());
   }

   public static void assertExecuteRequest(PlatformMessage msg, String target, String directive, Object args, String corrTok, Boolean allowDeferred) {
      assertEquals(AlexaService.ExecuteRequest.NAME, msg.getMessageType());

      MessageBody body = msg.getValue();
      assertEquals(target, AlexaService.ExecuteRequest.getTarget(body));
      assertEquals(directive, AlexaService.ExecuteRequest.getDirective(body));
      assertEquals(args, AlexaService.ExecuteRequest.getArguments(body));
      assertEquals(corrTok, AlexaService.ExecuteRequest.getCorrelationToken(body));
      assertEquals(allowDeferred, AlexaService.ExecuteRequest.getAllowDeferred(body));
   }

}


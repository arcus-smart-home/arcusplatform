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
package com.iris.alexa.shs.handlers.v3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.iris.alexa.AlexaInterfaces;
import com.iris.alexa.error.AlexaErrors;
import com.iris.alexa.error.AlexaException;
import com.iris.alexa.message.AlexaMessage;
import com.iris.alexa.message.Header;
import com.iris.alexa.shs.ShsAssertions;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.service.AlexaService;
import com.iris.util.IrisUUID;

public class TestTxfmAcceptGrant {

   private AlexaMessage acceptGrant;

   @Before
   public void setup() {
      Header h = Header.v3(IrisUUID.randomUUID().toString(), AlexaInterfaces.Authorization.REQUEST_ACCEPTGRANT, AlexaInterfaces.Authorization.NAMESPACE, "corrtok");
      Map<String, Object> payload = ImmutableMap.of(
         AlexaInterfaces.Authorization.ARG_GRANT, ImmutableMap.of("type", "OAuth2.AuthorizationCode", "code", "VGhpcyBpcyBhbiBhdXRob3JpemF0aW9uIGNvZGUuIDotKQ=="),
         AlexaInterfaces.Authorization.ARG_GRANTEE, ImmutableMap.of("type", "BearerToken", "token", "token")
      );
      acceptGrant = new AlexaMessage(h, payload, null, null);
   }

   @Test
   public void testAcceptGrant() {
      PlatformMessage platMsg = TxfmTestUtil.txfmReq(acceptGrant);

      assertEquals(AlexaService.AcceptGrantRequest.NAME, platMsg.getMessageType());

      MessageBody body = platMsg.getValue();
      assertEquals("VGhpcyBpcyBhbiBhdXRob3JpemF0aW9uIGNvZGUuIDotKQ==", AlexaService.AcceptGrantRequest.getCode(body));
   }

   @Test
   public void testAcceptGrantMissingGrant() {
      Map<String, Object> payload = ImmutableMap.of(
         AlexaInterfaces.Authorization.ARG_GRANTEE, ImmutableMap.of("type", "BearerToken", "token", "token")
      );
      AlexaMessage msg = new AlexaMessage(acceptGrant.getHeader(), payload, null, null);
      try {
         TxfmTestUtil.txfmReq(msg);
      } catch(AlexaException ae) {
         MessageBody err = ae.getErrorMessage();
         assertEquals(AlexaErrors.TYPE_INVALID_DIRECTIVE, err.getAttributes().get("type"));
      }
   }

   @Test
   public void testAcceptGrantMissingCode() {
      Map<String, Object> payload = ImmutableMap.of(
         AlexaInterfaces.Authorization.ARG_GRANT, ImmutableMap.of("type", "OAuth2.AuthorizationCode"),
         AlexaInterfaces.Authorization.ARG_GRANTEE, ImmutableMap.of("type", "BearerToken", "token", "token")
      );
      AlexaMessage msg = new AlexaMessage(acceptGrant.getHeader(), payload, null, null);
      try {
         TxfmTestUtil.txfmReq(msg);
      } catch(AlexaException ae) {
         MessageBody err = ae.getErrorMessage();
         assertEquals(AlexaErrors.TYPE_INVALID_DIRECTIVE, err.getAttributes().get("type"));
      }
   }

   @Test
   public void testAcceptGrantSuccess() {
      MessageBody body = AlexaService.AcceptGrantResponse.instance();
      AlexaMessage msg = TxfmTestUtil.txfmResponse(acceptGrant, body);
      ShsAssertions.assertCommonResponseHeader(acceptGrant, msg, AlexaInterfaces.Authorization.RESPONSE_ACCEPTGRANT, "3");
   }

   @Test
   public void testExtractOauthToken() {
      Optional<String> token = Txfm.transformerFor(acceptGrant).extractRequestOauthToken(acceptGrant);
      assertEquals("token", token.get());
   }

   @Test
   public void testExtractOauthTokenNoPayload() {
      AlexaMessage msg = new AlexaMessage(acceptGrant.getHeader(), null, null, null);
      Optional<String> token = Txfm.transformerFor(msg).extractRequestOauthToken(msg);
      assertFalse(token.isPresent());
   }

   @Test
   public void testExtractOauthTokenNoGrantee() {
      Map<String, Object> payload = ImmutableMap.of(
         AlexaInterfaces.Authorization.ARG_GRANT, ImmutableMap.of("type", "OAuth2.AuthorizationCode", "code", "VGhpcyBpcyBhbiBhdXRob3JpemF0aW9uIGNvZGUuIDotKQ==")
      );
      AlexaMessage msg = new AlexaMessage(acceptGrant.getHeader(), payload, null, null);
      Optional<String> token = Txfm.transformerFor(msg).extractRequestOauthToken(msg);
      assertFalse(token.isPresent());
   }

   @Test
   public void testExtractOauthTokenGranteeHasNoToken() {
      Map<String, Object> payload = ImmutableMap.of(
         AlexaInterfaces.Authorization.ARG_GRANT, ImmutableMap.of("type", "OAuth2.AuthorizationCode", "code", "VGhpcyBpcyBhbiBhdXRob3JpemF0aW9uIGNvZGUuIDotKQ=="),
         AlexaInterfaces.Authorization.ARG_GRANTEE, ImmutableMap.of("type", "BearerToken")
      );
      AlexaMessage msg = new AlexaMessage(acceptGrant.getHeader(), payload, null, null);
      Optional<String> token = Txfm.transformerFor(msg).extractRequestOauthToken(msg);
      assertFalse(token.isPresent());
   }

}


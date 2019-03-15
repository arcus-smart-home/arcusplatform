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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.iris.alexa.AlexaInterfaces;
import com.iris.alexa.error.AlexaErrors;
import com.iris.alexa.error.AlexaException;
import com.iris.alexa.message.AlexaMessage;
import com.iris.alexa.message.Endpoint;
import com.iris.alexa.message.Header;
import com.iris.alexa.message.Scope;
import com.iris.alexa.shs.ShsAssertions;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.util.IrisUUID;

// this tests txfm of the adjustbrightness as a representation of all commands.  other than those with specific test
// cases the rest are identical
public class TestTxfmCommand {

   private AlexaMessage adjustBrightness;
   private Endpoint e;

   @Before
   public void setup() {

      Header h = Header.v3(IrisUUID.randomUUID().toString(), AlexaInterfaces.BrightnessController.REQUEST_ADJUSTBRIGHTNESS, AlexaInterfaces.BrightnessController.NAMESPACE, "corrtok");
      Map<String, Object> payload = ImmutableMap.of(
         AlexaInterfaces.BrightnessController.ARG_BRIGHTNESSDELTA, 3
      );
      Scope s = new Scope("BearerToken", "token");
      e = new Endpoint(s, Address.platformDriverAddress(IrisUUID.randomUUID()).getRepresentation(), null);
      adjustBrightness = new AlexaMessage(h, payload, e, null);
   }

   @Test
   public void testToExecute() {
      PlatformMessage platMsg = TxfmTestUtil.txfmReq(adjustBrightness);

      ShsAssertions.assertExecuteRequest(
         platMsg,
         e.getEndpointId(),
         AlexaInterfaces.BrightnessController.REQUEST_ADJUSTBRIGHTNESS,
         ImmutableMap.of(AlexaInterfaces.BrightnessController.ARG_BRIGHTNESSDELTA, 3),
         "corrtok",
         true
      );
   }

   @Test
   public void testMissingEndpointThrows() {
      AlexaMessage msg = new AlexaMessage(adjustBrightness.getHeader(), adjustBrightness.getPayload(), null, null);
      try {
         TxfmTestUtil.txfmReq(msg);
      } catch(AlexaException ae) {
         MessageBody body = ae.getErrorMessage();
         assertEquals(AlexaErrors.TYPE_INVALID_DIRECTIVE, body.getAttributes().get("type"));
      }
   }

   @Test
   public void testMissingEndpointWithNoIdThrows() {
      Endpoint newEndpoint = new Endpoint(e.getScope(), null, null);
      AlexaMessage msg = new AlexaMessage(adjustBrightness.getHeader(), adjustBrightness.getPayload(), newEndpoint, null);
      try {
         TxfmTestUtil.txfmReq(msg);
      } catch(AlexaException ae) {
         MessageBody body = ae.getErrorMessage();
         assertEquals(AlexaErrors.TYPE_INVALID_DIRECTIVE, body.getAttributes().get("type"));
      }
   }

   @Test
   public void testResponseWithPayloadAndProperties() {
      Map<String, Object> payload = ImmutableMap.of();
      List<Map<String, Object>> props = ImmutableList.of(TxfmTestUtil.report(AlexaInterfaces.BrightnessController.PROP_BRIGHTNESS, AlexaInterfaces.BrightnessController.NAMESPACE, 53).toMap());

      MessageBody resp = TxfmTestUtil.executeResponse(payload, props, false);
      AlexaMessage msg = TxfmTestUtil.txfmResponse(adjustBrightness, resp);
      TxfmTestUtil.assertResponse(adjustBrightness, msg, payload, props);
   }

   @Test
   public void testResponseNoPayloadOrProps() {
      MessageBody resp = TxfmTestUtil.executeResponse(null, null, false);
      AlexaMessage msg = TxfmTestUtil.txfmResponse(adjustBrightness, resp);
      TxfmTestUtil.assertResponse(adjustBrightness, msg, null, null);
   }

   @Test
   public void testResponseDeferred() {
      MessageBody resp = TxfmTestUtil.executeResponse(null, null, true);
      AlexaMessage msg = TxfmTestUtil.txfmResponse(adjustBrightness, resp);
      TxfmTestUtil.assertDeferred(adjustBrightness, msg);
   }

   @Test
   public void testExtractOauthToken() {
      Optional<String> token = Txfm.transformerFor(adjustBrightness).extractRequestOauthToken(adjustBrightness);
      assertEquals("token", token.get());
   }

   @Test
   public void testExtractOauthTokenNoEndpoint() {
      AlexaMessage msg = new AlexaMessage(adjustBrightness.getHeader(), adjustBrightness.getPayload(), null, null);
      Optional<String> token = Txfm.transformerFor(msg).extractRequestOauthToken(msg);
      assertFalse(token.isPresent());
   }

   @Test
   public void testExtractOauthTokenNoScope() {
      Endpoint e = new Endpoint(null, adjustBrightness.getEndpoint().getEndpointId(), null);
      AlexaMessage msg = new AlexaMessage(adjustBrightness.getHeader(), adjustBrightness.getPayload(), e, null);
      Optional<String> token = Txfm.transformerFor(msg).extractRequestOauthToken(msg);
      assertFalse(token.isPresent());
   }

   @Test
   public void testExtractOauthTokenScopeMissingToken() {
      Scope s = new Scope("BearerToken", null);
      Endpoint e = new Endpoint(s, adjustBrightness.getEndpoint().getEndpointId(), null);
      AlexaMessage msg = new AlexaMessage(adjustBrightness.getHeader(), adjustBrightness.getPayload(), e, null);
      Optional<String> token = Txfm.transformerFor(msg).extractRequestOauthToken(msg);
      assertFalse(token.isPresent());
   }

}


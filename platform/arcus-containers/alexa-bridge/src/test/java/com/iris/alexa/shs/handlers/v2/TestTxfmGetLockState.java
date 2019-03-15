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
package com.iris.alexa.shs.handlers.v2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.iris.alexa.AlexaInterfaces;
import com.iris.alexa.message.AlexaMessage;
import com.iris.alexa.message.Header;
import com.iris.alexa.message.v2.Appliance;
import com.iris.alexa.message.v2.error.DriverInternalError;
import com.iris.alexa.message.v2.error.ErrorPayloadException;
import com.iris.alexa.message.v2.error.UnableToGetValueError;
import com.iris.alexa.message.v2.request.GetLockStateRequest;
import com.iris.alexa.message.v2.response.GetLockStateResponse;
import com.iris.alexa.shs.ShsAssertions;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.service.AlexaService;
import com.iris.messages.type.AlexaPropertyReport;
import com.iris.util.IrisUUID;

public class TestTxfmGetLockState {

   private AlexaMessage getLock;
   private Appliance app;

   @Before
   public void setup() {
      Header h = Header.v2(IrisUUID.randomUUID().toString(), "GetLockStateRequest", "Alexa.ConnectedHome.Query");

      app = new Appliance();
      app.setApplianceId(Address.platformDriverAddress(IrisUUID.randomUUID()).getRepresentation());

      GetLockStateRequest payload = new GetLockStateRequest();
      payload.setAccessToken("token");
      payload.setAppliance(app);

      getLock = new AlexaMessage(h, payload);
   }

   @Test
   public void testGetLock() {
      PlatformMessage platMsg = TxfmTestUtil.txfmReq(getLock);

      ShsAssertions.assertExecuteRequest(
         platMsg,
         app.getApplianceId(),
         AlexaInterfaces.REQUEST_REPORTSTATE,
         ImmutableMap.of(),
         null,
         false
      );
   }

   @Test
   public void testSuccessResponse() {
      AlexaPropertyReport lockState = new AlexaPropertyReport();
      lockState.setValue("LOCKED");
      lockState.setUncertaintyInMilliseconds(0L);
      lockState.setTimeOfSample(ZonedDateTime.now(ZoneOffset.UTC).toString());
      lockState.setName(AlexaInterfaces.LockController.PROP_LOCKSTATE);
      lockState.setNamespace(AlexaInterfaces.LockController.NAMESPACE);

      MessageBody resp = AlexaService.ExecuteResponse.builder()
         .withProperties(ImmutableList.of(lockState.toMap()))
         .build();

      AlexaMessage response = TxfmTestUtil.txfmResponse(getLock, resp);
      ShsAssertions.assertCommonResponseHeader(getLock, response, "GetLockStateResponse", "2");
      assertTrue(response.getPayload() instanceof GetLockStateResponse);
      GetLockStateResponse confirmation = (GetLockStateResponse) response.getPayload();
      assertEquals("LOCKED", confirmation.getLockState());
   }

   @Test
   public void testJammedResponse() {
      AlexaPropertyReport lockState = new AlexaPropertyReport();
      lockState.setValue("JAMMED");
      lockState.setUncertaintyInMilliseconds(0L);
      lockState.setTimeOfSample(ZonedDateTime.now(ZoneOffset.UTC).toString());
      lockState.setName(AlexaInterfaces.LockController.PROP_LOCKSTATE);
      lockState.setNamespace(AlexaInterfaces.LockController.NAMESPACE);

      MessageBody resp = AlexaService.ExecuteResponse.builder()
         .withProperties(ImmutableList.of(lockState.toMap()))
         .build();

      try {
         TxfmTestUtil.txfmResponse(getLock, resp);
      } catch(ErrorPayloadException epe) {
         assertTrue(epe.getPayload() instanceof UnableToGetValueError);
         UnableToGetValueError e = (UnableToGetValueError) epe.getPayload();
         assertEquals("DEVICE_JAMMED", e.getErrorInfo().getCode());
         assertEquals("Alexa.ConnectedHome.Query", e.getNamespace());
      }
   }

   @Test
   public void testNullPropertiesResponse() {
      try {
         TxfmTestUtil.txfmResponse(getLock, AlexaService.ExecuteResponse.builder().build());
      } catch(ErrorPayloadException epe) {
         assertTrue(epe.getPayload() instanceof DriverInternalError);
         DriverInternalError die = (DriverInternalError) epe.getPayload();
         assertEquals("Alexa.ConnectedHome.Control", die.getNamespace());
      }
   }

   @Test
   public void testNoStatePropertyResponse() {
      MessageBody resp = AlexaService.ExecuteResponse.builder()
         .withProperties(ImmutableList.of())
         .build();
      try {
         TxfmTestUtil.txfmResponse(getLock, resp);
      } catch(ErrorPayloadException epe) {
         assertTrue(epe.getPayload() instanceof DriverInternalError);
         DriverInternalError die = (DriverInternalError) epe.getPayload();
         assertEquals("Alexa.ConnectedHome.Control", die.getNamespace());
      }
   }
}


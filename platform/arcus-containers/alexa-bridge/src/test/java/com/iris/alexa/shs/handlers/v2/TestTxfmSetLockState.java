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
import com.iris.alexa.message.v2.error.UnableToSetValueError;
import com.iris.alexa.message.v2.request.SetLockStateRequest;
import com.iris.alexa.message.v2.response.SetLockStateConfirmation;
import com.iris.alexa.shs.ShsAssertions;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.service.AlexaService;
import com.iris.messages.type.AlexaPropertyReport;
import com.iris.util.IrisUUID;

public class TestTxfmSetLockState {

   private AlexaMessage setLock;
   private Appliance app;

   @Before
   public void setup() {
      Header h = Header.v2(IrisUUID.randomUUID().toString(), "SetLockStateRequest", "Alexa.ConnectedHome.Control");

      app = new Appliance();
      app.setApplianceId(Address.platformDriverAddress(IrisUUID.randomUUID()).getRepresentation());

      SetLockStateRequest payload = new SetLockStateRequest();
      payload.setAccessToken("token");
      payload.setAppliance(app);

      setLock = new AlexaMessage(h, payload);
   }

   @Test
   public void testLock() {
      PlatformMessage platMsg = TxfmTestUtil.txfmReq(setLock);

      ShsAssertions.assertExecuteRequest(
         platMsg,
         app.getApplianceId(),
         AlexaInterfaces.LockController.REQUEST_LOCK,
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

      AlexaMessage response = TxfmTestUtil.txfmResponse(setLock, resp);
      ShsAssertions.assertCommonResponseHeader(setLock, response, "SetLockStateConfirmation", "2");
      assertTrue(response.getPayload() instanceof SetLockStateConfirmation);
      SetLockStateConfirmation confirmation = (SetLockStateConfirmation) response.getPayload();
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
         TxfmTestUtil.txfmResponse(setLock, resp);
      } catch(ErrorPayloadException epe) {
         assertTrue(epe.getPayload() instanceof UnableToSetValueError);
         UnableToSetValueError e = (UnableToSetValueError) epe.getPayload();
         assertEquals("DEVICE_JAMMED", e.getErrorInfo().getCode());
         assertEquals("Alexa.ConnectedHome.Control", e.getNamespace());
      }
   }

   @Test
   public void testNullPropertiesResponse() {
      try {
         TxfmTestUtil.txfmResponse(setLock, AlexaService.ExecuteResponse.builder().build());
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
         TxfmTestUtil.txfmResponse(setLock, resp);
      } catch(ErrorPayloadException epe) {
         assertTrue(epe.getPayload() instanceof DriverInternalError);
         DriverInternalError die = (DriverInternalError) epe.getPayload();
         assertEquals("Alexa.ConnectedHome.Control", die.getNamespace());
      }
   }
}


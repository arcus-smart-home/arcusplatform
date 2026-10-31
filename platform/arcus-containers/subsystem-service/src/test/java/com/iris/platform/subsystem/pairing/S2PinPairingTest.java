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
package com.iris.platform.subsystem.pairing;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import com.iris.common.subsystem.SubsystemUtils;
import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.HubCapability.PairingRequestRequest;
import com.iris.messages.capability.HubCapability.UnpairingRequestResponse;
import com.iris.messages.capability.HubZwaveCapability;
import com.iris.messages.capability.HubZwaveCapability.S2PinFinishedEvent;
import com.iris.messages.capability.HubZwaveCapability.S2PinRequiredEvent;
import com.iris.messages.capability.PairingSubsystemCapability;
import com.iris.messages.capability.PairingSubsystemCapability.CancelS2PinRequest;
import com.iris.messages.capability.PairingSubsystemCapability.CancelS2PinResponse;
import com.iris.messages.capability.PairingSubsystemCapability.GetS2PinRequestRequest;
import com.iris.messages.capability.PairingSubsystemCapability.GetS2PinRequestResponse;
import com.iris.messages.capability.PairingSubsystemCapability.PairingFailedEvent;
import com.iris.messages.capability.PairingSubsystemCapability.SearchRequest;
import com.iris.messages.capability.PairingSubsystemCapability.StartPairingRequest;
import com.iris.messages.capability.PairingSubsystemCapability.SubmitS2PinRequest;
import com.iris.messages.capability.PairingSubsystemCapability.SubmitS2PinResponse;
import com.iris.messages.errors.Errors;
import com.iris.messages.event.MessageReceivedEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.test.ModelFixtures;
import com.iris.platform.subsystem.pairing.state.PairingStateName;
import com.iris.platform.subsystem.pairing.state.PairingSubsystemTestCase;
import com.iris.security.apikey.ApiKeyPrincipal;

public class S2PinPairingTest extends PairingSubsystemTestCase {
   private static final String REQUEST_ID = "aaaaaaaa-0000-4000-8000-000000000001";
   private static final int NODE = 12;
   private static final String DSK = "00000-11111-22222-33333-44444-55555-11111-22222";
   private static final String PIN = "12345";
   private static final long TIMEOUT_MS = 200000;

   private Model hub;
   private Address hubSource;
   private Address person;

   @Before
   public void stageSearching() throws Exception {
      expectLoadProductAndReturn(productZWave()).anyTimes();
      expectCurrentProductAndReturn(productZWave()).anyTimes();
      replay();

      hub = stageSearchingHub();
      hubSource = Address.hubService(hub.getId(), "zwave");
      person = addModel(ModelFixtures.createPersonAttributes()).getAddress();
   }

   //////////
   // The hub asking
   //////////

   @Test
   public void testRequestIsPublishedWithoutItsSecrets() {
      hubRequestsPin();

      assertEquals(REQUEST_ID, context.model().getS2PinRequestId());
      assertTrue(context.model().getS2PinExpires().after(new Date()));
      assertNothingLeaked();
   }

   @Test
   public void testPlatformClosesBeforeTheHubDoes() {
      long before = System.currentTimeMillis();
      hubRequestsPin();

      long expires = context.model().getS2PinExpires().getTime();
      assertTrue(expires <= System.currentTimeMillis() + TIMEOUT_MS - S2PinPairing.GRACE_MS);
      assertTrue(expires >= before + TIMEOUT_MS - S2PinPairing.GRACE_MS);
   }

   @Test
   public void testRequestCountsAsADeviceFound() {
      hubRequestsPin();

      assertTrue(context.model().getSearchDeviceFound());
      assertFalse(context.model().getSearchIdle());
   }

   @Test
   public void testSearchOutlastsTheWholePinWindow() {
      // the product under test times out after 20 seconds, far short of the PIN window
      Date before = context.model().getSearchTimeout();
      hubRequestsPin();

      Date search = context.model().getSearchTimeout();
      assertTrue(search.after(before));
      assertEquals(context.model().getS2PinExpires().getTime() + S2PinPairing.ANSWERED_WAIT_MS, search.getTime());
      assertTimeoutSet(PairingStateName.Searching, search);
   }

   @Test
   public void testStartingTheSearchDoesNotCutTheWindowShort() {
      context.setVariable("pairingState", PairingStateName.PairingSteps.name());
      SubsystemUtils.setTimeout(new Date(System.currentTimeMillis() + 20000), context, PairingStateName.PairingSteps.name());
      hubRequestsPin();
      Date hold = new Date(context.model().getS2PinExpires().getTime() + S2PinPairing.ANSWERED_WAIT_MS);

      // the search resets the timeout to the product's, five minutes by default, which is shorter
      search(SearchRequest.builder().withProductAddress(productAddress).build());
      popRequest().getAction().onResponse(context, buildStartPairingResponse(hub.getAddress()));

      assertPairingState(PairingStateName.Searching);
      assertEquals(hold, context.model().getSearchTimeout());
      assertEquals(REQUEST_ID, context.model().getS2PinRequestId());
   }

   @Test
   public void testRequestDuringPairingStepsIsAccepted() throws Exception {
      context.setVariable("pairingState", PairingStateName.PairingSteps.name());
      SubsystemUtils.setTimeout(new Date(System.currentTimeMillis() + 20000), context, PairingStateName.PairingSteps.name());

      hubRequestsPin();

      assertEquals(REQUEST_ID, context.model().getS2PinRequestId());
   }

   @Test
   public void testRepeatedRequestChangesNothing() {
      hubRequestsPin();
      Date expires = context.model().getS2PinExpires();
      Date timer = SubsystemUtils.getTimeout(context, S2PinPairing.VAR_TIMEOUT).get();

      fromHub(required(REQUEST_ID, DSK, TIMEOUT_MS + 10000));

      assertEquals(expires, context.model().getS2PinExpires());
      assertEquals(timer, SubsystemUtils.getTimeout(context, S2PinPairing.VAR_TIMEOUT).get());
      assertNoRequests();
   }

   @Test
   public void testNewRequestReplacesAnOldOne() {
      hubRequestsPin();

      String next = "aaaaaaaa-0000-4000-8000-000000000002";
      fromHub(required(next, DSK, TIMEOUT_MS));

      assertEquals(next, context.model().getS2PinRequestId());
      assertEquals(next, GetS2PinRequestResponse.getRequestId(asPerson(GetS2PinRequestRequest.instance())));
   }

   @Test
   public void testWindowCountsFromWhenTheBridgeTookTheRequest() {
      Date received = new Date(System.currentTimeMillis() - 10000);
      fromHub(required(REQUEST_ID, DSK, TIMEOUT_MS), received);

      assertEquals(received.getTime() + TIMEOUT_MS - S2PinPairing.GRACE_MS, context.model().getS2PinExpires().getTime());
   }

   @Test
   public void testRequestThatArrivesAfterTheHubGaveUpIsIgnored() {
      fromHub(required(REQUEST_ID, DSK, TIMEOUT_MS), new Date(System.currentTimeMillis() - TIMEOUT_MS));

      assertNoPromptShown();
      assertNoRequests();
   }

   @Test
   public void testLateCopyOfADeclinedRequestIsCancelledAgain() {
      hubRequestsPin();
      asPerson(CancelS2PinRequest.builder().withRequestId(REQUEST_ID).build());

      hubRequestsPin();

      assertRequestGone();
      assertCancelSent(2);
      assertNoFailureBroadcast();
   }

   @Test
   public void testLateCopyOfATimedOutRequestDoesNotPromptAgain() {
      hubRequestsPin();
      subsystem.onEvent(timeout(S2PinPairing.VAR_TIMEOUT), context);

      hubRequestsPin();

      assertRequestGone();
      assertCancelSent(2);
   }

   @Test
   public void testNewRequestAfterAnEndedOneIsAccepted() {
      hubRequestsPin();
      asPerson(CancelS2PinRequest.builder().withRequestId(REQUEST_ID).build());

      String next = "aaaaaaaa-0000-4000-8000-000000000002";
      fromHub(required(next, DSK, TIMEOUT_MS));

      assertEquals(next, context.model().getS2PinRequestId());
   }

   @Test
   public void testSearchEndsWhenTheHoldIsOver() {
      // the product search still has its idle check pending when the hub asks
      hubRequestsPin();

      subsystem.onEvent(timeout(PairingStateName.Searching.name()), context);

      assertPairingState(PairingStateName.Idle);
   }

   @Test
   public void testRequestFromAnotherHubIsIgnored() {
      fromHub(required(REQUEST_ID, DSK, TIMEOUT_MS), Address.hubService("ZZZ-9999", "zwave"), placeId.toString());

      assertNoPromptShown();
      assertNoRequests();
   }

   @Test
   public void testRequestNotFromAHubIsIgnored() {
      fromHub(required(REQUEST_ID, DSK, TIMEOUT_MS), clientAddress, placeId.toString());
      fromHub(required(REQUEST_ID, DSK, TIMEOUT_MS), Address.platformService(UUID.randomUUID(), "person"), placeId.toString());

      assertNoPromptShown();
      assertNoRequests();
   }

   @Test
   public void testRequestWithAnotherPlaceHeaderIsIgnored() {
      fromHub(required(REQUEST_ID, DSK, TIMEOUT_MS), hubSource, UUID.randomUUID().toString());

      assertNoPromptShown();
      assertNoRequests();
   }

   @Test
   public void testBroadcastRequestIsIgnored() {
      PlatformMessage broadcast =
         PlatformMessage
            .buildBroadcast(required(REQUEST_ID, DSK, TIMEOUT_MS), hubSource)
            .withPlaceId(placeId)
            .create();
      subsystem.onEvent(new MessageReceivedEvent(broadcast), context);

      assertNoPromptShown();
   }

   @Test
   public void testRequestWhileNotPairingIsCancelled() {
      context.model().setPairingMode(PairingSubsystemCapability.PAIRINGMODE_IDLE);

      hubRequestsPin();

      assertNoPromptShown();
      assertCancelSent(1);
   }

   @Test
   public void testDskThatCarriesAPinIsCancelled() {
      fromHub(required(REQUEST_ID, "12345-11111-22222-33333-44444-55555-11111-22222", TIMEOUT_MS));

      assertNoPromptShown();
      assertCancelSent(1);
   }

   @Test
   public void testMalformedDskIsCancelled() {
      fromHub(required(REQUEST_ID, "not a dsk", TIMEOUT_MS));

      assertNoPromptShown();
      assertCancelSent(1);
   }

   @Test
   public void testTimeoutOutsideTheSpecIsCancelled() {
      fromHub(required(REQUEST_ID, DSK, S2PinPairing.MAX_TIMEOUT_MS + 1));
      fromHub(required(REQUEST_ID, DSK, S2PinPairing.MIN_TIMEOUT_MS - 1));

      assertNoPromptShown();
      assertCancelSent(2);
   }

   @Test
   public void testRequestThatCanNotBeAnsweredIsDropped() {
      fromHub(required("not/an id", DSK, TIMEOUT_MS));
      fromHub(S2PinRequiredEvent.builder().withRequestId(REQUEST_ID).withNode(0).withDsk(DSK).withTimeoutMs(TIMEOUT_MS).build());
      fromHub(S2PinRequiredEvent.builder().withRequestId(REQUEST_ID).withNode(233).withDsk(DSK).withTimeoutMs(TIMEOUT_MS).build());

      assertNoPromptShown();
      assertNoRequests();
   }

   //////////
   // Reading the DSK
   //////////

   @Test
   public void testGetReturnsTheDsk() {
      hubRequestsPin();

      MessageBody response = asPerson(GetS2PinRequestRequest.instance());

      assertEquals(GetS2PinRequestResponse.NAME, response.getMessageType());
      assertEquals(REQUEST_ID, GetS2PinRequestResponse.getRequestId(response));
      assertEquals(DSK, GetS2PinRequestResponse.getDsk(response));
      assertEquals(context.model().getS2PinExpires(), GetS2PinRequestResponse.getExpires(response));
   }

   @Test
   public void testGetWithNothingWaitingIsRejected() {
      assertError(GetS2PinRequestResponse.CODE_REQUEST_STATE_INVALID, asPerson(GetS2PinRequestRequest.instance()));
   }

   @Test
   public void testGetAfterAnsweringIsRejected() {
      hubRequestsPin();
      asPerson(submit(REQUEST_ID, PIN));

      assertError(GetS2PinRequestResponse.CODE_REQUEST_STATE_INVALID, asPerson(GetS2PinRequestRequest.instance()));
   }

   //////////
   // Answering
   //////////

   @Test
   public void testAnswerGoesToTheHubThatAsked() {
      hubRequestsPin();

      asPerson(submit(REQUEST_ID, PIN));

      SendAndExpect request = popRequest();
      assertEquals(hub.getAddress(), request.getRequestAddress());
      assertEquals(HubZwaveCapability.SubmitS2PinRequest.NAME, request.getMessage().getMessageType());
      assertEquals(REQUEST_ID, HubZwaveCapability.SubmitS2PinRequest.getRequestId(request.getMessage()));
      assertEquals(Integer.valueOf(NODE), HubZwaveCapability.SubmitS2PinRequest.getNode(request.getMessage()));
      assertEquals(PIN, HubZwaveCapability.SubmitS2PinRequest.getPin(request.getMessage()));

      request.getAction().onResponse(context, hubResponse(HubZwaveCapability.SubmitS2PinResponse.instance()));
      assertEquals(SubmitS2PinResponse.instance(), lastResponse());
   }

   @Test
   public void testAnswerClosesThePromptAndKeepsNothing() {
      hubRequestsPin();

      asPerson(submit(REQUEST_ID, PIN));

      assertPromptEmptied();
      assertNothingLeaked();
   }

   @Test
   public void testTimedOutRequestIsShownEmptyRatherThanRemoved() {
      hubRequestsPin();

      subsystem.onEvent(timeout(S2PinPairing.VAR_TIMEOUT), context);

      // A null would be dropped on the way to clients, which would go on showing the prompt
      assertPromptEmptied();
   }

   @Test
   public void testAnswerCanOnlyBeGivenOnce() {
      hubRequestsPin();
      asPerson(submit(REQUEST_ID, PIN));
      popRequest();

      assertError(SubmitS2PinResponse.CODE_REQUEST_STATE_INVALID, asPerson(submit(REQUEST_ID, "54321")));
      assertNoHubRequestsSent();
   }

   @Test
   public void testAnswerForAnotherRequestIsRejected() {
      hubRequestsPin();

      assertError(SubmitS2PinResponse.CODE_REQUEST_STATE_INVALID, asPerson(submit("aaaaaaaa-0000-4000-8000-000000000009", PIN)));
      assertNoHubRequestsSent();
      assertEquals(REQUEST_ID, context.model().getS2PinRequestId());
   }

   @Test
   public void testAnswerWithNothingWaitingIsRejected() {
      assertError(SubmitS2PinResponse.CODE_REQUEST_STATE_INVALID, asPerson(submit(REQUEST_ID, PIN)));
      assertNoHubRequestsSent();
   }

   @Test
   public void testExpiredAnswerIsRejectedEvenIfTheTimerIsLate() {
      context.setVariable(S2PinPairing.VAR_REQUEST, new S2PinRequest(REQUEST_ID, NODE, hub.getAddress().getRepresentation(), System.currentTimeMillis() - 1, false));

      assertError(SubmitS2PinResponse.CODE_REQUEST_STATE_INVALID, asPerson(submit(REQUEST_ID, PIN)));
      assertNoHubRequestsSent();
   }

   @Test
   public void testMalformedPinsAreRejectedAndCanBeRetried() {
      hubRequestsPin();

      for(Object pin: new Object[] { "1234", "123456", "1234a", "", " 1234", "65536", "99999", 12345, null }) {
         MessageBody body = MessageBody.buildMessage(SubmitS2PinRequest.NAME, attributes(REQUEST_ID, pin));
         assertError(SubmitS2PinResponse.CODE_REQUEST_PARAM_INVALID, asPerson(body));
      }
      assertNoHubRequestsSent();
      assertEquals(REQUEST_ID, context.model().getS2PinRequestId());

      asPerson(submit(REQUEST_ID, PIN));
      assertEquals(PIN, HubZwaveCapability.SubmitS2PinRequest.getPin(popRequest().getMessage()));
   }

   @Test
   public void testErrorsDoNotQuoteThePin() {
      hubRequestsPin();

      ErrorEvent error = (ErrorEvent) asPerson(submit(REQUEST_ID, "999x9"));

      assertFalse(error.getMessage(), error.getMessage().contains("999x9"));
   }

   @Test
   public void testAnswerIsRejectedIfTheHubHasGone() {
      hubRequestsPin();
      removeModel(hub);

      assertError(SubmitS2PinResponse.CODE_REQUEST_STATE_INVALID, asPerson(submit(REQUEST_ID, PIN)));
      assertNoHubRequestsSent();
      assertNoPromptShown();
   }

   @Test
   public void testHubRefusingTheAnswerReachesTheClient() {
      hubRequestsPin();
      asPerson(submit(REQUEST_ID, PIN));

      popRequest().getAction().onResponse(context, hubResponse(Errors.fromCode(SubmitS2PinResponse.CODE_REQUEST_STATE_INVALID, "No longer waiting")));

      assertError(SubmitS2PinResponse.CODE_REQUEST_STATE_INVALID, lastResponse());
      assertRequestGone();
      // other clients only hear of it this way
      assertFailureBroadcast(S2PinPairing.CODE_FAILED);
   }

   @Test
   public void testHubRefusingAfterItHasFinishedReportsOnce() {
      hubRequestsPin();
      asPerson(submit(REQUEST_ID, PIN));
      SendAndExpect answer = popRequest();

      fromHub(finished(REQUEST_ID, S2PinFinishedEvent.RESULT_TIMEOUT));
      answer.getAction().onResponse(context, hubResponse(Errors.fromCode(SubmitS2PinResponse.CODE_REQUEST_STATE_INVALID, "No longer waiting")));

      assertError(SubmitS2PinResponse.CODE_REQUEST_STATE_INVALID, lastResponse());
      assertFailureBroadcast(S2PinPairing.CODE_TIMEOUT);
   }

   @Test
   public void testHubNotAnsweringReportsItOffline() {
      hubRequestsPin();
      asPerson(submit(REQUEST_ID, PIN));

      popRequest().getAction().onTimeout(context);

      assertError(SubmitS2PinResponse.CODE_HUB_OFFLINE, lastResponse());
      // it may still have the PIN, so the outcome is left to S2PinFinished or the timer
      assertTimeoutSet(S2PinPairing.VAR_TIMEOUT);
   }

   //////////
   // Who may answer
   //////////

   @Test
   public void testApiKeyMayAnswer() {
      hubRequestsPin();

      send(submit(REQUEST_ID, PIN), Address.platformService(UUID.randomUUID(), ApiKeyPrincipal.ACTOR_NAMESPACE), placeId.toString());

      assertEquals(PIN, HubZwaveCapability.SubmitS2PinRequest.getPin(popRequest().getMessage()));
   }

   @Test
   public void testSessionOnAnotherPlaceIsRefused() {
      hubRequestsPin();

      assertRefused(submit(REQUEST_ID, PIN), person, UUID.randomUUID().toString());
      assertRefused(CancelS2PinRequest.builder().withRequestId(REQUEST_ID).build(), person, UUID.randomUUID().toString());
      assertRefused(GetS2PinRequestRequest.instance(), person, UUID.randomUUID().toString());
   }

   @Test
   public void testSessionWithoutAPlaceIsRefused() {
      hubRequestsPin();

      assertRefused(submit(REQUEST_ID, PIN), person, null);
      assertRefused(GetS2PinRequestRequest.instance(), person, null);
   }

   @Test
   public void testPersonNotAtThisPlaceIsRefused() {
      hubRequestsPin();
      Address stranger = Address.platformService(UUID.randomUUID(), "person");

      assertRefused(submit(REQUEST_ID, PIN), stranger, placeId.toString());
      assertRefused(GetS2PinRequestRequest.instance(), stranger, placeId.toString());
      assertRefused(CancelS2PinRequest.builder().withRequestId(REQUEST_ID).build(), stranger, placeId.toString());
   }

   @Test
   public void testMissingActorIsRefused() {
      hubRequestsPin();

      assertRefused(submit(REQUEST_ID, PIN), null, placeId.toString());
   }

   @Test
   public void testOtherActorsAreRefused() {
      hubRequestsPin();

      assertRefused(submit(REQUEST_ID, PIN), hubSource, placeId.toString());
      assertRefused(submit(REQUEST_ID, PIN), Address.platformService(placeId, PairingSubsystemCapability.NAMESPACE), placeId.toString());
   }

   @Test
   public void testPersonRemovedFromThePlaceIsRefused() {
      hubRequestsPin();
      removeModel(person);

      assertRefused(submit(REQUEST_ID, PIN), person, placeId.toString());
   }

   //////////
   // How it ends
   //////////

   @Test
   public void testGrantedEndsQuietly() {
      hubRequestsPin();
      asPerson(submit(REQUEST_ID, PIN));

      fromHub(finished(REQUEST_ID, S2PinFinishedEvent.RESULT_GRANTED));

      assertRequestGone();
      assertNoFailureBroadcast();
   }

   @Test
   public void testWrongPinIsReported() {
      hubRequestsPin();
      asPerson(submit(REQUEST_ID, PIN));

      fromHub(finished(REQUEST_ID, S2PinFinishedEvent.RESULT_FAILED));

      assertRequestGone();
      assertFailureBroadcast(S2PinPairing.CODE_FAILED);
   }

   @Test
   public void testHubTimingOutIsReported() {
      hubRequestsPin();

      fromHub(finished(REQUEST_ID, S2PinFinishedEvent.RESULT_TIMEOUT));

      assertRequestGone();
      assertFailureBroadcast(S2PinPairing.CODE_TIMEOUT);
   }

   @Test
   public void testNodeGivingUpWhileWaitingIsReported() {
      hubRequestsPin();

      fromHub(finished(REQUEST_ID, S2PinFinishedEvent.RESULT_CANCELLED));

      assertRequestGone();
      assertFailureBroadcast(S2PinPairing.CODE_FAILED);
   }

   @Test
   public void testFinishedForAnotherRequestIsIgnored() {
      hubRequestsPin();

      fromHub(finished("aaaaaaaa-0000-4000-8000-000000000009", S2PinFinishedEvent.RESULT_FAILED));

      assertEquals(REQUEST_ID, context.model().getS2PinRequestId());
      assertNoFailureBroadcast();
   }

   @Test
   public void testFinishedForAnotherNodeIsIgnored() {
      hubRequestsPin();

      fromHub(S2PinFinishedEvent.builder().withRequestId(REQUEST_ID).withNode(NODE + 1).withResult(S2PinFinishedEvent.RESULT_FAILED).build());

      assertEquals(REQUEST_ID, context.model().getS2PinRequestId());
      assertNoFailureBroadcast();
   }

   @Test
   public void testFinishedFromAnotherHubIsIgnored() {
      hubRequestsPin();

      fromHub(finished(REQUEST_ID, S2PinFinishedEvent.RESULT_GRANTED), Address.hubService("ZZZ-9999", "zwave"), placeId.toString());

      assertEquals(REQUEST_ID, context.model().getS2PinRequestId());
   }

   @Test
   public void testCancelReachesTheHub() {
      hubRequestsPin();

      MessageBody response = asPerson(CancelS2PinRequest.builder().withRequestId(REQUEST_ID).build());

      assertEquals(CancelS2PinResponse.instance(), response);
      assertRequestGone();
      assertCancelSent(1);
      assertEquals(hub.getAddress(), requestAddresses.getValue());
   }

   @Test
   public void testCancelledRequestCanNotBeAnswered() {
      hubRequestsPin();
      asPerson(CancelS2PinRequest.builder().withRequestId(REQUEST_ID).build());

      assertError(SubmitS2PinResponse.CODE_REQUEST_STATE_INVALID, asPerson(submit(REQUEST_ID, PIN)));
      assertNoHubRequestsSent();
   }

   @Test
   public void testFinishedAfterACancelSaysNothingMore() {
      hubRequestsPin();
      asPerson(CancelS2PinRequest.builder().withRequestId(REQUEST_ID).build());

      fromHub(finished(REQUEST_ID, S2PinFinishedEvent.RESULT_CANCELLED));

      assertNoFailureBroadcast();
   }

   @Test
   public void testCancelForAnotherRequestIsRejected() {
      hubRequestsPin();

      MessageBody response = asPerson(CancelS2PinRequest.builder().withRequestId("aaaaaaaa-0000-4000-8000-000000000009").build());

      assertError(CancelS2PinResponse.CODE_REQUEST_STATE_INVALID, response);
      assertEquals(REQUEST_ID, context.model().getS2PinRequestId());
      assertCancelSent(0);
   }

   @Test
   public void testNobodyAnsweringEndsTheRequest() {
      hubRequestsPin();

      subsystem.onEvent(timeout(S2PinPairing.VAR_TIMEOUT), context);

      assertRequestGone();
      assertCancelSent(1);
      assertFailureBroadcast(S2PinPairing.CODE_TIMEOUT);
   }

   @Test
   public void testAnswerAfterTheTimerIsRejected() {
      hubRequestsPin();
      subsystem.onEvent(timeout(S2PinPairing.VAR_TIMEOUT), context);

      assertError(SubmitS2PinResponse.CODE_REQUEST_STATE_INVALID, asPerson(submit(REQUEST_ID, PIN)));
      assertNoHubRequestsSent();
   }

   @Test
   public void testHubGoingQuietAfterAnAnswerEndsTheRequest() {
      hubRequestsPin();
      asPerson(submit(REQUEST_ID, PIN));

      subsystem.onEvent(timeout(S2PinPairing.VAR_TIMEOUT), context);

      assertRequestGone();
      assertCancelSent(0);
      assertFailureBroadcast(S2PinPairing.CODE_FAILED);
   }

   @Test
   public void testPinTimerLeavesTheSearchRunning() {
      hubRequestsPin();

      subsystem.onEvent(timeout(S2PinPairing.VAR_TIMEOUT), context);

      assertPairingState(PairingStateName.Searching);
   }

   @Test
   public void testStoppingTheSearchCancelsTheRequest() {
      hubRequestsPin();

      stopSearching();

      assertRequestGone();
      assertCancelSent(1);
      assertNoFailureBroadcast();
   }

   @Test
   public void testDismissingCancelsTheRequest() {
      hubRequestsPin();

      dismissAll();

      assertRequestGone();
      assertCancelSent(1);
   }

   @Test
   public void testFactoryResetCancelsTheRequest() {
      hubRequestsPin();
      Date hold = S2PinPairing.holdSearchUntil(context);

      factoryReset();
      PlatformMessage response = PlatformMessage.builder().from(hub.getAddress()).to(context.model().getAddress()).withPayload(UnpairingRequestResponse.instance()).create();
      popRequest().getAction().onResponse(context, response);

      assertPairingState(PairingStateName.FactoryResetSteps);
      assertRequestGone();
      assertCancelSent(1);
      // the reset steps keep their own timer, not the one held open for the PIN
      assertTrue(context.model().getSearchTimeout().before(hold));
   }

   @Test
   public void testSearchTimingOutAfterTheWindowCancelsNothingNew() {
      hubRequestsPin();
      asPerson(CancelS2PinRequest.builder().withRequestId(REQUEST_ID).build());
      clearRequests();

      sendTimeout();

      assertPairingState(PairingStateName.Idle);
      assertCancelSent(0);
   }

   @Test
   public void testStoppingAfterAnAnswerDoesNotCancel() {
      hubRequestsPin();
      asPerson(submit(REQUEST_ID, PIN));
      popRequest();

      stopSearching();

      assertRequestGone();
      assertCancelSent(0);
   }

   //////////
   // Restarts
   //////////

   @Test
   public void testRestartWhileWaitingRestoresTheTimer() {
      hubRequestsPin();

      S2PinPairing.onStarted(context);

      assertEquals(REQUEST_ID, context.model().getS2PinRequestId());
      assertTimeoutSet(S2PinPairing.VAR_TIMEOUT);
   }

   @Test
   public void testLosingTheDskEndsTheRequest() {
      String unknown = "aaaaaaaa-0000-4000-8000-000000000042";
      context.setVariable(S2PinPairing.VAR_REQUEST, new S2PinRequest(unknown, NODE, hub.getAddress().getRepresentation(), System.currentTimeMillis() + TIMEOUT_MS, false));
      context.model().setS2PinRequestId(unknown);

      S2PinPairing.onStarted(context);

      assertRequestGone();
      assertCancelSent(1);
      assertFailureBroadcast(S2PinPairing.CODE_FAILED);
   }

   @Test
   public void testRestartAfterAnAnswerKeepsWaitingForTheResult() {
      String unknown = "aaaaaaaa-0000-4000-8000-000000000043";
      context.setVariable(S2PinPairing.VAR_REQUEST, new S2PinRequest(unknown, NODE, hub.getAddress().getRepresentation(), System.currentTimeMillis() + TIMEOUT_MS, true));

      S2PinPairing.onStarted(context);

      assertCancelSent(0);
      assertNotNull(context.getVariable(S2PinPairing.VAR_REQUEST).as(S2PinRequest.class));
   }

   //////////
   // Telling the hub a client can answer
   //////////

   @Test
   public void testSearchPassesOnPinEntry() {
      search(SearchRequest.builder().withS2PinEntry(true).build());

      assertEquals(Boolean.TRUE, PairingRequestRequest.getS2PinEntry(popRequest().getMessage()));
   }

   @Test
   public void testStartPairingPassesOnPinEntry() {
      search(StartPairingRequest.builder().withProductAddress(productAddress).withS2PinEntry(true).build());

      assertEquals(Boolean.TRUE, PairingRequestRequest.getS2PinEntry(popRequest().getMessage()));
   }

   @Test
   public void testPinEntryIsOffUnlessAskedFor() {
      search(SearchRequest.builder().build());

      assertEquals(Boolean.FALSE, PairingRequestRequest.getS2PinEntry(popRequest().getMessage()));
   }

   //////////
   // Helpers
   //////////

   private void search(MessageBody request) {
      sendPairingRequest(request);
   }

   private void hubRequestsPin() {
      fromHub(required(REQUEST_ID, DSK, TIMEOUT_MS));
   }

   private MessageBody required(String requestId, String dsk, long timeoutMs) {
      return S2PinRequiredEvent
            .builder()
            .withRequestId(requestId)
            .withNode(NODE)
            .withDsk(dsk)
            .withTimeoutMs(timeoutMs)
            .build();
   }

   private MessageBody finished(String requestId, String result) {
      return S2PinFinishedEvent.builder().withRequestId(requestId).withNode(NODE).withResult(result).build();
   }

   private MessageBody submit(String requestId, String pin) {
      return SubmitS2PinRequest.builder().withRequestId(requestId).withPin(pin).build();
   }

   private Map<String, Object> attributes(String requestId, Object pin) {
      Map<String, Object> attributes = new HashMap<>();
      attributes.put(SubmitS2PinRequest.ATTR_REQUESTID, requestId);
      attributes.put(SubmitS2PinRequest.ATTR_PIN, pin);
      return attributes;
   }

   private void fromHub(MessageBody body) {
      fromHub(body, hubSource, placeId.toString());
   }

   private void fromHub(MessageBody body, Date received) {
      fromHub(body, hubSource, placeId.toString(), received);
   }

   private void fromHub(MessageBody body, Address source, String place) {
      fromHub(body, source, place, new Date());
   }

   private void fromHub(MessageBody body, Address source, String place, Date received) {
      PlatformMessage message =
         PlatformMessage
            .builder()
            .from(source)
            .to(context.model().getAddress())
            .withPlaceId(place)
            .withTimestamp(received)
            .withPayload(body)
            .create();
      subsystem.onEvent(new MessageReceivedEvent(message), context);
      commit();
   }

   private MessageBody asPerson(MessageBody body) {
      return send(body, person, placeId.toString());
   }

   private MessageBody send(MessageBody body, Address actor, String place) {
      PlatformMessage.Builder request =
         PlatformMessage
            .buildRequest(body, clientAddress, context.model().getAddress())
            .withActor(actor)
            .withCorrelationId("correlation");
      if(place != null) {
         request.withPlaceId(place);
      }
      return sendRequest(new MessageReceivedEvent(request.create()));
   }

   private void assertRefused(MessageBody body, Address actor, String place) {
      MessageBody response = send(body, actor, place);

      assertEquals(ErrorEvent.MESSAGE_TYPE, response.getMessageType());
      assertEquals(Errors.CODE_NOT_FOUND, ((ErrorEvent) response).getCode());
      assertNoHubRequestsSent();
      assertCancelSent(0);
      assertEquals(REQUEST_ID, context.model().getS2PinRequestId());
   }

   private PlatformMessage hubResponse(MessageBody body) {
      return PlatformMessage.builder().from(hub.getAddress()).to(context.model().getAddress()).withPayload(body).create();
   }

   private MessageBody lastResponse() {
      return responses.getValues().get(responses.getValues().size() - 1);
   }

   /** No prompt is showing: empty once a request has ended, absent if there never was one. */
   private void assertNoPromptShown() {
      String requestId = context.model().getS2PinRequestId();
      assertTrue(requestId == null || requestId.isEmpty());
      Date expires = context.model().getS2PinExpires();
      assertTrue(expires == null || expires.getTime() == 0);
   }

   /** The attributes of a request that has ended, which clients can see change. */
   private void assertPromptEmptied() {
      assertEquals(S2PinPairing.NO_REQUEST, context.model().getS2PinRequestId());
      assertEquals(0L, context.model().getS2PinExpires().getTime());
   }

   private void assertRequestGone() {
      assertNoPromptShown();
      assertNull(context.getVariable(S2PinPairing.VAR_REQUEST).as(S2PinRequest.class));
      assertTimeoutCleared(S2PinPairing.VAR_TIMEOUT);
   }

   private void assertCancelSent(int count) {
      List<MessageBody> cancels =
         requests
            .getValues()
            .stream()
            .filter((body) -> HubZwaveCapability.CancelS2PinRequest.NAME.equals(body.getMessageType()))
            .collect(Collectors.toList());
      assertEquals(count, cancels.size());
      for(MessageBody cancel: cancels) {
         assertEquals(Integer.valueOf(NODE), HubZwaveCapability.CancelS2PinRequest.getNode(cancel));
      }
   }

   private void assertFailureBroadcast(String code) {
      List<MessageBody> failures = failures();
      assertEquals(1, failures.size());
      assertEquals(code, PairingFailedEvent.getCode(failures.get(0)));
   }

   private void assertNoFailureBroadcast() {
      assertTrue(failures().toString(), failures().isEmpty());
   }

   private List<MessageBody> failures() {
      return
         broadcasts
            .getValues()
            .stream()
            .filter((body) -> PairingFailedEvent.NAME.equals(body.getMessageType()))
            .collect(Collectors.toList());
   }

   /**
    * Everything saved with the subsystem, every attribute and variable, and
    * everything broadcast or requested.
    */
   private void assertNothingLeaked() {
      String all = context.model().toMap() + " " + attributesOf(broadcasts.getValues()) + " " + attributesOf(requests.getValues());
      assertFalse(all, all.contains("11111-22222"));
      assertFalse(all, all.contains(PIN));
   }

   private String attributesOf(List<MessageBody> bodies) {
      return bodies.stream().map(MessageBody::getAttributes).collect(Collectors.toList()).toString();
   }
}

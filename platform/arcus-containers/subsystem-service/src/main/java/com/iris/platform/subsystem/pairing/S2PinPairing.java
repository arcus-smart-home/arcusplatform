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
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.SubsystemContext.ResponseAction;
import com.iris.common.subsystem.SubsystemUtils;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.HubZwaveCapability;
import com.iris.messages.capability.HubZwaveCapability.S2PinFinishedEvent;
import com.iris.messages.capability.HubZwaveCapability.S2PinRequiredEvent;
import com.iris.messages.capability.PairingSubsystemCapability;
import com.iris.messages.capability.PairingSubsystemCapability.CancelS2PinRequest;
import com.iris.messages.capability.PairingSubsystemCapability.CancelS2PinResponse;
import com.iris.messages.capability.PairingSubsystemCapability.GetS2PinRequestResponse;
import com.iris.messages.capability.PairingSubsystemCapability.PairingFailedEvent;
import com.iris.messages.capability.PairingSubsystemCapability.SubmitS2PinRequest;
import com.iris.messages.capability.PairingSubsystemCapability.SubmitS2PinResponse;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.errors.Errors;
import com.iris.messages.errors.UnauthorizedRequestException;
import com.iris.messages.model.Model;
import com.iris.messages.model.subs.PairingSubsystemModel;
import com.iris.platform.subsystem.pairing.state.PairingStateMachine;
import com.iris.protocol.zwave.S2Pin;
import com.iris.security.apikey.ApiKeyPrincipal;

/**
 * Connects a hub waiting for a Z-Wave S2 PIN with the client answering it. The
 * contract is in docs/zwave-s2-pin-pairing.md.
 *
 * The PIN and the DSK are secrets. Neither may reach a log, a subsystem
 * variable, an attribute or a broadcast, and messages carrying them are
 * redacted by MessageBody. So the PIN is only passed through, and the DSK is
 * held in memory here and only leaves in the response to GetS2PinRequest.
 * What gets saved is an S2PinRequest. If the process restarts the DSK is gone
 * and the request is ended, since a PIN entered without checking the DSK
 * proves very little.
 */
public final class S2PinPairing {
   /** The variable holding the timeout for a request. */
   public static final String VAR_TIMEOUT = "s2PinTimeout";

   public static final String CODE_TIMEOUT = "zwave.s2.pin.timeout";
   public static final String CODE_FAILED = "zwave.s2.pin.failed";

   /** What s2PinRequestId is set to once a request ends, with s2PinExpires at 0. */
   public static final String NO_REQUEST = "";

   /** The most a hub may ask for: TAI2, which also has to cover the rest of the key exchange. */
   static final long MAX_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(240);
   static final long MIN_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(30);
   /** Answers stop this long before the hub gives up, so that an accepted PIN still reaches it. */
   static final long GRACE_MS = TimeUnit.SECONDS.toMillis(5);
   /**
    * How long the hub has to report the key exchange ended once it has a PIN:
    * what is left of TAI2 for the echo, then every key's get, report and verify.
    */
   static final long ANSWERED_WAIT_MS = TimeUnit.SECONDS.toMillis(150);

   static final String VAR_REQUEST = "s2PinRequest";
   /** The id of the request that ended last, so that a late copy of it isn't taken for a new one. */
   static final String VAR_ENDED = "s2PinEnded";
   private static final int MIN_NODE = 1;
   private static final int MAX_NODE = 232;
   private static final Pattern REQUEST_ID = Pattern.compile("[A-Za-z0-9-]{1,64}");

   // A hub's wait is over well before an entry expires, this only stops a DSK outliving it
   private static final Cache<String, String> DSKS =
         CacheBuilder
            .newBuilder()
            .expireAfterWrite(MAX_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .maximumSize(1024)
            .build();

   private S2PinPairing() {
   }

   //////////
   // From the hub
   //////////

   public static void onRequired(SubsystemContext<PairingSubsystemModel> context, PlatformMessage message) {
      Optional<Model> hub = getSendingHub(context, message);
      if(!hub.isPresent() || message.getDestination().isBroadcast()) {
         context.logger().warn("Ignoring {} from [{}]: not sent to this subsystem by the hub at this place", message.getMessageType(), message.getSource());
         return;
      }

      MessageBody body = message.getValue();
      String requestId = read(() -> S2PinRequiredEvent.getRequestId(body));
      Integer node = read(() -> S2PinRequiredEvent.getNode(body));
      if(requestId == null || !REQUEST_ID.matcher(requestId).matches() || node == null || node < MIN_NODE || node > MAX_NODE) {
         // nothing to answer it with, so the hub times out
         context.logger().warn("Ignoring {} from [{}]: malformed request id or node", message.getMessageType(), message.getSource());
         return;
      }

      S2PinRequest current = loadRequest(context);
      if(current != null && requestId.equals(current.getRequestId())) {
         context.logger().debug("Ignoring repeated {} for request [{}]", message.getMessageType(), requestId);
         return;
      }
      if(requestId.equals(context.getVariable(VAR_ENDED).as(String.class))) {
         // The hub always answers a cancel and ignores one for a request it has finished,
         // so say it again in case the first one was lost
         context.logger().debug("Cancelling repeated {} for request [{}], which has ended", message.getMessageType(), requestId);
         context.request(hub.get().getAddress(), cancelMessage(requestId, node));
         return;
      }

      String dsk = read(() -> S2PinRequiredEvent.getDsk(body));
      Long timeoutMs = read(() -> S2PinRequiredEvent.getTimeoutMs(body));
      boolean wellFormed = S2Pin.isValidBlankedDsk(dsk) && timeoutMs != null && timeoutMs >= MIN_TIMEOUT_MS && timeoutMs <= MAX_TIMEOUT_MS;
      if(!wellFormed || !context.model().isPairingModeHUB()) {
         context.logger().warn("Refusing {} for request [{}]: pairing mode [{}], well formed [{}]", message.getMessageType(), requestId, context.model().getPairingMode(), wellFormed);
         context.request(hub.get().getAddress(), cancelMessage(requestId, node));
         return;
      }

      // Counted from when the hub bridge took the request, so time spent queued here comes out of
      // the user's window rather than past the hub's
      long now = System.currentTimeMillis();
      Date received = message.getTimestamp();
      Date expires = new Date(Math.min(now, received == null ? now : received.getTime()) + timeoutMs - GRACE_MS);
      if(expires.getTime() <= now) {
         context.logger().warn("Ignoring {} for request [{}]: it arrived after the hub stopped waiting", message.getMessageType(), requestId);
         return;
      }

      // Inclusion is one node at a time, so a new request means the hub has moved on from the last one
      clear(context);

      saveRequest(context, new S2PinRequest(requestId, node, hub.get().getAddress().getRepresentation(), expires.getTime(), false));
      DSKS.put(dskKey(context, requestId), dsk);
      SubsystemUtils.setTimeout(expires, context, VAR_TIMEOUT);

      context.model().setS2PinRequestId(requestId);
      context.model().setS2PinExpires(expires);
      // The node has joined, even though it can't show up as a pairing device until it has its keys
      context.model().setSearchDeviceFound(true);
      context.model().setSearchIdle(false);
      context.model().setSearchIdleTimeout(new Date(0));
      PairingStateMachine.get(context).keepSearchingUntil(holdSearchUntil(context));
   }

   public static void onFinished(SubsystemContext<PairingSubsystemModel> context, PlatformMessage message) {
      if(!getSendingHub(context, message).isPresent()) {
         context.logger().warn("Ignoring {} from [{}]: not sent by the hub at this place", message.getMessageType(), message.getSource());
         return;
      }

      MessageBody body = message.getValue();
      S2PinRequest request = loadRequest(context);
      if(
            request == null ||
            !Objects.equals(request.getRequestId(), read(() -> S2PinFinishedEvent.getRequestId(body))) ||
            !Objects.equals(request.getNode(), read(() -> S2PinFinishedEvent.getNode(body)))
      ) {
         context.logger().debug("Ignoring {} for a request that is no longer pending", message.getMessageType());
         return;
      }

      clear(context);
      String result = read(() -> S2PinFinishedEvent.getResult(body));
      if(S2PinFinishedEvent.RESULT_GRANTED.equals(result)) {
         return;
      }
      // A cancel that came from here has already cleared the request, so this one came from the hub
      failed(context, S2PinFinishedEvent.RESULT_TIMEOUT.equals(result) ? CODE_TIMEOUT : CODE_FAILED);
   }

   //////////
   // From the client
   //////////

   public static MessageBody getRequest(SubsystemContext<PairingSubsystemModel> context, PlatformMessage request) {
      assertMayPair(context, request);

      S2PinRequest pending = loadRequest(context);
      if(pending == null || !isOpen(pending)) {
         throw new PairingSubsystemCapability.RequestStateInvalidException("No device is waiting for a PIN");
      }
      String dsk = DSKS.getIfPresent(dskKey(context, pending.getRequestId()));
      if(dsk == null) {
         throw new PairingSubsystemCapability.RequestStateInvalidException("No device is waiting for a PIN");
      }
      return
         GetS2PinRequestResponse
            .builder()
            .withRequestId(pending.getRequestId())
            .withDsk(dsk)
            .withExpires(new Date(pending.getExpires()))
            .build();
   }

   public static MessageBody submit(SubsystemContext<PairingSubsystemModel> context, PlatformMessage request) {
      assertMayPair(context, request);

      MessageBody body = request.getValue();
      S2PinRequest pending = getOpenRequest(context, read(() -> SubmitS2PinRequest.getRequestId(body)));
      Object pin = body.getAttributes().get(SubmitS2PinRequest.ATTR_PIN);
      if(!S2Pin.isValidPin(pin)) {
         throw new PairingSubsystemCapability.RequestParamInvalidException("The PIN must be five digits from 00000 to 65535");
      }
      Address hub = Address.fromString(pending.getHub());
      if(!PairingUtils.getHubModel(context).filter((model) -> hub.equals(model.getAddress())).isPresent()) {
         clear(context);
         throw new PairingSubsystemCapability.RequestStateInvalidException("The hub that requested this PIN has left the place");
      }

      // Spent from here on, whatever the hub says: there is one attempt for each request
      saveRequest(context, pending.toAnswered());
      forgetPrompt(context, pending);
      SubsystemUtils.setTimeout(ANSWERED_WAIT_MS, context, VAR_TIMEOUT);

      MessageBody answer =
         HubZwaveCapability.SubmitS2PinRequest
            .builder()
            .withRequestId(pending.getRequestId())
            .withNode(pending.getNode())
            .withPin((String) pin)
            .build();
      context.sendAndExpectResponse(hub, answer, PairingUtils.requestTimeoutMs(), TimeUnit.MILLISECONDS, new ResponseAction<PairingSubsystemModel>() {
         @Override
         public void onResponse(SubsystemContext<PairingSubsystemModel> context, PlatformMessage response) {
            if(response.isError()) {
               // The hub has stopped waiting. Its S2PinFinished may come after this and be
               // ignored, so whichever of the two ends the request reports the failure.
               if(clearIfCurrent(context, pending)) {
                  failed(context, CODE_FAILED);
               }
               context.sendResponse(request, response.getValue());
            }
            else {
               context.sendResponse(request, SubmitS2PinResponse.instance());
            }
         }

         @Override
         public void onError(SubsystemContext<PairingSubsystemModel> context, Throwable cause) {
            if(clearIfCurrent(context, pending)) {
               failed(context, CODE_FAILED);
            }
            context.sendResponse(request, Errors.fromException(cause));
         }

         @Override
         public void onTimeout(SubsystemContext<PairingSubsystemModel> context) {
            // the hub may still have the PIN, so leave the request to be finished or to time out
            context.sendResponse(request, Errors.fromException(new PairingSubsystemCapability.HubOfflineException("Timeout contacting hub")));
         }
      });
      return MessageBody.noResponse();
   }

   public static MessageBody cancel(SubsystemContext<PairingSubsystemModel> context, PlatformMessage request) {
      assertMayPair(context, request);

      MessageBody body = request.getValue();
      S2PinRequest pending = getOpenRequest(context, read(() -> CancelS2PinRequest.getRequestId(body)));
      clear(context);
      sendCancel(context, pending);
      return CancelS2PinResponse.instance();
   }

   //////////
   // Subsystem lifecycle
   //////////

   public static void onStarted(SubsystemContext<PairingSubsystemModel> context) {
      S2PinRequest pending = loadRequest(context);
      if(pending == null) {
         return;
      }
      if(!pending.isAnswered() && DSKS.getIfPresent(dskKey(context, pending.getRequestId())) == null) {
         context.logger().info("Ending S2 PIN request [{}]: the DSK did not survive a restart", pending.getRequestId());
         clear(context);
         sendCancel(context, pending);
         failed(context, CODE_FAILED);
         return;
      }
      SubsystemUtils.restoreTimeout(context, VAR_TIMEOUT);
   }

   public static void onTimeout(SubsystemContext<PairingSubsystemModel> context) {
      S2PinRequest pending = loadRequest(context);
      if(pending == null) {
         return;
      }

      clear(context);
      if(pending.isAnswered()) {
         failed(context, CODE_FAILED);
      }
      else {
         sendCancel(context, pending);
         failed(context, CODE_TIMEOUT);
      }
   }

   /**
    * Called when pairing ends, for any reason. The hub may have left pairing
    * mode already, so its own STOP_PAIRING can't be relied on to end the wait.
    */
   public static void abandon(SubsystemContext<PairingSubsystemModel> context) {
      S2PinRequest pending = loadRequest(context);
      if(pending == null) {
         return;
      }

      clear(context);
      if(!pending.isAnswered()) {
         sendCancel(context, pending);
      }
   }

   /**
    * The earliest the search may end without cutting off a request, or the
    * epoch when there is none. A state that resets the search timeout has to
    * respect this, or a PIN asked for while showing the pairing steps would be
    * cut short when the search starts.
    */
   public static Date holdSearchUntil(SubsystemContext<PairingSubsystemModel> context) {
      S2PinRequest pending = loadRequest(context);
      return new Date(pending == null ? 0 : pending.getExpires() + ANSWERED_WAIT_MS);
   }

   //////////
   // Helpers
   //////////

   /**
    * The subsystem is chosen by the destination of a request, so the place of
    * the caller's session has to be checked here. The bridges set the place and
    * the actor themselves, and they only accept a session's active place after
    * checking its grants. Pairing is open to anyone with access to the place.
    * An API key gets here by holding subpairing permission on its one place,
    * which the api-bridge checks.
    */
   private static void assertMayPair(SubsystemContext<PairingSubsystemModel> context, PlatformMessage request) {
      Address actor = request.getActor();
      boolean onPlace = context.getPlaceId().toString().equals(request.getPlaceId());
      boolean person = actor != null && PersonCapability.NAMESPACE.equals(actor.getGroup()) && context.models().getModelByAddress(actor) != null;
      boolean apiKey = actor != null && ApiKeyPrincipal.ACTOR_NAMESPACE.equals(actor.getGroup());
      if(!onPlace || !(person || apiKey)) {
         context.logger().warn("Rejecting {} from actor [{}] with place [{}]", request.getMessageType(), actor, request.getPlaceId());
         throw new UnauthorizedRequestException(request.getDestination(), "Not authorized to pair at this place");
      }
   }

   private static Optional<Model> getSendingHub(SubsystemContext<PairingSubsystemModel> context, PlatformMessage message) {
      String hubId = message.getSource().getHubId();
      if(hubId == null || !context.getPlaceId().toString().equals(message.getPlaceId())) {
         return Optional.empty();
      }
      return PairingUtils.getHubModel(context).filter((hub) -> hubId.equals(hub.getId()));
   }

   private static S2PinRequest getOpenRequest(SubsystemContext<PairingSubsystemModel> context, String requestId) {
      S2PinRequest pending = loadRequest(context);
      if(pending == null || !isOpen(pending) || !pending.getRequestId().equals(requestId)) {
         throw new PairingSubsystemCapability.RequestStateInvalidException("That request is not waiting for a PIN");
      }
      return pending;
   }

   private static boolean isOpen(S2PinRequest request) {
      return !request.isAnswered() && System.currentTimeMillis() < request.getExpires();
   }

   private static S2PinRequest loadRequest(SubsystemContext<PairingSubsystemModel> context) {
      return context.getVariable(VAR_REQUEST).as(S2PinRequest.class);
   }

   private static void saveRequest(SubsystemContext<PairingSubsystemModel> context, S2PinRequest request) {
      context.setVariable(VAR_REQUEST, request);
   }

   private static void clear(SubsystemContext<PairingSubsystemModel> context) {
      S2PinRequest pending = loadRequest(context);
      if(pending != null) {
         forgetPrompt(context, pending);
         context.setVariable(VAR_ENDED, pending.getRequestId());
      }
      context.setVariable(VAR_REQUEST, null);
      SubsystemUtils.clearTimeout(context, VAR_TIMEOUT);
   }

   /** Clears the request if it is still the given one, and says whether it was. */
   private static boolean clearIfCurrent(SubsystemContext<PairingSubsystemModel> context, S2PinRequest request) {
      S2PinRequest pending = loadRequest(context);
      if(pending != null && pending.getRequestId().equals(request.getRequestId())) {
         clear(context);
         return true;
      }
      return false;
   }

   private static void forgetPrompt(SubsystemContext<PairingSubsystemModel> context, S2PinRequest request) {
      DSKS.invalidate(dskKey(context, request.getRequestId()));
      // Empty rather than null: a null is dropped when the value change is read off the
      // platform bus, so clients would go on showing the prompt
      context.model().setS2PinRequestId(NO_REQUEST);
      context.model().setS2PinExpires(new Date(0));
   }

   private static void sendCancel(SubsystemContext<PairingSubsystemModel> context, S2PinRequest request) {
      context.request(Address.fromString(request.getHub()), cancelMessage(request.getRequestId(), request.getNode()));
   }

   private static MessageBody cancelMessage(String requestId, int node) {
      return
         HubZwaveCapability.CancelS2PinRequest
            .builder()
            .withRequestId(requestId)
            .withNode(node)
            .build();
   }

   private static void failed(SubsystemContext<PairingSubsystemModel> context, String code) {
      String description = CODE_TIMEOUT.equals(code)
            ? "The Z-Wave PIN was not entered in time"
            : "The Z-Wave device did not complete secure pairing";
      context.broadcast(PairingFailedEvent.builder().withCode(code).withDescription(description).build());
   }

   private static String dskKey(SubsystemContext<PairingSubsystemModel> context, String requestId) {
      return context.getPlaceId() + ":" + requestId;
   }

   // The generated getters throw on a value that can't be coerced, and the
   // exception would quote it, so a bad value becomes null here
   private static <T> T read(Supplier<T> getter) {
      try {
         return getter.get();
      }
      catch(RuntimeException e) {
         return null;
      }
   }
}

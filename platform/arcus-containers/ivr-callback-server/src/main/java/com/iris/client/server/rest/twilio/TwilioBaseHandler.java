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
package com.iris.client.server.rest.twilio;

import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import org.apache.commons.codec.digest.HmacUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.google.common.net.MediaType;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.handlers.TemplatedHttpHandler;
import com.iris.bridge.server.http.impl.auth.AlwaysAllow;
import com.iris.core.notification.Notifications;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.core.template.TemplateService;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.NotificationCapability;
import com.iris.messages.capability.PersonCapability;
import com.iris.metrics.IrisMetricSet;
import com.iris.metrics.IrisMetrics;
import com.iris.platform.notification.provider.ivr.TwilioHelper;
import com.iris.population.PlacePopulationCacheManager;

import io.netty.handler.codec.http.FullHttpRequest;

public abstract class TwilioBaseHandler extends TemplatedHttpHandler {
   
   public static final Address ADDRESS_NOTIFICATION = Address.platformService(NotificationCapability.NAMESPACE);
   private static final Logger LOGGER = LoggerFactory.getLogger(TwilioBaseHandler.class);
   private static final String SERVICE_NAME="twilio.callback.handler";
   private static final IrisMetricSet METRICS = IrisMetrics.metrics(SERVICE_NAME);
   private final Counter FAILED_SIGNATURE_VERFICATIONS_COUNTER = METRICS.counter("request.verificationfailed.count");

   protected static final String SCRIPT_PARAM = "script";

   @Inject @Named("twilio.account.auth") private String twilioAccountAuth;
   @Inject(optional = true) @Named("twilio.verify.signature") private boolean twilioVerifyAuth=true;
   @Inject @Named("twilio.callback.host") private String callbackHost;
   @Inject(optional = true) @Named("twilio.callback.protocol") private String callbackProtocol="https";
   
   @Inject private PlatformMessageBus bus;
   protected final PlacePopulationCacheManager populationCacheMgr;

   public TwilioBaseHandler(AlwaysAllow alwaysAllow, HttpSender httpSender, TemplateService templateService, PlacePopulationCacheManager populationCacheMgr) {
      super(alwaysAllow, httpSender,templateService);
      this.populationCacheMgr = populationCacheMgr;
   }
   
   protected boolean verifyRequest(FullHttpRequest request){
      
      if(!request.headers().contains(TwilioHelper.SIGNATURE_HEADER_KEY)){
         throw new IllegalArgumentException(String.format("%s header not found",TwilioHelper.SIGNATURE_HEADER_KEY));
      }
      
      String signature = request.headers().get(TwilioHelper.SIGNATURE_HEADER_KEY);
      String host = request.headers().get(TwilioHelper.HOST_HEADER_KEY);

      if(callbackHost != null){
         host=callbackHost;
      }
      
      String fullURL=callbackProtocol + "://" + host + request.getUri();
      
      boolean verified = verifySignature(twilioAccountAuth, fullURL, signature);
      
      if(!verified && twilioVerifyAuth){
         LOGGER.warn("Twilio Failed Signature Verfication: Signature: {} URL: {} ", signature, fullURL);
         FAILED_SIGNATURE_VERFICATIONS_COUNTER.inc();
         return false;
      }
      return true;
   }
   
   private boolean verifySignature(String key, String uri, String signature) {
      byte[] generatedSig = HmacUtils.hmacSha1(key, uri);
      String encodedGeneratedSig = Base64.getEncoder().encodeToString(generatedSig);
      if (signature.equals(encodedGeneratedSig.toString())) {
         return true;
      }
      return false;
   }
  
   protected void broadcastIvrNotificationAcknowledgedEvent(String notificationId, Date timestamp, String response,String personId,String msgKey){
      MessageBody payload = NotificationCapability.IvrNotificationAcknowledgedEvent.builder()
         .withNotificationId(notificationId)
         .withTimestamp(timestamp)
         .withResponse(response)
         .withMsgKey(msgKey)
         .build();
      broadcastNotificationEvent(payload, notificationId,
         personId != null ? Address.platformService(personId, PersonCapability.NAMESPACE) : null);
   }
   
   protected void broadcastIvrNotificationVerifiedPinEvent(String notificationId, Date timestamp, String personId,String msgKey){
      MessageBody payload = NotificationCapability.IvrNotificationVerifiedPinEvent.builder()
         .withNotificationId(notificationId)
         .withTimestamp(timestamp)
         .withMsgKey(msgKey)
         .build();
      broadcastNotificationEvent(payload, notificationId,
         personId != null ? Address.platformService(personId, PersonCapability.NAMESPACE) : null);
   }
   
   protected void broadcastIvrNotificationRefusedEvent(String notificationId, Date timestamp, String reason, String code, String personId, String msgKey){
      MessageBody payload = NotificationCapability.IvrNotificationRefusedEvent.builder()
         .withNotificationId(notificationId)
         .withTimestamp(timestamp)
         .withReason(reason)
         .withCode(code)
         .withMsgKey(msgKey)
         .build();
      broadcastNotificationEvent(payload, notificationId,
         personId != null ? Address.platformService(personId, PersonCapability.NAMESPACE) : null);
   }
   
   private void broadcastNotificationEvent(MessageBody payload,String notificationId, Address actor){
   	UUID placeId = Notifications.getPlaceId(notificationId);
      PlatformMessage message = PlatformMessage.broadcast()
            .from(ADDRESS_NOTIFICATION)
            .withPayload(payload)
            .withActor(actor)
            .withPlaceId(placeId)
            .withPopulation(populationCacheMgr.getPopulationByPlaceId(placeId))
            .create();
         bus.send(message); 
   }
   
   @Override
   public String getContentType() {
      return MediaType.APPLICATION_XML_UTF_8.toString();
   }

}


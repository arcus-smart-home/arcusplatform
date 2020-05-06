/*
 * Copyright 2020 Arcus Project
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
package com.iris.client.server.rest;

import javax.naming.OperationNotSupportedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.client.Client;
import com.iris.bridge.server.client.ClientFactory;
import com.iris.bridge.server.config.RESTHandlerConfig;
import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.annotation.HttpPost;
import com.iris.bridge.server.http.handlers.RESTHandler;
import com.iris.bridge.server.http.impl.auth.SessionAuth;
import com.iris.bridge.server.netty.Authenticator;
import com.iris.core.dao.MobileDeviceDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.notification.Notifications;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.messages.ClientMessage;
import com.iris.messages.MessageBody;
import com.iris.messages.address.PlatformServiceAddress;
import com.iris.messages.capability.PlaceCapability;
import com.iris.messages.errors.ErrorEventException;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.MobileDevice;
import com.iris.messages.model.Person;
import com.iris.messages.service.SessionService;
import com.iris.messages.service.SessionService.LockDeviceRequest;
import com.iris.messages.service.SessionService.LockDeviceResponse;
import com.iris.population.PlacePopulationCacheManager;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;

@Singleton
@HttpPost("/" + SessionService.NAMESPACE + "/LockDevice")
public class LockDeviceRESTHandler extends RESTHandler {
   private final MobileDeviceDAO mobileDeviceDao;
   private final PlatformMessageBus bus;
   private final ClientFactory factory;
   private final Authenticator authenticator;
   private final PersonDAO personDao;
   private final PlacePopulationCacheManager populationCacheMgr;
   private static final Logger logger = LoggerFactory.getLogger(LockDeviceRESTHandler.class);

   @Inject
   public LockDeviceRESTHandler(SessionAuth auth,
                                Authenticator authenticator,
                                BridgeMetrics metrics,
                                RESTHandlerConfig restHandlerConfig,
                                MobileDeviceDAO mobileDeviceDao,
                                PlatformMessageBus bus,
                                ClientFactory factory,
                                PersonDAO personDao,
                                PlacePopulationCacheManager populationCacheMgr) {
      super(auth, new HttpSender(LockDeviceRESTHandler.class, metrics), restHandlerConfig);
      this.mobileDeviceDao = mobileDeviceDao;
      this.bus = bus;
      this.factory = factory;
      this.authenticator = authenticator;
      this.personDao = personDao;
      this.populationCacheMgr = populationCacheMgr;
   }



   @Override
   public FullHttpResponse respond(FullHttpRequest req, ChannelHandlerContext ctx) throws Exception {
      try {
         FullHttpResponse response = super.respond(req, ctx);
         response.headers().set(HttpHeaders.Names.SET_COOKIE, ServerCookieEncoder.STRICT.encode(authenticator.expireCookie()));
         return response;
      } finally {
         try {
            factory.get(ctx.channel()).logout();
         } catch (Throwable t) {
            logger.debug("Error attempting to logout current user", t);
         }
      }
   }


   @Override
   protected MessageBody doHandle(ClientMessage request, ChannelHandlerContext ctx) {
      MessageBody payload = request.getPayload();
      String deviceIdentifier = LockDeviceRequest.getDeviceIdentifier(payload);
      String reason = LockDeviceRequest.getReason(payload);
      validateParameters(deviceIdentifier, reason);

      MobileDevice mobileDeviceRecord = mobileDeviceDao.findWithToken(deviceIdentifier);
      if (mobileDeviceRecord != null) {
         //0. Make sure the person id in the mobile device record matches the person logged in
         Client client = factory.get(ctx.channel());
         if (client.getPrincipalId().equals(mobileDeviceRecord.getPersonId())) {
            //1. Device the mobile device record
            mobileDeviceDao.delete(mobileDeviceRecord);
            //2. Send notification if reason is TOUCH_FAILED
            boolean isSend = sendNotificationIfNecessary(mobileDeviceRecord);
            if (!isSend) {
               logger.warn("Failed to send notification during device lock for deviceIdentifier [{}]", deviceIdentifier);
            } else {
               //success
               return LockDeviceResponse.instance();
            }
         } else {
            logger.warn("The person id in the mobile device record does not match the person logged in");
         }
      } else {
         logger.warn("Failed to send notification during device lock for deviceIdentifier [{}] because mobile device record is not found", deviceIdentifier);
      }
      return Errors.invalidRequest("Lock Device request failed");
   }

   @Override
   protected MessageBody doHandle(ClientMessage request) throws Exception {
      return Errors.fromException(new OperationNotSupportedException());
   }

   private boolean sendNotificationIfNecessary(MobileDevice mobileDeviceRecord){
      if (mobileDeviceRecord.getPersonId() != null) {
         Person person = personDao.findById(mobileDeviceRecord.getPersonId());
         if (person != null && person.getCurrPlace() != null) {
            Notifications.sendEmailNotification(bus, person.getId().toString(), person.getCurrPlace().toString(), populationCacheMgr.getPopulationByPlaceId(person.getCurrPlace()), Notifications.TouchFailed.KEY,
                    ImmutableMap.<String, String>of(Notifications.TouchFailed.PARAM_DEVICE_MODEL, emptyIfNull(mobileDeviceRecord.getDeviceModel()),
                            Notifications.TouchFailed.PARAM_DEVICE_VENDOR, emptyIfNull(mobileDeviceRecord.getDeviceVendor()),
                            Notifications.TouchFailed.PARAM_OS_TYPE, emptyIfNull(mobileDeviceRecord.getOsType()),
                            Notifications.TouchFailed.PARAM_OS_VERSION, emptyIfNull(mobileDeviceRecord.getOsVersion())),
                    PlatformServiceAddress.platformService(person.getCurrPlace(), PlaceCapability.NAMESPACE));
            return true;
         }
      }
      return false;
   }

   private String emptyIfNull(String str) {
      if (str == null) {
         return "";
      } else{
         return str;
      }
   }

   private void validateParameters(String deviceId, String reason) {
      Errors.assertRequiredParam(deviceId, LockDeviceRequest.ATTR_DEVICEIDENTIFIER);
      Errors.assertRequiredParam(reason, LockDeviceRequest.ATTR_REASON);
      if (!LockDeviceRequest.REASON_TOUCH_FAILED.equals(reason) && !LockDeviceRequest.REASON_USER_REQUESTED.equals(reason)) {
         new ErrorEventException(Errors.invalidParam(LockDeviceRequest.ATTR_REASON));
      }

   }
}

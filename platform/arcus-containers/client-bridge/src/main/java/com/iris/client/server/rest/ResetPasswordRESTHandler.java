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
package com.iris.client.server.rest;

import static com.iris.core.dao.PersonDAO.ResetPasswordResult.FAILURE;
import static com.iris.core.dao.PersonDAO.ResetPasswordResult.TOKEN_FAILURE;
import static com.iris.messages.service.PersonService.ResetPasswordResponse.CODE_PERSON_NOT_FOUND;
import static com.iris.messages.service.PersonService.ResetPasswordResponse.CODE_PERSON_RESET_FAILED;
import static com.iris.messages.service.PersonService.ResetPasswordResponse.CODE_PERSON_RESET_TOKEN_FAILED;

import java.net.URLEncoder;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.CookieConfig;
import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.annotation.HttpPost;
import com.iris.bridge.server.http.impl.HttpResource;
import com.iris.bridge.server.http.impl.auth.AlwaysAllow;
import com.iris.bridge.server.netty.Authenticator;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PersonDAO.ResetPasswordResult;
import com.iris.core.notification.Notifications;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.io.json.JSON;
import com.iris.messages.ClientMessage;
import com.iris.messages.ErrorEvent;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.capability.NotificationCapability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.errors.Errors;
import com.iris.messages.model.Person;
import com.iris.messages.service.PersonService;
import com.iris.messages.services.PlatformConstants;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.util.CharsetUtil;

@Singleton
@HttpPost("/person/ResetPassword")
public class ResetPasswordRESTHandler extends HttpResource {
   private static final String PERSON_NOT_FOUND_MSG = "Unable to locate record for person";
   private static final String PASSWORD_RESET_FAILED_MSG = "Unable to reset password, perhaps try again.";
   private static final String PASSWORD_RESET_TOKEN_FAILED_MSG = "Unable to reset password because reset token doesn't match, perhaps try again.";
   public static final int MAX_PASSWORD_LENGTH = 100;
   public static final int MIN_PASSWORD_LENGTH = 8;

   private static final Logger logger = LoggerFactory.getLogger(ResetPasswordRESTHandler.class);
   private final CookieConfig cookieConfig;
   private final PersonDAO personDao;
   private final PlatformMessageBus platformBus;
   private final Authenticator authenticator;

   @Inject
   public ResetPasswordRESTHandler(CookieConfig cookieConfig, PersonDAO personDao, PlatformMessageBus platformBus,
         Authenticator authenticator, BridgeMetrics metrics, AlwaysAllow alwaysAllow) {
      super(alwaysAllow, new HttpSender(ResetPasswordRESTHandler.class, metrics));
      this.cookieConfig = cookieConfig;
      this.personDao = personDao;
      this.platformBus = platformBus;
      this.authenticator = authenticator;
   }

   @Override
   public FullHttpResponse respond(FullHttpRequest req, ChannelHandlerContext ctx) throws Exception {
      String json = req.content().toString(CharsetUtil.UTF_8);
      ClientMessage clientMessage = JSON.fromJson(json, ClientMessage.class);

      MessageBody body = clientMessage.getPayload();

      String email = PersonService.ResetPasswordRequest.getEmail(body);
      String password = PersonService.ResetPasswordRequest.getPassword(body);
      Person person = personDao.findByEmail(email);

      Errors.assertValidRequest(password.length() < MAX_PASSWORD_LENGTH, "New password is too long.");
      Errors.assertValidRequest(password.length() > MIN_PASSWORD_LENGTH, "New password is missing or too short.");

      if(person == null) {
         return error(CODE_PERSON_NOT_FOUND, PERSON_NOT_FOUND_MSG);
      }

      ResetPasswordResult result = personDao.resetPassword(email,
            PersonService.ResetPasswordRequest.getToken(body),
            password);

      if (result == FAILURE) {
         logger.info("person=[{}] failed to reset their password - general failure", person.getId()); // Audit event
         return error(CODE_PERSON_RESET_FAILED, PASSWORD_RESET_FAILED_MSG);
      }
      else if (result == TOKEN_FAILURE) {
         logger.info("person=[{}] failed to reset their password - wrong token", person.getId()); // Audit event
         return error(CODE_PERSON_RESET_TOKEN_FAILED, PASSWORD_RESET_TOKEN_FAILED_MSG);
      }

      logger.info("person=[{}] reset their password", person.getId()); // Audit event
      notify(person);
      FullHttpResponse response = login(email, password, ctx);

      Set<Cookie> cookies = ServerCookieDecoder.STRICT.decode(response.headers().get(HttpHeaders.Names.SET_COOKIE));
      String newSessId = cookies.stream()
         .filter(c -> Objects.equals(c.name(), cookieConfig.getAuthCookieName()))
         .findFirst()
         .map(Cookie::value)
         .orElse(null);

      // internal message to all client bridges to boot active sessions for this user now that there password
      // has changed
      platformBus.send(
         PlatformMessage.buildBroadcast(
            PersonCapability.PasswordChangedEvent.builder().withSession(newSessId).build(),
            Address.fromString(person.getAddress())
         )
         .create()
      );

      if(response.getStatus() == HttpResponseStatus.OK) {
         ClientMessage message = ClientMessage.builder()
               .withCorrelationId(clientMessage.getCorrelationId())
               .withSource(Address.platformService(PersonService.NAMESPACE).getRepresentation())
               .withPayload(PersonService.SendPasswordResetResponse.instance())
               .create();
         DefaultFullHttpResponse http = new DefaultFullHttpResponse(
               HttpVersion.HTTP_1_1,
               HttpResponseStatus.OK,
               Unpooled.copiedBuffer(JSON.toJson(message), CharsetUtil.UTF_8)
         );
         http.headers().set(response.headers());
         return http;
      }
      return response;
   }

   private void notify(Person person) {
      PlatformMessage msg = Notifications.builder()
            .withPersonId(person.getId())
            .withSource(Address.platformService(PlatformConstants.SERVICE_PEOPLE))
            .withPriority(NotificationCapability.NotifyRequest.PRIORITY_LOW)
            .withMsgKey(Notifications.PasswordChanged.KEY)
            .create();
      platformBus.send(msg);
   }

   private FullHttpResponse error(String code, String msg) {
      ErrorEvent err = ErrorEvent.fromCode(code, msg);
      ClientMessage clientMsg = ClientMessage.builder()
            .withPayload(err)
            .withSource(Address.platformService(PlatformConstants.SERVICE_PEOPLE).getRepresentation())
            .create();
      return new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            HttpResponseStatus.BAD_REQUEST,
            Unpooled.copiedBuffer(JSON.toJson(clientMsg), CharsetUtil.UTF_8));
   }

   // this is ridiculous...
   private FullHttpResponse login(String email, String password, ChannelHandlerContext ctx) throws Exception {
      FullHttpRequest fakeLogin = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/login");
      fakeLogin.headers().add(HttpHeaders.Names.CONTENT_TYPE, "application/x-www-form-urlencoded");

      String params = new StringBuilder("password=")
         .append(URLEncoder.encode(password, CharsetUtil.UTF_8.name()))
         .append("&")
         .append("user=")
         .append(URLEncoder.encode(email, CharsetUtil.UTF_8.name()))
         .toString();

      ByteBuf buffer = Unpooled.copiedBuffer(params, CharsetUtil.UTF_8);

      fakeLogin.headers().add(HttpHeaders.Names.CONTENT_LENGTH, buffer.readableBytes());
      fakeLogin.content().clear().writeBytes(buffer);
      return authenticator.authenticateRequest(ctx.channel(), fakeLogin);
   }


}


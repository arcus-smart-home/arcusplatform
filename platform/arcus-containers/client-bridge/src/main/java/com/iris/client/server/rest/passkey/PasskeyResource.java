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
package com.iris.client.server.rest.passkey;

import org.eclipse.jdt.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.client.Client;
import com.iris.bridge.server.client.ClientFactory;
import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.RequestAuthorizer;
import com.iris.bridge.server.http.impl.HttpResource;
import com.iris.bridge.server.netty.BridgeHeaders;
import com.iris.client.server.passkey.PasskeyRateLimiter;
import com.iris.client.server.passkey.PasskeyService;
import com.iris.client.server.passkey.PasskeyService.PendingRequest;
import com.iris.core.dao.PersonDAO;
import com.iris.messages.model.Person;
import com.iris.security.passkey.Passkey;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;

/**
 * Shared JSON handling for the passkey endpoints. Requests and responses are plain
 * JSON; the credential objects are what the browser's PublicKeyCredential.toJSON()
 * produces and what PublicKeyCredential.parse*OptionsFromJSON() accepts.
 */
abstract class PasskeyResource extends HttpResource {
   protected final PasskeyService passkeyService;
   @Nullable
   private final PasskeyRateLimiter rateLimiter;

   PasskeyResource(RequestAuthorizer authorizer, Class<?> type, BridgeMetrics metrics, PasskeyService passkeyService) {
      this(authorizer, type, metrics, passkeyService, null);
   }

   /** For endpoints that don't need a session: requests are limited per client address. */
   PasskeyResource(RequestAuthorizer authorizer, Class<?> type, BridgeMetrics metrics, PasskeyService passkeyService, @Nullable PasskeyRateLimiter rateLimiter) {
      super(authorizer, new HttpSender(type, metrics));
      this.passkeyService = passkeyService;
      this.rateLimiter = rateLimiter;
   }

   @Override
   public FullHttpResponse respond(FullHttpRequest request, ChannelHandlerContext ctx) throws Exception {
      if (!passkeyService.isEnabled()) {
         return error(HttpResponseStatus.NOT_FOUND, "Passkeys aren't enabled");
      }
      if (rateLimiter != null) {
         long waitNs = rateLimiter.tryAcquire(ctx.channel());
         if (waitNs > 0) {
            FullHttpResponse response = error(HttpResponseStatus.TOO_MANY_REQUESTS, "Too many passkey requests, try again later");
            response.headers().set(HttpHeaderNames.RETRY_AFTER, (long) Math.ceil(waitNs / 1e9));
            return response;
         }
      }
      JsonObject body;
      try {
         String content = request.content().toString(CharsetUtil.UTF_8);
         JsonElement parsed = content.trim().isEmpty() ? new JsonObject() : JsonParser.parseString(content);
         if (!parsed.isJsonObject()) {
            return error(HttpResponseStatus.BAD_REQUEST, "Expected a JSON object");
         }
         body = parsed.getAsJsonObject();
      }
      catch (JsonParseException e) {
         return error(HttpResponseStatus.BAD_REQUEST, "Expected a JSON object");
      }
      try {
         return respond(body, ctx);
      }
      catch (PasskeyService.TooManyPasskeysException e) {
         return error(HttpResponseStatus.CONFLICT, e.getMessage());
      }
      catch (PasskeyService.PasskeyException e) {
         return error(HttpResponseStatus.BAD_REQUEST, e.getMessage());
      }
   }

   protected abstract FullHttpResponse respond(JsonObject body, ChannelHandlerContext ctx) throws Exception;

   /** The logged-in person, for endpoints that require a session. */
   @Nullable
   protected static Person currentPerson(ClientFactory clientFactory, PersonDAO personDao, ChannelHandlerContext ctx) {
      Client client = clientFactory.get(ctx.channel());
      if (client == null || !client.isAuthenticated() || client.getPrincipalId() == null) {
         return null;
      }
      return personDao.findById(client.getPrincipalId());
   }

   @Nullable
   protected static String getString(JsonObject body, String name) {
      JsonElement value = body.get(name);
      return value == null || !value.isJsonPrimitive() ? null : value.getAsString();
   }

   /**
    * The credential object, re-serialized for the WebAuthn library. The library requires
    * clientExtensionResults, but some password managers leave it out of the credential
    * they return. These are unsigned client outputs that nothing here reads, so a missing
    * value is the same as a client reporting none.
    */
   @Nullable
   protected static String getCredential(JsonObject body) {
      JsonElement value = body.get("credential");
      if (value == null || !value.isJsonObject()) {
         return null;
      }
      JsonObject credential = value.getAsJsonObject();
      JsonElement extensionResults = credential.get("clientExtensionResults");
      if (extensionResults == null || extensionResults.isJsonNull()) {
         credential.add("clientExtensionResults", new JsonObject());
      }
      return credential.toString();
   }

   protected static FullHttpResponse options(PendingRequest pending) {
      JsonObject response = JsonParser.parseString(pending.getOptions()).getAsJsonObject();
      response.addProperty("requestId", pending.getRequestId());
      return json(HttpResponseStatus.OK, response);
   }

   protected static JsonObject toJson(Passkey passkey) {
      JsonObject json = new JsonObject();
      json.addProperty("id", passkey.getCredentialId());
      json.addProperty("name", passkey.getName());
      if (passkey.getCreated() != null) {
         json.addProperty("created", passkey.getCreated().toEpochMilli());
      }
      if (passkey.getLastUsed() != null) {
         json.addProperty("lastUsed", passkey.getLastUsed().toEpochMilli());
      }
      return json;
   }

   protected static FullHttpResponse error(HttpResponseStatus status, String message) {
      JsonObject json = new JsonObject();
      json.addProperty("error", message);
      return json(status, json);
   }

   protected static FullHttpResponse json(HttpResponseStatus status, JsonElement json) {
      FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer(json.toString(), CharsetUtil.UTF_8));
      response.headers().set(HttpHeaderNames.CONTENT_TYPE, BridgeHeaders.CONTENT_TYPE_JSON_UTF8);
      return response;
   }
}

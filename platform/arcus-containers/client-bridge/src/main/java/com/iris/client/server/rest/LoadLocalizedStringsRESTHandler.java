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

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.metrics.BridgeMetrics;
import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.annotation.HttpPost;
import com.iris.bridge.server.http.impl.HttpResource;
import com.iris.bridge.server.http.impl.auth.AlwaysAllow;
import com.iris.i18n.DBResourceBundleControl;
import com.iris.i18n.I18NBundle;
import com.iris.io.json.JSON;
import com.iris.messages.ClientMessage;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.errors.Errors;

@Singleton
@HttpPost("/i18n/LoadLocalizedStrings")
public class LoadLocalizedStringsRESTHandler extends HttpResource {

   @Inject
   public LoadLocalizedStringsRESTHandler(BridgeMetrics metrics, AlwaysAllow alwaysAllow) {
      super(alwaysAllow, new HttpSender(LoadLocalizedStringsRESTHandler.class, metrics));
   }

   @SuppressWarnings("unchecked")
   @Override
   public FullHttpResponse respond(FullHttpRequest request, ChannelHandlerContext ctx) {
      String json = request.content().toString(CharsetUtil.UTF_8);

      ClientMessage clientMessage = JSON.fromJson(json, ClientMessage.class);
      Map<String,Object> attributes = clientMessage.getPayload().getAttributes();

      Collection<String> bundleNames = (Collection<String>) attributes.get("bundleNames");
      String localeString = (String) attributes.get("locale");

      if(StringUtils.isBlank(localeString)) {
         localeString = "en-US";
      }

      if(bundleNames == null || bundleNames.isEmpty()) {
         bundleNames = new HashSet<>();
         for(I18NBundle bundle : I18NBundle.values()) { bundleNames.add(bundle.getBundleName()); }
      }

      MessageBody response = null;
      HttpResponseStatus responseCode = HttpResponseStatus.OK;

      try {
         Locale locale = Locale.forLanguageTag(localeString);
         Map<String,String> localizedStrings = loadBundles(locale, bundleNames);
         response = MessageBody.buildResponse(clientMessage.getPayload(), ImmutableMap.of("localizedStrings", localizedStrings));
      } catch(Exception e) {
         response = Errors.fromException(e);
         responseCode = HttpResponseStatus.INTERNAL_SERVER_ERROR;
      }

      ClientMessage message = ClientMessage.builder()
      		.withDestination(clientMessage.getDestination())
      		.withCorrelationId(clientMessage.getCorrelationId())
      		.withSource(Address.platformService("i18n").getRepresentation())
      		.withPayload(response).create();

      return new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1,
            responseCode,
            Unpooled.copiedBuffer(JSON.toJson(message), CharsetUtil.UTF_8));
   }

   private Map<String,String> loadBundles(Locale locale, Collection<String> bundleNames) {
      Map<String,String> localizedStrings = new HashMap<>();
      for(String bundleName : bundleNames) {
         localizedStrings.putAll(loadBundle(bundleName, locale));
      }
      return localizedStrings;
   }

   private Map<String,String> loadBundle(String bundleName, Locale locale) {
      ResourceBundle bundle = ResourceBundle.getBundle(bundleName, locale, new DBResourceBundleControl());
      Map<String,String> localizedStrings = new HashMap<>();
      Enumeration<String> keys = bundle.getKeys();
      while(keys.hasMoreElements()) {
         String key = keys.nextElement();
         localizedStrings.put(bundleName + ":" + key, bundle.getString(key));
      }
      return localizedStrings;
   }
}


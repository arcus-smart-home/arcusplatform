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
package com.iris.agent.logging;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.LayoutBase;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.iris.agent.hal.IrisHal;

public class IrisLayout extends LayoutBase<ILoggingEvent> {
   private List<String> STACK_TRACE_OPTIONS = Arrays.asList("full");
   private static final Gson GSON = new GsonBuilder()
      .disableHtmlEscaping()
      .create();

   private final JsonObject object;
   private final ThrowableProxyConverter converter;

   public IrisLayout() {
      this.converter = new ThrowableProxyConverter();
      this.converter.setOptionList(STACK_TRACE_OPTIONS);
      this.converter.start();

      this.object = new JsonObject();
   }

   @Override
   public String doLayout(@Nullable ILoggingEvent event) {
      if (event == null) {
         return "";
      }

      for(Map.Entry<String, String> e: event.getMDCPropertyMap().entrySet()) {
         object.addProperty(e.getKey(), e.getValue());
      }

      object.addProperty("ts", event.getTimeStamp());
      object.addProperty("lvl", event.getLevel().toString());
      object.addProperty("thd", event.getThreadName());
      object.addProperty("log", event.getLoggerName());
      object.addProperty("msg", event.getFormattedMessage());
      object.addProperty("svc", "agent");

      String hubId = IrisHal.getHubIdOrNull();
      if (hubId != null) {
         object.addProperty("hst", hubId);
      }

      String hubVersion = IrisHal.getOperatingSystemVersionOrNull();
      if (hubVersion != null) {
         object.addProperty("svr", hubVersion);
      }

      IThrowableProxy thrw = event.getThrowableProxy();
      if (thrw != null) {
         String stackTrace = converter.convert(event);
         object.addProperty("exc", stackTrace);
      } else {
         object.remove("exc");
      }

      return IrisAgentSotConverter.SOT + GSON.toJson(object);
   }
}


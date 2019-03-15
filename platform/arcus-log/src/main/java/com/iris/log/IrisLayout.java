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
package com.iris.log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;

import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.iris.info.IrisApplicationInfo;

public class IrisLayout extends PatternLayout {
   private List<String> STACK_TRACE_OPTIONS = Arrays.asList("full");
   private static final Gson GSON = new GsonBuilder()
      .disableHtmlEscaping()
      .create();

   private final ThrowableProxyConverter converter;
   private boolean isJsonString;
   private boolean isJson;

   public IrisLayout() {
      super.setPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");

      this.converter = new ThrowableProxyConverter();
      this.converter.setOptionList(STACK_TRACE_OPTIONS);
      this.converter.start();

      setType(System.getenv("IRIS_LOGTYPE"));
   }

   public void setType(String type) {
      boolean isj;
      boolean isjs;
      if ("json".equalsIgnoreCase(type)) {
         isj = true;
         isjs = false;
      } else if ("jsonstr".equalsIgnoreCase(type)) {
         isj = true;
         isjs = true;
      } else {
         isj = false;
         isjs = false;
      }

      this.isJson = isj;
      this.isJsonString = isjs;
   }

   @Override
   public String doLayout(ILoggingEvent event) {
      if (!isJson) {
         return super.doLayout(event);
      }

      JsonObject object = new JsonObject();
      for(Map.Entry<String, String> e: event.getMDCPropertyMap().entrySet()) {
         object.addProperty(e.getKey(), e.getValue());
      }

      object.addProperty("ts", event.getTimeStamp());
      object.addProperty("sev", event.getLevel().toString());
      object.addProperty("host", IrisApplicationInfo.getHostName());
      if (IrisApplicationInfo.getContainerName() != null) {
         object.addProperty("ctn", IrisApplicationInfo.getContainerName());
      }
      object.addProperty("svc", IrisApplicationInfo.getApplicationName());
      object.addProperty("svr", IrisApplicationInfo.getApplicationVersion());
      object.addProperty("thd", event.getThreadName());
      object.addProperty("log", event.getLoggerName());
      object.addProperty("msg", event.getFormattedMessage());

      IThrowableProxy thrw = event.getThrowableProxy();
      if (thrw != null) {
         String stackTrace = converter.convert(event);
         object.addProperty("exc", stackTrace);
      } else {
         object.remove("exc");
      }

      String json = GSON.toJson(object);
      if (isJsonString) {
         json = GSON.toJson(json);
      }

      return json + "\n";
   }

}


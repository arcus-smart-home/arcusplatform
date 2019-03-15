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
package com.iris.oauth.app;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.oauth.OAuthConfig;
import com.iris.resource.Resource;
import com.iris.resource.Resources;

@Singleton
public class AppRegistry {

   private static final Logger logger = LoggerFactory.getLogger(AppRegistry.class);

   private final Gson gson = new Gson();
   private Map<String,Application> apps;

   @Inject
   public AppRegistry(OAuthConfig config) {
      Resource res = Resources.getResource(config.getAppsPath());
      try(Reader r = new InputStreamReader(res.open())) {
         List<Application> apps = gson.fromJson(r, new TypeToken<List<Application>>() {}.getType());
         this.apps = Collections.unmodifiableMap(
               apps.stream()
                  .collect(Collectors.toMap(Application::getId, Function.identity())));
      } catch(IOException ioe) {
         logger.warn("failed to read application configuration from {}", config.getAppsPath(), ioe);
         apps = Collections.emptyMap();
      }
   }

   public Application getApplication(String id) {
      return apps.get(id);
   }

   public Iterable<Application> getApplications() {
      return apps.values();
   }
}


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
package com.iris.alexa.server;

import java.util.Arrays;
import java.util.Collection;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.SecurityManager;

import com.google.inject.Inject;
import com.google.inject.Module;
import com.iris.bootstrap.ServiceLocator;
import com.iris.bridge.server.BridgeServer;
import com.iris.bridge.server.ServerRunner;
import com.iris.core.IrisAbstractApplication;

public class AlexaServer extends BridgeServer {

   @Inject
   public AlexaServer(ServerRunner runner) {
      super(runner);

   }

   @Override
   protected void start() throws Exception {
      SecurityUtils.setSecurityManager(ServiceLocator.getInstance(SecurityManager.class));
      super.start();
   }

   public static void main(String[] args) {
      Collection<Class<? extends Module>> modules = Arrays.asList(AlexaServerModule.class);
      IrisAbstractApplication.exec(AlexaServer.class, modules, args);
   }

}


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
/**
 * 
 */
package com.iris.hubcom.server.session.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.hubcom.server.session.HubSession;

/**
 * 
 */
public class HubSessionAdapter implements HubSessionListener {
   private static final Logger logger = LoggerFactory.getLogger(HubSession.class);

   @Override
   public void onConnected(HubSession session) {
      logger.debug("Hub connected [{}] -- awaiting authorization", session.getHubId());
   }

   @Override
   public void onRegisterRequested(HubSession session) {
      logger.debug("Registration requested [{}]", session.getHubId());
   }

   @Override
   public void onRegistered(HubSession session) {
      logger.debug("Hub registered [{}]", session.getHubId());
   }

   @Override
   public void onAuthorized(HubSession session) {
      logger.debug("Hub authorized [{}]", session.getHubId());
   }

   @Override
   public void onDisconnected(HubSession session) {
      logger.debug("Hub disconnected [{}]", session.getHubId());
   }

}


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
package com.iris.platform.history.service;

import java.util.List;

import org.slf4j.LoggerFactory;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.messages.PlatformMessage;
import com.iris.platform.history.appender.HistoryAppender;
import com.iris.platform.history.appender.HistoryAppenders;
import com.iris.platform.history.appender.ObjectNameCache;

/**
 * 
 */
@Singleton
public class HistoryAppenderService {
   private static final Logger logger = LoggerFactory.getLogger(HistoryAppenderService.class);
   
   private final HistoryAppender appender;
   private final ObjectNameCache cache;

   @Inject
   public HistoryAppenderService(
         List<HistoryAppender> appenders,
         ObjectNameCache cache
   ) {
      this.appender = HistoryAppenders.dispatcher(appenders);
      this.cache = cache;
   }
   
   public void dispatch(PlatformMessage message) {
      try {
         cache.update(message);
      }
      catch(Exception e) {
         logger.debug("Error updating cache from message {}", message, e);
      }
      this.appender.append(message);
   }
}


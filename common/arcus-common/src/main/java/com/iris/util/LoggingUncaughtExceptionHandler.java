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
package com.iris.util;

import java.lang.Thread.UncaughtExceptionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingUncaughtExceptionHandler implements UncaughtExceptionHandler {
   private static final Logger DFLT_LOGGER = LoggerFactory.getLogger(LoggingUncaughtExceptionHandler.class);

   private final Logger logger;
   
   public LoggingUncaughtExceptionHandler() {
      this(DFLT_LOGGER);
   }

   public LoggingUncaughtExceptionHandler(Logger logger) {
      this.logger = logger;
   }

   @Override
   public void uncaughtException(Thread t, Throwable e) {
      logger.warn("Uncaught exception on thread [{}]", t.getName(), e);
   }

}


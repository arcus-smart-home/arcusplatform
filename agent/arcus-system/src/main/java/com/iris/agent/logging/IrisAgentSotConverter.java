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

import org.eclipse.jdt.annotation.Nullable;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;


public class IrisAgentSotConverter extends ClassicConverter {
   public static final char SOT_CHAR = '\u0002';
   public static final String SOT = "\u0002";
   public static final int SOT_LENGTH = SOT.length();

   @Override
   public String convert(@Nullable ILoggingEvent event) {
      return SOT;
   }
}


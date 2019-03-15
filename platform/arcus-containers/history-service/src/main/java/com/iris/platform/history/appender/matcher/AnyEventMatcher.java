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
package com.iris.platform.history.appender.matcher;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.messages.MessageBody;

public class AnyEventMatcher implements Matcher {

   private static final Logger logger = LoggerFactory.getLogger(AnyEventMatcher.class);

   private final Set<String> eventNames = new HashSet<>();

   public AnyEventMatcher(String... eventNames) {
      if(eventNames != null) {
         this.eventNames.addAll(Arrays.asList(eventNames));
      }
   }

   @Override
   public MatchResults matches(MessageBody value) {
      if(eventNames.isEmpty() || eventNames.contains(value.getMessageType())) {
         return new MatchResults(true, value);
      }
      // FIXME why would you return false with a value?
      return new MatchResults(false, value);
   }
}


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
package com.iris.platform.history.appender;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.iris.messages.PlatformMessage;

/**
 * 
 */
public class HistoryAppenders {
   private static final Logger logger = LoggerFactory.getLogger(HistoryAppenders.class);

   /**
    * 
    */
   public HistoryAppenders() {
      // TODO Auto-generated constructor stub
   }

   public static HistoryAppender dispatcher(Iterable<HistoryAppender> appenders) {
      final List<HistoryAppender> delegate = ImmutableList.copyOf(appenders);
      return new HistoryAppender() {
         /* (non-Javadoc)
          * @see com.iris.platform.history.appender.HistoryAppender#append(com.iris.messages.PlatformMessage)
          */
         @Override
         public boolean append(PlatformMessage message) {
            for(HistoryAppender appender: delegate) {
               try {
                  if(appender.append(message)) {
                     return true;
                  }
               }
               catch(Exception e) {
                  logger.warn("Error applying message to appender {}", appender, e);
               }
            }
            return false;
         }
         
         @Override
         public String toString() {
            return "HistoryAppenderDispatcher " + appenders;
         }
      };
   }

   public static HistoryAppender marshaller(Iterable<HistoryAppender> appenders) {
      final List<HistoryAppender> delegate = ImmutableList.copyOf(appenders);
      return new HistoryAppender() {
         /* (non-Javadoc)
          * @see com.iris.platform.history.appender.HistoryAppender#append(com.iris.messages.PlatformMessage)
          */
         @Override
         public boolean append(PlatformMessage message) {
            boolean rval = false;
            for(HistoryAppender appender: delegate) {
               try {
                  rval |= appender.append(message);
               }
               catch(Exception e) {
                  logger.warn("Error applying message to appender {}", appender, e);
               }
            }
            return rval;
         }
         
         @Override
         public String toString() {
            return "HistoryAppenderMarshaller " + appenders;
         }
      };
   }

}


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
package com.iris.driver.groovy.context;

import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.iris.capability.key.NamedKey;
import com.iris.messages.MessageBody;
import com.iris.util.IrisAttributeLookup;

public class RequestHandlerDefinition extends CapabilityHandlerDefinition<RequestHandlerDefinition.MatchRequest> {
   private static final Logger log = LoggerFactory.getLogger(RequestHandlerDefinition.class);

   private @Nullable MessageBody response;
   private boolean forwarded;

   public void addMatch(GroovyCapabilityDefinition cap, GroovyCommandDefinition cmd) {
      addMatch(new MatchRequestAny(cmd));
   }

   public void addMatch(GroovyCapabilityDefinition cap, GroovyCommandDefinition cmd, Map<String,Object> args) {
      addMatch(new MatchRequestExact(cmd,args));
   }

   public MessageBody getResponse() {
      return response;
   }

   public void setResponse(MessageBody response) {
      this.response = response;
   }

   public boolean isForwarded() {
      return forwarded;
   }

   public void setForwarded(boolean forwarded) {
      this.forwarded = forwarded;
   }

   @Override
   public String toString() {
      StringBuilder bld = new StringBuilder();
      bld.append("RequestHandlerDefinition [");

      super.toString(bld);

      bld.append(",rsp=").append(response);
      bld.append(",forwarded=").append(forwarded);
      bld.append("]");
      return bld.toString();
   }

   public static interface MatchRequest extends CapabilityHandlerDefinition.Match {
      NamedKey getCommandKey();
      boolean matches(MessageBody msg);
   }

   public static final class MatchRequestAny implements MatchRequest {
      private final GroovyCommandDefinition cmd;

      public MatchRequestAny(GroovyCommandDefinition cmd) {
         this.cmd = cmd;
      }

      @Override
      public NamedKey getCommandKey() {
         return cmd.getKey();
      }

      @Override
      public boolean matches(MessageBody msg) {
         return true;
      }

      @Override
      public String toString() {
         StringBuilder bld = new StringBuilder();
         bld.append("MatchRequestAny [");

         toString(bld);

         bld.append("]");
         return bld.toString();
      }

      @Override
      public void toString(StringBuilder bld) {
         bld.append(getCommandKey())
            .append("==<any>");
      }
   }

   public static final class MatchRequestExact implements MatchRequest {
      private final GroovyCommandDefinition cmd;
      private final Map<String,Object> args;

      public MatchRequestExact(GroovyCommandDefinition cmd, Map<String,Object> args) {
         this.cmd = cmd;

         ImmutableMap.Builder<String,Object> bld = ImmutableMap.builder();
         for (Map.Entry<String,Object> entry : args.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();

            try {
               bld.put(name, IrisAttributeLookup.coerce(name,value)); 
            } catch (Exception ex) {
               log.warn("could not coerce match value to correct type:", ex);
               bld.put(name, value);
            }
         }

         this.args = bld.build();
      }

      @Override
      public NamedKey getCommandKey() {
         return cmd.getKey();
      }

      @Override
      public boolean matches(MessageBody msg) {
         Map<String,Object> sent = msg.getAttributes();
         for (Map.Entry<String,Object> entry : args.entrySet()) {
            if (!sent.containsKey(entry.getKey())) {
               return false;
            }

            Object expected = entry.getValue();
            Object recevied = sent.get(entry.getKey());

            try {
               recevied = IrisAttributeLookup.coerce(entry.getKey(), recevied);
            } catch (Exception ex) {
               log.warn("could not coerce received value to correct type:", ex);
            }

            if (!Objects.equals(expected,recevied)) {
               return false;
            }
         }

         return true;
      }

      @Override
      public String toString() {
         StringBuilder bld = new StringBuilder();
         bld.append("MatchRequestExact [");

         toString(bld);

         bld.append("]");
         return bld.toString();
      }

      @Override
      public void toString(StringBuilder bld) {
         bld.append(getCommandKey())
            .append("==")
            .append(args);
      }
   }
}


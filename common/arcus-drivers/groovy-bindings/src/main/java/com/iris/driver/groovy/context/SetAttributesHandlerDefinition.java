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

import java.util.Objects;

import com.iris.driver.groovy.GroovyValidator;

public class SetAttributesHandlerDefinition extends CapabilityHandlerDefinition<SetAttributesHandlerDefinition.MatchAttr> {
   private boolean forwarded;

   public void addMatch(GroovyCapabilityDefinition cap, GroovyAttributeDefinition attr, Object... args) {
      if (args == null || args.length == 0) {
         addMatch(new MatchAttrAny(attr));
      } else if (args.length == 1) {
         addMatch(new MatchAttrValue(attr, args[0]));
      } else {
         GroovyValidator.error("set attributes handler should have either no value or exactly one value");
      }
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
      bld.append("SetAttributesHandlerDefinition [");

      super.toString(bld);

      bld.append(",forwarded=").append(forwarded);
      bld.append("]");
      return bld.toString();
   }

   public static interface MatchAttr extends CapabilityHandlerDefinition.Match {
      String getAttributeName();
      boolean matches(Object value);
   }

   public static final class MatchAttrAny implements MatchAttr {
      private final GroovyAttributeDefinition attr;

      public MatchAttrAny(GroovyAttributeDefinition attr) {
         this.attr = attr;
      }

      @Override
      public String getAttributeName() {
         return attr.getName();
      }

      @Override
      public boolean matches(Object value) {
         return true;
      }

      @Override
      public String toString() {
         StringBuilder bld = new StringBuilder();
         bld.append("MatchAttrAny [");

         toString(bld);

         bld.append("]");
         return bld.toString();
      }

      @Override
      public void toString(StringBuilder bld) {
         bld.append(getAttributeName())
            .append("==<any>");
      }
   }

   public static final class MatchAttrValue implements MatchAttr {
      private final GroovyAttributeDefinition attr;
      private final Object arg;

      public MatchAttrValue(GroovyAttributeDefinition attr, Object arg) {
         this.attr = attr;
         this.arg = arg;
      }

      @Override
      public String getAttributeName() {
         return attr.getName();
      }

      @Override
      public boolean matches(Object value) {
         return Objects.equals(arg,value);
      }

      @Override
      public String toString() {
         StringBuilder bld = new StringBuilder();
         bld.append("MatchAttrValue [");

         toString(bld);

         bld.append("]");
         return bld.toString();
      }

      @Override
      public void toString(StringBuilder bld) {
         bld.append(getAttributeName())
            .append("==")
            .append(arg);
      }
   }
}


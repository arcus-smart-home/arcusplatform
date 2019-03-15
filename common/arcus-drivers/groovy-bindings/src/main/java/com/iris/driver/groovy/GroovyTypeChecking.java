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
package com.iris.driver.groovy;

import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;

public final class GroovyTypeChecking {
   public static void invokeLifecycleClosure(Closure<?> closure) {
      closure.call();
   }

   public static void invokeProtocolMessageHandler(Closure<?> closure) {
      closure.setDelegate(new ProtocolMessageContext());
      closure.call();
   }

   private static final class ProtocolMessageContext extends GroovyObjectSupport {
      @Override
      public Object getProperty(String name) {
         switch (name) {
         case "message":
            return new AnyContext();

         case "log":
            return new LogContext();

         default:
            return super.getProperty(name);
         }
      }
   }

   private static final class AnyContext extends GroovyObjectSupport {
      @Override
      public Object getProperty(String name) {
         return this;
      }

      @Override
      public void setProperty(String property, Object newValue) {
      }

      @Override
      public Object invokeMethod(String name, Object args) {
         return this;
      }
   }

   private static final class LogContext extends GroovyObjectSupport {
      public void trace(String msg, Object... args) {
      }
   }
}


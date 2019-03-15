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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.iris.validators.ValidationException;
import com.iris.validators.Validator;

public class GroovyValidator {
   private static final ThreadLocal<List<Env>> validators = new ThreadLocal<List<Env>>() {
      @Override
      protected List<Env> initialValue() {
         return new ArrayList<>(1);
      }
   };

   public static void clear() {
      validators.set(new ArrayList<Env>(1));
   }

   public static void error(String message) {
      getValidator().error(appendSource(message));
   }

   public static void error(String message, Throwable cause) {
      getValidator().error(appendSource(message,cause));
   }

   public static void addErrors(Validator other) {
      getValidator().addErrors(other);
   }

   public static void assertTrue(boolean value, String message) {
      if (!value) {
         error(message);
      }
   }

   public static void assertFalse(boolean value, String message) {
      assertTrue(!value, message);
   }

   public static void assertEmpty(String value, String message) {
      assertTrue(StringUtils.isEmpty(value), message);
   }

   public static void assertNotEmpty(String value, String message) {
      assertTrue(StringUtils.isNotEmpty(value), message);
   }

   public static void assertNotNull(Object value, String message) {
      assertTrue(value != null, message);
   }

   public static void assertNull(Object value, String message) {
      assertTrue(value == null, message);
   }

   public static void throwIfErrors() throws ValidationException {
      getValidator().throwIfErrors();
   }

   public static Validator getValidator() {
      return top().validator;
   }

   public static void addDriverClassname(String driverClassname) {
      top().driverClassnames.add(driverClassname);
   }

   private static Env top() {
      List<Env> stack = validators.get();
      if (stack.isEmpty()) {
         Env env = new Env();
         stack.add(env);
         return env;
      }

      return stack.get(stack.size() - 1);
   }

   public static AutoCloseable push() {
      List<Env> stack = validators.get();
      stack.add(new Env());
      return GroovyValidatorPopper.INSTANCE;
   }

   public static AutoCloseable pushCopy() {
      List<Env> stack = validators.get();
      stack.add(new Env(top()));
      return GroovyValidatorPopper.INSTANCE;
   }

   public static void pop() {
      List<Env> stack = validators.get();
      stack.remove(stack.size() - 1);
   }

   private static String appendSource(String message) {
      return appendSource(message, new Exception());
   }
   
   private static String appendSource(String message, Throwable e) {
      Set<String> driverClassnames = top().driverClassnames;
      if(!driverClassnames.isEmpty()) {
         for(StackTraceElement se: e.getStackTrace()) {
            for (String driverClassname : driverClassnames) {
               if(se.getClassName().startsWith(driverClassname)) {
                  return message + "\n\tIn " + se.getFileName() + " on line number " + se.getLineNumber();
               }
            }
         }
      }

      return message + "\n\tIn <Unknown source>";
   }

   private static enum GroovyValidatorPopper implements AutoCloseable {
      INSTANCE;

      @Override
      public void close() {
         GroovyValidator.pop();
      }
   }

   private static final class Env {
      private final Validator validator;
      private final Set<String> driverClassnames;

      public Env() {
         this.validator = new Validator("The driver failed validation for the following reasons:");
         this.driverClassnames = new HashSet<>();
      }

      public Env(Env clone) {
         this.validator = new Validator(clone.validator);
         this.driverClassnames = new HashSet<>(clone.driverClassnames);
      }
   }
}


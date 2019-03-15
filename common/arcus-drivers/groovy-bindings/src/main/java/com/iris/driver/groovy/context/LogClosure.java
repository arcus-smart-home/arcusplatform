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
package com.iris.driver.groovy.context;

import groovy.lang.Closure;

import org.slf4j.Logger;

import com.iris.driver.groovy.GroovyContextObject;

/**
 *
 */
public class LogClosure extends Closure<Object> {

   public LogClosure(Object owner) {
      super(owner);
   }
   
   private Logger getLogger() {
      return GroovyContextObject.getContext().getLogger();
   }

   public void doCall(String format, Object... arguments) {
      getLogger().debug(format, arguments);
   }

   public void trace(String format, Object... arguments) {
      getLogger().trace(format, arguments);
   }

   public void debug(String format, Object... arguments) {
      getLogger().debug(format, arguments);
   }

   public void info(String format, Object... arguments) {
      getLogger().info(format, arguments);
   }

   public void warn(String format, Object... arguments) {
      getLogger().warn(format, arguments);
   }

   public void error(String format, Object... arguments) {
      getLogger().error(format, arguments);
   }

}


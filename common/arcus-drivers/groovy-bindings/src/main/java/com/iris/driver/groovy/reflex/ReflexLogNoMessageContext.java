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
package com.iris.driver.groovy.reflex;

import org.slf4j.Logger;

import com.iris.driver.DeviceDriverContext;
import com.iris.driver.groovy.GroovyValidator;
import com.iris.driver.groovy.context.CapabilityHandlerDefinition;
import com.iris.driver.groovy.context.GroovyCapabilityDefinition;
import com.iris.driver.reflex.ReflexActionLog;

import groovy.lang.GroovyObjectSupport;

public abstract class ReflexLogNoMessageContext extends GroovyObjectSupport {
   private boolean logStatementAdded = false;

   public void addLogAction(ReflexActionLog log) {
      final GroovyCapabilityDefinition.CapabilityHandlerContext ctx = GroovyCapabilityDefinition.CapabilityHandlerContext.getContext(this);
      ctx.addAction(new LogAction(log));
   }

   private void doAddLogAction(ReflexActionLog log) {
      logStatementAdded = true;
      addLogAction(log);
   }

   protected void checkLogAction() {
      if (logStatementAdded) {
         GroovyValidator.error("a reflex block is only allowed to have one log statement");
      }
   }

   public void trace(String msg) {
      checkLogAction();
      doAddLogAction(new ReflexActionLog(ReflexActionLog.Level.TRACE, msg));
   }

   public void debug(String msg) {
      checkLogAction();
      doAddLogAction(new ReflexActionLog(ReflexActionLog.Level.TRACE, msg));
   }

   public void info(String msg) {
      checkLogAction();
      doAddLogAction(new ReflexActionLog(ReflexActionLog.Level.TRACE, msg));
   }

   public void warn(String msg) {
      checkLogAction();
      doAddLogAction(new ReflexActionLog(ReflexActionLog.Level.TRACE, msg));
   }

   public void error(String msg) {
      checkLogAction();
      doAddLogAction(new ReflexActionLog(ReflexActionLog.Level.ERROR, msg));
   }

   public static final class LogAction implements CapabilityHandlerDefinition.Action {
      private final ReflexActionLog lg;

      public LogAction(ReflexActionLog lg) {
         this.lg = lg;
      }

      @Override
      public void run(DeviceDriverContext context, Object value) {
         Logger log = context.getLogger();

         switch (lg.getLevel()) {
         case TRACE:
            if (log.isTraceEnabled()) {
               log.trace(message());
            }
            break;

         case DEBUG:
            if (log.isDebugEnabled()) {
               log.debug(message());
            }
            break;

         case INFO:
            if (log.isInfoEnabled()) {
               log.info(message());
            }
            break;

         case WARN:
            if (log.isWarnEnabled()) {
               log.warn(message());
            }
            break;

         case ERROR:
            if (log.isErrorEnabled()) {
               log.error(message());
            }
            break;

         default:
            if (log.isDebugEnabled()) {
               log.debug(message());
            }
            break;
         }
      }

      private final String message() {
         return lg.getMsg();
      }
   }
}


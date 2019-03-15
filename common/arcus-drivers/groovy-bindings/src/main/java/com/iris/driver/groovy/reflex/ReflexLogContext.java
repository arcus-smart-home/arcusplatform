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

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.iris.driver.groovy.GroovyValidator;
import com.iris.driver.reflex.ReflexAction;
import com.iris.driver.reflex.ReflexActionLog;
import com.iris.driver.reflex.ReflexMatch;

public abstract class ReflexLogContext extends ReflexContext {
   private boolean logStatementAdded = false;

   public ReflexLogContext() {
      super();
   }

   public ReflexLogContext(List<ReflexMatch> matches, List<ReflexAction> actions) {
      super(matches,actions);
   }

   @Override
   public Object getProperty(String name) {
      if (name == null) {
         return super.getProperty(name);
      }

      switch (name) {
      case "msg":
      case "message":
         return LoggerArguments.MESSAGE;

      default:
         return super.getProperty(name);
      }
   }

   protected List<ReflexActionLog.Arg> toargs(LoggerArguments... args) {
      if (args == null || args.length == 0) {
         return ImmutableList.of();
      }

      ImmutableList.Builder<ReflexActionLog.Arg> result = ImmutableList.builder();
      for (LoggerArguments arg : args) {
         switch (arg) {
         case MESSAGE:
            result.add(ReflexActionLog.Arg.MESSAGE);
            break;

         default:
            GroovyValidator.error("unknown logger argument: " + arg);
            break;
         }
      }

      return result.build();
   }

   protected abstract void addLogAction(ReflexActionLog log);

   private void doAddLogAction(ReflexActionLog log) {
      logStatementAdded = true;
      addLogAction(log);
   }

   protected void checkLogAction() {
      if (logStatementAdded) {
         GroovyValidator.error("a reflex block is only allowed to have one log statement");
      }
   }

   public void trace(String msg, LoggerArguments... args) {
      checkLogAction();
      doAddLogAction(new ReflexActionLog(ReflexActionLog.Level.TRACE, msg, toargs(args)));
   }

   public void debug(String msg, LoggerArguments... args) {
      checkLogAction();
      doAddLogAction(new ReflexActionLog(ReflexActionLog.Level.DEBUG, msg, toargs(args)));
   }

   public void info(String msg, LoggerArguments... args) {
      checkLogAction();
      doAddLogAction(new ReflexActionLog(ReflexActionLog.Level.INFO, msg, toargs(args)));
   }

   public void warn(String msg, LoggerArguments... args) {
      checkLogAction();
      doAddLogAction(new ReflexActionLog(ReflexActionLog.Level.WARN, msg, toargs(args)));
   }

   public void error(String msg, LoggerArguments... args) {
      checkLogAction();
      doAddLogAction(new ReflexActionLog(ReflexActionLog.Level.ERROR, msg, toargs(args)));
   }

   public static enum LoggerArguments {
      MESSAGE
   }
}


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
package com.iris.common.rule.action.stateful;

import org.apache.commons.lang3.text.StrSubstitutor;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;

import com.iris.common.rule.action.ActionContext;
import com.iris.common.rule.action.Actions;
import com.iris.common.rule.event.RuleEvent;

/**
 * 
 */
// copied here to add isSatisfiable
public class LogAction extends BaseStatefulAction {
   public static final String VAR_MESSAGE = "message";
   public static final String NAME = "log";
   
   private final String message;
   
   public LogAction() {
      this(null);
   }
   
   public LogAction(@Nullable String message) {
      this.message = message;
   }
   
   @Override
   public String getName() {
      return NAME;
   }

   @Override
   public String getDescription() {
      return NAME + " " + Actions.valueOrVar(VAR_MESSAGE, message);
   }

   @Override
   public boolean isSatisfiable(ActionContext context) {
      return true;
   }

   @Override
   public ActionState execute(ActionContext context) {
      String message = this.message;
      if(message == null) {
         message = context.getVariable(VAR_MESSAGE, String.class);
      }
      
      Logger logger = context.logger();
      if(message == null) {
         logger.warn("Unable to retrieve log message from context [{}]", context);
      }
      else if(logger.isInfoEnabled()) {
         String logMessage = StrSubstitutor.replace(message, context.getVariables());
         logger.info(logMessage);
      }
      return ActionState.IDLE;
   }
   
   @Override
   public ActionState keepFiring(ActionContext context, RuleEvent event, boolean conditionMatches) {
      return execute(context);
   }

   @Override
   public String toString() {
      return getDescription();
   }

}


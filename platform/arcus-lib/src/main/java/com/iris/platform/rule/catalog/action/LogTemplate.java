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
package com.iris.platform.rule.catalog.action;

import java.util.Map;

import com.google.common.base.Preconditions;
import com.iris.platform.rule.catalog.ActionTemplate;
import com.iris.platform.rule.catalog.action.config.LogActionConfig;
import com.iris.platform.rule.catalog.template.TemplatedValue;

/**
 * 
 */
public class LogTemplate implements ActionTemplate {

   private TemplatedValue<String> message;

   /**
    * @return the message
    */
   public TemplatedValue<String> getMessage() {
      return message;
   }

   /**
    * @param message the message to set
    */
   public void setMessage(TemplatedValue<String> message) {
      this.message = message;
   }

   @Override
   public LogActionConfig generateActionConfig(Map<String, Object> variables) {
      Preconditions.checkState(message != null, "messsage may not be null");
      LogActionConfig config = new LogActionConfig(message.apply(variables));
      return config;
   }
}


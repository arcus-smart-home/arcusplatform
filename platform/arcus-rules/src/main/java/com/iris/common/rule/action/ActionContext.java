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
package com.iris.common.rule.action;

import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;

import com.iris.common.rule.Context;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;

/**
 * 
 */
public interface ActionContext extends Context {

   /**
    * A read-only view of the variables in this
    * context.
    * @return
    */
   Map<String, Object> getVariables();
   
   @Nullable Object getVariable(String name);
   
   @Nullable <T> T getVariable(String name, Class<T> type);
   
   @Nullable Object setVariable(String name, @Nullable Object value);
   
   void broadcast(MessageBody message);
   
   void send(Address address, MessageBody message);
   
   String request(Address address, MessageBody message);
   
   /**
    * Returns a new child of this context where any variables that
    * are set do not affect this context, but all other calls
    * go to the parent context (this).
    * @param variables
    * @return
    */
   ActionContext override(Map<String, Object> variables);
   ActionContext override(String namespace);

   // TODO isExecuting()
   // TODO getLastExecutionTime()
}


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
package com.iris.common.rule.trigger.stateful;

import java.io.Serializable;

import com.iris.common.rule.RuleContext;
import com.iris.common.rule.event.RuleEvent;
import com.iris.common.rule.event.RuleEventType;

/**
 * 
 */
public interface State extends Serializable {

   boolean transitionsOnEventOfType(RuleEventType type);
   
   boolean isFiring(RuleContext context);
   
   void onEnter(RuleContext context);

   void onRestore(RuleContext context);
   
   State transition(RuleContext context, RuleEvent event);
   
   void onExit(RuleContext context);
   
   
   String name();
}


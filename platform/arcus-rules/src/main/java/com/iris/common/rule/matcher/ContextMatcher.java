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
package com.iris.common.rule.matcher;

import java.io.Serializable;
import java.util.Set;

import com.iris.common.rule.Context;
import com.iris.common.rule.event.RuleEventType;

/**
 * 
 */
public interface ContextMatcher extends Serializable {

   Set<RuleEventType> reevaluteOnEventsOfType();
   
   boolean isSatisfiable(Context context);
   
   boolean matches(Context context);
}


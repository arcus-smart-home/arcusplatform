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
package com.iris.common.subsystem.care.behavior;

import org.apache.commons.lang3.StringUtils;

public class SubsystemVariableKey {
   
   private final String prefix;
   
   public SubsystemVariableKey(String prefix){
      this.prefix=prefix;
   }
   
   public String create(String id){
      return prefix+id;
   }

   public String create(String... arguments){
      return prefix + StringUtils.join(arguments,"-");
   }
   
}


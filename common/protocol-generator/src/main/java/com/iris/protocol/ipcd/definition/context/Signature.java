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
package com.iris.protocol.ipcd.definition.context;

import java.util.ArrayList;
import java.util.List;

public class Signature {
   private final String name;
   private final List<Property> args = new ArrayList<>();
   
   public Signature(String name) {
      this.name = name;
   }
   
   public String getName() {
      return name;
   }
   
   public List<Property> getArgs() {
      return args;
   }
   
   public void add(Property prop) {
      args.add(prop);
   }
   
   public String getRepresentation() {
      StringBuffer sb = new StringBuffer(name);
      sb.append("(");
      boolean first = true;
      for (Property arg : args) {
         if (!first) {
            sb.append(", ");
         }
         else {
            first = false;
         }
         sb.append(arg.getType());
      }
      sb.append(")");
      return sb.toString();
   }

   @Override
   public int hashCode() {
      return getRepresentation().hashCode();
   }

   @Override
   public boolean equals(Object obj) {
      if (obj instanceof Signature) {
         return getRepresentation().equals(((Signature)obj).getRepresentation());
      }
      else {
         return false;
      }
   }
}


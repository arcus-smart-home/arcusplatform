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
package com.iris.protoc.ast;

import java.util.LinkedHashMap;
import java.util.Map;

public class StructNode extends AbstractNode {
   private final Map<String,FieldNode> fields;
   private final Map<String,ValueNode> constants;

   public StructNode() {
      this.fields = new LinkedHashMap<String,FieldNode>();
      this.constants = new LinkedHashMap<String,ValueNode>();
   }

   public void addConst(String name, ValueNode node) {
      if (fields.containsKey(name) || constants.containsKey(name)) {
         throw new IllegalStateException("duplication field name: " + name);
      }

      constants.put(name, node);
   }

   public boolean addField(Aliasing resolver, String name, FieldNode node) {
      if (fields.containsKey(name) || constants.containsKey(name)) {
         throw new IllegalStateException("duplication field name: " + name);
      }

      boolean result = false;
      String sizer = node.getArraySizerName(resolver);
      if (sizer != null) {
         FieldNode size = fields.get(sizer);
         if (size != null) {
            node.markWithArraySizer(size);
            size.markAsArraySizer(node);
            result = true;
         }
      }

      fields.put(name, node);
      return result;
   }

   public Map<String, ValueNode> getConstants() {
      return constants;
   }

   public Map<String, FieldNode> getFields() {
      return fields;
   }

   @Override
   public String toString() {
      return "struct: " + str();
   }

   protected String str() {
      return "fields=" + fields;
   }
}


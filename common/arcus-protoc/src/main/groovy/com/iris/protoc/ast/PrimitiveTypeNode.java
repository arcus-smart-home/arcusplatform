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

public class PrimitiveTypeNode extends TypeNode {
   public enum PRIM {
      U8, U16, U32, U64,
      I8, I16, I32, I64,
      F32, F64,
   };

   private final PRIM primitive;

   public PrimitiveTypeNode(PRIM primitive) {
      this.primitive = primitive;
   }

   @Override
   public boolean isPrimitive() {
      return true;
   }

   public PRIM getPrimitiveType() {
      return primitive;
   }

   @Override
   public String toString() {
      return primitive.name().toLowerCase();
   }
}


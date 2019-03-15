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

public class SizedArrayTypeNode extends ArrayTypeNode {
   private final ValueNode length;

   public SizedArrayTypeNode(TypeNode valueType, ValueNode length) {
      super(valueType);
      this.length = length;
   }

   public ValueNode getLengthValue() {
      return length;
   }

   @Override
   public String getLength(Aliasing resolver) {
      String len = length.getValue();
      PrimitiveTypeNode ptn = (PrimitiveTypeNode)length.getType(resolver);
      switch (ptn.getPrimitiveType()) {
      case U8:  return "(" + len + " & 0xFF)";
      case U16: return "(" + len + " & 0xFFFF)";
      case U32: return "(" + len + ")";
      case U64: return "((int)" + len + ")";

      case I8:  return "((" + len + " <= 0) ? 0 : " + len + " & 0xFF)";
      case I16: return "((" + len + " <= 0) ? 0 : " + len + " & 0xFFFF)";
      case I32: return "((" + len + " <= 0) ? 0 : " + len + ")";
      case I64: return "((" + len + " <= 0) ? 0 : " + "(int)" + len + ")";

      case F32: throw new IllegalStateException("cannot use float values to size array");
      case F64: throw new IllegalStateException("cannot use float values to size array");
      }

      return len;
   }

   @Override
   public String toString() {
      return valueType.toString() + "[" + length + "]";
   }
}


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

public abstract class TypeNode extends AbstractNode {
   public abstract boolean isPrimitive();

   public static TypeNode fromPrimitive(String primitive) {
      switch (primitive) {
      case "u8":  return new PrimitiveTypeNode(PrimitiveTypeNode.PRIM.U8);
      case "u16": return new PrimitiveTypeNode(PrimitiveTypeNode.PRIM.U16);
      case "u32": return new PrimitiveTypeNode(PrimitiveTypeNode.PRIM.U32);
      case "u64": return new PrimitiveTypeNode(PrimitiveTypeNode.PRIM.U64);

      case "i8":  return new PrimitiveTypeNode(PrimitiveTypeNode.PRIM.I8);
      case "i16": return new PrimitiveTypeNode(PrimitiveTypeNode.PRIM.I16);
      case "i32": return new PrimitiveTypeNode(PrimitiveTypeNode.PRIM.I32);
      case "i64": return new PrimitiveTypeNode(PrimitiveTypeNode.PRIM.I64);

      case "f32": return new PrimitiveTypeNode(PrimitiveTypeNode.PRIM.F32);
      case "f64": return new PrimitiveTypeNode(PrimitiveTypeNode.PRIM.F64);

      default: throw new IllegalStateException("unknown primitive type: " + primitive);
      }
   }

   public static TypeNode fromUser(String named) {
      return new UserTypeNode(named);
   }
}


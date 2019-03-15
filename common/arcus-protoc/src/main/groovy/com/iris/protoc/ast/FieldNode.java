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

public class FieldNode extends AbstractNode {
   private final String name;
   private final TypeNode type;
   private final String when;
   private final String encoding;
   private final String decoding;
   private final String random;
   private final String extraSize;
   private FieldNode arraySized;
   private FieldNode arraySizer;

   public FieldNode(String name, TypeNode type, String when, String encoding, String decoding, String random, String extraSize) {
      this.name = name;
      this.type = type;
      this.when = when;
      this.encoding = encoding;
      this.decoding = decoding;
      this.random = random;
      this.extraSize = extraSize;
      this.arraySized = null;
      this.arraySizer = null;
   }

   public void markAsArraySizer(FieldNode field) {
      this.arraySized = field;
   }

   public void markWithArraySizer(FieldNode field) {
      this.arraySizer = field;
   }

   public boolean isArraySizer() {
      return arraySized != null;
   }

   public boolean hasArraySizer() {
      return arraySizer != null;
   }

   public String getWhen() {
      return when;
   }

   public String getEncoding() {
      return encoding;
   }

   public String getDecoding() {
      return decoding;
   }

   public String getRandom() {
      return random;
   }

   public String getExtraSize() {
      return extraSize;
   }

   public FieldNode getArraySized() {
      return arraySized;
   }

   public FieldNode getArraySizer() {
      return arraySizer;
   }

   public String getArraySizerName(Aliasing resolver) {
      if (type instanceof SizedArrayTypeNode) {
         ValueNode value = ((SizedArrayTypeNode)type).getLengthValue();
         if (value instanceof FieldValueNode) {
            FieldValueNode fvn = (FieldValueNode)value;
            return fvn.getValue();
         }
      }

      return null;
   }

   @Override
   public String toString() {
      return "field: name=" + name + ", type=" + type;
   }

   public String getName() {
      return name;
   }

   public TypeNode getType(Aliasing resolver) {
      return resolver.resolve(type);
   }
}


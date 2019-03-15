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
package com.iris.regex;

public final class RegexSymbolEncoders {
   private RegexSymbolEncoders() {
   }

   public static RegexSymbolEncoder<Byte> byteSymbolEncoder() {
      return new RegexSymbolEncoder<Byte>() {
         @Override
         public byte[] encode(Byte value) {
            return new byte[] { value };
         }
      };
   }

   public static RegexSymbolEncoder<Character> asciiSymbolEncoder() {
      return new RegexSymbolEncoder<Character>() {
         @Override
         public byte[] encode(Character value) {
            return new byte[] { (byte)value.charValue() };
         }
      };
   }
}


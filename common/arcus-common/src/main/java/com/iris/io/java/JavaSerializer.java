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
package com.iris.io.java;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import com.iris.io.Serializer;

/**
 * 
 */
public class JavaSerializer<T> implements Serializer<T> {
   @SuppressWarnings("unchecked")
   public static <T> JavaSerializer<T> getInstance() {
      return (JavaSerializer<T>) Reference.INSTANCE;
   }
   
   private JavaSerializer() {
   }


   @Override
   public byte[] serialize(T value) throws IllegalArgumentException {
      try(ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
         serialize(value, baos);
         return baos.toByteArray();
      }
      catch (IOException e) {
         throw new RuntimeException("Got an IOException reading from a byte array", e);
      }
   }

   @Override
   public void serialize(T value, OutputStream out) throws IOException, IllegalArgumentException {
      ObjectOutputStream oos = new ObjectOutputStream(out);
      oos.writeObject(value);
      oos.flush();
   }

   private static final class Reference {
      static final JavaSerializer<?> INSTANCE = new JavaSerializer<>();
   }
}


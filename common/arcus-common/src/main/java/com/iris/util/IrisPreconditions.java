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
package com.iris.util;

import java.util.IllegalFormatException;

import org.eclipse.jdt.annotation.Nullable;

/**
 * Utility definitions for checking preconditions.
 */
public final class IrisPreconditions {
   public static void checkArgument(boolean expression) throws IllegalArgumentException {
      if (!expression) {
         throw new IllegalArgumentException("Assertion failed");
      }
   }

   public static void checkArgument(boolean expression, @Nullable Object message) throws IllegalArgumentException {
      if (!expression) {
         throw new IllegalArgumentException(message != null ? message.toString() : "Assertion failed");
      }
   }

   public static void checkArgument(boolean expression, @Nullable String message, @Nullable Object... args) throws IllegalArgumentException {
      if (!expression) {
         throw new IllegalArgumentException(format("Assertion failed", message,args));
      }
   }

   public static void checkState(boolean expression) throws IllegalStateException {
      if (!expression) {
         throw new IllegalStateException();
      }
   }

   public static void checkState(boolean expression, @Nullable Object message) throws IllegalStateException {
      if (!expression) {
         throw new IllegalStateException(String.valueOf(message));
      }
   }

   public static void checkState(boolean expression, @Nullable String message, @Nullable Object... args) throws IllegalStateException {
      if (!expression) {
         throw new IllegalStateException(format("Assertion failed",message,args));
      }
   }

   public static <T> T checkNotNull(@Nullable T reference) throws IllegalArgumentException {
      if (reference == null) {
         throw new NullPointerException();
      }

      return reference;
   }

   public static <T> T checkNotNull(T reference, @Nullable Object message) throws IllegalArgumentException {
      if (reference == null) {
         throw new NullPointerException(message != null ? message.toString() : "Argument may not be null");
      }

      return reference;
   }

   public static <T> T checkNotNull(T reference, @Nullable String message, @Nullable Object... args) throws IllegalArgumentException {
      if (reference == null) {
         throw new IllegalArgumentException(format("Argument may not be null", message,args));
      }

      return reference;
   }

   public static int checkElementIndex(int index, int size) throws IllegalArgumentException {
      if (index >= size) {
         throw new IllegalArgumentException("Index [" + index + "] greater than [" + size + "]");
      }

      return index;
   }

   public static int checkElementIndex(int index, int size, @Nullable Object message) throws IllegalArgumentException {
      if (index >= size) {
         throw new IllegalArgumentException(message != null ? message.toString() : "Index [" + index + "] greater than [" + size + "]");
      }

      return index;
   }

   public static int checkElementIndex(int index, int size, @Nullable String message, @Nullable Object... args) throws IllegalArgumentException {
      if (index >= size) {
         throw new IllegalArgumentException(format("Index [" + index + "] greater than [" + size + "]", message,args));
      }

      return index;
   }

   private static String format(String defaultMessage, @Nullable String message, @Nullable Object... args) {
      if (message == null) {
         return defaultMessage;
      }

      try {
         return String.format(message, args);
      } catch (IllegalFormatException ex) {
         return message;
      }
   }
}


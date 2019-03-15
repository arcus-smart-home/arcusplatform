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
package com.iris.protoc.runtime;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import io.netty.buffer.ByteBuf;

public interface ProtocSerDe<T> {

   Io<T> ioSerDe();
   Nio<T> nioSerDe();
   Netty<T> nettySerDe();
   T fromBytes(ByteOrder order, byte[] data);
   T fromBytes(ByteOrder order, byte[] data, int offset, int length);

   /////////////////////////////////////////////////////////////////////////////
   // Marshalling to/from Java IO streams
   /////////////////////////////////////////////////////////////////////////////

   public interface Io<T> {
      T decode(DataInput input) throws IOException;
      void encode(DataOutput dst, T value) throws IOException;
   }

   /////////////////////////////////////////////////////////////////////////////
   // Marshalling to/from Java NIO streams
   /////////////////////////////////////////////////////////////////////////////

   public interface Nio<T> {
      T decode(ByteBuffer input) throws IOException;
      void encode(ByteBuffer dst, T value) throws IOException;
   }

   public static final Nio<ByteBuffer> NioEmpty = new Nio<ByteBuffer>() {
      @Override
      public ByteBuffer decode(ByteBuffer input) throws IOException {
         return input;
      }

      @Override
      public void encode(ByteBuffer dst, ByteBuffer value) throws IOException {
         dst.put(value);
      }
   };

   /////////////////////////////////////////////////////////////////////////////
   // Marshalling to/from Netty streams
   /////////////////////////////////////////////////////////////////////////////

   public interface Netty<T> {
      T decode(ByteBuf input) throws IOException;
      void encode(ByteBuf dst, T value) throws IOException;
   }

   public static final Netty<ByteBuf> NettyEmpty = new Netty<ByteBuf>() {
      @Override
      public ByteBuf decode(ByteBuf input) throws IOException {
         return input;
      }

      @Override
      public void encode(ByteBuf dst, ByteBuf value) throws IOException {
         dst.writeBytes(value);
      }
   };
}


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
package com.iris.message;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import com.google.common.collect.ImmutableMap;
import com.iris.io.Serializer;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;

@State(Scope.Thread)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class PerfMessage {
   @Benchmark
   public PlatformMessage perfPlatformMessageSafe(TestSetup test) throws InterruptedException {
      String type = test.payload.getMessageType();
      Serializer<MessageBody> serializer = PlatformMessage.getSerializer(type);
      return PlatformMessage.builder()
         .from(test.src)
         .to(test.dst)
         .withPayload(type, serializer.serialize(test.payload))
         .create();
   }

   @Benchmark
   public PlatformMessage perfPlatformMessageUnsafe(TestSetup test) throws InterruptedException {
      String type = test.payload.getMessageType();
      Serializer<MessageBody> serializer = PlatformMessage.getSerializer(type);
      return PlatformMessage.builder()
         .from(test.src)
         .to(test.dst)
         .withPayload(type, serializer, test.payload)
         .create();
   }

   @State(Scope.Benchmark)
   public static class TestSetup {
      Address src = Address.deviceAddress("test", UUID.randomUUID());
      Address dst = Address.deviceAddress("test", UUID.randomUUID());
      Map<String,Object> attrs = ImmutableMap.<String,Object>of(
         "test1", Integer.valueOf(1),
         "test2", "this is a string",
         "test3", Long.valueOf(1L)
      );

      MessageBody payload = MessageBody.buildMessage("test", attrs);

      @Setup
      public void setup() {
      }
   }

}


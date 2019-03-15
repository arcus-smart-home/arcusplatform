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

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
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

import org.msgpack.jackson.dataformat.MessagePackFactory;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;

@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class PerfGsonSerDe {
   @Benchmark
   public byte[] perfSerToBytesGetBytes(TestSetup test) throws InterruptedException {
      return test.gson.toJson(test.msg).getBytes(StandardCharsets.UTF_8);
   }

   @Benchmark
   public byte[] perfSerToBytesJackson(TestSetup test) throws IOException, InterruptedException {
      return test.mapper.writeValueAsBytes(test.msg);
   }

   @Benchmark
   public byte[] perfSerToBytesJacksonAB(TestSetup test) throws IOException, InterruptedException {
      return test.abmapper.writeValueAsBytes(test.msg);
   }

   @Benchmark
   public byte[] perfSerToBytesMsgPack(TestSetup test) throws IOException, InterruptedException {
      return test.mpmapper.writeValueAsBytes(test.msg);
   }

   @State(Scope.Benchmark)
   public static class TestSetup {
      Gson gson;
      PlatformMessage msg;
      ObjectMapper mapper = new ObjectMapper();
      ObjectMapper abmapper = new ObjectMapper();
      ObjectMapper mpmapper = new ObjectMapper(new MessagePackFactory());

      @Setup
      public void setup() {
         gson = new GsonBuilder().create();

         abmapper.registerModule(new AfterburnerModule());
         mpmapper.registerModule(new AfterburnerModule());

         Map<String,Object> attrs = ImmutableMap.<String,Object>of(
            "test1", Integer.valueOf(1),
            "test2", "this is a string",
            "test3", Long.valueOf(1L)
         );

         MessageBody payload = MessageBody.buildMessage("test", attrs);
         Address src = Address.deviceAddress("test", UUID.randomUUID());
         Address dst = Address.deviceAddress("test", UUID.randomUUID());

         msg = PlatformMessage.buildRequest(payload, src, dst).create();
      }
   }
}


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
package com.iris.protocol.zwave.model;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.BeforeClass;
import org.junit.Test;

import com.iris.protocol.zwave.ZWaveProtocol;
import com.iris.protocol.zwave.message.ZWaveCommandMessage;

public class TestZWaveDeserializer {
   final int count = 1000;
   final ExecutorService executor = Executors.newFixedThreadPool(count);

   private static final byte[] RECV_BYTES1 = new byte[] {
      -1, 0, 0, -2, -2
   };

   private static final byte[] RECV_BYTES2 = new byte[] {
      -2, 1, 1, -1, -1
  };

   @BeforeClass
   public static void setup() {
      ZWaveAllCommandClasses.loadDefaultClasses();
   }

   @Test
   public void testIt() {
      final CountDownLatch latch = new CountDownLatch(count);
      List<Future<?>> results = new ArrayList<>(count);
      for(int i=0; i<count; i++) {
         //final int index = i;
         Future<?> result = executor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
               latch.countDown();
               latch.await();

               for (int i = 0; i < 10; ++i) {
                  final byte[] payload = (i % 2 == 0) ? createBytes1() : createBytes2();
                  ZWaveCommandMessage message = (ZWaveCommandMessage)ZWaveProtocol.INSTANCE.createDeserializer().deserialize(payload);

                  ZWaveCommand cmd = message.getCommand();
                  assertEquals("operation_report", cmd.commandName);
                  assertEquals(98, cmd.commandClass);
                  assertEquals(3, cmd.commandNumber);
                  if (i % 2 == 0) {
                     assertArrayEquals(RECV_BYTES1, cmd.recvBytes);
                  } else {
                     assertArrayEquals(RECV_BYTES2, cmd.recvBytes);
                  }
               }
               return null;
            }
         });
         results.add(result);
      }
      int succeeded = 0;
      for(Future<?> result: results) {
         try {
            result.get();
            succeeded++;
         }
         catch(ExecutionException e) {
            e.getCause().printStackTrace();
         }
         catch (InterruptedException e) {
            e.printStackTrace();
         }
      }
      assertEquals(count, succeeded);
   }

   private static byte[] createBytes1() {
      return new byte[] {
         0x01, 0x00, 0x00, 0x00, 0x0c, 0x03, 0x62, 0x03, 0x00, 0x00, 0x00, 0x05, (byte)0xff, 0x00, 0x00, (byte)0xfe, (byte)0xfe
      };
   }

   private static byte[] createBytes2() {
      return new byte[] {
         0x01, 0x00, 0x00, 0x00, 0x0c, 0x03, 0x62, 0x03, 0x00, 0x00, 0x00, 0x05, (byte)0xfe, 0x01, 0x01, (byte)0xff, (byte)0xff
      };
   }
}


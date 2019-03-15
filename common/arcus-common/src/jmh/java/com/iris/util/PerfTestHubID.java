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

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class PerfTestHubID {
   @Benchmark
   public long measureOrigMacToLong(TestSetup test) throws InterruptedException {
      return OrigHubID.toLong(test.mac);
   }

   @Benchmark
   public String measureOrigFromMacLong(TestSetup test) throws InterruptedException {
      return OrigHubID.fromMac(test.macLong);
   }

   @Benchmark
   public String measureOrigFromMacString(TestSetup test) throws InterruptedException {
      return OrigHubID.fromMac(test.mac);
   }

   @Benchmark
   public long measureNewMacToLong(TestSetup test) throws InterruptedException {
      return MACAddress.macToLong(test.mac);
   }

   @Benchmark
   public String measureNewFromMacLong(TestSetup test) throws InterruptedException {
      return HubID.fromMac(test.macLong);
   }

   @Benchmark
   public String measureNewFromMacString(TestSetup test) throws InterruptedException {
      return HubID.fromMac(test.mac);
   }

   public static void main(String[] args) throws Exception {
      Options opt = new OptionsBuilder()
         .include(PerfTestHubID.class.getSimpleName())
         .build();

      new Runner(opt).run();
   }

   @State(Scope.Benchmark)
   public static class TestSetup {
      String mac = "00:16:A2:05:DC:0C";
      long macLong = OrigHubID.toLong(mac);
   }

   public static class OrigHubID {
	   public static long toLong(String mac) {
		   long l = 0;
		   int b;
		   int i;

		   String macbytes[] = mac.split(":");
		   if ( macbytes.length < 6 ) return l;
		   for ( i = 0; i < 6; i++ ) {
			   l <<= 8;
			   b = Hex.fromString (macbytes[i]);
			   l |= b;
		   }
		   return l;
	   }

	   public static String fromMac(String mac) {
         long macl = toLong(mac);
         return fromMac(macl);
	   }
	   
	   public static String fromMac(long mac) {
		   String allowed_chars = "ABCDEFGHJKLNPQRSTUVWXYZ";
		   int num_chars = allowed_chars.length();
		   long remainder 	= mac;
		   long digits;
		   char alpha[] = new char[3];
		   String s;

	      /* Shift by 1 */
	      remainder >>= 1;

	      /* Get digits */
	      digits = remainder % 10000;
	      remainder = remainder / 10000;


	      /* Get each alpha digit */
	      alpha[2] = allowed_chars.charAt((int) (remainder % num_chars));
	      remainder = remainder / num_chars;
	      alpha[1] = allowed_chars.charAt((int) (remainder % num_chars));
	      remainder = remainder / num_chars;
	      alpha[0] = allowed_chars.charAt((int) (remainder % num_chars));
	      remainder = remainder / num_chars;


	      s = String.format("%c%c%c-%04d", alpha[0], alpha[1], alpha[2], digits);

		   return s;
	   }
   }
}


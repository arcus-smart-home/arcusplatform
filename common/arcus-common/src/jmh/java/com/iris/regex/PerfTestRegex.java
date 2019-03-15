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

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import com.google.common.base.Function;

@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 6, time = 10, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 6, time = 10, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class PerfTestRegex {
   @Benchmark
   public boolean javaMatch(TestSetup test) throws InterruptedException {
      return test.pat.matcher(test.test).matches();
   }

   @Benchmark
   public boolean dfaminMatch(TestSetup test) throws InterruptedException {
      return test.dfamin.matches(RegexUtil.iterator(test.test));
   }

   @Benchmark
   public boolean dfabyteMatch(TestSetup test) throws InterruptedException {
      return test.dfabyte.matches(test.bytetest);
   }

   public static void main(String[] args) throws Exception {
      Options opt = new OptionsBuilder()
         .include(PerfTestRegex.class.getSimpleName())
         .build();

      new Runner(opt).run();
   }

   @State(Scope.Benchmark)
   public static class TestSetup {
      //@Param({"1", "10", "100", "1000"})
      //int rate;
   
      String test;
      Iterable<Byte> asciitest;
      byte[] bytetest;
      RegexNfa<Character,Object> nfa;
      RegexDfa<Character,List<Object>> dfa;
      RegexDfa<Character,List<Object>> dfamin;
      RegexDfaByte<List<Object>> dfabyte;
      Pattern pat;

      @Setup
      public void setup() {
         //String regex = "(a?|a+)b*(a?|a+)|(b+|(ca)+)?|((((((((((((((((ba*b))))))))))))))))|cac";
         /*
         StringBuilder rbld = new StringBuilder();
         for (int i = 0; i < 1001; ++i) {
            switch (i % 5) {
            case 0:  rbld.append("a?"); break;
            case 1:  rbld.append("b?"); break;
            case 2:  rbld.append("c?"); break;
            case 3:  rbld.append("d?"); break;
            default: rbld.append("|"); break;
            }
         }
         */
         String regex = "ba*b|ab*a|(ca)+|abcde";
         //String regex = "(ca)*";

         StringBuilder bld = new StringBuilder();
         for (int i = 0; i < 5; ++i) {
            bld.append("ca");
         }

         test = bld.toString();
         asciitest = RegexUtil.asciiIterable(test);
         bytetest = test.getBytes(StandardCharsets.US_ASCII);

         nfa = Regex.parse(regex);
         dfa = RegexUtil.nfaConvertToDfa(nfa);
         dfamin = RegexUtil.dfaRemoveIndistinguishable(dfa);
         pat = Pattern.compile(regex);

         dfabyte = RegexUtil.dfaToByteRep(RegexUtil.dfaConvertSymbolSpace(dfamin, new Function<Character,Byte>() {
            @Override
            public Byte apply(Character ch) {
               return (byte)ch.charValue();
            }
         }));
      }
   }
}


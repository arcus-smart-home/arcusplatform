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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.junit.Test;

public class TestByteRegex extends RegexTestCase {

   @Test
   public void testSingleMatch() {
      for (int i = 0; i < 256; ++i) {
         RegexDfaByte<List<String>> regex = parse(Integer.toHexString(i));
         assertFalse(regex.matches(new byte[] { }));
         assertTrue(regex.matches(new byte[] { (byte)i }));
         assertFalse(regex.matches(new byte[] { (byte)(i+1) }));
         assertFalse(regex.matches(new byte[] { (byte)(i-1) }));
      }
   }

   @Test
   public void testZeroOrMoreMatch() {
      for (int i = 0; i < 256; ++i) {
         RegexDfaByte<List<String>> regex = parse(Integer.toHexString(i) + "*");
         assertTrue(regex.matches(new byte[] { }));
         assertTrue(regex.matches(new byte[] { (byte)i }));
         assertTrue(regex.matches(new byte[] { (byte)i, (byte)i }));
         assertTrue(regex.matches(new byte[] { (byte)i, (byte)i, (byte)i }));
         assertTrue(regex.matches(new byte[] { (byte)i, (byte)i, (byte)i, (byte)i }));
         assertFalse(regex.matches(new byte[] { (byte)(i+1) }));
         assertFalse(regex.matches(new byte[] { (byte)i, (byte)(i+1) }));
         assertFalse(regex.matches(new byte[] { (byte)(i-1) }));
         assertFalse(regex.matches(new byte[] { (byte)i, (byte)(i-1) }));
      }
   }

   @Test
   public void testOneOrMoreMatch() {
      for (int i = 0; i < 256; ++i) {
         RegexDfaByte<List<String>> regex = parse(Integer.toHexString(i) + "+");
         assertFalse(regex.matches(new byte[] { }));
         assertTrue(regex.matches(new byte[] { (byte)i }));
         assertTrue(regex.matches(new byte[] { (byte)i, (byte)i }));
         assertTrue(regex.matches(new byte[] { (byte)i, (byte)i, (byte)i }));
         assertTrue(regex.matches(new byte[] { (byte)i, (byte)i, (byte)i, (byte)i }));
         assertFalse(regex.matches(new byte[] { (byte)(i+1) }));
         assertFalse(regex.matches(new byte[] { (byte)i, (byte)(i+1) }));
         assertFalse(regex.matches(new byte[] { (byte)(i-1) }));
         assertFalse(regex.matches(new byte[] { (byte)i, (byte)(i-1) }));
      }
   }

   @Test
   public void testAlternatesMatch() {
      for (int i = 0; i < 256; ++i) {
         RegexDfaByte<List<String>> regex = parse(Integer.toHexString(i) + "|" + Integer.toHexString((i+1) % 256));
         assertFalse(regex.matches(new byte[] { }));
         assertTrue(regex.matches(new byte[] { (byte)i }));
         assertTrue(regex.matches(new byte[] { (byte)(i+1) }));
         assertFalse(regex.matches(new byte[] { (byte)(i-1) }));
      }
   }

   @Test
   public void testOptionalMatch() {
      for (int i = 0; i < 256; ++i) {
         RegexDfaByte<List<String>> regex = parse(Integer.toHexString(i) + "?");
         assertTrue(regex.matches(new byte[] { }));
         assertTrue(regex.matches(new byte[] { (byte)i }));
         assertFalse(regex.matches(new byte[] { (byte)(i+1) }));
         assertFalse(regex.matches(new byte[] { (byte)(i-1) }));
      }
   }

   @Test
   public void testWildcardMatch() {
      RegexDfaByte<List<String>> regex = parse(".");
      assertFalse(regex.matches(new byte[] { }));
      for (int i = 0; i < 256; ++i) {
         assertTrue(regex.matches(new byte[] { (byte)i }));
      }
   }

   @Test
   public void testParenMatch() {
      RegexDfaByte<List<String>> regex = parse("(..)+");
      assertFalse(regex.matches(new byte[] { }));
      for (int i = 0; i < 256; ++i) {
         assertFalse(regex.matches(new byte[] { (byte)i }));
         assertTrue(regex.matches(new byte[] { (byte)i, (byte)(i+1) }));
      }
   }

   private RegexDfaByte<List<String>> parse(String regex) {
      return parse(regex,null);
   }

   private RegexDfaByte<List<String>> parse(String regex, @Nullable String value) {
      return RegexUtil.dfaToByteRep(RegexUtil.dfaMinimize(RegexUtil.nfaConvertToDfa(Regex.parseByteRegex(regex,value))));
   }

   private RegexDfaByte<List<String>> parse(Map<String,String> regexs) {
      List<RegexNfa<Byte,String>> all = new ArrayList<>();
      for (Map.Entry<String,String> entry : regexs.entrySet()) {
         all.add(Regex.parseByteRegex(entry.getKey(),entry.getValue()));
      }

      RegexNfa<Byte,String> top = RegexNfa.<Byte,String>append(all);
      return RegexUtil.dfaToByteRep(RegexUtil.dfaMinimize(RegexUtil.nfaConvertToDfa(top)));
   }

   /*
   public static void main(String[] args) throws Exception {
      StringBuilder rbld = new StringBuilder();
      for (int i = 0; i < 51; ++i) {
         switch (i % 5) {
         case 0:  rbld.append("a?"); break;
         case 1:  rbld.append("b?"); break;
         case 2:  rbld.append("c?"); break;
         case 3:  rbld.append("d?"); break;
         default: rbld.append("|"); break;
         }
      }

      String regex1 = "(ca)+";
      String regex2 = "ba*b";
      String regex3 = "ab*a";
      String regex4 = "(a?|a+)b*(a?|a+)|(b+|(ca)+)?|((((((((((((((((ba*b))))))))))))))))|cac";
      String regex5 = rbld.toString();
      String regex6 = "a.*b";

      RegexNfa<Character,String> nfa1 = parse(regex1, "r1");
      RegexNfa<Character,String> nfa2 = parse(regex2, "r2");
      RegexNfa<Character,String> nfa3 = parse(regex3, "r3");
      RegexNfa<Character,String> nfa4 = parse(regex4, "r4");
      RegexNfa<Character,String> nfa5 = parse(regex5, "r5");
      RegexNfa<Character,String> nfa6 = parse(regex6, "r6");

      RegexNfa<Character,String> nfa = RegexNfa.append(nfa1, nfa2, nfa3, nfa4, nfa5, nfa6);
      try (BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream("/Users/peckw/test.dot"))) {
         os.write(nfa.toDotGraph().getBytes(StandardCharsets.UTF_8));
      }

      RegexDfa<Character,List<String>> dfa = RegexUtil.nfaConvertToDfa(nfa);
      try (BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream("/Users/peckw/testdfa.dot"))) {
         os.write(dfa.toDotGraph().getBytes(StandardCharsets.UTF_8));
      }

      RegexDfa<Character,List<String>> dfamin = RegexUtil.dfaMinimize(dfa);
      try (BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream("/Users/peckw/testdfamin.dot"))) {
         os.write(dfamin.toDotGraph().getBytes(StandardCharsets.UTF_8));
      }

      RegexDfaByte<List<String>> dfabyte = RegexUtil.dfaToByteRep(RegexUtil.dfaConvertSymbolSpace(dfamin, new Function<Character,Byte>() {
         @Override
         public Byte apply(Character ch) {
            return (byte)ch.charValue();
         }
      }));
      try (BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream("/Users/peckw/testdfabyte.dot"))) {
         os.write(dfabyte.toDotGraph().getBytes(StandardCharsets.UTF_8));
      }

      String bregex = "00 ((AA|55) 01)+ 00";
      RegexDfaByte<List<String>> dfabyte2 = RegexUtil.dfaToByteRep(RegexUtil.dfaMinimize(RegexUtil.nfaConvertToDfa(parseByteRegex(bregex,(String)null))));
      try (BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream("/Users/peckw/testdfabyte2.dot"))) {
         os.write(dfabyte2.toDotGraph().getBytes(StandardCharsets.UTF_8));
      }

      List<String> tests = new ArrayList<>();
      tests.add("");
      tests.add("a");
      tests.add("aa");
      tests.add("aba");

      tests.add("ab");
      tests.add("abb");
      tests.add("abbb");
      tests.add("abbbb");
      tests.add("abbbba");

      tests.add("b");
      tests.add("bb");

      tests.add("ba");
      tests.add("baa");
      tests.add("baaa");
      tests.add("baaaa");
      tests.add("baaaab");

      tests.add("ca");
      tests.add("cac");
      tests.add("caca");
      tests.add("z");

      for (String test : tests) {
         byte[] testbytes = test.getBytes(StandardCharsets.US_ASCII);
         boolean nfares = nfa.matches(RegexUtil.iterator(test));
         boolean dfares = dfa.matches(RegexUtil.iterator(test));
         boolean dfaminres = dfamin.matches(RegexUtil.iterator(test));
         boolean dfabyteres = dfabyte.matches(testbytes);

         Set<String> nfav = nfa.matching(RegexUtil.iterator(test));
         List<String> dfav = dfa.matching(RegexUtil.iterator(test));
         List<String> dfaminv = dfamin.matching(RegexUtil.iterator(test));
         List<String> dfabytev = dfabyte.matching(testbytes);
         if (nfares != dfares || nfares != dfaminres || nfares != dfabyteres) {
            System.out.print("\n'" + test + "' matches: " + 
                  "nfa=" + nfares + "(" + nfav + ")" +
                  ", dfa=" + dfares + "(" + dfav + ")" + 
                  ", mindfa=" + dfaminres + "(" + dfaminv + ")" + 
                  ", bytedfa=" + dfabyteres + "(" + dfabytev + ")" + 
                  " (miss match)");
         } else {
            System.out.print("\n'" + test.substring(0,Math.min(10,test.length())) + "' matches: " + 
                  "nfa=" + nfares + "(" + nfav + ")" +
                  ", dfa=" + dfares + "(" + dfav + ")" + 
                  ", mindfa=" + dfaminres + "(" + dfaminv + ")" +
                  ", bytedfa=" + dfaminres + "(" + dfaminv + ")");
         }
      }
   }
   */
}


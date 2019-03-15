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

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Function;
import com.iris.regex.RegexNfa.BuilderState;

public final class Regex {
   private Regex() {
   }

   /////////////////////////////////////////////////////////////////////////////
   // Parse a string into an NFA for testing purposes.
   // NOTE: This isn't really meant for to support full regex on strings as
   //       because like the wild card operator don't actually support the
   //       full UTF character range.
   /////////////////////////////////////////////////////////////////////////////
   
   public static <V> RegexNfa<Byte,V> parseByteRegex(String regex) {
      return parseByteRegex(regex,null);
   }
   
   public static <V> RegexNfa<Byte,V> parseByteRegex(String regex, @Nullable V value) {
      // example: '(1 2 00 3 0A)? . B*'

      StringBuilder num = new StringBuilder();
      List<Token<Byte>> tokRegex = new ArrayList<>();
      for (int i=0, e=regex.length(); i < e; ++i) {
         char ch  = regex.charAt(i);
         switch (ch) {
         case '*':
            addNumIfNeeded(tokRegex, num);
            tokRegex.add(Regex.<Byte>star());
            break;

         case '+':
            addNumIfNeeded(tokRegex, num);
            tokRegex.add(Regex.<Byte>plus());
            break;

         case '?':
            addNumIfNeeded(tokRegex, num);
            tokRegex.add(Regex.<Byte>hook());
            break;

         case '|':
            addNumIfNeeded(tokRegex, num);
            tokRegex.add(Regex.<Byte>pipe());
            break;

         case '(':
            addNumIfNeeded(tokRegex, num);
            tokRegex.add(Regex.<Byte>lparen());
            break;

         case ')':
            addNumIfNeeded(tokRegex, num);
            tokRegex.add(Regex.<Byte>rparen());
            break;

         case '.':
            addNumIfNeeded(tokRegex, num);
            tokRegex.add(Regex.<Byte>range(BYTE_WILDCARD));
            break;
         
         case ' ':
            addNumIfNeeded(tokRegex, num);
            break;

         default:
            num.append(ch);
            break;
         }
      }

      addNumIfNeeded(tokRegex, num);
      return parse(tokRegex, value);
   }

   private static void addNumIfNeeded(List<Token<Byte>> tokRegex, StringBuilder num) {
      if (num.length() > 0) {
         try {
            byte val = (byte)Integer.parseInt(num.toString(), 16);
            tokRegex.add(symbol(val));
         } catch (Exception ex) {
            throw new RuntimeException(ex);
         }
      }

      num.setLength(0);
   }
   
   public static <V> RegexNfa<Character,V> parse(String regex) {
      return parse(regex,null);
   }
   
   public static <V> RegexNfa<Character,V> parse(String regex, @Nullable V value) {
      boolean escape = false;
      List<Token<Character>> tokRegex = new ArrayList<>();
      for (int i=0, e=regex.length(); i < e; ++i) {
         char ch  = regex.charAt(i);
         switch (ch) {
         case '\\':
            if (escape) {
               tokRegex.add(symbol(ch));
            }
            escape = !escape;
            break;

         case '*':
            addSymbolOrEscaped(tokRegex, escape, ch, Regex.<Character>star());
            escape = false;
            break;

         case '+':
            addSymbolOrEscaped(tokRegex, escape, ch, Regex.<Character>plus());
            escape = false;
            break;

         case '?':
            addSymbolOrEscaped(tokRegex, escape, ch, Regex.<Character>hook());
            escape = false;
            break;

         case '|':
            addSymbolOrEscaped(tokRegex, escape, ch, Regex.<Character>pipe());
            escape = false;
            break;

         case '(':
            addSymbolOrEscaped(tokRegex, escape, ch, Regex.<Character>lparen());
            escape = false;
            break;

         case ')':
            addSymbolOrEscaped(tokRegex, escape, ch, Regex.<Character>rparen());
            escape = false;
            break;

         case '.':
            addSymbolOrEscaped(tokRegex, escape, ch, Regex.<Character>range(CHAR_WILDCARD));
            escape = false;
            break;

         default:
            tokRegex.add(symbol(ch));
            escape = false;
            break;
         }
      }

      if (escape) {
         tokRegex.add(symbol('\\'));
      }

      return parse(tokRegex, value);
   }
   
   /////////////////////////////////////////////////////////////////////////////
   // Parse a regex token stream into an NFA
   /////////////////////////////////////////////////////////////////////////////

   private static final <A> void addSymbolOrEscaped(List<Token<A>> tokRegex, boolean escape, A ch, Token<A> tok) {
      if (escape) {
         tokRegex.add(symbol(ch));
      } else {
         tokRegex.add(tok);
      }
   }

   public static <A,V> RegexNfa<A,V> parse(Iterable<Token<A>> regex) {
      return parse(regex,null);
   }

   public static <A,V> RegexNfa<A,V> parse(Iterable<Token<A>> regex, @Nullable V value) {
      RegexNfa.Builder<A,V> bld = RegexNfa.builder();
      if (!regex.iterator().hasNext()) {
         throw new IllegalArgumentException("invalid regex: '" + regex + "'");
      }

      List<Fragment<A,V>> stack = new ArrayList<>();
      Iterable<Token<A>> postfix = expand(regex);
      if (postfix == null) {
         throw new IllegalArgumentException("invalid regex: '" + regex + "'");
      }

      for (Token<A> tok : postfix) {
         switch (tok.type) {
         case HOOK:
            BuilderState<A,V> zoost = bld.createState();
            Fragment<A,V> zoo = pop(stack);
            zoost.addTransition(null, zoo.start);
            push(stack,new Fragment<A,V>(zoost,zoo.append(zoost,null)));
            break;

         case STAR:
            BuilderState<A,V> zomst = bld.createState();
            Fragment<A,V> zom = pop(stack);
            zom.patch(zomst);
            zomst.addTransition(null, zom.start);
            push(stack,new Fragment<A,V>(zomst,(A)null));
            break;

         case PLUS:
            BuilderState<A,V> oomst = bld.createState();
            Fragment<A,V> oom = pop(stack);
            oom.patch(oomst);
            oomst.addTransition(null, oom.start);
            push(stack,new Fragment<A,V>(oom.start,oom.out));
            break;

         case PIPE:
            Fragment<A,V> a2 = pop(stack);
            Fragment<A,V> a1 = pop(stack);
            BuilderState<A,V> altst = bld.createState();
            altst.addTransition(null, a1.start);
            altst.addTransition(null, a2.start);
            push(stack,new Fragment<A,V>(altst,a1.append(a2.out)));
            break;

         case CONCAT:
            Fragment<A,V> c2 = pop(stack);
            Fragment<A,V> c1 = pop(stack);
            c1.patch(c2.start);

            push(stack,new Fragment<A,V>(c1.start,c2.out));
            break;

         case RANGE:
            push(stack,new Fragment<A,V>(bld.createState(),tok.symbolRange));
            break;

         case SYMBOL:
            push(stack,new Fragment<A,V>(bld.createState(),tok.symbol));
            break;

         default:
            throw new IllegalStateException("unknown token: " + tok);
         }
      }

      Fragment<A,V> top = pop(stack);
      if (!stack.isEmpty()) {
         throw new IllegalArgumentException("invalid regex: '" + regex + "'");
      }

      RegexNfa.BuilderState<A,V> match = bld.createState();
      match.setFinalState(value);
      top.patch(match);

      top.start.setInitialState();
      return bld.build();
   }

   @Nullable
   private static final <A> Iterable<Token<A>> expand(Iterable<Token<A>> regex) {
      List<Token<A>> bld = new ArrayList<>();

      ExpandState current = new ExpandState();
      List<ExpandState> stack = new ArrayList<>();
      for (Token<A> tok : regex) {
         switch (tok.type) {
         case LPAREN:
            if (current.natom > 1){
               current.natom--;
               bld.add(Regex.<A>concat());
            }

            push(stack, current);
            current = new ExpandState();
            break;

         case RPAREN:
            if (stack.isEmpty() || current.natom == 0) {
               return null;
            }

            while (--current.natom > 0) {
               bld.add(Regex.<A>concat());
            }

            for (; current.nalt > 0; current.nalt--) {
               bld.add(Regex.<A>pipe());
            }

            current = pop(stack);
            current.natom++;
            break;

         case PIPE:
            if (current.natom == 0) {
               return null;
            }
            while (--current.natom > 0) {
               bld.add(Regex.<A>concat());
            }
            current.nalt++;
            break;

         case STAR:
         case PLUS:
         case HOOK:
            if (current.natom == 0) {
               return null;
            }
            bld.add(tok);
            break;

         default:
            if (current.natom > 1) {
               current.natom--;
               bld.add(Regex.<A>concat());
            }

            bld.add(tok);
            current.natom++;
            break;
         }
      }

      if (!stack.isEmpty()) {
         return null;
      }

      while (--current.natom > 0) {
         bld.add(Regex.<A>concat());
      }

      for (; current.nalt > 0; current.nalt--) {
         bld.add(Regex.<A>pipe());
      }

      return bld;
   }

   /////////////////////////////////////////////////////////////////////////////
   // Token Stream API
   /////////////////////////////////////////////////////////////////////////////
   
   public static enum TokenType {
      SYMBOL,
      RANGE,
      HOOK,
      STAR,
      PLUS,
      PIPE,
      LPAREN,
      RPAREN,

      // Internal tokens produced by the expand function
      CONCAT,
   }

   public static final class Token<A> {
      private final TokenType type;
      private final @Nullable A symbol;
      private final @Nullable Iterable<A> symbolRange;

      public Token(TokenType type) {
         this.type = type;
         this.symbol = null;
         this.symbolRange = null;
      }

      public Token(TokenType type, A symbol) {
         this.type = type;
         this.symbol = symbol;
         this.symbolRange = null;
      }

      public Token(TokenType type, Iterable<A> symbolRange) {
         this.type = type;
         this.symbol = null;
         this.symbolRange = symbolRange;
      }
   }

   private static final Token<?> HOOK = new Token<Object>(TokenType.HOOK);
   private static final Token<?> STAR = new Token<Object>(TokenType.STAR);
   private static final Token<?> PLUS = new Token<Object>(TokenType.PLUS);
   private static final Token<?> PIPE = new Token<Object>(TokenType.PIPE);
   private static final Token<?> LPAREN = new Token<Object>(TokenType.LPAREN);
   private static final Token<?> RPAREN = new Token<Object>(TokenType.RPAREN);
   private static final Token<?> CONCAT = new Token<Object>(TokenType.CONCAT);

   public static <A> Token<A> symbol(A symbol) {
      return new Token<A>(TokenType.SYMBOL, symbol);
   }

   public static <A> Token<A> range(Iterable<A> symbolRange) {
      return new Token<A>(TokenType.RANGE, symbolRange);
   }

   @SuppressWarnings("unchecked")
   public static <A> Token<A> hook() {
      return (Token<A>)HOOK;
   }

   @SuppressWarnings("unchecked")
   public static <A> Token<A> star() {
      return (Token<A>)STAR;
   }

   @SuppressWarnings("unchecked")
   public static <A> Token<A> plus() {
      return (Token<A>)PLUS;
   }

   @SuppressWarnings("unchecked")
   public static <A> Token<A> pipe() {
      return (Token<A>)PIPE;
   }

   @SuppressWarnings("unchecked")
   public static <A> Token<A> lparen() {
      return (Token<A>)LPAREN;
   }

   @SuppressWarnings("unchecked")
   public static <A> Token<A> rparen() {
      return (Token<A>)RPAREN;
   }

   @SuppressWarnings("unchecked")
   private static <A> Token<A> concat() {
      return (Token<A>)CONCAT;
   }

   /////////////////////////////////////////////////////////////////////////////
   // Implementation Details
   /////////////////////////////////////////////////////////////////////////////

   private static <T> T pop(List<T> stack) {
      if (stack.isEmpty()) throw new IllegalArgumentException("invalid regex");
      return stack.remove(stack.size()-1);
   }

   private static <T> void push(List<T> stack, T value) {
      stack.add(value);
   }

   private static final class Fragment<A,V> {
      private final RegexNfa.BuilderState<A,V> start;
      private List<Output<A,V>> out;

      public Fragment(BuilderState<A,V> state, @Nullable A symbol) {
         this.start = state;
         this.out = Collections.singletonList(new Output<>(state,symbol));
      }

      public Fragment(BuilderState<A,V> state, List<Output<A,V>> out) {
         this.start = state;
         this.out = out;
      }

      public Fragment(BuilderState<A,V> state, Iterable<A> range) {
         this.start = state;

         this.out = new ArrayList<>();
         for (A symbol : range) {
            this.out.add(new Output<>(state,symbol));
         }
      }

      public void patch(BuilderState<A,V> to) {
         for (Output<A,V> o : out) {
            o.state.addTransition(o.symbol,to);
         }
      }

      public List<Output<A,V>> append(BuilderState<A,V> state, @Nullable A symbol) {
         List<Output<A,V>> result = new ArrayList<>(out.size() + 1);
         result.addAll(out);
         result.add(new Output<>(state,symbol));
         return result;
      }

      public List<Output<A,V>> append(List<Output<A,V>> other) {
         List<Output<A,V>> result = new ArrayList<>(out.size() + other.size());
         result.addAll(out);
         result.addAll(other);
         return result;
      }
   }

   private static final class Output<A,V> {
      private final RegexNfa.BuilderState<A,V> state;
      private final @Nullable A symbol;

      public Output(BuilderState<A,V> state, @Nullable A symbol) {
         this.state = state;
         this.symbol = symbol;
      }
   }

   private static final class ExpandState {
      int nalt = 0;
      int natom = 0;
   }

   private static final Character[] CHAR_WILDRAW = new Character[] {
      'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z',
      'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z',
      '0','1','2','3','4','5','6','7','8','9',
      ' ','~','`','!','@','#','$','%','^','&','*','(',')','-','_','+','=','|','\\',']','}','[','{','\'','"',
      ';',':','/','?','.','>','<',',',
   };

   private static final List<Character> CHAR_WILDCARD = Collections.unmodifiableList(Arrays.asList(CHAR_WILDRAW));

   private static final Iterable<Byte> BYTE_WILDCARD = new Iterable<Byte>() {
      @Override
      public Iterator<Byte> iterator() {
         return new AllByteIterator();
      }
   };

   private static final class AllByteIterator implements Iterator<Byte> {
      private int val = 0;

      @Override
      public boolean hasNext() {
         return val < 256;
      }

      @Override
      public Byte next() {
         if (val >= 256) {
            throw new NoSuchElementException();
         }

         int result = val & 0xFF;
         val++;
         return (byte)result;
      }

      @Override
      public void remove() {
         throw new UnsupportedOperationException();
      }
   }
}


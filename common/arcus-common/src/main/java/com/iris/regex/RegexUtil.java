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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Function;

public final class RegexUtil {
   private RegexUtil() {
   }

   /////////////////////////////////////////////////////////////////////////////
   // Convert a DFA using bytes as the alphabet into an optimized representation
   /////////////////////////////////////////////////////////////////////////////
   
   public static final <V> RegexDfaByte<V> dfaToByteRep(RegexDfa<Byte,V> dfa) {
      RegexDfaByte.Builder<V> bld = RegexDfaByte.builder();

      Map<RegexDfa.State<Byte,V>,RegexDfaByte.BuilderState<V>> states = new IdentityHashMap<>();
      for (RegexDfa.State<Byte,V> state : dfa.states) {
         RegexDfaByte.BuilderState<V> nstate = bld.createState();
         nstate.setInitialState(state.isInitialState());
         nstate.setFinalState(state.isFinalState(), state.value);
         states.put(state,nstate);
      }

      for (RegexDfa.State<Byte,V> state : dfa.states) {
         RegexDfaByte.BuilderState<V> from = states.get(state);
         for (Map.Entry<Byte,RegexDfa.State<Byte,V>> tran : state.transitions.entrySet()) {
            RegexDfaByte.BuilderState<V> to = states.get(tran.getValue());
            from.addTransition(tran.getKey(), to);
         }
      }

      return bld.build();
   }

   /////////////////////////////////////////////////////////////////////////////
   // Convert a DFA from one symbol space to another
   /////////////////////////////////////////////////////////////////////////////
   
   public static final <A,B,V> RegexDfa<B,V> dfaConvertSymbolSpace(RegexDfa<A,V> dfa, Function<A,B> func) {
      RegexDfa.Builder<B,V> bld = RegexDfa.builder();

      Map<RegexDfa.State<A,V>,RegexDfa.BuilderState<B,V>> states = new IdentityHashMap<>();
      for (RegexDfa.State<A,V> state : dfa.states) {
         RegexDfa.BuilderState<B,V> nstate = bld.createState();
         nstate.setInitialState(state.isInitialState());
         nstate.setFinalState(state.isFinalState(), state.value);
         states.put(state,nstate);
      }

      for (RegexDfa.State<A,V> state : dfa.states) {
         RegexDfa.BuilderState<B,V> from = states.get(state);
         for (Map.Entry<A,RegexDfa.State<A,V>> tran : state.transitions.entrySet()) {
            RegexDfa.BuilderState<B,V> to = states.get(tran.getValue());
            from.addTransition(func.apply(tran.getKey()), to);
         }
      }

      return bld.build();
   }
   
   public static final <A,V,W> RegexDfa<A,W> dfaConvertValueSpace(RegexDfa<A,V> dfa, Function<V,W> func) {
      RegexDfa.Builder<A,W> bld = RegexDfa.builder();

      Map<RegexDfa.State<A,V>,RegexDfa.BuilderState<A,W>> states = new IdentityHashMap<>();
      for (RegexDfa.State<A,V> state : dfa.states) {
         RegexDfa.BuilderState<A,W> nstate = bld.createState();
         nstate.setInitialState(state.isInitialState());
         nstate.setFinalState(state.isFinalState(), func.apply(state.value));
         states.put(state,nstate);
      }

      for (RegexDfa.State<A,V> state : dfa.states) {
         RegexDfa.BuilderState<A,W> from = states.get(state);
         for (Map.Entry<A,RegexDfa.State<A,V>> tran : state.transitions.entrySet()) {
            RegexDfa.BuilderState<A,W> to = states.get(tran.getValue());
            from.addTransition(tran.getKey(), to);
         }
      }

      return bld.build();
   }

   /////////////////////////////////////////////////////////////////////////////
   // Minimize the states and transitions in a DFA
   /////////////////////////////////////////////////////////////////////////////
   
   public static final <A,V> RegexDfa<A,V> dfaMinimize(RegexDfa<A,V> dfa) {
      return dfaRemoveUnreachable(dfaRemoveIndistinguishable(dfaRemoveUnreachable(dfa)));
   }
   
   /////////////////////////////////////////////////////////////////////////////
   // Remove unreachable states from a DFA
   /////////////////////////////////////////////////////////////////////////////
   
   public static final <A,V> RegexDfa<A,V> dfaRemoveUnreachable(RegexDfa<A,V> dfa) {
      Set<RegexDfa.State<A,V>> reachable = new HashSet<>();
      reachable.add(dfa.initialState);

      Set<RegexDfa.State<A,V>> newst = new HashSet<>(reachable);
      while (!newst.isEmpty()) {
         Set<RegexDfa.State<A,V>> tmp = new HashSet<>();
         for (RegexDfa.State<A,V> st : newst) {
            tmp.addAll(st.transitions.values());
         }

         tmp.removeAll(reachable);
         newst = tmp;

         reachable.addAll(newst);
      }

      RegexDfa.Builder<A,V> bld = RegexDfa.builder();
      Map<RegexDfa.State<A,V>,RegexDfa.BuilderState<A,V>> building = new HashMap<>();
      for (RegexDfa.State<A,V> reach : reachable) {
         RegexDfa.BuilderState<A,V> st = bld.createState();

         st.setInitialState(reach.isInitialState());
         st.setFinalState(reach.isFinalState(), reach.value);

         building.put(reach,st);
      }

      for (RegexDfa.State<A,V> st : dfa.states) {
         RegexDfa.BuilderState<A,V> reach = building.get(st);
         if (reach == null) {
            continue;
         }

         for (Map.Entry<A,RegexDfa.State<A,V>> tran : st.transitions.entrySet()) {
            RegexDfa.State<A,V> dst = tran.getValue();
            reach.addTransition(tran.getKey(),building.get(dst));
         }
      }

      return bld.build();
   }
   
   /////////////////////////////////////////////////////////////////////////////
   // Remove indistinguishable states from a DFA using Hopcroft's algorithm
   /////////////////////////////////////////////////////////////////////////////
   
   public static final <A,V> RegexDfa<A,V> dfaRemoveIndistinguishable(RegexDfa<A,V> dfa) {
      Set<Set<RegexDfa.State<A,V>>> equivalence = new HashSet<>();
      Set<RegexDfa.State<A,V>> nonfinl = new HashSet<>();
      Map<V,Set<RegexDfa.State<A,V>>> finl = new HashMap<>();

      Map<RegexDfa.State<A,V>,Set<A>> incoming = new HashMap<>();
      for (RegexDfa.State<A,V> st : dfa.states) {
         if (st.isFinalState()) {
            Set<RegexDfa.State<A,V>> cls = finl.get(st.value);
            if (cls == null) {
               cls = new HashSet<>();
               finl.put(st.value,cls);
            }

            cls.add(st);
         } else {
            nonfinl.add(st);
         }

         for (Map.Entry<A,RegexDfa.State<A,V>> entry : st.transitions.entrySet()) {
            Set<A> alpha = incoming.get(entry.getValue());
            if (alpha == null) {
               alpha = new HashSet<>();
               incoming.put(entry.getValue(), alpha);
            }

            alpha.add(entry.getKey());
         }
      }

      List<Set<RegexDfa.State<A,V>>> stack = new ArrayList<>();
      stack.addAll(finl.values());

      equivalence.add(nonfinl);
      equivalence.addAll(finl.values());
      while (!stack.isEmpty()) {
         Set<RegexDfa.State<A,V>> next = stack.remove(stack.size() - 1);

         Set<A> alpha = new LinkedHashSet<>();
         for (RegexDfa.State<A,V> nxt : next) {
            Set<A> s = incoming.get(nxt);
            if (s != null) {
               alpha.addAll(s);
            }
         }

         if (alpha.isEmpty()) {
            continue;
         }

         for (A al : alpha) {
            Set<RegexDfa.State<A,V>> upd = new HashSet<>();
            for (RegexDfa.State<A,V> st : dfa.states) {
               if (next.contains(st.transitions.get(al))) {
                  upd.add(st);
               }
            }

            if (upd.isEmpty()) {
               continue;
            }

            List<Set<RegexDfa.State<A,V>>> sstack = new ArrayList<>(equivalence);
            while (!sstack.isEmpty()) {
               Set<RegexDfa.State<A,V>> tst = sstack.remove(sstack.size() - 1);

               Set<RegexDfa.State<A,V>> complement = new HashSet<>(tst);
               complement.removeAll(upd);
               if (complement.isEmpty()) {
                  continue;
               }

               Set<RegexDfa.State<A,V>> intersection = new HashSet<>(tst);
               intersection.retainAll(upd);
               if (intersection.isEmpty()) {
                  continue;
               }

               equivalence.remove(tst);
               equivalence.add(complement);
               equivalence.add(intersection);

               sstack.add(complement);
               sstack.add(intersection);

               if (stack.contains(tst)) {
                  stack.remove(tst);
                  stack.add(complement);
                  stack.add(intersection);
               } else {
                  if (complement.size() <= intersection.size()) {
                     stack.add(complement);
                  } else {
                     stack.add(intersection);
                  }
               }
            }
         }
      }

      Map<RegexDfa.State<A,V>,Set<RegexDfa.State<A,V>>> forwarding = new HashMap<>();
      Map<Set<RegexDfa.State<A,V>>,RegexDfa.BuilderState<A,V>> building = new HashMap<>();

      RegexDfa.Builder<A,V> bld = RegexDfa.builder();
      for (Set<RegexDfa.State<A,V>> cls : equivalence) {
         RegexDfa.BuilderState<A,V> st = bld.createState();
         st.setInitialState(computeDfaInitialState(cls));
         st.setFinalState(computeDfaFinalState(cls), computeDfaFinalValue(cls));
         building.put(cls,st);

         for (RegexDfa.State<A,V> fst : cls) {
            forwarding.put(fst,cls);
         }
      }

      for (RegexDfa.State<A,V> st : dfa.states) {
         Set<RegexDfa.State<A,V>> scls = forwarding.get(st);

         for (Map.Entry<A,RegexDfa.State<A,V>> tran : st.transitions.entrySet()) {
            RegexDfa.State<A,V> dst = tran.getValue();
            Set<RegexDfa.State<A,V>> cls = forwarding.get(dst);
            building.get(scls).addTransition(tran.getKey(),building.get(cls));
         }
      }

      return bld.build();
   }

   private static final <A,V> boolean computeDfaInitialState(Set<RegexDfa.State<A,V>> states) {
      for (RegexDfa.State<A,V> state : states) {
         if (state.isInitialState()) {
            return true;
         }
      }

      return false;
   }

   private static final <A,V> boolean computeDfaFinalState(Set<RegexDfa.State<A,V>> states) {
      for (RegexDfa.State<A,V> state : states) {
         if (state.isFinalState()) {
            return true;
         }
      }

      return false;
   }

   @Nullable
   private static final <A,V> V computeDfaFinalValue(Set<RegexDfa.State<A,V>> states) {
      V value = null;
      for (RegexDfa.State<A,V> state : states) {
         if (state.isFinalState()) {
            if (value == null) {
               value = state.value;
            } else if (!value.equals(state.value)) {
               throw new IllegalStateException("dfa minimization combined final states with differnt values when it shouldn't");
            }
         }
      }

      return value;
   }

   /////////////////////////////////////////////////////////////////////////////
   // Convert an NFA into a DFA using the power set construction algorithm
   /////////////////////////////////////////////////////////////////////////////

   public static final <A,V> RegexDfa<A,List<V>> nfaConvertToDfa(RegexNfa<A,V> nfa) {
      RegexDfa.Builder<A,List<V>> bld = RegexDfa.builder();

      Map<Set<RegexNfa.State<A,V>>,RegexDfa.BuilderState<A,List<V>>> statemap = new HashMap<>();
      List<Set<RegexNfa.State<A,V>>> stack = new ArrayList<>();

      Set<RegexNfa.State<A,V>> initial = computeEpsilonClosure(nfa.initialStates);
      RegexDfa.BuilderState<A,List<V>> initialdfa = bld.createState();
      initialdfa.setInitialState();
      initialdfa.setFinalState(computeFinalState(initial), computeFinalValue(initial));

      stack.add(initial);
      statemap.put(initial,initialdfa);

      while (!stack.isEmpty()) {
         Set<RegexNfa.State<A,V>> next = stack.remove(stack.size() - 1);
         RegexDfa.BuilderState<A,List<V>> start = statemap.get(next);
      
         Set<A> trans = computeTransitionClosure(next);
         for (A symbol : trans) {
            if (symbol == null) {
               continue;
            }

            Set<RegexNfa.State<A,V>> nst = new HashSet<>();
            for (RegexNfa.State<A,V> nxt : next) {
               Set<RegexNfa.State<A,V>> out = nxt.transitions.get(symbol);
               if (out != null) {
                  nst.addAll(out);
               }
            }

            if (!nst.isEmpty()) {
               Set<RegexNfa.State<A,V>> nfast = computeEpsilonClosure(nst);
               RegexDfa.BuilderState<A,List<V>> dfast = statemap.get(nfast);
               if (dfast == null) {
                  dfast = bld.createState();
                  dfast.setFinalState(computeFinalState(nfast), computeFinalValue(nfast));

                  stack.add(nfast);
                  statemap.put(nfast,dfast);
               }

               start.addTransition(symbol,dfast);
            }
         }
      }

      return bld.build();
   }

   private static final <A,V> Set<A> computeTransitionClosure(Set<RegexNfa.State<A,V>> states) {
      Set<A> result = new HashSet<>();
      for (RegexNfa.State<A,V> state : states) {
         result.addAll(state.transitions.keySet());
      }

      return result;
   }

   private static final <A,V> Set<RegexNfa.State<A,V>> computeEpsilonClosure(Set<RegexNfa.State<A,V>> start) {
      Set<RegexNfa.State<A,V>> result = new HashSet<>();
      Set<RegexNfa.State<A,V>> tmp = new HashSet<>();
      result.addAll(start);

      while (true) {
         tmp.clear();
         for (RegexNfa.State<A,V> st : result) {
            addEpsilonTransitions(result,tmp,st);
         }

         if (tmp.isEmpty()) {
            break;
         }

         result.addAll(tmp);
      }

      return result;
   }

   private static <A,V> void addEpsilonTransitions(Set<RegexNfa.State<A,V>> current, Set<RegexNfa.State<A,V>> tmp, RegexNfa.State<A,V> from) {
      Set<RegexNfa.State<A,V>> trans = from.transitions.get(null);
      if (trans != null) {
         for (RegexNfa.State<A,V> dst : trans) {
            if (!current.contains(dst)) {
               tmp.add(dst);
            }
         }
      }
   }

   private static final <A,V> boolean computeFinalState(Set<RegexNfa.State<A,V>> states) {
      for (RegexNfa.State<A,V> state : states) {
         if (state.isFinalState()) {
            return true;
         }
      }

      return false;
   }

   @Nullable
   private static final <A,V> List<V> computeFinalValue(Set<RegexNfa.State<A,V>> states) {
      List<V> result = new ArrayList<>();
      for (RegexNfa.State<A,V> state : states) {
         if (state.isFinalState()) {
            result.add(state.value);
         }
      }

      return result;
   }

   /////////////////////////////////////////////////////////////////////////////
   // Character iterators
   /////////////////////////////////////////////////////////////////////////////
   
   public static Iterable<Character> iterable(CharSequence seq) {
      return new CharSequenceIterable(seq);
   }
   
   public static Iterator<Character> iterator(CharSequence seq) {
      return new CharSequenceIterator(seq);
   }
   
   public static Iterable<Byte> asciiIterable(CharSequence seq) {
      return new AsciiByteIterable(seq);
   }
   
   public static Iterator<Byte> asciiIterator(CharSequence seq) {
      return new AsciiByteIterator(seq);
   }

   private static final class CharSequenceIterable implements Iterable<Character> {
      private final CharSequence seq;

      public CharSequenceIterable(CharSequence seq) {
         this.seq = seq;
      }

      @Override
      public Iterator<Character> iterator() {
         return new CharSequenceIterator(seq);
      }
   }

   private static final class CharSequenceIterator implements Iterator<Character> {
      private final CharSequence seq;
      private int idx;

      public CharSequenceIterator(CharSequence seq) {
         this.seq = seq;
      }

      @Override
      public boolean hasNext() {
         return idx < seq.length();

      }

      @Override
      public Character next() {
         if (idx >= seq.length()) {
            throw new NoSuchElementException();
         }

         return seq.charAt(idx++);
      }

      @Override
      public void remove() {
         throw new UnsupportedOperationException();
      }
   }

   private static final class AsciiByteIterable implements Iterable<Byte> {
      private final CharSequence seq;

      public AsciiByteIterable(CharSequence seq) {
         this.seq = seq;
      }

      @Override
      public Iterator<Byte> iterator() {
         return new AsciiByteIterator(seq);
      }
   }

   private static final class AsciiByteIterator implements Iterator<Byte> {
      private final CharSequence seq;
      private int idx;

      public AsciiByteIterator(CharSequence seq) {
         this.seq = seq;
      }

      @Override
      public boolean hasNext() {
         return idx < seq.length();

      }

      @Override
      public Byte next() {
         if (idx >= seq.length()) {
            throw new NoSuchElementException();
         }

         return (byte)seq.charAt(idx++);
      }

      @Override
      public void remove() {
         throw new UnsupportedOperationException();
      }
   }
}


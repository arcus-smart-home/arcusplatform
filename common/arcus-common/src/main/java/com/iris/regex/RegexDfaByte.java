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
import java.util.Arrays;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class RegexDfaByte<V> {
   final State<V> initialState;
   final Set<State<V>> states;

   public RegexDfaByte(State<V> initialState, Set<State<V>> states) {
      this.initialState = initialState;
      this.states = states;
   }

   public static <V> Builder<V> builder() {
      return new Builder<>();
   }

   public <T> RegexDfaByte<T> transform(Function<V,T> transformer) {
      IdentityHashMap<State<V>,State<T>> stateMap = new IdentityHashMap<>();

      State<T> newInitialState = null;
      for (State<V> state : states) {
         State<T> transformed = state.transform(transformer);
         if (transformed.isInitialState()) {
            newInitialState = transformed;
         }

         stateMap.put(state, transformed);
      }

      for (Map.Entry<State<V>,State<T>> entry : stateMap.entrySet()) {
         State<V> oldState = entry.getKey();
         State<T> newState = entry.getValue();
         newState.setTransitions(oldState.getTransitions().transform(stateMap,transformer));
      }

      if (newInitialState == null) {
         throw new IllegalStateException("no initial state");
      }

      Set<State<T>> newStates = ImmutableSet.copyOf(stateMap.values());
      return new RegexDfaByte<T>(newInitialState, newStates);
   }

   public State<V> getInitialState() {
      return initialState;
   }

   public Set<State<V>> getStates() {
      return states;
   }

   public int getNumStates() {
      return states.size();
   }

   public int getNumTransitions() {
      int num = 0;
      for (State<V> st : states) {
         num += st.getTransitions().getNumTransitions();
      }

      return num;
   }

   public boolean matches(byte[] input) {
      Matcher match = matcher();
      for (int i=0, e=input.length; i<e; ++i) {
         if (match.process(input[i])) {
            break;
         }
      }

      return match.matched();
   }

   @Nullable
   public V matching(byte[] input) {
      Matcher match = matcher();
      for (int i=0, e=input.length; i<e; ++i) {
         if (match.process(input[i])) {
            break;
         }
      }

      return match.match();
   }


   public boolean matches(Iterable<Byte> input) {
      return matches(input.iterator());
   }

   public boolean matches(Iterator<Byte> input) {
      Matcher match = matcher();
      while (input.hasNext()) {
         if (match.process(input.next())) {
            break;
         }
      }

      return match.matched();
   }

   @Nullable
   public V matching(Iterable<Byte> input) {
      return matching(input.iterator());
   }

   @Nullable
   public V matching(Iterator<Byte> input) {
      Matcher match = matcher();
      while (input.hasNext()) {
         if (match.process(input.next())) {
            break;
         }
      }

      return match.match();
   }

   public Matcher matcher() {
      return new Matcher();
   }

   public String toDotGraph() {
      return toDotGraph("", " ");
   }

   public String toDotGraph(String newline, String tab) {
      StringBuilder bld = new StringBuilder();
      bld.append("digraph nfa {").append(newline);

      int next = 0;
      Map<State<?>,String> nodeNames = new IdentityHashMap<>();
      for (State<V> state : states) {
         nodeNames.put(state,"s" + next++);
      }

      for (State<V> state : states) {
         String shape;
         switch (state.type) {
         case INITIALFINAL: shape = "doubleoctagon"; break;
         case INITIAL: shape = "doublecircle"; break;
         case FINAL: shape = "octagon"; break;
         default: shape = "circle"; break;
         }

         bld.append(tab)
            .append(nodeNames.get(state))
            .append(" [shape=")
            .append(shape)
            .append("];")
            .append(newline);
      }

      for (State<V> state : states) {
         state.transitions.toDotGraph(bld,state,nodeNames,newline,tab);
      }

      bld.append("}");
      return bld.toString();
   }

   public final class Matcher {
      private @Nullable State<V> current;

      public Matcher() {
         current = initialState;
      }

      public @Nullable State<V> current() {
         return current;
      }

      public boolean process(byte symbol) {
         State<V> cur = current;
         if (cur != null) {
            current = cur.transitions.get(symbol);
         }

         return current == null;
      }

      public boolean matched() {
         return current != null && current.isFinalState();
      }

      @Nullable
      public V match() {
         return matched() ? current.value : null;
      }

      public List<Byte> getTransitionsFromCurrent() {
         State<?> cur = current;
         if (cur == null) {
            return ImmutableList.of();
         }

         TransitionTable<?> table = cur.getTransitions();
         if (table == null) {
            return ImmutableList.of();
         }

         return table.knownTransitionSymbols();
      }
   }

   public static final class State<V> {
      public static enum Type { INITIALFINAL, INITIAL, FINAL, NORMAL };

      private final Type type;
      final @Nullable V value;
      @Nullable TransitionTable<V> transitions;

      public State(Type type, V value) {
         this.type = type;
         this.value = value;
      }

      public void setTransitions(TransitionTable<V> transitions) {
         this.transitions = transitions;
      }

      public boolean isInitialState() {
         return type == Type.INITIAL || type == Type.INITIALFINAL;
      }

      public boolean isFinalState() {
         return type == Type.FINAL || type == Type.INITIALFINAL;
      }

      public @Nullable V getValue() {
         return value;
      }

      public TransitionTable<V> getTransitions() {
         return transitions;
      }

      public <T> State<T> transform(Function<V,T> transformer) {
         return new State<T>(type, transformer.apply(value));
      }

      @Override
      public String toString() {
         switch (type) {
         case INITIALFINAL: return "if" + hashCode();
         case INITIAL: return "i" + hashCode();
         case FINAL: return "f" + hashCode();
         default: return "n" + hashCode();
         }
      }
   }

   public static interface TransitionTable<V> {
      @Nullable State<V> get(byte symbol);
      int getNumTransitions();
      void toDotGraph(StringBuilder bld, State<?> start, Map<State<?>,String> names, String newline, String tab);
      <T> TransitionTable<T> transform(Map<State<V>,State<T>> states, Function<V,T> transformer);
      List<Byte> knownTransitionSymbols();
   }

   public static enum EmptyTransitionTable implements TransitionTable<Object> {
      INSTANCE;

      @Override
      @Nullable
      public State<Object> get(byte symbol) {
         return null;
      }

      @Override
      public void toDotGraph(StringBuilder bld, State<?> start, Map<State<?>,String> names, String newline, String tab) {
      }

      @Override
      public int getNumTransitions() {
         return 0;
      }

      @Override
      public <T> TransitionTable<T> transform(Map<State<Object>,State<T>> states, Function<Object,T> transformer) {
         return (TransitionTable<T>)INSTANCE;
      }
      
      @Override
      public List<Byte> knownTransitionSymbols() {
         return ImmutableList.of();
      }
   }

   public static final class SingletonTransitionTable<V> implements TransitionTable<V> {
      private final byte symbol;
      private final State<V> state;

      public SingletonTransitionTable(byte symbol, State<V> state) {
         this.symbol = symbol;
         this.state = state;
      }

      public byte getSymbol() {
         return symbol;
      }
      
      public State<V> getState() {
         return state;
      }

      @Override
      @Nullable
      public State<V> get(byte symbol) {
         return (this.symbol == symbol) ? state : null;
      }

      @Override
      public void toDotGraph(StringBuilder bld, State<?> start, Map<State<?>,String> names, String newline, String tab) {
         appendEdgeToGraph(bld, start, state, names, labelForSymbol(symbol), newline, tab);
      }

      @Override
      public int getNumTransitions() {
         return 1;
      }

      @Override
      public <T> TransitionTable<T> transform(Map<State<V>,State<T>> states, Function<V,T> transformer) {
         return new SingletonTransitionTable<T>(symbol,states.get(state));
      }
      
      @Override
      public List<Byte> knownTransitionSymbols() {
         return ImmutableList.of(symbol);
      }
   }

   public static final class RangeTransitionTable<V> implements TransitionTable<V> {
      private final int lower;
      private final int upper;
      private final State<V> state;

      public RangeTransitionTable(int lower, int upper, State<V> state) {
         this.lower = lower;
         this.upper = upper;
         this.state = state;
      }

      public int getLower() {
         return lower;
      }

      public int getUpper() {
         return upper;
      }

      public State<V> getState() {
         return state;
      }

      @Override
      @Nullable
      public State<V> get(byte symbol) {
         int sym = symbol & 0xFF;
         return (lower <= sym && sym <= upper)
            ? state
            : null;
      }

      @Override
      public void toDotGraph(StringBuilder bld, State<?> start, Map<State<?>,String> names, String newline, String tab) {
         String label = labelForSymbol((byte)lower) + "-" + labelForSymbol((byte)upper);
         appendEdgeToGraph(bld, start, state, names, label, newline, tab);
      }

      @Override
      public int getNumTransitions() {
         return 1;
      }

      @Override
      public <T> TransitionTable<T> transform(Map<State<V>,State<T>> states, Function<V,T> transformer) {
         return new RangeTransitionTable<T>(lower,upper,states.get(state));
      }
      
      @Override
      public List<Byte> knownTransitionSymbols() {
         ImmutableList.Builder<Byte> bld = ImmutableList.builder();
         for (int i = lower; i <= upper; ++i) {
            bld.add((byte)i);
         }

         return bld.build();
      }
   }

   public static final class AllTransitionTable<V> implements TransitionTable<V> {
      private final State<V> state;

      public AllTransitionTable(State<V> state) {
         this.state = state;
      }

      public State<V> getState() {
         return state;
      }

      @Override
      @Nullable
      public State<V> get(byte symbol) {
         return state;
      }

      @Override
      public void toDotGraph(StringBuilder bld, State<?> start, Map<State<?>,String> names, String newline, String tab) {
         appendEdgeToGraph(bld, start, state, names, ".", newline, tab);
      }

      @Override
      public int getNumTransitions() {
         return 1;
      }

      @Override
      public <T> TransitionTable<T> transform(Map<State<V>,State<T>> states, Function<V,T> transformer) {
         return new AllTransitionTable<T>(states.get(state));
      }
      
      @Override
      public List<Byte> knownTransitionSymbols() {
         ImmutableList.Builder<Byte> bld = ImmutableList.builder();
         for (int i = 0; i <= 255; ++i) {
            bld.add((byte)i);
         }

         return bld.build();
      }
   }

   public static final class LookupTransitionTable<V> implements TransitionTable<V> {
      private final int offset;
      private final State<V>[] states;

      public LookupTransitionTable(State<V>[] states, int offset) {
         this.states = states;
         this.offset = offset;
      }

      public int getOffset() {
         return offset;
      }

      public State<V>[] getStates() {
         return states;
      }

      @Override
      @Nullable
      public State<V> get(byte symbol) {
         int idx = (symbol & 0xFF) - offset;
         return (idx >= 0 && idx < states.length)
            ? states[idx]
            : null;
      }

      @Override
      public void toDotGraph(StringBuilder bld, State<?> start, Map<State<?>,String> names, String newline, String tab) {
         for (int i=0; i<states.length; ++i) {
            if (states[i] == null) {
               continue;
            }

            byte symbol = (byte)(i + offset);
            appendEdgeToGraph(bld, start, states[i], names, labelForSymbol(symbol), newline, tab);
         }
      }

      @Override
      public int getNumTransitions() {
         return states.length;
      }

      @Override
      public <T> TransitionTable<T> transform(Map<State<V>,State<T>> states, Function<V,T> transformer) {
         State<T>[] newStates = new State[this.states.length];

         int i = 0;
         for (State<V> state : this.states) {
            newStates[i++] = states.get(state);
         }

         return new LookupTransitionTable<T>(newStates, offset);
      }

      @Override
      public List<Byte> knownTransitionSymbols() {
         ImmutableList.Builder<Byte> bld = ImmutableList.builder();
         for (int i = 0; i <= states.length; ++i) {
            bld.add((byte)(offset + i));
         }

         return bld.build();
      }
   }

   public static final class AlternatesTransitionTable<V> implements TransitionTable<V> {
      private final int[] lowers;
      private final TransitionTable<V>[] alternates;

      public AlternatesTransitionTable(int[] lowers, TransitionTable<V>[] alternates) {
         this.lowers = lowers;
         this.alternates = alternates;
      }

      public int[] getLowers() {
         return lowers;
      }

      public TransitionTable<V>[] getAlternates() {
         return alternates;
      }

      @Override
      @Nullable
      public State<V> get(byte symbol) {
         int sym = symbol & 0xFF;

         int idx = Arrays.binarySearch(lowers,sym);
         if (idx < 0) {
            idx = -(idx + 2);
         }

         if (idx < 0 || idx >= alternates.length) {
            return null;
         }

         TransitionTable<V> alt = alternates[idx];
         return (alt != null) ? alt.get(symbol) : null;
      }

      @Override
      public void toDotGraph(StringBuilder bld, State<?> start, Map<State<?>,String> names, String newline, String tab) {
         for (int i = 0; i < alternates.length; ++i) {
            if (alternates[i] != null) {
               alternates[i].toDotGraph(bld, start, names, newline, tab);
            }
         }
      }

      @Override
      public int getNumTransitions() {
         int num = 0;
         for (TransitionTable<V> tt : alternates) {
            num += tt.getNumTransitions();
         }

         return num;
      }

      @Override
      public <T> TransitionTable<T> transform(Map<State<V>,State<T>> states, Function<V,T> transformer) {
         TransitionTable<T>[] newAlternates = new TransitionTable[alternates.length];

         int i = 0;
         for (TransitionTable<V> tt : alternates) {
            newAlternates[i++] = tt.transform(states,transformer);
         }

         return new AlternatesTransitionTable<T>(lowers, newAlternates);
      }
      
      @Override
      public List<Byte> knownTransitionSymbols() {
         ImmutableList.Builder<Byte> bld = ImmutableList.builder();
         for (int i = 0; i < alternates.length; ++i) {
            bld.addAll(alternates[i].knownTransitionSymbols());
         }

         return bld.build();
      }
   }

   private static String labelForSymbol(byte symbol) {
      int sym = symbol & 0xFF;
      if (sym < 16) return "0" + Integer.toHexString(sym);
      else return Integer.toHexString(sym);
   }

   private static void appendEdgeToGraph(StringBuilder bld, State<?> start, State<?> end, Map<State<?>,String> names, String label, String newline, String tab) {
      bld.append(tab)
         .append(names.get(start))
         .append(" -> ")
         .append(names.get(end))
         .append(" [label=\"")
         .append(label)
         .append("\"];")
         .append(newline);
   }

   /////////////////////////////////////////////////////////////////////////////
   // Builder pattern implementation
   /////////////////////////////////////////////////////////////////////////////

   public static final class BuilderState<V> {
      private final Map<Integer,BuilderState<V>> transitions;
      private boolean initialState;
      private boolean finalState;
      private @Nullable V value;

      private BuilderState() {
         this.transitions = new TreeMap<>();
      }

      void addTransition(Byte symbol, BuilderState<V> state) {
         BuilderState<V> old = transitions.put(symbol & 0xFF,state);
         if (old != null && old != state) {
            throw new IllegalStateException("dfa already contains transition for: " + symbol);
         }
      }

      boolean isInitialState() {
         return initialState;
      }

      void setInitialState() {
         setInitialState(true);
      }

      void setInitialState(boolean initialState) {
         this.initialState = initialState;
      }

      boolean isFinalState() {
         return finalState;
      }

      void setFinalState() {
         setFinalState(true);
      }

      void setFinalState(boolean finalState) {
         setFinalState(finalState,null);
      }

      void setFinalState(@Nullable V value) {
         setFinalState(true,value);
      }

      void setFinalState(boolean finalState, @Nullable V value) {
         this.finalState = finalState;
         this.value = finalState ? value : null;
      }
   }

   public static final class Builder<V> {
      private final Set<BuilderState<V>> states;

      public Builder() {
         this.states = new HashSet<>();
      }

      public BuilderState<V> createState() {
         BuilderState<V> result = new BuilderState<V>();
         states.add(result);

         return result;
      }

      public RegexDfaByte<V> build() {
         Set<State<V>> states = new LinkedHashSet<>();
         State<V> initialState = null;

         Map<BuilderState<V>,State<V>> stateMapping = new IdentityHashMap<>();
         for (BuilderState<V> state : this.states) {
            State<V> st = convert(state);

            states.add(st);
            if (st.isInitialState()) {
               if (initialState != null) {
                  throw new IllegalStateException("dfa cannot have more than one initial state");
               }

               initialState = st;
            }

            stateMapping.put(state,st);
         }

         for (BuilderState<V> state : this.states) {
            convertTransitions(state, stateMapping);
         }

         if (initialState == null) {
            throw new IllegalStateException("dfa must have an initial state");
         }

         return new RegexDfaByte<>(initialState, states);
      }

      private static <V> State<V> convert(BuilderState<V> state) {
         State.Type type;
         if (state.isInitialState() && state.isFinalState()) {
            type = State.Type.INITIALFINAL;
         } else if (state.isInitialState()) {
            type = State.Type.INITIAL;
         } else if (state.isFinalState()) {
            type = State.Type.FINAL;
         } else {
            type = State.Type.NORMAL;
         }

         return new State<>(type,state.value);
      }

      private static <V> void convertTransitions(BuilderState<V> state, Map<BuilderState<V>,State<V>> stateMapping) {
         TransitionTable<V> transitions = null;
         switch (state.transitions.size()) {
         case 0:
            transitions = (TransitionTable<V>)(TransitionTable<?>)EmptyTransitionTable.INSTANCE;
            break;

         case 1:
            Map.Entry<Integer,BuilderState<V>> entry = state.transitions.entrySet().iterator().next();
            State<V> st = stateMapping.get(entry.getValue());
            transitions = new SingletonTransitionTable<V>((byte)(int)entry.getKey(), st);
            break;

         default:
            transitions = examineTransitions(state, stateMapping);
            break;
         }

         State<V> st = stateMapping.get(state);

         st.setTransitions(transitions);
      }

      private static <V> TransitionTable<V> examineTransitions(BuilderState<V> state, Map<BuilderState<V>,State<V>> stateMapping) {
         TransitionRange<V> last = null;
         List<TransitionRange<V>> ranges = new ArrayList<>();
         for (Map.Entry<Integer,BuilderState<V>> entry : state.transitions.entrySet()) {
            int sym = entry.getKey();
            State<V> st = stateMapping.get(entry.getValue());

            if (last == null) {
               last = new TransitionRange<>(sym,sym,st);
               ranges.add(last);
               continue;
            }

            int nxt = last.upper + 1;
            if (sym == nxt && st == last.state) {
               last.upper = sym;
            } else {
               last = new TransitionRange<>(sym,sym,st);
               ranges.add(last);
            }
         }

         if (ranges.size() == 1) {
            return simpleTransitions(ranges.get(0));
         }

         if (ranges.size() < 128) {
            int i = 0;
            int[] lowers = new int[ranges.size()];
            TransitionTable<V>[] alternates = new TransitionTable[ranges.size()];
            for (TransitionRange<V> tr : ranges) {
               lowers[i] = tr.lower;
               alternates[i] = simpleTransitions(tr);
               i++;
            }
         
            return new AlternatesTransitionTable<V>(lowers, alternates);
         } else {
            int lower = ranges.get(0).lower;
            int upper = ranges.get(ranges.size()-1).upper;
            @SuppressWarnings("unchecked")
            State<V>[] states = new State[upper - lower + 1];
            for (TransitionRange<V> tr : ranges) {
               for (int symbol=tr.lower; symbol <= tr.upper; ++symbol) {
                  states[symbol - lower] = tr.state;
               }
            }

            return new LookupTransitionTable<>(states,lower);
         }
      }
   }

   private static <V> TransitionTable<V> simpleTransitions(TransitionRange<V> tr) {
      if (tr.lower == 0 && tr.upper == 255) {
         return new AllTransitionTable<>(tr.state);
      }

      if (tr.lower == tr.upper) {
         return new SingletonTransitionTable<>((byte)tr.lower, tr.state);
      }

      return new RangeTransitionTable<>(tr.lower,tr.upper,tr.state);
   }

   private static final class TransitionRange<V> {
      int lower;
      int upper;
      State<V> state;

      public TransitionRange(int lower, int upper, State<V> state) {
         this.lower = lower;
         this.upper = upper;
         this.state = state;
      }
   }
}


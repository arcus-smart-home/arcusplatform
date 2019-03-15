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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;

public class RegexNfa<A,V> {
   final Set<State<A,V>> states;
   final Set<State<A,V>> initialStates;

   private RegexNfa(Set<State<A,V>> initialStates, Set<State<A,V>> states) {
      this.initialStates = initialStates;
      this.states = states;
   }

   public static <A,V> Builder<A,V> builder() {
      return new Builder<>();
   }

   @SafeVarargs
   public static <A,V> RegexNfa<A,V> append(RegexNfa<A,V> r1, RegexNfa<A,V> r2, RegexNfa<A,V>... rs) {
      Set<State<A,V>> allStates = new HashSet<>();
      Set<State<A,V>> initStates = new HashSet<>();

      allStates.addAll(r1.states);
      allStates.addAll(r2.states);
      for (RegexNfa<A,V> r : rs) {
         allStates.addAll(r.states);
      }

      initStates.addAll(r1.initialStates);
      initStates.addAll(r2.initialStates);
      for (RegexNfa<A,V> r : rs) {
         initStates.addAll(r.initialStates);
      }

      return new RegexNfa<A,V>(initStates, allStates);
   }

   public static <A,V> RegexNfa<A,V> append(Iterable<RegexNfa<A,V>> rs) {
      Set<State<A,V>> allStates = new HashSet<>();
      Set<State<A,V>> initStates = new HashSet<>();

      for (RegexNfa<A,V> r : rs) {
         allStates.addAll(r.states);
      }

      for (RegexNfa<A,V> r : rs) {
         initStates.addAll(r.initialStates);
      }

      return new RegexNfa<A,V>(initStates, allStates);
   }

   public boolean matches(Iterable<A> input) {
      return matches(input.iterator());
   }

   public boolean matches(Iterator<A> input) {
      Matcher match = matcher();
      while (input.hasNext()) {
         if (match.process(input.next())) {
            break;
         }
      }

      return match.matched();
   }

   public Set<V> matching(Iterable<A> input) {
      return matching(input.iterator());
   }

   public Set<V> matching(Iterator<A> input) {
      Matcher match = matcher();
      while (input.hasNext()) {
         if (match.process(input.next())) {
            break;
         }
      }

      return match.matched()
         ? match.matches()
         : Collections.<V>emptySet();
   }

   public Matcher matcher() {
      return new Matcher();
   }

   public String toDotGraph() {
      StringBuilder bld = new StringBuilder();
      bld.append("digraph nfa {\n");

      int next = 0;
      Map<State<A,V>,String> nodeNames = new IdentityHashMap<>();
      for (State<A,V> state : states) {
         nodeNames.put(state,"s" + next++);
      }

      for (State<A,V> state : states) {
         String shape;
         switch (state.type) {
         case INITIALFINAL: shape = "doubleoctagon"; break;
         case INITIAL: shape = "doublecircle"; break;
         case FINAL: shape = "octagon"; break;
         default: shape = "circle"; break;
         }

         bld.append("    ")
            .append(nodeNames.get(state))
            .append(" [shape=")
            .append(shape)
            .append("];\n");
      }

      for (State<A,V> state : states) {
         for (Map.Entry<A,Set<State<A,V>>> entry : state.transitions.entrySet()) {
            String label = (entry.getKey() == null)
               ? "<&epsilon;>"
               : "\"" + entry.getKey() + "\"";

            for (State<A,V> target : entry.getValue()) {
               bld.append("    ")
                  .append(nodeNames.get(state))
                  .append(" -> ")
                  .append(nodeNames.get(target))
                  .append( " [label=")
                  .append(label)
                  .append("];\n");
            }
         }
      }

      bld.append("}");
      return bld.toString();
   }

   public final class Matcher {
      private Set<State<A,V>> current;

      public Matcher() {
         current = new HashSet<>(initialStates);
         addEpsilonTransitions(current,new HashSet<State<A,V>>());
      }

      public boolean process(A symbol) {
         Set<State<A,V>> updated = new HashSet<>();
         for (State<A,V> st : current) {
            Set<State<A,V>> trans = st.transitions.get(symbol);
            if (trans != null) {
               updated.addAll(trans);
            }
         }

         addEpsilonTransitions(updated,current);
         current = updated;
         return current.isEmpty();
      }

      public boolean matched() {
         for (State<A,V> st : current) {
            if (st.isFinalState()) {
               return true;
            }
         }

         return false;
      }

      public Set<V> matches() {
         Set<V> result = new HashSet<V>();
         for (State<A,V> st : current) {
            if (st.isFinalState()) {
               result.add(st.value);
            }
         }

         return result;
      }

      private void addEpsilonTransitions(Set<State<A,V>> current, Set<State<A,V>> tmp) {
         while (true) {
            tmp.clear();
            for (State<A,V> st : current) {
               addEpsilonTransitions(current,tmp,st);
            }

            if (tmp.isEmpty()) {
               break;
            }

            current.addAll(tmp);
         }
      }

      private void addEpsilonTransitions(Set<State<A,V>> current, Set<State<A,V>> tmp, State<A,V> from) {
         Set<State<A,V>> trans = from.transitions.get(null);
         if (trans != null) {
            for (State<A,V> dst : trans) {
               if (!current.contains(dst)) {
                  tmp.add(dst);
               }
            }
         }
      }
   }

   public static final class State<A,V> {
      private static enum Type { INITIALFINAL, INITIAL, FINAL, NORMAL };

      private final Type type;
      final Map<A,Set<State<A,V>>> transitions;
      final @Nullable V value;

      private State(Type type, @Nullable V value) {
         this.type = type;
         this.transitions = new HashMap<>();
         this.value = value;
      }

      boolean isInitialState() {
         return type == Type.INITIAL || type == Type.INITIALFINAL;
      }

      boolean isFinalState() {
         return type == Type.FINAL || type == Type.INITIALFINAL;
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Builder pattern implementation
   /////////////////////////////////////////////////////////////////////////////

   public static final class BuilderState<A,V> {
      private final Map<A,Set<BuilderState<A,V>>> transitions;
      private boolean initialState;
      private boolean finalState;
      private @Nullable V value;

      private BuilderState() {
         this.transitions = new HashMap<>();
      }

      void addTransition(@Nullable A symbol, BuilderState<A,V> state) {
         Set<BuilderState<A,V>> transitionsForSymbol = transitions.get(symbol);
         if (transitionsForSymbol == null) {
            transitionsForSymbol = new HashSet<>();
            transitions.put(symbol, transitionsForSymbol);
         }

         transitionsForSymbol.add(state);
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
         setFinalState(true,null);
      }

      void setFinalState(boolean finalState) {
         setFinalState(finalState,null);
      }

      void setFinalState(V value) {
         setFinalState(true,value);
      }

      void setFinalState(boolean finalState, V value) {
         this.finalState = finalState;
         this.value = finalState ? value : null;
      }
   }

   public static final class Builder<A,V> {
      private final Set<BuilderState<A,V>> states;

      public Builder() {
         this.states = new HashSet<>();
      }

      public BuilderState<A,V> createState() {
         BuilderState<A,V> result = new BuilderState<A,V>();
         states.add(result);

         return result;
      }

      public RegexNfa<A,V> build() {
         Set<State<A,V>> states = new HashSet<>();
         Set<State<A,V>> initialStates = new HashSet<>();

         Map<BuilderState<A,V>,State<A,V>> stateMapping = new IdentityHashMap<>();
         for (BuilderState<A,V> state : this.states) {
            State<A,V> st = convert(state);

            states.add(st);
            if (st.isInitialState()) {
               initialStates.add(st);
            }

            stateMapping.put(state,st);
         }

         for (BuilderState<A,V> state : this.states) {
            convertTransitions(state, stateMapping);
         }

         return new RegexNfa<A,V>(initialStates, states);
      }

      private static <A,V> State<A,V> convert(BuilderState<A,V> state) {
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

         return new State<A,V>(type, state.value);
      }

      private static <A,V> void convertTransitions(BuilderState<A,V> state, Map<BuilderState<A,V>,State<A,V>> stateMapping) {
         State<A,V> st = stateMapping.get(state);

         for (Map.Entry<A,Set<BuilderState<A,V>>> entry : state.transitions.entrySet()) {
            Set<State<A,V>> value = new HashSet<>();
            for (BuilderState<A,V> target : entry.getValue()) {
               value.add(stateMapping.get(target));
            }

            st.transitions.put(entry.getKey(), value);
         }
      }
   }
}


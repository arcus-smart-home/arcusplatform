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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.jdt.annotation.Nullable;

public class RegexDfa<A,V> {
   final State<A,V> initialState;
   final Set<State<A,V>> states;

   private RegexDfa(State<A,V> initialState, Set<State<A,V>> states) {
      this.initialState = initialState;
      this.states = states;
   }

   public static <A,V> Builder<A,V> builder() {
      return new Builder<>();
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

   @Nullable
   public V matching(Iterable<A> input) {
      return matching(input.iterator());
   }

   @Nullable
   public V matching(Iterator<A> input) {
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
         for (Map.Entry<A,State<A,V>> entry : state.transitions.entrySet()) {
            String label = (entry.getKey() == null)
               ? "<&epsilon;>"
               : "\"" + entry.getKey() + "\"";

            bld.append("    ")
               .append(nodeNames.get(state))
               .append(" -> ")
               .append(nodeNames.get(entry.getValue()))
               .append( " [label=")
               .append(label)
               .append("];\n");
         }
      }

      bld.append("}");
      return bld.toString();
   }

   public final class Matcher {
      private @Nullable State<A,V> current;

      public Matcher() {
         current = initialState;
      }

      public boolean process(A symbol) {
         State<A,V> cur = current;
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
   }

   public static final class State<A,V> {
      private static enum Type { INITIALFINAL, INITIAL, FINAL, NORMAL };

      private final Type type;
      final Map<A,State<A,V>> transitions;
      final @Nullable V value;

      private State(Type type, V value, Map<A,State<A,V>> transitions) {
         this.type = type;
         this.transitions = transitions;
         this.value = value;
      }

      boolean isInitialState() {
         return type == Type.INITIAL || type == Type.INITIALFINAL;
      }

      boolean isFinalState() {
         return type == Type.FINAL || type == Type.INITIALFINAL;
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

   /////////////////////////////////////////////////////////////////////////////
   // Builder pattern implementation
   /////////////////////////////////////////////////////////////////////////////

   public static final class BuilderState<A,V> {
      private final Map<A,BuilderState<A,V>> transitions;
      private boolean initialState;
      private boolean finalState;
      private @Nullable V value;

      private BuilderState() {
         this.transitions = new HashMap<>();
      }

      void addTransition(@Nullable A symbol, BuilderState<A,V> state) {
         BuilderState<A,V> old = transitions.put(symbol,state);
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

      public RegexDfa<A,V> build() {
         Set<State<A,V>> states = new HashSet<>();
         State<A,V> initialState = null;

         Map<BuilderState<A,V>,State<A,V>> stateMapping = new IdentityHashMap<>();
         for (BuilderState<A,V> state : this.states) {
            State<A,V> st = convert(state);

            states.add(st);
            if (st.isInitialState()) {
               if (initialState != null) {
                  throw new IllegalStateException("dfa cannot have more than one initial state");
               }

               initialState = st;
            }

            stateMapping.put(state,st);
         }

         for (BuilderState<A,V> state : this.states) {
            convertTransitions(state, stateMapping);
         }

         if (initialState == null) {
            throw new IllegalStateException("dfa must have an initial state");
         }

         return new RegexDfa<>(initialState, states);
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

         Map<A,State<A,V>> transitions;
         switch (state.transitions.size()) {
         case 0:
            transitions = Collections.emptyMap();
            break;

         case 1: case 2: case 3: case 4:
         case 5: case 6: case 7: case 8:
            transitions = new TreeMap<A,State<A,V>>();
            break;

         default:
            //transitions = new TreeMap<A,State<A,V>>();
            transitions = new HashMap<A,State<A,V>>();
            break;
         }

         return new State<>(type,state.value,transitions);
      }

      private static <A,V> void convertTransitions(BuilderState<A,V> state, Map<BuilderState<A,V>,State<A,V>> stateMapping) {
         State<A,V> st = stateMapping.get(state);

         for (Map.Entry<A,BuilderState<A,V>> entry : state.transitions.entrySet()) {
            st.transitions.put(entry.getKey(), stateMapping.get(entry.getValue()));
         }
      }
   }
}


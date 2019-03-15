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
package com.iris.driver.reflex;

import java.lang.reflect.Type;
import java.nio.ByteOrder;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.binary.Base64;
import org.eclipse.jdt.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import com.iris.io.json.JSON;
import com.iris.messages.MessageBody;
import com.iris.model.Version;
import com.iris.protocol.zigbee.ZclData;
import com.iris.regex.RegexDfaByte;

@Deprecated
public class ReflexDB {
   static enum Format {
      V1(1);

      private final int num;

      private Format(int num) {
         this.num = num;
      }
   }

   private static final Format LATEST = Format.V1;

   private final String version;
   private final Set<Driver> drivers;

   public ReflexDB(String version, Set<Driver> drivers) {
      this.version = version;
      this.drivers = ImmutableSet.copyOf(drivers);
   }

   public String getVersion() {
      return version;
   }

   public Set<Driver> getDrivers() {
      return drivers;
   }

   public static String toJson(ReflexDB db) {
      return ReflexJson.gson.toJson(db);
   }

   public static ReflexDB fromJson(String json) {
      return ReflexJson.gson.fromJson(json, ReflexDB.class);
   }

   public static ReflexDB fromJson(JsonObject json) {
      return ReflexJson.gson.fromJson(json, ReflexDB.class);
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((drivers == null) ? 0 : drivers.hashCode());
      result = prime * result + ((version == null) ? 0 : version.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj)
         return true;
      if (obj == null)
         return false;
      if (getClass() != obj.getClass())
         return false;
      ReflexDB other = (ReflexDB) obj;
      if (version == null) {
         if (other.version != null)
            return false;
      } else if (!version.equals(other.version))
         return false;
      if (drivers == null) {
         if (other.drivers != null)
            return false;
      } else if (!drivers.equals(other.drivers))
         return false;
      return true;
   }

   public static final class Driver {
      private final String name;
      private final Version version;
      private final String hash;
      private final long offlineTimeout;
      private final Set<String> capabilities;
      private final List<ReflexDefinition> reflexes;
      private final @Nullable ReflexDFA dfa;

      public Driver(String name, Version version, String hash, long offlineTimeout, Set<String> capabilities, List<ReflexDefinition> reflexes, @Nullable RegexDfaByte<List<ReflexAction>> dfa) {
         this(name, version, hash, offlineTimeout, capabilities, reflexes, (dfa == null) ? null : new ReflexDFA(dfa));
      }

      private Driver(String name, Version version, String hash, long offlineTimeout, Set<String> capabilities, List<ReflexDefinition> reflexes, @Nullable ReflexDFA dfa) {
         this.name = name;
         this.version = version;
         this.hash = hash;
         this.offlineTimeout = offlineTimeout;
         this.capabilities = capabilities;
         this.reflexes = reflexes;
         this.dfa = dfa;
      }

      public String getName() {
         return name;
      }

      public Version getVersion() {
         return version;
      }

      public String getHash() {
         return hash;
      }

      public long getOfflineTimeout() {
         return offlineTimeout;
      }

      public Set<String> getCapabilities() {
         return capabilities;
      }

      public List<ReflexDefinition> getReflexes() {
         return reflexes;
      }

      public @Nullable ReflexDFA getDfa() {
         return dfa;
      }
   }

   public static final class ReflexDFA {
      private final RegexDfaByte<List<ReflexAction>> dfa;

      public ReflexDFA(RegexDfaByte<List<ReflexAction>> dfa) {
         this.dfa = dfa;
      }

      public RegexDfaByte<List<ReflexAction>> getDfa() {
         return dfa;
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // DB Type Adapters
   //
   // The JSON serializer always produces JSON output in the latest format
   // while the JSON deserializer takes care to support all formats.
   /////////////////////////////////////////////////////////////////////////////
   
   private static final Type LIST_CAPS = (new TypeToken<List<String>>() {}).getType();
   private static final Type LIST_REFLEXES = (new TypeToken<List<ReflexDefinition>>() {}).getType();
   private static final Type REFLEX_DFA = (new TypeToken<ReflexDFA>() {}).getType();
   static final class ReflexDBAdapter implements JsonSerializer<ReflexDB>, JsonDeserializer<ReflexDB> {
      private ReflexDB parseV1(JsonObject rootDb, JsonDeserializationContext context) throws JsonParseException {
         String version = rootDb.get("ver").getAsString();
         Set<Driver> drivers = new HashSet<>();

         JsonArray db = rootDb.get("db").getAsJsonArray();
         for (JsonElement child : db) {
            JsonObject drvset = child.getAsJsonObject();

            String drvname = drvset.get("n").getAsString();
            JsonArray drvs = drvset.get("d").getAsJsonArray();
            for (JsonElement dchild : drvs) {
               JsonObject drv = dchild.getAsJsonObject();

               Version ver = Version.fromRepresentation(drv.get("v").getAsString());
               String hash = drv.get("h").getAsString();
               long offlineTimeout = drv.get("o").getAsLong();
               List<String> caps = context.deserialize(drv.get("c"), LIST_CAPS);
               List<ReflexDefinition> reflexes = context.deserialize(drv.get("r"), LIST_REFLEXES);

               ReflexDFA dfa = null;
               JsonElement jdfa = drv.get("d");
               if (jdfa != null && !jdfa.isJsonNull()) {
                  dfa = context.deserialize(jdfa, REFLEX_DFA);
               }

               Set<String> capabilities = ImmutableSet.copyOf(caps);
               drivers.add(new Driver(drvname, ver, hash, offlineTimeout, capabilities, reflexes, dfa));
            }
         }

         return new ReflexDB(version, drivers);
      }

      @Override
      public ReflexDB deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
         JsonObject rootDb = json.getAsJsonObject();
         int fmt = rootDb.get("fmt").getAsInt();
         if (fmt == Format.V1.num) {
            return parseV1(rootDb, context);
         }

         throw new JsonParseException("unknown hub local reflex db format: " + fmt);
      }

      @Override
      public JsonElement serialize(ReflexDB src, Type typeOfSrc, JsonSerializationContext context) {
         JsonObject rootDb = new JsonObject();
         rootDb.addProperty("fmt", LATEST.num);
         rootDb.addProperty("ver", src.version);

         Map<String,Set<Driver>> combined = new TreeMap<>();
         for (Driver driver : src.drivers) {
            try {
               Set<Driver> existing = combined.get(driver.getName());
               if (existing == null) {
                  existing = new TreeSet<Driver>(new Comparator<Driver>() {
                     @Override
                     public int compare(Driver l, Driver r) {
                        return l.getVersion().compareTo(r.getVersion());
                     }
                  });

                  combined.put(driver.getName(), existing);
               }

               existing.add(driver);
            } catch (Exception ex) {
               throw ex;
            }
         }

         JsonArray db = new JsonArray();
         for (Map.Entry<String,Set<Driver>> entry : combined.entrySet()) {
            JsonObject drvset = new JsonObject();
            drvset.addProperty("n", entry.getKey());

            JsonArray drvs = new JsonArray();
            drvset.add("d", drvs);

            for (Driver driver : entry.getValue()) {
               JsonObject drv = new JsonObject();
               drv.addProperty("v", driver.getVersion().getRepresentation());
               drv.addProperty("h", driver.getHash());
               drv.addProperty("o", driver.getOfflineTimeout());
               drv.add("c", context.serialize(ImmutableList.copyOf(driver.getCapabilities()), LIST_CAPS));
               drv.add("r", context.serialize(driver.getReflexes(), LIST_REFLEXES));

               if (driver.getDfa() != null) {
                  drv.add("d", context.serialize(driver.getDfa(), REFLEX_DFA));
               }

               drvs.add(drv);
            }

            db.add(drvset);
         }

         rootDb.add("db", db);
         return rootDb;
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Reflex Json Type Adapters
   /////////////////////////////////////////////////////////////////////////////
   
   private static final class ReflexDefinitionAdapter implements JsonSerializer<ReflexDefinition>, JsonDeserializer<ReflexDefinition> {
      @Override
      public ReflexDefinition deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
         JsonObject obj = json.getAsJsonObject();
         JsonElement ms = obj.get("m");
         JsonElement as = obj.get("a");
         return new ReflexDefinition(convertMatches(ms,context), convertActions(as,context));
      }

      @Override
      public JsonElement serialize(ReflexDefinition src, Type typeOfSrc, JsonSerializationContext context) {
         JsonObject json = new JsonObject();
         json.add("m", convertMatches(src.getMatchers(), context));
         json.add("a", convertActions(src.getActions(), context));
         return json;
      }
   }

   private static final class ReflexDFAAdapter implements JsonSerializer<ReflexDFA>, JsonDeserializer<ReflexDFA> {
      private RegexDfaByte.TransitionTable<List<ReflexAction>> toTransitionTable(Map<String,RegexDfaByte.State<List<ReflexAction>>> names, @Nullable JsonElement element) {
         if (element == null || element.isJsonNull()) {
            return (RegexDfaByte.TransitionTable<List<ReflexAction>>)(RegexDfaByte.TransitionTable<?>)RegexDfaByte.EmptyTransitionTable.INSTANCE;
         }

         if (!element.isJsonObject()) {
            throw new IllegalStateException("cannot deserialize regex dfa");
         }

         int i;
         RegexDfaByte.State<List<ReflexAction>> st;
         JsonObject elem = element.getAsJsonObject();
         String ty = elem.get("t").getAsString();
         switch (ty) {
         case "SG":
            int sym = elem.get("y").getAsInt();
            st = getState(names, elem.get("s").getAsString());
            return new RegexDfaByte.SingletonTransitionTable<List<ReflexAction>>((byte)sym, st);

         case "RA":
            int lw = elem.get("l").getAsInt();
            int up = elem.get("u").getAsInt();
            st = getState(names, elem.get("s").getAsString());
            return new RegexDfaByte.RangeTransitionTable<List<ReflexAction>>(lw, up, st);

         case "AL":
            st = getState(names, elem.get("s").getAsString());
            return new RegexDfaByte.AllTransitionTable<List<ReflexAction>>(st);

         case "LU":
            int off = elem.get("o").getAsInt();
            JsonArray jsts = elem.get("s").getAsJsonArray();

            i = 0;
            RegexDfaByte.State<List<ReflexAction>>[] sts = new RegexDfaByte.State[jsts.size()];
            for (JsonElement jst : jsts) {
               sts[i++] = getState(names, jst.getAsString());
            }
            return new RegexDfaByte.LookupTransitionTable<List<ReflexAction>>(sts, off);

         case "AT":
            JsonArray lws = elem.get("l").getAsJsonArray();
            JsonArray als = elem.get("a").getAsJsonArray();

            i = 0;
            int[] lowers = new int[lws.size()];
            for (JsonElement el : lws) {
               lowers[i++] = el.getAsInt();
            }

            i = 0;
            RegexDfaByte.TransitionTable<List<ReflexAction>>[] tts = new RegexDfaByte.TransitionTable[als.size()];
            for (JsonElement el : als) {
               tts[i++] = toTransitionTable(names, el);
            }

            return new RegexDfaByte.AlternatesTransitionTable<List<ReflexAction>>(lowers, tts);

         default:
            throw new IllegalStateException("cannot deserialize regex dfa of type: " + ty);
         }
      }

      private @Nullable JsonElement fromTransitionTable(Map<RegexDfaByte.State<List<ReflexAction>>,String> names, RegexDfaByte.TransitionTable<List<ReflexAction>> transitions) {
         if (((RegexDfaByte.TransitionTable)transitions) == RegexDfaByte.EmptyTransitionTable.INSTANCE) {
            // don't serialize into db
            return null;
         } else if (transitions instanceof RegexDfaByte.SingletonTransitionTable) {
            RegexDfaByte.SingletonTransitionTable<List<ReflexAction>> tt = (RegexDfaByte.SingletonTransitionTable<List<ReflexAction>>)transitions;

            JsonObject tr = new JsonObject();
            tr.addProperty("t", "SG");
            tr.addProperty("y", tt.getSymbol() & 0xFF);
            tr.addProperty("s", getStateName(names, tt.getState()));
            return tr;
         } else if (transitions instanceof RegexDfaByte.RangeTransitionTable) {
            RegexDfaByte.RangeTransitionTable<List<ReflexAction>> tt = (RegexDfaByte.RangeTransitionTable<List<ReflexAction>>)transitions;

            JsonObject tr = new JsonObject();
            tr.addProperty("t", "RA");
            tr.addProperty("l", tt.getLower());
            tr.addProperty("u", tt.getUpper());
            tr.addProperty("s", getStateName(names, tt.getState()));
            return tr;
         } else if (transitions instanceof RegexDfaByte.AllTransitionTable) {
            RegexDfaByte.AllTransitionTable<List<ReflexAction>> tt = (RegexDfaByte.AllTransitionTable<List<ReflexAction>>)transitions;

            JsonObject tr = new JsonObject();
            tr.addProperty("t", "AL");
            tr.addProperty("s", getStateName(names, tt.getState()));
            return tr;
         } else if (transitions instanceof RegexDfaByte.LookupTransitionTable) {
            RegexDfaByte.LookupTransitionTable<List<ReflexAction>> tt = (RegexDfaByte.LookupTransitionTable<List<ReflexAction>>)transitions;

            JsonObject tr = new JsonObject();
            tr.addProperty("t", "LU");
            tr.addProperty("o", tt.getOffset());

            JsonArray lst = new JsonArray();
            for (RegexDfaByte.State<List<ReflexAction>> lstate : tt.getStates()) {
               lst.add(new JsonPrimitive(getStateName(names,lstate)));
            }

            tr.add("s", lst);
            return tr;
         } else if (transitions instanceof RegexDfaByte.AlternatesTransitionTable) {
            RegexDfaByte.AlternatesTransitionTable<List<ReflexAction>> tt = (RegexDfaByte.AlternatesTransitionTable<List<ReflexAction>>)transitions;

            JsonObject tr = new JsonObject();
            tr.addProperty("t", "AT");

            JsonArray lws = new JsonArray();
            for (int lw : tt.getLowers()) {
               lws.add(new JsonPrimitive(lw));
            }

            JsonArray als = new JsonArray();
            for (RegexDfaByte.TransitionTable<List<ReflexAction>> att : tt.getAlternates()) {
               als.add(fromTransitionTable(names,att));
            }

            tr.add("l", lws);
            tr.add("a", als);
            return tr;
         } else if (transitions != null) {
            throw new IllegalStateException("unknown transition table type: " + transitions.getClass());
         } else {
            return null;
         }
      }

      @Override
      public ReflexDFA deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
         JsonObject obj = json.getAsJsonObject();

         Map<String,RegexDfaByte.State<List<ReflexAction>>> names = new LinkedHashMap<>();
         for (Map.Entry<String,JsonElement> jst : obj.entrySet()) {
            String name = jst.getKey();
            JsonObject ost = jst.getValue().getAsJsonObject();
            JsonElement oval = ost.get("a");

            List<ReflexAction> actions = ImmutableList.of();
            if (oval != null && !oval.isJsonNull()) {
               actions = convertActions(oval, context);
            }

            RegexDfaByte.State.Type type = getStateType(name);
            RegexDfaByte.State<List<ReflexAction>> st = new RegexDfaByte.State<List<ReflexAction>>(type,actions);
            names.put(name, st);
         }

         RegexDfaByte.State<List<ReflexAction>> initialState = null;
         for (Map.Entry<String,JsonElement> jst : obj.entrySet()) {
            String name = jst.getKey();
            JsonObject ost = jst.getValue().getAsJsonObject();

            RegexDfaByte.State<List<ReflexAction>> st = names.get(name);
            RegexDfaByte.TransitionTable<List<ReflexAction>> tt = toTransitionTable(names, ost.get("t"));
            st.setTransitions(tt);

            if (st.isInitialState()) {
               if (initialState != null) {
                  throw new IllegalArgumentException("regex dfa with multiple initial states");
               } else {
                  initialState = st;
               }
            }
         }

         if (initialState == null) {
            throw new IllegalArgumentException("regex dfa with no initial state");
         }

         return new ReflexDFA(new RegexDfaByte<List<ReflexAction>>(initialState, ImmutableSet.copyOf(names.values())));
      }

      @Override
      public JsonElement serialize(ReflexDFA src, Type typeOfSrc, JsonSerializationContext context) {
         Map<RegexDfaByte.State<List<ReflexAction>>,String> names = new IdentityHashMap<>();

         int i = 0;
         for (RegexDfaByte.State<List<ReflexAction>> state : src.dfa.getStates()) {
            addStateName(names, state, i++);
         }

         JsonObject json = new JsonObject();
         for (RegexDfaByte.State<List<ReflexAction>> state : src.dfa.getStates()) {
            JsonObject st = new JsonObject();

            List<ReflexAction> actions = state.getValue();
            if (actions != null && !actions.isEmpty()) {
               st.add("a", convertActions(actions, context));
            }

            RegexDfaByte.TransitionTable<List<ReflexAction>> transitions = state.getTransitions();
            st.add("t", fromTransitionTable(names,transitions));

            String name = names.get(state);
            json.add(name, st);
         }

         return json;
      }

      private <V> void addStateName(Map<RegexDfaByte.State<V>,String> names, RegexDfaByte.State<V> state, int id) {
         names.put(state, getStateName(state,id));
      }

      private RegexDfaByte.State.Type getStateType(String name) {
         if (name.startsWith("if")) {
            return RegexDfaByte.State.Type.INITIALFINAL;
         } else if (name.startsWith("is")) {
            return RegexDfaByte.State.Type.INITIAL;
         } else if (name.startsWith("fs")) {
            return RegexDfaByte.State.Type.FINAL;
         } else if (name.startsWith("st")) {
            return RegexDfaByte.State.Type.NORMAL;
         } else {
            throw new IllegalArgumentException("unknown state type for state: " + name);
         }
      }

      private String getStateName(RegexDfaByte.State<?> state, int id) {
         if (state.isInitialState() && state.isFinalState()) {
            return "if" + id;
         } else if (state.isInitialState()) {
            return "is" + id;
         } else if (state.isFinalState()) {
            return "fs" + id;
         } else {
            return "st" + id;
         }
      }

      private <V> String getStateName(Map<RegexDfaByte.State<V>,String> names, RegexDfaByte.State<V> state) {
         String name = names.get(state);
         if (name == null || name.isEmpty()) {
            throw new IllegalStateException("regex dfa state with no name: " + state);
         }

         return name;
      }

      private <V> RegexDfaByte.State<V> getState(Map<String,RegexDfaByte.State<V>> names, String name) {
         RegexDfaByte.State<V> state = names.get(name);
         if (state == null) {
            throw new IllegalStateException("no regex dfa state for name '" + name + "': " + new TreeSet<>(names.keySet()));
         }

         return state;
      }
   }

   private static final class ReflexMatchAdapter implements JsonSerializer<ReflexMatch>, JsonDeserializer<ReflexMatch> {
      @Override
      public ReflexMatch deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
         return convertMatch(json.getAsJsonObject(), context);
      }

      @Override
      public JsonElement serialize(ReflexMatch src, Type typeOfSrc, JsonSerializationContext context) {
         return convertMatch(src,context);
      }
   }
   
   private static final class ReflexActionAdapter implements JsonSerializer<ReflexAction>, JsonDeserializer<ReflexAction> {
      @Override
      public ReflexAction deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
         return convertAction(json.getAsJsonObject(), context);
      }

      @Override
      public JsonElement serialize(ReflexAction src, Type typeOfSrc, JsonSerializationContext context) {
         return convertAction(src,context);
      }
   }

   private static JsonArray convertMatches(List<ReflexMatch> matches, JsonSerializationContext context) {
      JsonArray jmatches = new JsonArray();
      for (ReflexMatch match : matches) {
         jmatches.add(context.serialize(match, ReflexMatch.class));
      }

      return jmatches;
   }

   private static JsonArray convertActions(List<ReflexAction> actions, JsonSerializationContext context) {
      JsonArray jactions = new JsonArray();
      for (ReflexAction action : actions) {
         jactions.add(context.serialize(action, ReflexAction.class));
      }

      return jactions;
   }

   private static final Type LIST_OF_MATCHES = (new TypeToken<List<ReflexMatch>>() {}).getType();
   private static List<ReflexMatch> convertMatches(JsonElement json, JsonDeserializationContext context) {
      return context.deserialize(json, LIST_OF_MATCHES);
   }

   private static final Type LIST_OF_ACTIONS = (new TypeToken<List<ReflexAction>>() {}).getType();
   private static List<ReflexAction> convertActions(JsonElement json, JsonDeserializationContext context) {
      return context.deserialize(json, LIST_OF_ACTIONS);
   }

   private static JsonObject convertMatch(ReflexMatch match, JsonSerializationContext context) {
      if (match instanceof ReflexMatchAttribute) {
         return convertAt((ReflexMatchAttribute)match, context);
      } else if (match instanceof ReflexMatchLifecycle) {
         return convertLc((ReflexMatchLifecycle)match, context);
      } else if (match instanceof ReflexMatchMessage) {
         return convertMg((ReflexMatchMessage)match, context);
      } else if (match instanceof ReflexMatchPollRate) {
         return convertPr((ReflexMatchPollRate)match, context);
      } else if (match instanceof ReflexMatchZigbeeAttribute) {
         return convertZa((ReflexMatchZigbeeAttribute)match, context);
      } else if (match instanceof ReflexMatchZigbeeIasZoneStatus) {
         return convertZz((ReflexMatchZigbeeIasZoneStatus)match, context);
      } else if (match instanceof ReflexMatchAlertmeLifesign) {
         return convertAl((ReflexMatchAlertmeLifesign)match, context);
      } else {
         throw new RuntimeException("cannot convert unknown hub local reflex match: " + match);
      }
   }

   private static ReflexMatch convertMatch(JsonObject match, JsonDeserializationContext context) {
      switch (match.get("t").getAsString()) {
      case "AT": return convertAt(match,context);
      case "LC": return convertLc(match,context);
      case "MG": return convertMg(match,context);
      case "PR": return convertPr(match,context);
      case "ZA": return convertZa(match,context);
      case "ZZ": return convertZz(match,context);
      case "AL": return convertAl(match,context);
      default: throw new RuntimeException("unknown matcher type: " + match);
      }
   }

   private static JsonObject convertAt(ReflexMatchAttribute at, JsonSerializationContext context) {
      JsonObject json = new JsonObject();
      json.addProperty("t", "AT");
      json.addProperty("a", at.getAttr());
      json.add("v", context.serialize(at.getValue()));
      return json;
   }

   private static JsonObject convertLc(ReflexMatchLifecycle lc, JsonSerializationContext context) {
      JsonObject json = new JsonObject();
      json.addProperty("t", "LC");
      json.addProperty("l", lc.getType().name());
      return json;
   }

   private static JsonObject convertMg(ReflexMatchMessage mg, JsonSerializationContext context) {
      MessageBody msg = mg.getMessage();
      MessageBody roundTrip = JSON.fromJson(JSON.toJson(msg), MessageBody.class);

      JsonObject json = new JsonObject();
      json.addProperty("t", "MG");
      json.add("m", context.serialize(roundTrip));
      return json;
   }

   private static JsonObject convertPr(ReflexMatchPollRate poll, JsonSerializationContext context) {
      JsonObject json = new JsonObject();
      json.addProperty("t", "PR");
      json.addProperty("p", poll.getUnit().toNanos(poll.getTime()));
      return json;
   }

   private static JsonObject convertZa(ReflexMatchZigbeeAttribute za, JsonSerializationContext context) {
      try {
         JsonObject json = new JsonObject();
         json.addProperty("t", "ZA");
         json.addProperty("r", za.getType().name());
         json.addProperty("p", za.getProfile());
         json.addProperty("e", za.getEndpoint());
         json.addProperty("c", za.getCluster());
         json.addProperty("a", za.getAttr());

         ZclData vl = za.getValue();
         if (vl != null) {
            json.addProperty("v", Base64.encodeBase64String(vl.toBytes(ByteOrder.LITTLE_ENDIAN)));
         }

         return json;
      } catch (Exception ex) {
         throw new RuntimeException(ex);
      }
   }

   private static JsonObject convertZz(ReflexMatchZigbeeIasZoneStatus zz, JsonSerializationContext context) {
      try {
         JsonObject json = new JsonObject();
         json.addProperty("t", "ZZ");
         json.addProperty("r", zz.getType().name());
         json.addProperty("p", zz.getProfile());
         json.addProperty("e", zz.getEndpoint());
         json.addProperty("c", zz.getCluster());
         json.addProperty("s", zz.getSetMask());
         json.addProperty("u", zz.getClrMask());
         json.addProperty("m", zz.getMaxChangeDelay());

         return json;
      } catch (Exception ex) {
         throw new RuntimeException(ex);
      }
   }

   private static JsonObject convertAl(ReflexMatchAlertmeLifesign al, JsonSerializationContext context) {
      try {
         JsonObject json = new JsonObject();
         json.addProperty("t", "AL");
         json.addProperty("p", al.getProfile());
         json.addProperty("e", al.getEndpoint());
         json.addProperty("c", al.getCluster());
         json.addProperty("s", al.getSetMask());
         json.addProperty("u", al.getClrMask());

         return json;
      } catch (Exception ex) {
         throw new RuntimeException(ex);
      }
   }

   private static ReflexMatchAttribute convertAt(JsonObject at, JsonDeserializationContext context) {
      String attr = at.get("a").getAsString();
      Object val = context.deserialize(at.get("v"), Object.class);
      return new ReflexMatchAttribute(attr,val);
   }

   private static ReflexMatchLifecycle convertLc(JsonObject lc, JsonDeserializationContext context) {
      return new ReflexMatchLifecycle(ReflexMatchLifecycle.Type.valueOf(lc.get("l").getAsString()));
   }

   private static ReflexMatchMessage convertMg(JsonObject mg, JsonDeserializationContext context) {
      MessageBody msg = context.deserialize(mg.get("m"), MessageBody.class);
      return new ReflexMatchMessage(msg);
   }

   private static ReflexMatchPollRate convertPr(JsonObject pr, JsonDeserializationContext context) {
      return new ReflexMatchPollRate(pr.get("p").getAsLong(), TimeUnit.NANOSECONDS); 
   }

   private static ReflexMatchZigbeeAttribute convertZa(JsonObject za, JsonDeserializationContext context) {
      ReflexMatchZigbeeAttribute.Type type = ReflexMatchZigbeeAttribute.Type.valueOf(za.get("r").getAsString());
      int pr = za.get("p").getAsInt();
      int ep = za.get("e").getAsInt();
      int cl = za.get("c").getAsInt();
      int at = za.get("a").getAsInt();

      ZclData vl = null;
      JsonElement jvl = za.get("v");
      if (jvl != null && !jvl.isJsonNull()) {
         vl = ZclData.serde().fromBytes(ByteOrder.LITTLE_ENDIAN, Base64.decodeBase64(jvl.getAsString()));
      }

      return new ReflexMatchZigbeeAttribute(type, pr, ep, cl, at, vl, null, null);
   }

   private static ReflexMatchZigbeeIasZoneStatus convertZz(JsonObject zz, JsonDeserializationContext context) {
      ReflexMatchZigbeeIasZoneStatus.Type type = ReflexMatchZigbeeIasZoneStatus.Type.valueOf(zz.get("r").getAsString());
      int pr = zz.get("p").getAsInt();
      int ep = zz.get("e").getAsInt();
      int cl = zz.get("c").getAsInt();
      int st = zz.get("s").getAsInt();
      int us = zz.get("u").getAsInt();
      int mx = zz.get("m").getAsInt();

      return new ReflexMatchZigbeeIasZoneStatus(type, pr, ep, cl, st, us, mx, null, null);
   }

   private static ReflexMatchAlertmeLifesign convertAl(JsonObject al, JsonDeserializationContext context) {
      int pr = al.get("p").getAsInt();
      int ep = al.get("e").getAsInt();
      int cl = al.get("c").getAsInt();
      int st = al.get("s").getAsInt();
      int us = al.get("u").getAsInt();

      return new ReflexMatchAlertmeLifesign(pr, ep, cl, st, us);
   }

   private static JsonObject convertAction(ReflexAction action, JsonSerializationContext context) {
      if (action instanceof ReflexActionDelay) {
         return convertDl((ReflexActionDelay)action, context);
      } else if (action instanceof ReflexActionOrdered) {
         return convertOr((ReflexActionOrdered)action, context);
      } else if (action instanceof ReflexActionSendPlatform) {
         return convertPl((ReflexActionSendPlatform)action, context);
      } else if (action instanceof ReflexActionSendProtocol) {
         return convertPc((ReflexActionSendProtocol)action, context);
      } else if (action instanceof ReflexActionSetAttribute) {
         return convertSa((ReflexActionSetAttribute)action, context);
      } else if (action instanceof ReflexActionForward) {
         return convertFw((ReflexActionForward)action, context);
      } else if (action instanceof ReflexActionBuiltin) {
         return convertBi((ReflexActionBuiltin)action, context);
      } else if (action instanceof ReflexActionAlertmeLifesign) {
         return convertAl((ReflexActionAlertmeLifesign)action, context);
      } else if (action instanceof ReflexActionZigbeeIasZoneEnroll) {
         return convertZz((ReflexActionZigbeeIasZoneEnroll)action, context);
      } else {
         throw new RuntimeException("cannot convert unknown hub local reflex action: " + action);
      }
   }

   private static ReflexAction convertAction(JsonObject action , JsonDeserializationContext context) {
      switch (action.get("t").getAsString()) {
      case "DL": return convertDl(action,context);
      case "OR": return convertOr(action,context);
      case "PL": return convertPl(action,context);
      case "PC": return convertPc(action,context);
      case "SA": return convertSa(action,context);
      case "FW": return convertFw(action,context);
      case "BI": return convertBi(action,context);
      case "AL": return convertAlAction(action,context);
      case "ZZ": return convertZzAction(action,context);
      default: throw new RuntimeException("unknown action type: " + action);
      }
   }

   private static JsonObject convertDl(ReflexActionDelay delay, JsonSerializationContext context) {
      JsonObject json = new JsonObject();
      json.addProperty("t", "DL");
      json.addProperty("d", delay.getUnit().toNanos(delay.getTime()));
      json.add("a", convertActions(delay.getActions(),context));
      return json;
   }

   private static ReflexActionDelay convertDl(JsonObject dl, JsonDeserializationContext context) {
      long delay = dl.get("d").getAsLong();
      ReflexActionDelay result = new ReflexActionDelay(delay, TimeUnit.NANOSECONDS);
      result.setActions(convertActions(dl.get("a"),context));
      return result;
   }

   private static JsonObject convertOr(ReflexActionOrdered order, JsonSerializationContext context) {
      JsonObject json = new JsonObject();
      json.addProperty("t", "OR");
      json.add("a", convertActions(order.getActions(),context));
      return json;
   }

   private static ReflexActionOrdered convertOr(JsonObject or, JsonDeserializationContext context) {
      ReflexActionOrdered result = new ReflexActionOrdered();
      result.setActions(convertActions(or.get("a"),context));
      return result;
   }

   private static final Type SEND_PLATFORM_ARGS = (new TypeToken<Map<String,Object>>() {}).getType();
   private static JsonObject convertPl(ReflexActionSendPlatform plat, JsonSerializationContext context) {
      JsonObject json = new JsonObject();
      json.addProperty("t", "PL");
      json.addProperty("m", plat.getEvent());
      json.add("a", context.serialize(plat.getArgs(), SEND_PLATFORM_ARGS));
      return json;
   }

   private static ReflexActionSendPlatform convertPl(JsonObject pl, JsonDeserializationContext context) {
      String evt = pl.get("m").getAsString();
      Map<String,Object> args = context.deserialize(pl.get("a"), SEND_PLATFORM_ARGS);
      return new ReflexActionSendPlatform(evt, args, false);
   }

   private static JsonObject convertPc(ReflexActionSendProtocol pr, JsonSerializationContext context) {
      JsonObject json = new JsonObject();
      json.addProperty("t", "PC");
      switch (pr.getType()) {
      case ZWAVE:
         json.addProperty("p", "ZW");
         break;
      case ZIGBEE:
         json.addProperty("p", "ZB");
         break;
      default:
         throw new RuntimeException("unknown send protocol type: " + pr.getType());
      }

      json.addProperty("m", Base64.encodeBase64String(pr.getMessage()));
      return json;
   }

   private static ReflexActionSendProtocol convertPc(JsonObject pr, JsonDeserializationContext context) {
      ReflexActionSendProtocol.Type typ;
      switch (pr.get("p").getAsString()) {
      case "ZW":
         typ = ReflexActionSendProtocol.Type.ZWAVE;
         break;
      case "ZB":
         typ = ReflexActionSendProtocol.Type.ZIGBEE;
         break;
      default:
         throw new RuntimeException("unknown send protocol type: " + pr.get("p"));
      }

      byte[] msg = Base64.decodeBase64(pr.get("m").getAsString());
      return new ReflexActionSendProtocol(typ, msg);
   }

   private static JsonObject convertSa(ReflexActionSetAttribute attr, JsonSerializationContext context) {
      JsonObject json = new JsonObject();
      json.addProperty("t", "SA");
      json.addProperty("a", attr.getAttr());
      json.add("v", context.serialize(attr.getValue()));
      return json;
   }

   private static ReflexActionSetAttribute convertSa(JsonObject sa, JsonDeserializationContext context) {
      String attr = sa.get("a").getAsString();
      Object val = context.deserialize(sa.get("v"), Object.class);
      return new ReflexActionSetAttribute(attr,val);
   }

   private static JsonObject convertFw(ReflexActionForward fw, JsonSerializationContext context) {
      JsonObject json = new JsonObject();
      json.addProperty("t", "FW");
      return json;
   }

   private static ReflexActionForward convertFw(JsonObject fw, JsonDeserializationContext context) {
      return new ReflexActionForward();
   }

   private static JsonObject convertBi(ReflexActionBuiltin bi, JsonSerializationContext context) {
      JsonObject json = new JsonObject();
      json.addProperty("t", "BI");
      return json;
   }

   private static ReflexActionBuiltin convertBi(JsonObject bi, JsonDeserializationContext context) {
      return new ReflexActionBuiltin();
   }

   private static JsonObject convertAl(ReflexActionAlertmeLifesign al, JsonSerializationContext context) {
      JsonObject json = new JsonObject();
      json.addProperty("t", "AL");
      json.addProperty("a", al.getType().name());

      if (al.getMinimum() != null) {
         json.addProperty("m", al.getMinimum());
      }

      if (al.getNominal() != null) {
         json.addProperty("n", al.getNominal());
      }

      return json;
   }

   private static ReflexActionAlertmeLifesign convertAlAction(JsonObject al, JsonDeserializationContext context) {
      ReflexActionAlertmeLifesign.Type a = ReflexActionAlertmeLifesign.Type.valueOf(al.get("a").getAsString());

      Double m = null;
      Double n = null;

      JsonElement me = al.get("m");
      if (me != null && !me.isJsonNull()) {
         m = me.getAsDouble();
      }

      JsonElement ne = al.get("n");
      if (ne != null && !ne.isJsonNull()) {
         n = ne.getAsDouble();
      }

      if (m != null && n != null) {
         return new ReflexActionAlertmeLifesign(a,m,n);
      } else {
         return new ReflexActionAlertmeLifesign(a);
      }
   }

   private static JsonObject convertZz(ReflexActionZigbeeIasZoneEnroll zz, JsonSerializationContext context) {
      JsonObject json = new JsonObject();
      json.addProperty("t", "ZZ");
      json.addProperty("p", zz.getProfileId());
      json.addProperty("e", zz.getEndpointId());
      json.addProperty("c", zz.getClusterId());
      return json;
   }

   private static ReflexActionZigbeeIasZoneEnroll convertZzAction(JsonObject zz, JsonDeserializationContext context) {
      int pr = zz.get("p").getAsInt();
      int ep = zz.get("e").getAsInt();
      int cl = zz.get("c").getAsInt();
      return new ReflexActionZigbeeIasZoneEnroll(ep, pr, cl);
   }
}


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

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.mutable.MutableInt;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.iris.driver.reflex.ReflexAction;
import com.iris.driver.reflex.ReflexActionAlertmeLifesign;
import com.iris.driver.reflex.ReflexActionDelay;
import com.iris.driver.reflex.ReflexActionForward;
import com.iris.driver.reflex.ReflexActionOrdered;
import com.iris.driver.reflex.ReflexActionSendPlatform;
import com.iris.driver.reflex.ReflexActionSendProtocol;
import com.iris.driver.reflex.ReflexActionSetAttribute;
import com.iris.driver.reflex.ReflexActionZigbeeIasZoneEnroll;
import com.iris.driver.reflex.ReflexDefinition;
import com.iris.driver.reflex.ReflexDriverDFA;
import com.iris.driver.reflex.ReflexMatch;
import com.iris.driver.reflex.ReflexMatchAlertmeLifesign;
import com.iris.driver.reflex.ReflexMatchAttribute;
import com.iris.driver.reflex.ReflexMatchLifecycle;
import com.iris.driver.reflex.ReflexMatchMessage;
import com.iris.driver.reflex.ReflexMatchPollRate;
import com.iris.driver.reflex.ReflexMatchZigbeeAttribute;
import com.iris.driver.reflex.ReflexMatchZigbeeIasZoneStatus;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.capability.DevicePowerCapability;
import com.iris.messages.capability.TemperatureCapability;
import com.iris.model.Version;
import com.iris.protoc.runtime.ProtocUtil;
import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.ZWaveCommandClassFrame;
import com.iris.protocol.zigbee.ZclData;
import com.iris.protocol.zigbee.msg.ZigbeeMessage;
import com.iris.protocol.zigbee.zcl.IasZone;
import com.iris.protocol.zwave.Protocol;
import com.iris.regex.RegexDfaByte;

public class ReflexDriver {
   private static final Logger log = LoggerFactory.getLogger(ReflexDriver.class);

   public static final int V0 = 0;
   public static final int V1 = 1;
   public static final int V2 = 2;
   public static final int CURRENT_VERSION = V2;

   private final String driver;
   private final Version version;
   private final String hash;
   private final int reflexesHashCode;
   private final Set<String> capabilities;
   private final boolean degraded;

   private final List<Action> onAdded;
   private final List<Action> onConnected;
   private final List<Action> onDisconnected;
   private final List<Action> onRemoved;
   private final List<ProtocolMatch> protocolMatchers;
   private final Map<String,List<PlatformMatch>> platformMatchers;
   private final int maxReflexVersion;

   public ReflexDriver(
      String driver,
      Version version,
      String hash,
      int reflexesHashCode,
      Set<String> capabilities,
      List<Action> onAdded,
      List<Action> onConnected,
      List<Action> onDisconnected,
      List<Action> onRemoved,
      List<ProtocolMatch> protocolMatchers,
      Map<String,List<PlatformMatch>> platformMatchers,
      boolean degraded) {
      this.driver = driver;
      this.version = version;
      this.hash = hash;
      this.capabilities = capabilities;
      this.reflexesHashCode = reflexesHashCode;
      this.onAdded = onAdded;
      this.onConnected = onConnected;
      this.onDisconnected = onDisconnected;
      this.onRemoved = onAdded;
      this.protocolMatchers = protocolMatchers;
      this.platformMatchers = platformMatchers;
      this.degraded = degraded;

      int max = getMaxProtocolReflexVersion(protocolMatchers);
      max = Math.max(max,getMaxPlatformReflexVersion(platformMatchers));
      max = Math.max(max,getMaxReflexVersion(onAdded));
      max = Math.max(max,getMaxReflexVersion(onConnected));
      max = Math.max(max,getMaxReflexVersion(onDisconnected));
      max = Math.max(max,getMaxReflexVersion(onRemoved));
      this.maxReflexVersion = max;

      setupDebugging(protocolMatchers);
      setupDebugging(platformMatchers);
   }

   private static int getMaxPlatformReflexVersion(Map<String,List<PlatformMatch>> matchers) {
      int max = 0;
      for (List<PlatformMatch> matches : matchers.values()) {
         for (PlatformMatch match : matches) {
            max = Math.max(max, match.getReflexVersion());
            max = Math.max(max, getMaxReflexVersion(match.getActions()));
         }
      }

      return max;
   }

   private static int getMaxProtocolReflexVersion(List<ProtocolMatch> matchers) {
      int max = 0;
      for (ProtocolMatch match : matchers) {
         max = Math.max(max, match.getReflexVersion());
         max = Math.max(max, getMaxReflexVersion(match.getActions()));
      }

      return max;
   }

   private static void setupDebugging(List<ProtocolMatch> matchers) {
      for (ProtocolMatch match : matchers) {
         if (isDebuggingEnabled(match.getActions())) {
            match.setDebug(true);
         }
      }
   }

   private static void setupDebugging(Map<String,List<PlatformMatch>> matchers) {
      for (List<PlatformMatch> matches : matchers.values()) {
         for (PlatformMatch match : matches) {
            if (isDebuggingEnabled(match.getActions())) {
               match.setDebug(true);
            }
         }
      }
   }

   private static boolean isDebuggingEnabled(List<Action> actions) {
      for (Action action : actions) {
         if (action instanceof DebugAction) {
            return true;
         }
      }

      return false;
   }

   private static int getMaxReflexVersion(@Nullable List<Action> actions) {
      int max = 0;
      if (actions == null || actions.isEmpty()) {
         return max;
      }

      for (Action action : actions) {
         max = Math.max(max, action.getReflexVersion());
      }

      return max;
   }

   public String getDriver() {
      return driver;
   }

   public Version getVersion() {
      return version;
   }

   public String getHash() {
      return hash;
   }

   public Set<String> getCapabilities() {
      return capabilities;
   }

   public boolean isDegraded() {
      return degraded;
   }

   private void fire(ReflexDriverContext ctx, @Nullable List<Action> actions, Object msg, int minReflexVersion) {
      if (actions == null) {
         return;
      }

      for (Action action : actions) {
         if (action.getReflexVersion() > minReflexVersion) {
            action.run(ctx, msg);
         }
      }
   }

   public int getReflexesHashCode() {
      return reflexesHashCode;
   }

   /////////////////////////////////////////////////////////////////////////////
   // Lifecycle Handlers
   /////////////////////////////////////////////////////////////////////////////

   public void fireOnAdded(ReflexDriverContext ctx, int minReflexVersion) {
      fire(ctx, onAdded, "onAdded", minReflexVersion);
   }

   public void fireOnConnected(ReflexDriverContext ctx, int minReflexVersion) {
      fire(ctx, onConnected, "onConnected", minReflexVersion);
   }

   public void fireOnDisconnected(ReflexDriverContext ctx, int minReflexVersion) {
      fire(ctx, onDisconnected, "onDisconnected", minReflexVersion);
   }

   public void fireOnRemoved(ReflexDriverContext ctx, int minReflexVersion) {
      fire(ctx, onRemoved, "onConnected", minReflexVersion);
   }

   /////////////////////////////////////////////////////////////////////////////
   // Message Handlers
   /////////////////////////////////////////////////////////////////////////////
   
   public boolean handle(ReflexDriverContext ctx, PlatformMessage message) {
      try {
         MessageBody msg = message.getValue();
         List<PlatformMatch> platMatchers = platformMatchers.get(msg.getMessageType());
         if (platMatchers == null || platMatchers.isEmpty()) {
            return false;
         }

         for (PlatformMatch match : platMatchers) {
            try {
               List<Action> actions = match.match(ctx,msg);
               if (actions != null) {
                  ctx.markMessageHandled(true);
   
                  // NOTE: We always execute all actions here because there isn't
                  //       a way to partially execute a platform message on the
                  //       hub like there is for protocol messages.
                  fire(ctx, actions, msg, V0);
               }
            } catch (Exception ex) {
               log.warn("exception while matching platform message:", ex);
            }
         }

         Map<String,Object> attrs = ctx.getAttributesToEmit();
         if (attrs != null && !attrs.isEmpty()) {
            ctx.emit(MessageBody.buildMessage(Capability.CMD_SET_ATTRIBUTES, attrs));
         }

         ctx.commit();
         return ctx.wasMessageHandled();
      } finally {
         ctx.reset();
      }
   }

   public boolean handle(ReflexDriverContext ctx, ProtocolMessage msg, int minReflexVersion) {
      if (minReflexVersion >= maxReflexVersion) {
         return false;
      }

      try {
         byte[] buffer = msg.getBuffer();
         for (ProtocolMatch match : protocolMatchers) {
            if (match.getReflexVersion() <= minReflexVersion) {
               continue;
            }

            try {
               List<Action> actions = match.match(ctx,buffer);
               if (actions != null) {
                  ctx.markMessageHandled(true);
                  fire(ctx, actions, msg, minReflexVersion);
               } else if (match.isDebug()) {
                  log.info(
                     "[reflex debugging] protocol message did not match successfully:\n" +
                     "    protocol message: {}",
                     ProtocUtil.toHexString(buffer)
                  );

                  match.debug(ctx,buffer);
               }
            } catch (Exception ex) {
               log.warn("exception while matching protocol message:", ex);
            }
         }

         Map<String,Object> attrs = ctx.getAttributesToEmit();
         if (attrs != null && !attrs.isEmpty()) {
            ctx.emit(MessageBody.buildMessage(Capability.CMD_SET_ATTRIBUTES, attrs));
         }

         ctx.commit();
         return ctx.wasMessageHandled();
      } finally {
         ctx.reset();
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Required Reflex Version Extraction
   /////////////////////////////////////////////////////////////////////////////
   
   private static int getMinimumRequiredReflexVersion(@Nullable List<ReflexAction> actions) {
      if (actions == null) {
         return 0;
      }

      int min = 0;
      for (ReflexAction action : actions) {
         if (action instanceof ReflexActionSendProtocol) {
            min = Math.max(min, SendProtocolAction.getRequiredReflexVersion());
         } else if (action instanceof ReflexActionSendPlatform) {
            ReflexActionSendPlatform rasp = (ReflexActionSendPlatform)action;
            if (Capability.CMD_SET_ATTRIBUTES.equals(rasp.getEvent())) {
               min = Math.max(min, SetAttrActionNoStore.getRequiredReflexVersion());
            } else if (!rasp.isResponse()) {
               min = Math.max(min, SendPlatformAction.getRequiredReflexVersion());
            } else {
               min = Math.max(min, SendPlatformResponseAction.getRequiredReflexVersion());
            }
         } else if (action instanceof ReflexActionSetAttribute) {
            min = Math.max(min, SetAttrAction.getRequiredReflexVersion());
         } else if (action instanceof ReflexActionOrdered) {
            ReflexActionOrdered ordered = (ReflexActionOrdered)action;
            min = Math.max(min, OrderedAction.getRequiredReflexVersion(ordered.getActions()));
         } else if (action instanceof ReflexActionDelay) {
            ReflexActionDelay delay = (ReflexActionDelay)action;
            min = Math.max(min, DelayAction.getRequiredReflexVersion(delay.getActions()));
         } else if (action instanceof ReflexActionAlertmeLifesign) {
            min = Math.max(min, AlertmeLifesignAction.getRequiredReflexVersion());
         } else if (action instanceof ReflexActionZigbeeIasZoneEnroll) {
            min = Math.max(min, ZigbeeZoneEnrollAction.getRequiredReflexVersion());
         } else if (action instanceof ReflexActionLog) {
            min = Math.max(min, LogAction.getRequiredReflexVersion());
         } else if (action instanceof ReflexActionForward) {
            min = Math.max(min, ForwardAction.getRequiredReflexVersion());
         } else if (action instanceof ReflexActionDebug) {
            min = Math.max(min, DebugAction.getRequiredReflexVersion());
         } else if (action instanceof ReflexActionBuiltin) {
            min = Math.max(min, V1);
         } else {
            log.warn("unknown reflex action: {}", action);
            min = Math.max(min, V1);
         }
      }

      return min;
   }

   public static int getMinimumRequiredReflexVersion(List<ReflexDefinition> defs, @Nullable ReflexDriverDFA dfa) {
      int min = 0;
      for (ReflexDefinition reflex : defs) {
         min = Math.max(min, getMinimumRequiredReflexVersion(reflex.getActions()));

         for (ReflexMatch match : reflex.getMatchers()) {
            if (match instanceof ReflexMatchLifecycle) {
               min = Math.max(min, V1);
            } else if (match instanceof ReflexMatchPollRate) {
               min = Math.max(min, ZWaveAddPollAction.getRequiredReflexVersion());
            } else if (match instanceof ReflexMatchAlertmeLifesign) {
               min = Math.max(min, AlertmeLifesignMatch.getRequiredReflexVersion());
            } else if (match instanceof ReflexMatchZigbeeAttribute) {
               ReflexMatchZigbeeAttribute attr = (ReflexMatchZigbeeAttribute)match;
               min = Math.max(min, ZigbeeAttributeMatch.getRequiredReflexVersion(attr));
            } else if (match instanceof ReflexMatchZigbeeIasZoneStatus) {
               ReflexMatchZigbeeIasZoneStatus status = (ReflexMatchZigbeeIasZoneStatus)match;
               min = Math.max(min, ZigbeeIasZoneStatusMatch.getRequiredReflexVersion(status));
            } else if (match instanceof ReflexMatchAttribute) {
               min = Math.max(min, ReflexMatchAttr.getRequiredReflexVersion());
            } else if (match instanceof ReflexMatchMessage) {
               min = Math.max(min, ReflexMatchPlatform.getRequiredReflexVersion());
            } else {
               log.warn("unknown reflex match: {}", match);
               min = Math.max(min, V1);
            }
         }
      }

      if (dfa != null && dfa.getDfa() != null) {
         min = Math.max(min, RegexMatch.getRequiredReflexVersion());
      }

      return min;
   }

   /////////////////////////////////////////////////////////////////////////////
   // Reflex Processor Factory
   /////////////////////////////////////////////////////////////////////////////
   
   public static ReflexDriver create(String driver, Version version, String hash, Set<String> capabilities, List<ReflexDefinition> defs, @Nullable ReflexDriverDFA dfa) {
      ArrayList<Action> onAdded = new ArrayList<>();
      ArrayList<Action> onConnected = new ArrayList<>();
      ArrayList<Action> onDisconnected = new ArrayList<>();
      ArrayList<Action> onRemoved = new ArrayList<>();
      ArrayList<ProtocolMatch> matchers = new ArrayList<>();

      Map<String,List<PlatformMatch>> matchPlatform = new LinkedHashMap<>();

      final AtomicBoolean degraded = new AtomicBoolean(false);
      long start = System.nanoTime();
      for (ReflexDefinition reflex : defs) {
         List<Action> actions = convertActions(reflex.getActions(), degraded);
         if (actions.isEmpty()) {
            continue;
         }

         for (ReflexMatch match : reflex.getMatchers()) {
            if (match instanceof ReflexMatchLifecycle) {
               ReflexMatchLifecycle lc = (ReflexMatchLifecycle)match;
               switch (lc.getType()) {
               case ADDED:
                  onAdded.addAll(actions);
                  break;
               case CONNECTED:
                  onConnected.addAll(actions);
                  break;
               case DISCONNECTED:
                  onDisconnected.addAll(actions);
                  break;
               case REMOVED:
                  onRemoved.addAll(actions);
                  break;
               default:
                  log.warn("unknown reflex lifecycle match: {}", lc.getType());
                  degraded.set(true);
                  break;
               }
            } else if (match instanceof ReflexMatchPollRate) {
               ReflexMatchPollRate poll = (ReflexMatchPollRate)match;
               onConnected.add(new ZWaveAddPollAction(poll.getUnit().toNanos(poll.getTime()), extractProtocolPayloads(actions)));
            } else if (match instanceof ReflexMatchAlertmeLifesign) {
               matchers.add(new AlertmeLifesignMatch((ReflexMatchAlertmeLifesign)match, actions));
            } else if (match instanceof ReflexMatchZigbeeAttribute) {
               matchers.add(new ZigbeeAttributeMatch((ReflexMatchZigbeeAttribute)match, actions));
            } else if (match instanceof ReflexMatchZigbeeIasZoneStatus) {
               matchers.add(new ZigbeeIasZoneStatusMatch((ReflexMatchZigbeeIasZoneStatus)match, actions));
            } else if (match instanceof ReflexMatchAttribute) {
               List<PlatformMatch> existingPlat = matchPlatform.get(Capability.CMD_SET_ATTRIBUTES);
               if (existingPlat == null) {
                  existingPlat = new ArrayList<>();
                  matchPlatform.put(Capability.CMD_SET_ATTRIBUTES, existingPlat);
               }

               ReflexMatchAttribute mattr = (ReflexMatchAttribute)match;
               existingPlat.add(new ReflexMatchAttr(mattr, actions));
            } else if (match instanceof ReflexMatchMessage) {
               ReflexMatchMessage mmsg = (ReflexMatchMessage)match;
               List<PlatformMatch> existingPlat = matchPlatform.get(mmsg.getMessage().getMessageType());
               if (existingPlat == null) {
                  existingPlat = new ArrayList<>();
                  matchPlatform.put(mmsg.getMessage().getMessageType(), existingPlat);
               }

               existingPlat.add(new ReflexMatchPlatform(mmsg, actions));
            } else {
               log.warn("unknown reflex match: {}", match);
               degraded.set(true);
            }
         }
      }

      if (dfa != null && dfa.getDfa() != null) {
         RegexDfaByte<List<Action>> actionsDfa = dfa.getDfa().transform(new Function<List<ReflexAction>,List<Action>>() {
            @Override
            public List<Action> apply(List<ReflexAction> actions) {
               return convertActions(actions, degraded);
            }
         });

         matchers.add(new RegexMatch(actionsDfa));
      }

      onAdded.trimToSize();
      onConnected.trimToSize();
      onDisconnected.trimToSize();
      onRemoved.trimToSize();
      matchers.trimToSize();

      Map<String,List<PlatformMatch>> platmatchers = ImmutableMap.copyOf(matchPlatform);

      long elapsed = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - start);
      log.info("loaded driver defintion for {} {} in {}us", driver, version, elapsed);
      return new ReflexDriver(driver, version, hash, defs.hashCode(), capabilities, onAdded, onConnected, onDisconnected, onRemoved, matchers, platmatchers, degraded.get());
   }

   private static List<byte[]> extractProtocolPayloads(List<Action> actions) {
      List<byte[]> results = new ArrayList<>();
      for (Action action : actions) {
         if (action instanceof SendProtocolAction) {
            SendProtocolAction sp = (SendProtocolAction)action;
            results.add(sp.msg);
         } else {
            log.warn("poll reflex definitions should not contain: {}",action);
         }
      }

      return results;
   }

   /////////////////////////////////////////////////////////////////////////////
   // Reflex Actions
   /////////////////////////////////////////////////////////////////////////////

   private static List<Action> convertActions(List<ReflexAction> actions, AtomicBoolean degraded) {
      if (actions == null) {
         return ImmutableList.<Action>of();
      }

      ImmutableList.Builder<Action> builder = ImmutableList.builder();
      for (ReflexAction action : actions) {
         Action act = convert(action, degraded);
         if (act != null) {
            builder.add(act);
         }
      }

      return builder.build();
   }

   private static @Nullable Action convert(ReflexAction action, AtomicBoolean degraded) {
      if (action instanceof ReflexActionSendProtocol) {
         return new SendProtocolAction((ReflexActionSendProtocol)action);
      } else if (action instanceof ReflexActionSendPlatform) {
         ReflexActionSendPlatform rasp = (ReflexActionSendPlatform)action;
         if (Capability.CMD_SET_ATTRIBUTES.equals(rasp.getEvent())) {
            return new SetAttrActionNoStore(rasp.getArgs());
         } else if (!rasp.isResponse()) {
            return new SendPlatformAction(rasp);
         } else {
            return new SendPlatformResponseAction(rasp);
         }
      } else if (action instanceof ReflexActionSetAttribute) {
         return new SetAttrAction((ReflexActionSetAttribute)action);
      } else if (action instanceof ReflexActionOrdered) {
         return new OrderedAction((ReflexActionOrdered)action, degraded);
      } else if (action instanceof ReflexActionDelay) {
         return new DelayAction((ReflexActionDelay)action, degraded);
      } else if (action instanceof ReflexActionAlertmeLifesign) {
         return new AlertmeLifesignAction((ReflexActionAlertmeLifesign)action);
      } else if (action instanceof ReflexActionZigbeeIasZoneEnroll) {
         return new ZigbeeZoneEnrollAction((ReflexActionZigbeeIasZoneEnroll)action);
      } else if (action instanceof ReflexActionLog) {
         return new LogAction((ReflexActionLog)action);
      } else if (action instanceof ReflexActionForward) {
         return new ForwardAction((ReflexActionForward)action);
      } else if (action instanceof ReflexActionDebug) {
         return new DebugAction((ReflexActionDebug)action);
      } else if (action instanceof ReflexActionBuiltin) {
         log.trace("builtin reflex actions are not available on the platform: {}", action);
      } else {
         log.warn("unknown reflex action: {}", action);
         degraded.set(true);
      }

      return null;
   }

   private static interface Action {
      int getReflexVersion();
      void run(ReflexDriverContext ctx, Object msg);
   }

   private static abstract class ProtocolAction implements Action {
      private final @Nullable Protocol.Message zwaveMessage;
      private final @Nullable ZigbeeMessage.Protocol zigbeeMessage;
      private final int reflexVersion;

      public ProtocolAction(Protocol.Message zwaveMessage, com.iris.protocol.zigbee.msg.ZigbeeMessage.Protocol zigbeeMessage, int version) {
         this.zwaveMessage = zwaveMessage;
         this.zigbeeMessage = zigbeeMessage;
         this.reflexVersion = version;
      }

      public <T> ProtocolAction(T value, Function<T,Protocol.Message> zwave, Function<T,ZigbeeMessage.Protocol> zigbee, Function<T,Integer> version) {
         this.zwaveMessage = zwave.apply(value);
         this.zigbeeMessage = zigbee.apply(value);
         this.reflexVersion = version.apply(value);
      }

      @Override
      public void run(ReflexDriverContext ctx, Object _unused) {
         if (zwaveMessage != null) {
            ctx.zwaveSend(zwaveMessage);
         }

         if (zigbeeMessage != null) {
            ctx.zigbeeSend(zigbeeMessage);
         }
      }

      protected final @Nullable Protocol.Message getZWaveMessage() { 
         return zwaveMessage;
      }

      protected final @Nullable ZigbeeMessage.Protocol getZigbeeMessage() {
         return zigbeeMessage;
      }

      @Override
      public int getReflexVersion() {
         return reflexVersion;
      }
   }

   private static final class SendProtocolAction extends ProtocolAction {
      private final byte[] msg;

      public SendProtocolAction(ReflexActionSendProtocol action) {
         super(createZWaveMessage(action), createZigbeeMessage(action), getRequiredReflexVersion());
         this.msg = action.getMessage();
         switch (action.getType()) {
         case ZWAVE:
         case ZIGBEE:
            break;
         default:
            log.warn("cannot send reflex action protocol message: unknown protocol {}", action.getType());
            break;
         }
      }

      public static int getRequiredReflexVersion() {
         return V1;
      }

      private static @Nullable Protocol.Message createZWaveMessage(ReflexActionSendProtocol action) {
         try {
            if (action.getType() != ReflexActionSendProtocol.Type.ZWAVE) {
               return null;
            }

            ZWaveCommandClassFrame ccframe = ZWaveCommandClassFrame.serde().fromBytes(ByteOrder.BIG_ENDIAN, action.getMessage());
            Protocol.Command pcmd = Protocol.Command.builder()
               .setNodeId(0)
               .setCommandId(ccframe.getCommand())
               .setCommandClassId(ccframe.getCommandClassId())
               .setPayload(ccframe.getPayload())
               .create();

            return Protocol.Message.builder()
               .setType(Protocol.Command.ID)
               .setPayload(ByteOrder.BIG_ENDIAN, pcmd)
               .create();
         } catch (Exception ex) {
            log.warn("failed to convert protocol message", ex);
            return null;
         }
      }

      private static @Nullable ZigbeeMessage.Protocol createZigbeeMessage(ReflexActionSendProtocol action) {
         try {
            if (action.getType() != ReflexActionSendProtocol.Type.ZIGBEE) {
               return null;
            }

            return ZigbeeMessage.Protocol.serde().fromBytes(ByteOrder.LITTLE_ENDIAN, action.getMessage());
         } catch (Exception ex) {
            log.warn("failed to convert protocol message", ex);
            return null;
         }
      }
   }

   private static final class ZigbeeZoneEnrollAction extends ProtocolAction {
      public ZigbeeZoneEnrollAction(ReflexActionZigbeeIasZoneEnroll ias) {
         super(null, create(ias), getRequiredReflexVersion());
      }

      public static int getRequiredReflexVersion() {
         return V1;
      }

      private static ZigbeeMessage.Protocol create(ReflexActionZigbeeIasZoneEnroll ias) {
         try {
            ZigbeeMessage.IasZoneEnroll enroll = ZigbeeMessage.IasZoneEnroll.builder()
               .setEndpoint(ias.getEndpointId())
               .setProfile(ias.getProfileId())
               .setCluster(ias.getClusterId())
               .create();

            return ZigbeeMessage.Protocol.builder()
               .setType(enroll.getMessageId())
               .setPayload(ByteOrder.LITTLE_ENDIAN, enroll)
               .create();
         } catch (Exception ex) {
            throw new RuntimeException(ex);
         }
      }
   }

   private static abstract class MultiProtocolAction extends ProtocolAction {
      private final MutableInt maxReflexVersion = new MutableInt(0);

      public MultiProtocolAction(List<ReflexAction> subs, final Function<Protocol.Message[],Protocol.Message> zwave,
         final Function<ZigbeeMessage.Protocol[],ZigbeeMessage.Protocol> zigbee, AtomicBoolean degraded,
         final int reflexVersion) {
         super(
            createProtocolActions(subs,degraded),
            new Function<List<ProtocolAction>,Protocol.Message>() {
               @Override
               public Protocol.Message apply(List<ProtocolAction> subs) {
                  return createZWaveMessage(subs,zwave);
               }
            },
            new Function<List<ProtocolAction>,ZigbeeMessage.Protocol>() {
               @Override
               public ZigbeeMessage.Protocol apply(List<ProtocolAction> subs) {
                  return createZigbeeMessage(subs,zigbee);
               }
            },
            new Function<List<ProtocolAction>,Integer>() {
               @Override
               public Integer apply(List<ProtocolAction> subs) {
                  int max = reflexVersion;
                  for (ProtocolAction sub : subs) {
                     max = Math.max(max, sub.getReflexVersion());
                  }
                  
                  return max;
               }
            }
         );
      }

      public static int getRequiredReflexVersion(@Nullable List<ReflexAction> subs, int reflexVersion) {
         return Math.max(reflexVersion, getMinimumRequiredReflexVersion(subs));
      }

      private static List<ProtocolAction> createProtocolActions(List<ReflexAction> subs, AtomicBoolean degraded) {
         List<ProtocolAction> result = new ArrayList<>(subs.size());
         for (ReflexAction sub : subs) {
            ProtocolAction act = (ProtocolAction)convert(sub,degraded);
            if (act != null) {
               result.add(act);
            }
         }

         return result;
      }

      private static @Nullable Protocol.Message createZWaveMessage(List<ProtocolAction> subs, Function<Protocol.Message[],Protocol.Message> zwave) {
         ArrayList<Protocol.Message> zwActions = null;
         for (ProtocolAction act : subs) {
            if (act == null) {
               continue;
            }

            Protocol.Message zw = act.getZWaveMessage();
            if (zw != null) {
               ArrayList<Protocol.Message> list = zwActions;
               if (list == null) {
                  list = new ArrayList<>();
                  zwActions = list;
               }

               list.add(zw);
            }
         }

         if (zwActions == null || zwActions.isEmpty()) {
            return null;
         }

         Protocol.Message[] zw = zwActions.toArray(new Protocol.Message[zwActions.size()]);
         return zwave.apply(zw);
      }

      private static @Nullable ZigbeeMessage.Protocol createZigbeeMessage(List<ProtocolAction> subs, Function<ZigbeeMessage.Protocol[],ZigbeeMessage.Protocol> zigbee) {
         ArrayList<ZigbeeMessage.Protocol> zbActions = null;
         for (ProtocolAction act : subs) {
            if (act == null) {
               continue;
            }

            ZigbeeMessage.Protocol zb = act.getZigbeeMessage();
            if (zb != null) {
               ArrayList<ZigbeeMessage.Protocol> list = zbActions;
               if (list == null) {
                  list = new ArrayList<>();
                  zbActions = list;
               }

               list.add(zb);
            }
         }

         if (zbActions == null || zbActions.isEmpty()) {
            return null;
         }

         ZigbeeMessage.Protocol[] zb = zbActions.toArray(new ZigbeeMessage.Protocol[zbActions.size()]);
         return zigbee.apply(zb);
      }
   }

   private static final class DelayAction extends MultiProtocolAction {
      public DelayAction(final ReflexActionDelay action, AtomicBoolean degraded) {
         super(
            action.getActions(), 
            new Function<Protocol.Message[],Protocol.Message>() {
               @Override
               public Protocol.Message apply(Protocol.Message[] subs) {
                  return createZWaveMessage(action.getUnit().toNanos(action.getTime()), subs);
               }
            },
            new Function<ZigbeeMessage.Protocol[],ZigbeeMessage.Protocol>() {
               @Override
               public ZigbeeMessage.Protocol apply(ZigbeeMessage.Protocol[] subs) {
                  return createZigbeeMessage(action.getUnit().toNanos(action.getTime()), subs);
               }
            },
            degraded,
            getDelayRequiredReflexVersion()
         );
      }

      public static int getRequiredReflexVersion(List<ReflexAction> subs) {
         return getRequiredReflexVersion(subs, getDelayRequiredReflexVersion());
      }

      public static int getDelayRequiredReflexVersion() {
         return V1;
      }

      private static @Nullable Protocol.Message createZWaveMessage(long delayTimeInNs, Protocol.Message[] zwaveActions) {
         if (zwaveActions == null || zwaveActions.length == 0) {
            return null;
         }

         try {
            Protocol.DelayedCommands delay = Protocol.DelayedCommands.builder()
               .setDelay(delayTimeInNs)
               .setPayload(zwaveActions)
               .create();

            return Protocol.Message.builder()
               .setType(Protocol.DelayedCommands.ID)
               .setPayload(ByteOrder.BIG_ENDIAN, delay)
               .create();
         } catch (Exception ex) {
            log.warn("failed to convert delay action to protocol message", ex);
            return null;
         }
      }

      private static @Nullable ZigbeeMessage.Protocol createZigbeeMessage(long delayTimeInNs, ZigbeeMessage.Protocol[] zigbeeActions) {
         if (zigbeeActions == null || zigbeeActions.length == 0) {
            return null;
         }

         try {
            ZigbeeMessage.Delay delay = ZigbeeMessage.Delay.builder()
               .setDelay(delayTimeInNs)
               .setPayload(zigbeeActions)
               .create();

            return ZigbeeMessage.Protocol.builder()
               .setType(ZigbeeMessage.Delay.ID)
               .setPayload(ByteOrder.LITTLE_ENDIAN, delay)
               .create();
         } catch (Exception ex) {
            log.warn("failed to convert delay action to protocol message", ex);
            return null;
         }
      }
   }

   private static final class OrderedAction extends MultiProtocolAction {
      public OrderedAction(ReflexActionOrdered action, AtomicBoolean degraded) {
         super(
            action.getActions(), 
            new Function<Protocol.Message[],Protocol.Message>() {
               @Override
               public Protocol.Message apply(Protocol.Message[] subs) {
                  return createZWaveMessage(subs);
               }
            },
            new Function<ZigbeeMessage.Protocol[],ZigbeeMessage.Protocol>() {
               @Override
               public ZigbeeMessage.Protocol apply(ZigbeeMessage.Protocol[] subs) {
                  return createZigbeeMessage(subs);
               }
            },
            degraded,
            getOrderedRequiredReflexVersion()
         );
      }

      public static int getRequiredReflexVersion(List<ReflexAction> subs) {
         return getRequiredReflexVersion(subs, getOrderedRequiredReflexVersion());
      }

      public static int getOrderedRequiredReflexVersion() {
         return V1;
      }

      private static @Nullable Protocol.Message createZWaveMessage(Protocol.Message[] zwaveActions) {
         if (zwaveActions == null || zwaveActions.length == 0) {
            return null;
         }

         try {
            Protocol.OrderedCommands ordered = Protocol.OrderedCommands.builder()
               .setPayload(zwaveActions)
               .create();

            return Protocol.Message.builder()
               .setType(Protocol.OrderedCommands.ID)
               .setPayload(ByteOrder.BIG_ENDIAN, ordered)
               .create();
         } catch (Exception ex) {
            log.warn("failed to convert delay action to protocol message", ex);
            return null;
         }
      }

      private static @Nullable ZigbeeMessage.Protocol createZigbeeMessage(ZigbeeMessage.Protocol[] zigbeeActions) {
         if (zigbeeActions == null || zigbeeActions.length == 0) {
            return null;
         }

         try {
            ZigbeeMessage.Ordered ordered = ZigbeeMessage.Ordered.builder()
               .setPayload(zigbeeActions)
               .create();

            return ZigbeeMessage.Protocol.builder()
               .setType(ZigbeeMessage.Ordered.ID)
               .setPayload(ByteOrder.LITTLE_ENDIAN, ordered)
               .create();
         } catch (Exception ex) {
            log.warn("failed to convert delay action to protocol message", ex);
            return null;
         }
      }
   }

   private static final class SendPlatformAction implements Action {
      private final MessageBody msg;

      public SendPlatformAction(ReflexActionSendPlatform action) {
         this.msg = MessageBody.buildMessage(action.getEvent(), action.getArgs());
      }

      @Override
      public void run(ReflexDriverContext ctx, Object _unused) {
         ctx.emit(msg);
      }

      public static int getRequiredReflexVersion() {
         return V1;
      }

      @Override
      public int getReflexVersion() {
         return getRequiredReflexVersion();
      }
   }

   private static final class SendPlatformResponseAction implements Action {
      private final MessageBody msg;

      public SendPlatformResponseAction(ReflexActionSendPlatform action) {
         this.msg = MessageBody.buildMessage(action.getEvent(), action.getArgs());
      }

      @Override
      public void run(ReflexDriverContext ctx, Object _unused) {
         ctx.setResponse(msg);
      }

      public static int getRequiredReflexVersion() {
         return V1;
      }

      @Override
      public int getReflexVersion() {
         return getRequiredReflexVersion();
      }
   }

   private static final class SetAttrAction implements Action {
      private final String name;
      private final Object value;

      public SetAttrAction(ReflexActionSetAttribute action) {
         this.name = action.getAttr();
         this.value = action.getValue();
      }

      @Override
      public void run(ReflexDriverContext ctx, Object _unused) {
         ctx.setAttribute(name,value);
      }

      public static int getRequiredReflexVersion() {
         return V1;
      }

      @Override
      public int getReflexVersion() {
         return getRequiredReflexVersion();
      }
   }

   private static final class SetAttrActionNoStore implements Action {
      private final Map<String,Object> attrs;

      public SetAttrActionNoStore(Map<String,Object> attrs) {
         this.attrs = attrs;
      }

      @Override
      public void run(ReflexDriverContext ctx, Object _unused) {
         ctx.emitAttributes(attrs);
      }

      public static int getRequiredReflexVersion() {
         return V1;
      }

      @Override
      public int getReflexVersion() {
         return getRequiredReflexVersion();
      }
   }

   private static final class AlertmeLifesignAction implements Action {
      private final ReflexActionAlertmeLifesign ls;
      private int lastBattery = Integer.MIN_VALUE;
      private int lastSignal = Integer.MIN_VALUE;
      private int lastTemp = Integer.MIN_VALUE;

      public AlertmeLifesignAction(ReflexActionAlertmeLifesign ls) {
         this.ls = ls;
         switch (ls.getType()) {
         case BATTERY:
         case SIGNAL:
         case TEMPERATURE:
            // supported
            break;

         default:
            log.warn("unknown alertme lifesign action type: " + ls.getType());
            break;
         }
      }

      @Override
      public void run(ReflexDriverContext ctx, Object _unused) {
         switch (ls.getType()) {
         case BATTERY:
            Integer volts = ctx.getVariable("ameLifesignVolts");
            if (volts != null && ls.getNominal() != null && ls.getMinimum() != null) {
               double measured = volts / 1000.0;
               double percent = 100.0 / (ls.getNominal() - ls.getMinimum()) * (measured - ls.getMinimum());
               int value = Math.max(0, Math.min(100, (int)Math.round(percent)));

               int val = (value/5)*5;
               if (lastBattery != val) {
                  lastBattery = val;
                  ctx.setAttribute(DevicePowerCapability.ATTR_BATTERY,value);
               }
            }
            break;

         case SIGNAL:
            Integer lqi = ctx.getVariable("ameLifesignLqi");
            if (lqi != null) {
               double signal = (lqi * 100.0) / 255.0;
               int value = (int)Math.round(signal);

               int val = (value/5)*5;
               if (lastSignal != val) {
                  lastSignal = val;
                  ctx.setAttribute(DeviceConnectionCapability.ATTR_SIGNAL,value);
               }
            }
            break;

         case TEMPERATURE:
            Integer temp = ctx.getVariable("ameLifesignTemp");
            if (temp != null) {
               // cast shortValue to double so we get proper sign extension for 2 byte value in 4 byte integer field
               double value = (double)(temp.shortValue()) / 16.0;
               int fh = (int)Math.round((value*9.0/5.0) + 32);
               if (lastTemp != fh) {
                  lastTemp = fh;

                  double c = (fh-32) * 5.0/9.0;
                  ctx.setAttribute(TemperatureCapability.ATTR_TEMPERATURE,c);
               }
            }
            break;

         default:
            // ignore
            break;
         }
      }

      public static int getRequiredReflexVersion() {
         return V1;
      }

      @Override
      public int getReflexVersion() {
         return getRequiredReflexVersion();
      }
   }

   private static final class ForwardAction implements Action {
      public ForwardAction(ReflexActionForward action) {
      }

      @Override
      public void run(ReflexDriverContext ctx, Object _unused) {
         ctx.markMessageHandled(false);
      }

      public static int getRequiredReflexVersion() {
         return V1;
      }

      @Override
      public int getReflexVersion() {
         return getRequiredReflexVersion();
      }
   }

   private static final class ZWaveAddPollAction implements Action {
      private final long timeInNs;
      private final List<byte[]> polls;

      public ZWaveAddPollAction(long timeInNs, List<byte[]> polls) {
         this.timeInNs = timeInNs;
         this.polls = polls;
      }

      @Override
      public void run(ReflexDriverContext ctx, Object _unused) {
         ctx.zwaveAddScheduledPoll(timeInNs, TimeUnit.NANOSECONDS, polls);
      }

      public static int getRequiredReflexVersion() {
         return V1;
      }

      @Override
      public int getReflexVersion() {
         return getRequiredReflexVersion();
      }
   }

   private static final class LogAction implements Action {
      private static final Object[] EMPTY = new Object[0];
      private ReflexActionLog lg;

      public LogAction(ReflexActionLog lg) {
         this.lg = lg;
      }

      @Override
      public void run(ReflexDriverContext ctx, Object msg) {
         switch (lg.getLevel()) {
         case TRACE:
            if (ctx.getDriverLogger().isTraceEnabled()) {
               ctx.getDriverLogger().trace(message(), arguments(msg));
            }
            break;

         case DEBUG:
            if (ctx.getDriverLogger().isDebugEnabled()) {
               ctx.getDriverLogger().debug(message(), arguments(msg));
            }
            break;

         case INFO:
            if (ctx.getDriverLogger().isInfoEnabled()) {
               ctx.getDriverLogger().info(message(), arguments(msg));
            }
            break;

         case WARN:
            if (ctx.getDriverLogger().isWarnEnabled()) {
               ctx.getDriverLogger().warn(message(), arguments(msg));
            }
            break;

         case ERROR:
            if (ctx.getDriverLogger().isErrorEnabled()) {
               ctx.getDriverLogger().error(message(), arguments(msg));
            }
            break;

         default:
            if (ctx.getDriverLogger().isDebugEnabled()) {
               ctx.getDriverLogger().debug(message(), arguments(msg));
            }
            break;
         }
      }

      public static int getRequiredReflexVersion() {
         return V1;
      }

      @Override
      public int getReflexVersion() {
         return getRequiredReflexVersion();
      }

      private final String message() {
         return lg.getMsg();
      }

      private final Object[] arguments(Object msg) {
         List<ReflexActionLog.Arg> args = lg.getArguments();
         if (args == null || args.isEmpty()) {
            return EMPTY;
         }

         int sz = args.size();
         if (sz == 1 && args.get(0) == ReflexActionLog.Arg.MESSAGE) {
            return new Object[] { argument(args.get(0), msg) };
         } 

         int i = 0;
         Object[] results = new Object[sz];
         for (ReflexActionLog.Arg arg : args) {
            results[i++] = argument(arg, msg);
         }

         return results;
      }

      private final Object argument(ReflexActionLog.Arg arg, Object msg) {
         switch (arg) {
         case MESSAGE:
            return msg;
         
         default:
            return "<unknown argument" + arg + ">";
         }
      }
   }

   private static final class DebugAction implements Action {
      public DebugAction(ReflexActionDebug action) {
      }

      @Override
      public void run(ReflexDriverContext ctx, Object _unused) {
         ctx.markMessageHandled(false);
      }

      public static int getRequiredReflexVersion() {
         return V1;
      }

      @Override
      public int getReflexVersion() {
         return getRequiredReflexVersion();
      }
   }

   /////////////////////////////////////////////////////////////////////////////
   // Reflex Machers
   /////////////////////////////////////////////////////////////////////////////

   private static interface PlatformMatch {
      int getReflexVersion();
      @Nullable List<Action> getActions();

      void setDebug(boolean debug);
      boolean isDebug();

      @Nullable List<Action> match(ReflexDriverContext ctx, MessageBody msg);
   }

   private static abstract class AbstractPlatformMatch implements PlatformMatch {
      private boolean debug = false;

      @Override
      public void setDebug(boolean debug) {
         this.debug = debug;
      }

      @Override
      public boolean isDebug() {
         return this.debug;
      }
   }

   private static final class ReflexMatchPlatform extends AbstractPlatformMatch {
      private final MessageBody value;
      private final List<Action> actions;

      public ReflexMatchPlatform(ReflexMatchMessage mmsg, List<Action> actions) {
         this.value = mmsg.getMessage();
         this.actions = actions;
      }

      public static int getRequiredReflexVersion() {
         return V1;
      }

      @Override
      public int getReflexVersion() {
         return getRequiredReflexVersion();
      }

      @Override
      public @Nullable List<Action> getActions() {
         return actions;
      }

      @Override
      public @Nullable List<Action> match(ReflexDriverContext ctx, MessageBody msg) {
         if (Objects.equals(value,msg)) {
            return actions;
         }

         return null;
      }
   }

   private static final class ReflexMatchAttr extends AbstractPlatformMatch {
      private final String attr;
      private final Object value;
      private final List<Action> actions;

      public ReflexMatchAttr(ReflexMatchAttribute mattr, List<Action> actions) {
         this.attr = mattr.getAttr();
         this.value = mattr.getValue();
         this.actions = actions;
      }

      public static int getRequiredReflexVersion() {
         return V1;
      }

      @Override
      public int getReflexVersion() {
         return getRequiredReflexVersion();
      }

      @Override
      public @Nullable List<Action> getActions() {
         return actions;
      }

      @Override
      public @Nullable List<Action> match(ReflexDriverContext ctx, MessageBody msg) {
         Map<String,Object> attrs = msg.getAttributes();
         if (attrs != null && attrs.containsKey(attr) && Objects.equals(value,attrs.get(attr))) {
            ctx.markSetAttributeConsumed(attr);
            return actions;
         }

         return null;
      }
   }

   private static interface ProtocolMatch {
      int getReflexVersion();

      void setDebug(boolean debug);
      boolean isDebug();

      @Nullable List<Action> getActions();
      @Nullable List<Action> match(ReflexDriverContext ctx, byte[] protocol);

      void debug(ReflexDriverContext ctx, byte[] protocol);
   }

   private static abstract class AbstractProtocolMatch implements ProtocolMatch {
      private boolean debug;

      @Override
      public void setDebug(boolean debug) {
         this.debug = debug;
      }

      @Override
      public boolean isDebug() {
         return this.debug;
      }
   }

   private static final class RegexMatch extends AbstractProtocolMatch {
      RegexDfaByte<List<Action>> dfa;

      public RegexMatch(RegexDfaByte<List<Action>> dfa) {
         this.dfa = dfa;
      }

      public static int getRequiredReflexVersion() {
         return V1;
      }

      @Override
      public int getReflexVersion() {
         return getRequiredReflexVersion();
      }

      @Override
      public @Nullable List<Action> getActions() {
         ImmutableList.Builder<Action> bld = ImmutableList.<Action>builder();
         for (RegexDfaByte.State<List<Action>> state : dfa.getStates()) {
            List<Action> actions = state.getValue();
            if (actions != null) {
               bld.addAll(actions);
            }
         }

         return bld.build();
      }

      //////////////////////////////////////////////////////////////////////////
      // Parsing and Matching Support
      //
      // NOTE: If this code is updated the debugging code will need to be
      //       updated as well.
      //////////////////////////////////////////////////////////////////////////

      @Override
      public @Nullable List<Action> match(ReflexDriverContext ctx, byte[] protocol) {
         long start = 0;
         if (log.isTraceEnabled()) {
            start = System.nanoTime();
         }

         List<Action> results = dfa.matching(protocol);

         if (log.isTraceEnabled()) {
            long elapsed = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - start);
            if (results != null) {
               log.trace("regex match test took {}us (match)", elapsed);
            } else {
               log.trace("regex match test took {}us (no match)", elapsed);
            }
         }

         return results;
      }

      //////////////////////////////////////////////////////////////////////////
      // Debugging Support
      //////////////////////////////////////////////////////////////////////////

      @Override
      public void debug(ReflexDriverContext ctx, byte[] protocol) {
         StringBuilder prefixMatched = new StringBuilder();

         int mismatchLocation = -1;
         byte mismatchByte = 0;

         RegexDfaByte<?>.Matcher match = dfa.matcher();
         RegexDfaByte.State<?> lastGoodState = match.current();

         for (int i=0, e=protocol.length; i<e; ++i) {
            if (match.process(protocol[i])) {
               mismatchLocation = i;
               mismatchByte = protocol[i];
               break;
            }

            lastGoodState = match.current();
            if (prefixMatched.length() != 0) prefixMatched.append(" ");
            prefixMatched.append(ProtocUtil.toHexString(protocol[i]));
         }

         if (!match.matched()) {
            Set<Byte> available = (lastGoodState != null)
               ? ImmutableSet.<Byte>copyOf(lastGoodState.getTransitions().knownTransitionSymbols())
               : ImmutableSet.<Byte>of();

            List<Byte> sorted = new ArrayList<>(available);
            Collections.sort(sorted);

            StringBuilder avail = new StringBuilder();
            avail.append("{");
            for (Byte next : sorted) {
               if (avail.length() != 1) avail.append(",");
               avail.append(ProtocUtil.toHexString(next));
            }
            avail.append("}");

            log.info(
               "[reflex debugging] protocol message did not match dfa:\n" +
               "    match successful prefix: {}\n" +
               "    match failure location:  {}\n" +
               "    match failure reason:    {} not in transition set {}",
               prefixMatched,
               mismatchLocation,
               ProtocUtil.toHexString(mismatchByte),
               avail
            );
            return;
         }
      }
   }

   private static final class AlertmeLifesignMatch extends AbstractProtocolMatch {
      private static final byte ZCL_TYPE = (byte)ZigbeeMessage.Zcl.ID;
      private static final byte AME_LS = (byte)com.iris.protocol.zigbee.alertme.AMGeneral.Lifesign.ID;

      private final List<Action> actions;
      private final String parseCheck;

      private final int setMask;
      private final int clrMask;

      private final byte ep;
      private final byte prh;
      private final byte prl;
      private final byte clh;
      private final byte cll;

      public AlertmeLifesignMatch(ReflexMatchAlertmeLifesign ls, List<Action> actions) {
         this.actions = actions;

         this.setMask = (ls.getSetMask() < 0) ? 0 : ls.getSetMask();
         this.clrMask = (ls.getClrMask() < 0) ? 0 : ls.getClrMask();

         this.ep = (byte)ls.getEndpoint();
         this.prh = (byte)(ls.getProfile() >> 8);
         this.prl = (byte)ls.getProfile();
         this.clh = (byte)(ls.getCluster() >> 8);
         this.cll = (byte)ls.getCluster();

         this.parseCheck = "ameLifesignParsed-" + ep + "-" + clh + "-" + cll;
      }

      public static int getRequiredReflexVersion() {
         return V1;
      }

      @Override
      public int getReflexVersion() {
         return getRequiredReflexVersion();
      }

      @Override
      public @Nullable List<Action> getActions() {
         return actions;
      }

      //////////////////////////////////////////////////////////////////////////
      // Parsing and Matching Support
      //
      // NOTE: If this code is updated the debugging code will need to be
      //       updated as well.
      //////////////////////////////////////////////////////////////////////////

      @Override
      public @Nullable List<Action> match(ReflexDriverContext ctx, byte[] protocol) {
         parseAlertmeLifesign(ctx, protocol);
         if (matchesAlertmeLifesign(ctx)) {
            return actions;
         }

         return null;
      }

      private void parseAlertmeLifesign(ReflexDriverContext ctx, byte[] protocol) {
         if (ctx.getVariable(parseCheck,false)) {
            return;
         }

         ctx.setVariable(parseCheck, true);
         if (protocol.length < 29 ||
             protocol[0] != ZCL_TYPE ||   // ZCL message type
             protocol[5] != AME_LS  ||    // AlertMe Lifesign
             protocol[7] != prl ||        // Profile
             protocol[8] != prh ||        // Profile
             protocol[9] != cll ||        // Cluster
             protocol[10] != clh ||       // Cluster
             protocol[11] != ep) {        // Endpoint
            return;
         }

         int flgs = protocol[16] & 0xFF;
         ctx.setVariable("ameLifesignFlags", flgs);
         if ((flgs & com.iris.protocol.zigbee.alertme.AMGeneral.Lifesign.LIFESIGN_HAS_VOLTAGE) != 0) {
            int volts = ((protocol[22] & 0xFF) << 8) | (protocol[21] & 0xFF);
            ctx.setVariable("ameLifesignVolts", volts);
         }

         if ((flgs & com.iris.protocol.zigbee.alertme.AMGeneral.Lifesign.LIFESIGN_HAS_TEMPERATURE) != 0) {
            int temp = ((protocol[24] & 0xFF) << 8) | (protocol[23] & 0xFF);
            ctx.setVariable("ameLifesignTemp", temp);
         }

         if ((flgs & com.iris.protocol.zigbee.alertme.AMGeneral.Lifesign.LIFESIGN_HAS_LQI) != 0) {
            int lqi = protocol[26] & 0xFF;
            ctx.setVariable("ameLifesignLqi", lqi);
         }

         if ((flgs & com.iris.protocol.zigbee.alertme.AMGeneral.Lifesign.LIFESIGN_HAS_SWITCH_STATUS) != 0) {
            int msk = protocol[27] & 0xFF;
            int sw = protocol[28] & 0xFF;
            ctx.setVariable("ameLifesignMask", msk);
            ctx.setVariable("ameLifesignSw", sw);
         }
      }

      private boolean matchesAlertmeLifesign(ReflexDriverContext ctx) {
         Integer mask = ctx.getVariable("ameLifesignMask");
         Integer sw = ctx.getVariable("ameLifesignSw");
         if (mask == null || sw == null) {
            return false;
         }

         int sta = sw & mask;
         return ((sta & setMask) == setMask) && ((~sta & clrMask) == clrMask);
      }

      //////////////////////////////////////////////////////////////////////////
      // Debugging Support
      //////////////////////////////////////////////////////////////////////////
      
      @Override
      public void debug(ReflexDriverContext ctx, byte[] protocol) {
         debugParseAlertmeLifesign(ctx, protocol);
         debugMatchAlertmeLifesign(ctx);
      }

      private void debugParseAlertmeLifesign(ReflexDriverContext ctx, byte[] protocol) {
         if (protocol.length < 29) {
            log.info(
               "[reflex debugging] protocol message did not match alertme lifesign:\n" +
               "    length check:   {} >= 29",
               protocol.length
            );
            return;
         }

         if (protocol[0] != ZCL_TYPE ||   // ZCL message type
             protocol[5] != AME_LS  ||    // AlertMe Lifesign
             protocol[7] != prl ||        // Profile
             protocol[8] != prh ||        // Profile
             protocol[9] != cll ||        // Cluster
             protocol[10] != clh ||       // Cluster
             protocol[11] != ep) {        // Endpoint
            log.info(
               "[reflex debugging] protocol message did not match alertme lifesign:\n" +
               "    zcl check:      {} == {}\n" +
               "    command check:  {} == {}\n" +
               "    profile check:  {} == {} && {} == {}\n" +
               "    cluster check:  {} == {} && {} == {}\n" +
               "    endpoint check: {} == {}",
               ProtocUtil.toHexString(protocol[0]), ProtocUtil.toHexString(ZCL_TYPE),
               ProtocUtil.toHexString(protocol[5]), ProtocUtil.toHexString(AME_LS),
               ProtocUtil.toHexString(protocol[7]), ProtocUtil.toHexString(prl),
               ProtocUtil.toHexString(protocol[8]), ProtocUtil.toHexString(prh),
               ProtocUtil.toHexString(protocol[9]), ProtocUtil.toHexString(cll),
               ProtocUtil.toHexString(protocol[10]), ProtocUtil.toHexString(clh),
               ProtocUtil.toHexString(protocol[11]), ProtocUtil.toHexString(ep)
            );
            return;
         }

         int flgs = protocol[16] & 0xFF;
         boolean hasVolts = ((flgs & com.iris.protocol.zigbee.alertme.AMGeneral.Lifesign.LIFESIGN_HAS_VOLTAGE) != 0);

         boolean hasTemp = ((flgs & com.iris.protocol.zigbee.alertme.AMGeneral.Lifesign.LIFESIGN_HAS_TEMPERATURE) != 0);

         boolean hasLqi = ((flgs & com.iris.protocol.zigbee.alertme.AMGeneral.Lifesign.LIFESIGN_HAS_LQI) != 0);

         boolean hasSw = ((flgs & com.iris.protocol.zigbee.alertme.AMGeneral.Lifesign.LIFESIGN_HAS_SWITCH_STATUS) != 0);
         if (!hasSw) {
            log.info(
               "[reflex debugging] alertme lifesign protocol message does not contain switch status:\n" +
               "    msg flags:     {}\n" +
               "    has sw status: {}\n" +
               "    has voltage:   {}\n" +
               "    has temp:      {}\n" +
               "    has lqi:       {}",
               ProtocUtil.toHexString(flgs),
               hasSw,
               hasVolts,
               hasTemp,
               hasLqi
            );
            return;
         }
      }

      private void debugMatchAlertmeLifesign(ReflexDriverContext ctx) {
         Integer mask = ctx.getVariable("ameLifesignMask");
         Integer sw = ctx.getVariable("ameLifesignSw");
         if (mask == null || sw == null) {
            return;
         }

         int sta = sw & mask;
         if (!(((sta & setMask) == setMask) && ((~sta & clrMask) == clrMask))) {
            log.info(
               "[reflex debugging] alertme lifesign message did not match:\n" +
               "    switch flags:  {}\n" +
               "    switch mask:   {}\n" +
               "    switch status: {}\n" +
               "    set flags check: {} & {} == {}\n" +
               "    clr flags check: {} & {} == {}" +
               ProtocUtil.toHexString(sw),
               ProtocUtil.toHexString(mask),
               ProtocUtil.toHexString(sta),
               ProtocUtil.toHexString(sta), ProtocUtil.toHexString(setMask), ProtocUtil.toHexString(setMask),
               ProtocUtil.toHexString(~sta), ProtocUtil.toHexString(clrMask), ProtocUtil.toHexString(clrMask)
            );
         }
      }
   }

   private static class ZigbeeAttributeMatch extends AbstractProtocolMatch {
      private static final byte ZCL_TYPE = (byte)ZigbeeMessage.Zcl.ID;
      private static final byte ATTR_RSP = (byte)com.iris.protocol.zigbee.zcl.General.ZclReadAttributesResponse.ID;
      private static final byte ATTR_RPT = (byte)com.iris.protocol.zigbee.zcl.General.ZclReportAttributes.ID;

      private final List<Action> actions;
      private final String parseCheckRead;
      private final String parseCheckReport;

      private final boolean read;
      private final boolean report;

      private final int attr;
      private final @Nullable ZclData value;

      private final byte ep;
      private final byte prh;
      private final byte prl;
      private final byte clh;
      private final byte cll;

      private @Nullable final Byte msh;
      private @Nullable final Byte msl;
      private @Nullable final Byte flg;

      public ZigbeeAttributeMatch(ReflexMatchZigbeeAttribute attr, List<Action> actions) {
         this.actions = actions;

         switch (attr.getType()) {
         case READ:
            this.read = true;
            this.report = false;
            break;
         case REPORT:
            this.read = false;
            this.report = true;
            break;
         case BOTH:
            this.read = true;
            this.report = true;
            break;
         default:
            log.warn("unknown zigbee attributes match type: " + attr.getType());
            this.read = true;
            this.report = true;
            break;
         }

         this.ep = (byte)attr.getEndpoint();
         this.prh = (byte)(attr.getProfile() >> 8);
         this.prl = (byte)attr.getProfile();
         this.clh = (byte)(attr.getCluster() >> 8);
         this.cll = (byte)attr.getCluster();

         this.attr = attr.getAttr();
         this.value = attr.getValue();

         Integer manuf = attr.getManufacturer();
         if (manuf != null) {
            this.msh = (byte)(manuf.intValue() >> 8);
            this.msl = (byte)manuf.intValue();
         } else {
            this.msh = null;
            this.msl = null;
         }

         Integer flags = attr.getFlags();
         if (flags != null) {
            this.flg = flags.byteValue();
         } else {
            this.flg = null;
         }

         String parseCheckRead = "zbAttrReadParsed-" + ep + "-" + clh + "-" + cll;
         String parseCheckReport = "zbAttrReportParsed-" + ep + "-" + clh + "-" + cll;

         if (msh != null && msl != null) {
            parseCheckRead = parseCheckRead + "-" + msh + "-" + msl;
            parseCheckReport = parseCheckReport + "-" + msh + "-" + msl;
         }

         if (flg != null) {
            parseCheckRead = parseCheckRead + "-" + flg;
            parseCheckReport = parseCheckReport + "-" + flg;
         }

         this.parseCheckRead = parseCheckRead;
         this.parseCheckReport = parseCheckReport;
      }

      public static int getRequiredReflexVersion(boolean hasManuf, boolean hasFlags) {
         return (hasManuf || hasFlags) ? V2 : V1;
      }

      public static int getRequiredReflexVersion(ReflexMatchZigbeeAttribute attrs) {
         return getRequiredReflexVersion(attrs.getManufacturer() != null, attrs.getFlags() != null);
      }

      @Override
      public int getReflexVersion() {
         return getRequiredReflexVersion(msh != null && msl != null, flg != null);
      }

      @Override
      public @Nullable List<Action> getActions() {
         return actions;
      }

      //////////////////////////////////////////////////////////////////////////
      // Parsing and Matching Support
      //
      // NOTE: If this code is updated the debugging code will need to be
      //       updated as well.
      //////////////////////////////////////////////////////////////////////////

      @Override
      public @Nullable List<Action> match(ReflexDriverContext ctx, byte[] protocol) {
         if (read) {
            parseZigbeeAttrRead(ctx, protocol, parseCheckRead, ep, prh, prl, clh, cll, msh, msl, flg);
         }

         if (report) {
            parseZigbeeAttrReport(ctx, protocol, parseCheckRead, parseCheckReport, ep, prh, prl, clh, cll, msh, msl, flg);
         }

         if (matchesZigbeeAttributes(ctx, parseCheckRead, attr, value) != null) {
            return actions;
         }

         return null;
      }

      private static void parseZigbeeAttrRead(ReflexDriverContext ctx, byte[] protocol, String parseCheckRead, byte ep,
         byte prh, byte prl, byte clh, byte cll, @Nullable Byte msh, @Nullable Byte msl, @Nullable Byte flg) {
         if (ctx.getVariable(parseCheckRead,false)) {
            return;
         }

         ctx.setVariable(parseCheckRead, true);
         int offset;
         if (msh != null && msl != null) {
            offset = 2;
            if (protocol.length < 18 ||
               protocol[0] != ZCL_TYPE ||   // ZCL message type
               protocol[5] != ATTR_RSP  ||  // ZCL General Read Attribute Response
               protocol[7] != prl ||        // Profile
               protocol[8] != prh ||        // Profile
               protocol[9] != cll ||        // Cluster
               protocol[10] != clh ||       // Cluster
               protocol[11] != msl ||       // Manuf Code
               protocol[12] != msh ||       // Manuf Code
               protocol[13] != ep) {        // Endpoint
               return;
            }
         } else {
            offset = 0;
            if (protocol.length < 16 ||
               protocol[0] != ZCL_TYPE ||   // ZCL message type
               protocol[5] != ATTR_RSP  ||  // ZCL General Read Attribute Response
               protocol[7] != prl ||        // Profile
               protocol[8] != prh ||        // Profile
               protocol[9] != cll ||        // Cluster
               protocol[10] != clh ||       // Cluster
               protocol[11] != ep) {        // Endpoint
               return;
            }
         }

         if (flg != null && protocol[6] != flg) {
            return;
         }

         try {
            com.iris.protocol.zigbee.zcl.General.ZclReadAttributesResponse report = com.iris.protocol.zigbee.zcl.General.ZclReadAttributesResponse.serde().fromBytes(ByteOrder.LITTLE_ENDIAN, protocol, 16+offset, protocol.length-16-offset);
            for ( com.iris.protocol.zigbee.zcl.General.ZclReadAttributeRecord rd : report.getAttributes()) {
               if (rd.getStatus() == 0) {
                  ctx.setVariable(parseCheckRead + rd.getAttributeIdentifier(), rd.getAttributeData());
               }
            }
         } catch (Exception ex) {
            // ignore
         }
      }

      private static void parseZigbeeAttrReport(ReflexDriverContext ctx, byte[] protocol, String parseCheckRead, String parseCheckReport, byte ep,
         byte prh, byte prl, byte clh, byte cll, @Nullable Byte msh, @Nullable Byte msl, @Nullable Byte flg) {
         if (ctx.getVariable(parseCheckReport,false)) {
            return;
         }

         ctx.setVariable(parseCheckReport, true);
         int offset;
         if (msh != null && msl != null) {
            offset = 2;
            if (protocol.length < 18 ||
               protocol[0] != ZCL_TYPE ||   // ZCL message type
               protocol[5] != ATTR_RPT  ||  // ZCL General Attribute Report
               protocol[7] != prl ||        // Profile
               protocol[8] != prh ||        // Profile
               protocol[9] != cll ||        // Cluster
               protocol[10] != clh ||       // Cluster
               protocol[11] != msl ||       // Manuf Code
               protocol[12] != msh ||       // Manuf Code
               protocol[13] != ep) {        // Endpoint
               return;
            }
         } else {
            offset = 0;
            if (protocol.length < 16 ||
               protocol[0] != ZCL_TYPE ||   // ZCL message type
               protocol[5] != ATTR_RPT  ||  // ZCL General Attribute Report
               protocol[7] != prl ||        // Profile
               protocol[8] != prh ||        // Profile
               protocol[9] != cll ||        // Cluster
               protocol[10] != clh ||       // Cluster
               protocol[11] != ep) {        // Endpoint
               return;
            }
         }

         if (flg != null && protocol[6] != flg) {
            return;
         }

         try {
            com.iris.protocol.zigbee.zcl.General.ZclReportAttributes report = com.iris.protocol.zigbee.zcl.General.ZclReportAttributes.serde().fromBytes(ByteOrder.LITTLE_ENDIAN, protocol, offset+16, protocol.length-16-offset);
            for ( com.iris.protocol.zigbee.zcl.General.ZclAttributeReport rpt : report.getAttributes()) {
               ctx.setVariable(parseCheckRead + rpt.getAttributeIdenifier(), rpt.getAttributeData());
            }
         } catch (Exception ex) {
            // ignore
         }
      }

      private static @Nullable ZclData matchesZigbeeAttributes(ReflexDriverContext ctx, String parseCheckRead, int attr, @Nullable ZclData value) {
         ZclData attrValue = ctx.getVariable(parseCheckRead + attr);
         if (attrValue != null) {
            ZclData check = value;
            if (check == null || check.equals(attrValue)) {
               return attrValue;
            }
         }

         return null;
      }

      //////////////////////////////////////////////////////////////////////////
      // Debugging Support
      //////////////////////////////////////////////////////////////////////////

      @Override
      public void debug(ReflexDriverContext ctx, byte[] protocol) {
         if (read) {
            debugParseZigbeeAttrRead(ctx, protocol, parseCheckRead, ep, prh, prl, clh, cll, msh, msl, flg, "attribute read response");
         }

         if (report) {
            debugParseZigbeeAttrReport(ctx, protocol, parseCheckRead, parseCheckReport, ep, prh, prl, clh, cll, msh, msl, flg, "attribute report");
         }

         debugMatchesZigbeeAttributes(ctx, parseCheckRead, attr, value, "attribute");
      }

      private static void debugParseZigbeeAttrRead(ReflexDriverContext ctx, byte[] protocol, String parseCheckRead, byte ep,
         byte prh, byte prl, byte clh, byte cll, @Nullable Byte msh, @Nullable Byte msl, @Nullable Byte flg, String type) {
         int sz = (msh != null && msl != null) ? 18 : 16;
         if (protocol.length < sz) {
            log.info(
               "[reflex debugging] protocol message did not match zigbee {}:\n" +
               "    length check:   {} >= {}",
               type,
               protocol.length,
               sz
            );
            return;
         }

         if (msh != null && msl != null) {
            if (protocol[0] != ZCL_TYPE ||  // ZCL message type
               protocol[5] != ATTR_RSP  ||  // ZCL General Read Attribute Response
               protocol[7] != prl ||        // Profile
               protocol[8] != prh ||        // Profile
               protocol[9] != cll ||        // Cluster
               protocol[10] != clh ||       // Cluster
               protocol[11] != msl ||       // Manuf Code
               protocol[12] != msh ||       // Manuf Code
               protocol[13] != ep) {        // Endpoint
               log.info(
                  "[reflex debugging] protocol message did not match zigbee {}:\n" +
                  "    zcl check:      {} == {}\n" +
                  "    command check:  {} == {}\n" +
                  "    profile check:  {} == {} && {} == {}\n" +
                  "    cluster check:  {} == {} && {} == {}\n" +
                  "    manuf check:    {} == {} && {} == {}\n" +
                  "    endpoint check: {} == {}",
                  type,
                  ProtocUtil.toHexString(protocol[0]), ProtocUtil.toHexString(ZCL_TYPE),
                  ProtocUtil.toHexString(protocol[5]), ProtocUtil.toHexString(ATTR_RSP),
                  ProtocUtil.toHexString(protocol[7]), ProtocUtil.toHexString(prl),
                  ProtocUtil.toHexString(protocol[8]), ProtocUtil.toHexString(prh),
                  ProtocUtil.toHexString(protocol[9]), ProtocUtil.toHexString(cll),
                  ProtocUtil.toHexString(protocol[10]), ProtocUtil.toHexString(clh),
                  ProtocUtil.toHexString(protocol[11]), ProtocUtil.toHexString(msl),
                  ProtocUtil.toHexString(protocol[12]), ProtocUtil.toHexString(msh),
                  ProtocUtil.toHexString(protocol[13]), ProtocUtil.toHexString(ep)
               );
               return;
            }
         } else {
            if (protocol[0] != ZCL_TYPE ||  // ZCL message type
               protocol[5] != ATTR_RSP  ||  // ZCL General Read Attribute Response
               protocol[7] != prl ||        // Profile
               protocol[8] != prh ||        // Profile
               protocol[9] != cll ||        // Cluster
               protocol[10] != clh ||       // Cluster
               protocol[11] != ep) {        // Endpoint
               log.info(
                  "[reflex debugging] protocol message did not match zigbee {}:\n" +
                  "    zcl check:      {} == {}\n" +
                  "    command check:  {} == {}\n" +
                  "    profile check:  {} == {} && {} == {}\n" +
                  "    cluster check:  {} == {} && {} == {}\n" +
                  "    endpoint check: {} == {}",
                  type,
                  ProtocUtil.toHexString(protocol[0]), ProtocUtil.toHexString(ZCL_TYPE),
                  ProtocUtil.toHexString(protocol[5]), ProtocUtil.toHexString(ATTR_RSP),
                  ProtocUtil.toHexString(protocol[7]), ProtocUtil.toHexString(prl),
                  ProtocUtil.toHexString(protocol[8]), ProtocUtil.toHexString(prh),
                  ProtocUtil.toHexString(protocol[9]), ProtocUtil.toHexString(cll),
                  ProtocUtil.toHexString(protocol[10]), ProtocUtil.toHexString(clh),
                  ProtocUtil.toHexString(protocol[11]), ProtocUtil.toHexString(ep)
               );
               return;
            }
         }

         if (flg != null && protocol[6] != flg) {
            log.info(
               "[reflex debugging] protocol message did not match zigbee {}:\n" +
               "    flag check:     {} == {}",
               type, ProtocUtil.toHexString(protocol[6]), ProtocUtil.toHexString(flg)
            );
            return;
         }
      }

      private static void debugParseZigbeeAttrReport(ReflexDriverContext ctx, byte[] protocol, String parseCheckRead, String parseCheckReport, byte ep,
         byte prh, byte prl, byte clh, byte cll, @Nullable Byte msh, @Nullable Byte msl, @Nullable Byte flg, String type) {
         int sz = (msh != null && msl != null) ? 18 : 16;
         if (protocol.length < sz) {
            log.info(
               "[reflex debugging] protocol message did not match zigbee {}:\n" +
               "    length check:   {} >= {}",
               type,
               protocol.length,
               sz
            );
            return;
         }

         if (msh != null && msl != null) {
            if (protocol[0] != ZCL_TYPE ||   // ZCL message type
               protocol[5] != ATTR_RPT ||   // ZCL General Attribute Report
               protocol[7] != prl ||        // Profile
               protocol[8] != prh ||        // Profile
               protocol[9] != cll ||        // Cluster
               protocol[10] != clh ||       // Cluster
               protocol[11] != msl ||       // Manuf Code
               protocol[12] != msh ||       // Manuf Code
               protocol[13] != ep) {        // Endpoint
               log.info(
                  "[reflex debugging] protocol message did not match zigbee {}:\n" +
                  "    zcl check:      {} == {}\n" +
                  "    command check:  {} == {}\n" +
                  "    profile check:  {} == {} && {} == {}\n" +
                  "    cluster check:  {} == {} && {} == {}\n" +
                  "    endpoint check: {} == {}",
                  type,
                  ProtocUtil.toHexString(protocol[0]), ProtocUtil.toHexString(ZCL_TYPE),
                  ProtocUtil.toHexString(protocol[5]), ProtocUtil.toHexString(ATTR_RPT),
                  ProtocUtil.toHexString(protocol[7]), ProtocUtil.toHexString(prl),
                  ProtocUtil.toHexString(protocol[8]), ProtocUtil.toHexString(prh),
                  ProtocUtil.toHexString(protocol[9]), ProtocUtil.toHexString(cll),
                  ProtocUtil.toHexString(protocol[10]), ProtocUtil.toHexString(clh),
                  ProtocUtil.toHexString(protocol[11]), ProtocUtil.toHexString(msl),
                  ProtocUtil.toHexString(protocol[12]), ProtocUtil.toHexString(msh),
                  ProtocUtil.toHexString(protocol[13]), ProtocUtil.toHexString(ep)
               );
               return;
            }
         } else {
            if (protocol[0] != ZCL_TYPE ||   // ZCL message type
               protocol[5] != ATTR_RPT ||   // ZCL General Attribute Report
               protocol[7] != prl ||        // Profile
               protocol[8] != prh ||        // Profile
               protocol[9] != cll ||        // Cluster
               protocol[10] != clh ||       // Cluster
               protocol[11] != ep) {        // Endpoint
               log.info(
                  "[reflex debugging] protocol message did not match zigbee {}:\n" +
                  "    zcl check:      {} == {}\n" +
                  "    command check:  {} == {}\n" +
                  "    profile check:  {} == {} && {} == {}\n" +
                  "    cluster check:  {} == {} && {} == {}\n" +
                  "    endpoint check: {} == {}",
                  type,
                  ProtocUtil.toHexString(protocol[0]), ProtocUtil.toHexString(ZCL_TYPE),
                  ProtocUtil.toHexString(protocol[5]), ProtocUtil.toHexString(ATTR_RPT),
                  ProtocUtil.toHexString(protocol[7]), ProtocUtil.toHexString(prl),
                  ProtocUtil.toHexString(protocol[8]), ProtocUtil.toHexString(prh),
                  ProtocUtil.toHexString(protocol[9]), ProtocUtil.toHexString(cll),
                  ProtocUtil.toHexString(protocol[10]), ProtocUtil.toHexString(clh),
                  ProtocUtil.toHexString(protocol[11]), ProtocUtil.toHexString(ep)
               );
               return;
            }
         }

         if (flg != null && protocol[6] != flg) {
            log.info(
               "[reflex debugging] protocol message did not match zigbee {}:\n" +
               "    flag check:     {} == {}",
               type, ProtocUtil.toHexString(protocol[6]), ProtocUtil.toHexString(flg)
            );
            return;
         }
      }

      private static @Nullable ZclData debugMatchesZigbeeAttributes(ReflexDriverContext ctx, String parseCheckRead, int attr, @Nullable ZclData value, String type) {
         ZclData attrValue = ctx.getVariable(parseCheckRead + attr);
         if (attrValue == null) {
            log.info(
               "[reflex debugging] zigbee {} wasn't reported in message:\n" +
               "    attribute not reported:  {}",
               type,
               ProtocUtil.toHexString((short)attr)
            );
            return null;
         }

         ZclData check = value;
         if (!(check == null || check.equals(attrValue))) {
            log.info(
               "[reflex debugging] zigbee {} didn't match expected value:\n" +
               "    attribute expected: {}\n" +
               "    attribute reported: {}",
               type,
               check,
               attrValue
            );
            return null;
         }

         return attrValue;
      }
   }

   private static final class ZigbeeIasZoneStatusMatch extends AbstractProtocolMatch {
      private static final byte ZCL_TYPE = (byte)ZigbeeMessage.Zcl.ID;
      private static final byte IAS_SC = (byte)IasZone.ZoneStatusChangeNotification.ID;

      private final List<Action> actions;
      private final String parseCheckStatus;
      private final String parseCheckRead;
      private final String parseCheckReport;
      private final String iasZoneStatus;
      private final String iasZoneStatusDelay;

      private final boolean attr;
      private final boolean notification;

      private final int setMask;
      private final int clrMask;
      private final int maxDelay;

      private final byte ep;
      private final byte prh;
      private final byte prl;
      private final byte clh;
      private final byte cll;

      private final @Nullable Byte msh;
      private final @Nullable Byte msl;
      private final @Nullable Byte flg;

      public ZigbeeIasZoneStatusMatch(ReflexMatchZigbeeIasZoneStatus status, List<Action> actions) {
         this.actions = actions;

         switch (status.getType()) {
         case ATTR:
            this.attr = true;
            this.notification = false;
            break;
         case NOTIFICATION:
            this.attr = false;
            this.notification = true;
            break;
         case BOTH:
            this.attr = true;
            this.notification = true;
            break;

         default:
            log.warn("unknown zigbee ias zone status match type: " + status.getType());
            this.attr = true;
            this.notification = true;
            break;
         }

         this.setMask = (status.getSetMask() < 0) ? 0 : status.getSetMask();
         this.clrMask = (status.getClrMask() < 0) ? 0 : status.getClrMask();
         this.maxDelay = status.getMaxChangeDelay();

         this.ep = (byte)status.getEndpoint();
         this.prh = (byte)(status.getProfile() >> 8);
         this.prl = (byte)status.getProfile();
         this.clh = (byte)(status.getCluster() >> 8);
         this.cll = (byte)status.getCluster();

         Integer manuf = status.getManufacturer();
         if (manuf != null) {
            this.msh = (byte)(manuf.intValue() >> 8);
            this.msl = (byte)manuf.intValue();
         } else {
            this.msh = null;
            this.msl = null;
         }

         Integer flags = status.getFlags();
         if (flags != null) {
            this.flg = flags.byteValue();
         } else {
            this.flg = null;
         }

         String parseCheckStatus = "iasZoneStatusParsed-" + ep + "-" + clh + "-" + cll;
         String parseCheckRead = "zbAttrReadParsed-" + ep + "-" + clh + "-" + cll;
         String parseCheckReport = "zbAttrReportParsed-" + ep + "-" + clh + "-" + cll;
         String iasZoneStatusDelay = "iasZoneStatusDelay-" + ep + "-" + clh + "-" + cll;
         String iasZoneStatus = "iasZoneStatus-" + ep + "-" + clh + "-" + cll;

         if (msh != null && msl != null) {
            parseCheckStatus = parseCheckStatus + "-" + msh + "-" + msl;
            parseCheckRead = parseCheckRead + "-" + msh + "-" + msl;
            parseCheckReport = parseCheckReport + "-" + msh + "-" + msl;
            iasZoneStatusDelay = iasZoneStatusDelay + "-" + msh + "-" + msl;
            iasZoneStatus = iasZoneStatus + "-" + msh + "-" + msl;
         }

         if (flg != null) {
            parseCheckStatus = parseCheckStatus + "-" + flg;
            parseCheckRead = parseCheckRead + "-" + flg;
            parseCheckReport = parseCheckReport + "-" + flg;
            iasZoneStatusDelay = iasZoneStatusDelay + "-" + flg;
            iasZoneStatus = iasZoneStatus + "-" + flg;
         }

         this.parseCheckStatus = parseCheckStatus;
         this.parseCheckRead = parseCheckRead;
         this.parseCheckReport = parseCheckReport;
         this.iasZoneStatusDelay = iasZoneStatusDelay;
         this.iasZoneStatus = iasZoneStatus;
      }

      public static int getRequiredReflexVersion(boolean hasManuf, boolean hasFlag) {
         return (hasManuf || hasFlag) ? V2 : V1;
      }

      public static int getRequiredReflexVersion(ReflexMatchZigbeeIasZoneStatus status) {
         return getRequiredReflexVersion(status.getManufacturer() != null, status.getFlags() != null);
      }

      @Override
      public int getReflexVersion() {
         return getRequiredReflexVersion(msh != null && msl != null, flg != null);
      }

      @Override
      public @Nullable List<Action> getActions() {
         return actions;
      }

      //////////////////////////////////////////////////////////////////////////
      // Parsing and Matching Support
      //
      // NOTE: If this code is updated the debugging code will need to be
      //       updated as well.
      //////////////////////////////////////////////////////////////////////////

      @Override
      public @Nullable List<Action> match(ReflexDriverContext ctx, byte[] protocol) {
         if (notification) {
            parseIasZoneStatusChangeNotification(ctx, protocol);
            if (matchesIasZoneStatusChangeNotification(ctx)) {
               return actions;
            }
         }

         if (attr) {
            parseIasZoneAttributes(ctx, protocol);
            if (matchesIasZoneAttributes(ctx)) {
               return actions;
            }
         }

         return null;
      }

      private void parseIasZoneAttributes(ReflexDriverContext ctx, byte[] protocol) {
         ZigbeeAttributeMatch.parseZigbeeAttrRead(ctx, protocol, parseCheckRead, ep, prh, prl, clh, cll, msh, msl, flg);
         ZigbeeAttributeMatch.parseZigbeeAttrReport(ctx, protocol, parseCheckRead, parseCheckReport, ep, prh, prl, clh, cll, msh, msl, flg);
      }

      private boolean matchesIasZoneAttributes(ReflexDriverContext ctx) {
         ZclData zvalue = ZigbeeAttributeMatch.matchesZigbeeAttributes(ctx, parseCheckRead, IasZone.ATTR_ZONE_STATUS, null);
         if (zvalue == null) {
            return false;
         }

         Object value = zvalue.getDataValue(); 
         if (value instanceof Number) {
            int sta = ((Number)value).intValue();
            return ((sta & setMask) == setMask) && ((~sta & clrMask) == clrMask);
         }

         return false;
      }

      private void parseIasZoneStatusChangeNotification(ReflexDriverContext ctx, byte[] protocol) {
         if (ctx.getVariable(parseCheckStatus,false)) {
            return;
         }

         ctx.setVariable(parseCheckStatus, true);
         int offset;
         if (msh != null && msl != null) {
            offset = 2;
            if (protocol.length < 21 ||
               protocol[0] != ZCL_TYPE ||   // ZCL message type
               protocol[5] != IAS_SC  ||    // IAS Zone Status Change
               protocol[7] != prl ||        // Profile
               protocol[8] != prh ||        // Profile
               protocol[9] != cll ||        // Cluster
               protocol[10] != clh ||       // Cluster
               protocol[11] != msl ||       // Manuf Code
               protocol[12] != msh ||       // Manuf Code
               protocol[13] != ep) {        // Endpoint
               return;
            }
         } else {
            offset = 0;
            if (protocol.length < 19 ||
               protocol[0] != ZCL_TYPE ||   // ZCL message type
               protocol[5] != IAS_SC  ||    // IAS Zone Status Change
               protocol[7] != prl ||        // Profile
               protocol[8] != prh ||        // Profile
               protocol[9] != cll ||        // Cluster
               protocol[10] != clh ||       // Cluster
               protocol[11] != ep) {        // Endpoint
               return;
            }
         }

         if (flg != null && protocol[6] != flg) {
            return;
         }

         if (maxDelay > 0 && protocol.length >= (offset + 22)) {
            int delay = ((protocol[offset+21] & 0xFF) << 8) | (protocol[offset+20] & 0xFF);
            ctx.setVariable(iasZoneStatusDelay, delay);
         }

         int sta = ((protocol[offset+17] & 0xFF) << 8) | (protocol[offset+16] & 0xFF);
         ctx.setVariable(iasZoneStatus, sta);
      }

      private boolean matchesIasZoneStatusChangeNotification(ReflexDriverContext ctx) {
         Integer rdelay = ctx.getVariable(iasZoneStatusDelay);
         if (maxDelay > 0 && rdelay != null) {
            if (maxDelay < rdelay) {
               return false;
            }
         }

         Integer rstatus = ctx.getVariable(iasZoneStatus);
         if (rstatus == null) {
            return false;
         }

         int sta = rstatus;
         return ((sta & setMask) == setMask) && ((~sta & clrMask) == clrMask);
      }

      //////////////////////////////////////////////////////////////////////////
      // Debugging Support
      //////////////////////////////////////////////////////////////////////////

      @Override
      public void debug(ReflexDriverContext ctx, byte[] protocol) {
         if (notification) {
            debugParseIasZoneStatusChangeNotification(ctx, protocol);
            debugMatchesIasZoneStatusChangeNotification(ctx);
         }

         if (attr) {
            debugParseIasZoneAttributes(ctx, protocol);
            debugMatchesIasZoneAttributes(ctx);
         }
      }

      private void debugParseIasZoneAttributes(ReflexDriverContext ctx, byte[] protocol) {
         ZigbeeAttributeMatch.debugParseZigbeeAttrRead(ctx, protocol, parseCheckRead, ep, prh, prl, clh, cll, msh, msl, flg, "ias zone attribute read response");
         ZigbeeAttributeMatch.debugParseZigbeeAttrReport(ctx, protocol, parseCheckRead, parseCheckReport, ep, prh, prl, clh, cll, msh, msl, flg, "ias zone attribute report");
      }

      private void debugMatchesIasZoneAttributes(ReflexDriverContext ctx) {
         ZclData zvalue = ZigbeeAttributeMatch.debugMatchesZigbeeAttributes(ctx, parseCheckRead, IasZone.ATTR_ZONE_STATUS, null, "ias zone attribute");
         if (zvalue == null) {
            log.info("[reflex debugging] zigbee ias zone attribute did not match because value is null");
            return;
         }

         Object value = zvalue.getDataValue(); 
         if (value instanceof Number) {
            int sta = ((Number)value).intValue();
            if (!((sta & setMask) == setMask) && ((~sta & clrMask) == clrMask)) {
               log.info(
                  "[reflex debugging] zigbee ias zone status change notification zone status did not match:\n" +
                  "    zone status:     {}\n" +
                  "    set flags check: {} & {} == {}\n" +
                  "    clr flags check: {} & {} == {}" +
                  ProtocUtil.toHexString(sta),
                  ProtocUtil.toHexString(sta), ProtocUtil.toHexString(setMask), ProtocUtil.toHexString(setMask),
                  ProtocUtil.toHexString(~sta), ProtocUtil.toHexString(clrMask), ProtocUtil.toHexString(clrMask)
               );
            }

            return;
         }

         log.info("[reflex debugging] zigbee ias zone attribute did not match because value is not numeric");
      }

      private void debugParseIasZoneStatusChangeNotification(ReflexDriverContext ctx, byte[] protocol) {
         if (msh != null && msl != null) {
            if (protocol.length < 21) {
               log.info(
                  "[reflex debugging] protocol message did not match zigbee ias zone status change notification:\n" +
                  "    length check:   {} >= 21",
                  protocol.length
               );
               return;
            }
         } else {
            if (protocol.length < 19) {
               log.info(
                  "[reflex debugging] protocol message did not match zigbee ias zone status change notification:\n" +
                  "    length check:   {} >= 19",
                  protocol.length
               );
               return;
            }
         }

         if (msh != null && msl != null) {
            if (protocol[0] != ZCL_TYPE ||   // ZCL message type
               protocol[5] != IAS_SC ||     // IAS Zone Status Change
               protocol[7] != prl ||        // Profile
               protocol[8] != prh ||        // Profile
               protocol[9] != cll ||        // Cluster
               protocol[10] != clh ||       // Cluster
               protocol[11] != ep) {        // Endpoint
               log.info(
                  "[reflex debugging] protocol message did not match zigbee ias zone status change notification:\n" +
                  "    zcl check:      {} == {}\n" +
                  "    command check:  {} == {}\n" +
                  "    profile check:  {} == {} && {} == {}\n" +
                  "    cluster check:  {} == {} && {} == {}\n" +
                  "    endpoint check: {} == {}",
                  ProtocUtil.toHexString(protocol[0]), ProtocUtil.toHexString(ZCL_TYPE),
                  ProtocUtil.toHexString(protocol[5]), ProtocUtil.toHexString(IAS_SC),
                  ProtocUtil.toHexString(protocol[7]), ProtocUtil.toHexString(prl),
                  ProtocUtil.toHexString(protocol[8]), ProtocUtil.toHexString(prh),
                  ProtocUtil.toHexString(protocol[9]), ProtocUtil.toHexString(cll),
                  ProtocUtil.toHexString(protocol[10]), ProtocUtil.toHexString(clh),
                  ProtocUtil.toHexString(protocol[11]), ProtocUtil.toHexString(ep)
               );
               return;
            }
         } else { 
            if (protocol[0] != ZCL_TYPE ||   // ZCL message type
               protocol[5] != IAS_SC ||     // IAS Zone Status Change
               protocol[7] != prl ||        // Profile
               protocol[8] != prh ||        // Profile
               protocol[9] != cll ||        // Cluster
               protocol[10] != clh ||       // Cluster
               protocol[11] != ep) {        // Endpoint
               log.info(
                  "[reflex debugging] protocol message did not match zigbee ias zone status change notification:\n" +
                  "    zcl check:      {} == {}\n" +
                  "    command check:  {} == {}\n" +
                  "    profile check:  {} == {} && {} == {}\n" +
                  "    cluster check:  {} == {} && {} == {}\n" +
                  "    endpoint check: {} == {}",
                  ProtocUtil.toHexString(protocol[0]), ProtocUtil.toHexString(ZCL_TYPE),
                  ProtocUtil.toHexString(protocol[5]), ProtocUtil.toHexString(IAS_SC),
                  ProtocUtil.toHexString(protocol[7]), ProtocUtil.toHexString(prl),
                  ProtocUtil.toHexString(protocol[8]), ProtocUtil.toHexString(prh),
                  ProtocUtil.toHexString(protocol[9]), ProtocUtil.toHexString(cll),
                  ProtocUtil.toHexString(protocol[10]), ProtocUtil.toHexString(clh),
                  ProtocUtil.toHexString(protocol[11]), ProtocUtil.toHexString(ep)
               );
               return;
            }
         }

         if (flg != null && protocol[6] != flg) {
            log.info(
               "[reflex debugging] protocol message did not match zigbee ias zone status change notification:\n" +
               "    flag check:     {} == {}",
               ProtocUtil.toHexString(protocol[6]), ProtocUtil.toHexString(flg)
            );
            return;
         }
      }

      private void debugMatchesIasZoneStatusChangeNotification(ReflexDriverContext ctx) {
         Integer rdelay = ctx.getVariable(iasZoneStatusDelay);
         if (maxDelay > 0 && rdelay != null) {
            if (maxDelay < rdelay) {
               log.info(
                  "[reflex debugging] zigbee ias zone status change notification zone status did not match:\n" +
                  "    maximum delay:  {}\n" +
                  "    reported delay: {}",
                  rdelay,
                  maxDelay
               );
               return;
            }
         }

         Integer rstatus = ctx.getVariable(iasZoneStatus);
         if (rstatus == null) {
            log.info("[reflex debugging] zigbee ias zone status change notification did not report zone status");
            return;
         }

         int sta = rstatus;
         if (!(((sta & setMask) == setMask) && ((~sta & clrMask) == clrMask))) {
            log.info(
               "[reflex debugging] zigbee ias zone status change notification zone status did not match:\n" +
               "    zone status:     {}\n" +
               "    set flags check: {} & {} == {}\n" +
               "    clr flags check: {} & {} == {}" +
               ProtocUtil.toHexString(sta),
               ProtocUtil.toHexString(sta), ProtocUtil.toHexString(setMask), ProtocUtil.toHexString(setMask),
               ProtocUtil.toHexString(~sta), ProtocUtil.toHexString(clrMask), ProtocUtil.toHexString(clrMask)
            );
            return;
         }
      }
   }
}


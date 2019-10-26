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
package com.iris.agent.zwave.spy;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

import com.iris.agent.spy.SpyService;
import com.iris.agent.util.ByteUtils;
import com.iris.agent.util.ConcurrentCappedLinkedQueue;
import com.iris.agent.zwave.engine.ZWaveEngineMsg;
import com.iris.agent.zwave.spy.actions.SendLearnModeClassic;
import com.iris.agent.zwave.spy.actions.SendLearnModeDisable;
import com.iris.agent.zwave.spy.actions.SendLearnModeNwi;
import com.iris.agent.zwave.spy.actions.SendNifToolPlugin;

public class ZWSpy {
   private final static DateFormat df = new SimpleDateFormat("HH:mm:ss.SSS");
   private final static int MESSAGE_CAP = 50;
   
   public enum ZWMsgType {
      INCOMING_MSG   ("recv"), 
      OUTGOING_MSG   ("send"), 
      UNSOLICITED_MSG("uslt");
      
      private final String tag;
      
      ZWMsgType(String tag) {
         this.tag = tag;
      }
      
      String tag() {
         return tag;
      }
   }
   
   public final static ZWSpy INSTANCE = new ZWSpy();
   
   private ZWMsgStore zwMsgStore;
   private final ToolsStore toolsStore = new ToolsStore();
   private ZWSpy() {}
   
   public void initialize() {
      if (SpyService.INSTANCE.isActive()) {
         SpyService.INSTANCE.registerPlugin(new SummarySpyPlugIn());
         SpyService.INSTANCE.registerPlugin(new LoadNodesSpyPlugin());
         SpyService.INSTANCE.registerPlugin(new DatabaseSpyPlugin());
         SpyService.INSTANCE.registerPlugin(new DbApiSpyPlug());
         SpyService.INSTANCE.registerPlugin(new MsgSpyPlugin());
         SpyService.INSTANCE.registerPlugin(new MsgApiSpyPlugin());
         SpyService.INSTANCE.registerPlugin(new ZWSerialDumpPlugin());
         SpyService.INSTANCE.registerPlugin(new ToolsSpyPlugin());
         SpyService.INSTANCE.registerPlugin(new ToolsApiPlugin());
         SpyService.INSTANCE.registerPlugin(new SendNifToolPlugin());
         SpyService.INSTANCE.registerPlugin(new SendLearnModeDisable());
         SpyService.INSTANCE.registerPlugin(new SendLearnModeClassic());
         SpyService.INSTANCE.registerPlugin(new SendLearnModeNwi());
         zwMsgStore = new ActiveMsgStore();
      }
      else {
         zwMsgStore = new InactiveMsgStore();
      }
   }
   
   public void toolUsed(String toolUsage) {
      toolsStore.toolUse(toolUsage);
   }
   
   public void unsolicited(ZWaveEngineMsg msg) {
      zwMsgStore.msg(ZWMsgType.UNSOLICITED_MSG, msg.getNodeId(), msg.getPayload());
   }
   
   public void received(int nodeId, byte[] bytes) {
      zwMsgStore.msg(ZWMsgType.INCOMING_MSG, nodeId, bytes);
   }
   
   public void sent(int nodeId, byte[] bytes) {
      zwMsgStore.msg(ZWMsgType.OUTGOING_MSG, nodeId, bytes);
   }
   
   public Stream<String> getToolUsage() {
      List<String> msgs = toolsStore.getToolsMessages();
      Collections.reverse(msgs);
      return msgs.stream();
   }
   
   public Stream<String> getZipMessages() {
      List<String> msgs = zwMsgStore.getZipMessages();
      Collections.reverse(msgs);
      return msgs.stream();
   }
   
   public Stream<String> getZipMessagesAscending() {
      return zwMsgStore.getZipMessages().stream();
   }
   
   interface ZWMsgStore {
      void msg(ZWMsgType type, int nodeId, byte[] msg);
      
      List<String> getZipMessages(); 
   }
   
   private static class ToolsStore {
      
      private final Queue<String> toolMsgs = new ConcurrentLinkedQueue<>();
      
      void toolUse(String toolMsg) {
         StringBuffer sb = new StringBuffer();
         sb.append(df.format(new Date()));
         sb.append(' ').append(toolMsg);
         toolMsgs.add(sb.toString());
      }
      
      List<String> getToolsMessages() {
         return new ArrayList<>(toolMsgs);
      }
   }
   
   
   private static class InactiveMsgStore implements ZWMsgStore {

      @Override
      public void msg(ZWMsgType type, int nodeId, byte[] msg) {
         // No-op
      }

      @Override
      public List<String> getZipMessages() {
         // Should never get called.
         return null;
      }
   }
   
   private static class ActiveMsgStore implements ZWMsgStore {
      
      private final Queue<String> msgs = new ConcurrentCappedLinkedQueue<>(MESSAGE_CAP);

      @Override
      public void msg(ZWMsgType type, int nodeId, byte[] msg) {
         StringBuffer sb = new StringBuffer();
         sb.append(df.format(new Date()));
         sb.append(' ').append(type.tag());
         sb.append(String.format("[%03d]:", nodeId));
         int indentLength = sb.length() + 1;
         sb.append(ByteUtils.byteArray2StringBlock(msg, 8, indentLength, 1));
         msgs.add(sb.toString());
      }

      @Override
      public List<String> getZipMessages() {
         return new ArrayList<>(msgs);
      }
   }
   
}


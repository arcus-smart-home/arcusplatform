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
package com.iris.agent.zwave.code.cmdclass;

import java.util.HashMap;
import java.util.Map;

import com.iris.agent.zwave.code.CommandClass;

public class CmdClasses {
   private final static Map<Integer, CommandClass> cmdClasses = new HashMap<>();
   public final static NetInclusionCmdClass NETWORK_INCLUSION = new NetInclusionCmdClass();
   public final static NetMgmtProxyCmdClass NETWORK_MGMT_PROXY = new NetMgmtProxyCmdClass();
   public final static NetMgmtBasicNodeCmdClass NETWORK_MGMT_BASIC_NODE = new NetMgmtBasicNodeCmdClass();
   public final static ManufacturerSpecificCmdClass MANUFACTURER_SPECIFIC = new ManufacturerSpecificCmdClass();
   public final static AssociationCmdClass ASSOCIATION = new AssociationCmdClass();
   public final static VersionCmdClass VERSION = new VersionCmdClass();
   public final static BasicCmdClass BASIC = new BasicCmdClass();
   
   public final static int WAKEUP_COMMAND_CLASS_ID = 0x84;
      
   public static CommandClass getCmdClass(byte b) {
      return getCmdClass(0x00FF & b);
   }
      
   public static CommandClass getCmdClass(int i) {
      return cmdClasses.get(i);
   }
   
   static void register(CommandClass zcc) {
      cmdClasses.put(zcc.intId(), zcc);
   }
   
   // Utility Functions
   public static boolean isVersion(int cmdClass) {
      return cmdClass == VERSION.intId();
   }
   
   public static boolean isNetworkMgmtProxy(int cmdClass) {
      return cmdClass == NETWORK_MGMT_PROXY.intId();
   }
   
   public static boolean isNetworkInclusion(int cmdClass) {
      return cmdClass == NETWORK_INCLUSION.intId();
   }
   
   public static boolean isManufacturerSpecific(int cmdClass) {
      return cmdClass == MANUFACTURER_SPECIFIC.intId();
   }
   
   public static boolean isAssociation(int cmdClass) {
      return cmdClass == ASSOCIATION.intId();
   }
}



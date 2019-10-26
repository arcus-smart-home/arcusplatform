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
package com.iris.agent.zwave.code.entity;

import com.iris.agent.util.ByteUtils;
import com.iris.agent.zwave.code.cmdclass.AssociationCmdClass;
import com.iris.agent.zwave.code.cmdclass.CmdClasses;

/**
 * Association Get Cmd
 * 
 * 3 Bytes
 * 
 * 0     : CmdClass (0x85)
 * 1     : Cmd (0x02)
 * 2     : Grouping Identifier
 * 
 * @author Erik Larson
 *
 */
public class CmdAssocGet extends AbstractZCmd {
   private final static int BYTE_LENGTH = 3;   
   private final int groupingIdentifier;
   
   public CmdAssocGet(int groupingIdentifier) {
      super(CmdClasses.ASSOCIATION.intId(), AssociationCmdClass.CMD_ASSOCIATION_GET, BYTE_LENGTH);
      this.groupingIdentifier = groupingIdentifier;
   }
   
   public int getGroupingIdentifier() {
      return groupingIdentifier;
   }

   @Override
   public byte[] bytes() {
      return ByteUtils.ints2Bytes(cmdClass, cmd, groupingIdentifier);
   }
}


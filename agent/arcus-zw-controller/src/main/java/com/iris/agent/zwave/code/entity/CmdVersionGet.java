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
import com.iris.agent.zwave.code.cmdclass.CmdClasses;
import com.iris.agent.zwave.code.cmdclass.VersionCmdClass;

/**
 * Version Get Cmd
 * 
 * 2 Bytes
 * 
 * 0   : CmdClass (0x86)
 * 1   : Cmd (0x11)
 * 
 * @author Erik Larson
 */
public class CmdVersionGet extends AbstractZCmd {
   public final static CmdVersionGet COMMAND_VERSION_GET = new CmdVersionGet();
   private final static int BYTE_LENGTH = 2;
   
   public CmdVersionGet() {
      super(CmdClasses.VERSION.intId(), VersionCmdClass.CMD_VERSION_GET, BYTE_LENGTH);
   }

   @Override
   public byte[] bytes() {
      return ByteUtils.ints2Bytes(cmdClass, cmd);
   }
}


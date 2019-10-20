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
package com.iris.agent.zwave.code;

import com.iris.agent.zwave.code.cmdclass.CmdClasses;

public class ZWDecoder {
   
   public static Decoded decode(byte[] bytes, int offset) {
      int cmdClass = 0x00FF & bytes[offset];
      int cmd = 0x00FF & bytes[offset + 1];
      
      CommandClass zcc = CmdClasses.getCmdClass(cmdClass);
      if (zcc != null) {
         Command zc = zcc.command(cmd);
         if (zc != null) {
            Decoder decoder = zc.getDecoder();
            return decoder.decode(bytes, offset);
         }
      }
      
      // If no decoder is found, use the RawDecoder
      return RawDecoder.INSTANCE.decode(bytes, offset);
   }
}


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

import com.iris.agent.zwave.code.anno.Id;
import com.iris.agent.zwave.code.anno.Name;
import com.iris.agent.zwave.code.cmds.ManSpecificGetCmd;
import com.iris.agent.zwave.code.cmds.ManSpecificReportCmd;

@Id(0x72)
@Name("Manufacturer Specific")
public class ManufacturerSpecificCmdClass extends AbstractCmdClass {
   public static final int CMD_MANUFACTURER_SPECIFIC_GET = 0x04;
   public static final int CMD_MANUFACTURER_SPECIFIC_REPORT = 0x05;
   
   ManufacturerSpecificCmdClass() {
      super (
            new ManSpecificGetCmd(),
            new ManSpecificReportCmd()
            );
      
   }
}


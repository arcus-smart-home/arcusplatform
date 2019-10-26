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
import com.iris.agent.zwave.code.cmds.AssocGetCmd;
import com.iris.agent.zwave.code.cmds.AssocGroupingsGet;
import com.iris.agent.zwave.code.cmds.AssocGroupingsReport;
import com.iris.agent.zwave.code.cmds.AssocRemoveCmd;
import com.iris.agent.zwave.code.cmds.AssocReportCmd;
import com.iris.agent.zwave.code.cmds.AssocSetCmd;

@Id(0x85)
@Name("Association")
public class AssociationCmdClass extends AbstractCmdClass {
   public static final int CMD_ASSOCIATION_SET = 0x01;
   public static final int CMD_ASSOCIATION_GET = 0x02;
   public static final int CMD_ASSOCIATION_REPORT = 0x03;
   public static final int CMD_ASSOCIATION_REMOVE = 0x04;
   public static final int CMD_ASSOCIATION_GROUPINGS_GET = 0x05;
   public static final int CMD_ASSOCIATION_GROUPINGS_REPORT = 0x06;
   
   AssociationCmdClass() {
      super(
            new AssocSetCmd(),
            new AssocGetCmd(),
            new AssocReportCmd(),
            new AssocRemoveCmd(),
            new AssocGroupingsGet(),
            new AssocGroupingsReport()
            );      
   }
}


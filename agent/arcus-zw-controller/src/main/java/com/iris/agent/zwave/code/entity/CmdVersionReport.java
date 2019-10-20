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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.iris.agent.util.ByteUtils;
import com.iris.agent.zwave.code.Decoded;
import com.iris.agent.zwave.code.Decoder;
import com.iris.agent.zwave.code.cmdclass.CmdClasses;
import com.iris.agent.zwave.code.cmdclass.VersionCmdClass;

/**
 * Version Report Command
 * 
 * Variable Bytes
 * 
 * 0     : CmdClass (0x86)
 * 1     : Cmd (0x12)
 * 2     : Z-Wave Protocol Library Type
 *         0x00 N/A
 *         0x01 Static Controller
 *         0x02 Controller
 *         0x03 Enhanced Slave
 *         0x04 Slave
 *         0x05 Installer
 *         0x06 Routing Slave
 *         0x07 Bridge Controller
 *         0x08 Device Under Test
 *         0x09 N/A
 *         0x0A AV Remote
 *         0x0B AV Device
 * 3     : Z-Wave Protocol Version
 * 4     : Z-Wave Protocol Sub-Version
 * 5     : Firmware 0 Version
 * 6     : Firmware 0 Sub Version
 * 7     : Hardware Version
 * 8     : Number of Firmware Targets
 * 9     : Firmware 1 Version
 * 10    : Firmware 1 Subversion
 * ...
 * n     : Firmware N Version
 * n + 1 : Firmware N Subversion
 * 
 * @author Erik Larson
 */
public class CmdVersionReport extends AbstractZCmd {
   private final static CmdVersionReportDecoder DECODER = new CmdVersionReportDecoder();
   private final static int BASE_LENGTH = 9;
   
   private final int protocolLibraryType;
   private final int protocolVersion;
   private final int protocolSubversion;
   private final int hardwareVersion;
   
   private final List<ZWaveVersion> firmwareVersions;
   
   public CmdVersionReport(
         int protocolLibraryType, 
         int protocolVersion, 
         int protocolSubversion, 
         int hardwareVersion,
         List<ZWaveVersion> firmwareVersions) {
      super(CmdClasses.VERSION.intId(), VersionCmdClass.CMD_VERSION_REPORT, BASE_LENGTH);
      this.protocolLibraryType = protocolLibraryType;
      this.protocolVersion = protocolVersion;
      this.protocolSubversion = protocolSubversion;
      this.hardwareVersion = hardwareVersion;
      this.firmwareVersions = Collections.unmodifiableList(firmwareVersions);
   }
   
   public int getProtocolLibraryType() {
      return protocolLibraryType;
   }

   public int getProtocolVersion() {
      return protocolVersion;
   }

   public int getProtocolSubversion() {
      return protocolSubversion;
   }
   
   public int getHardwareVersion() {
      return hardwareVersion;
   }

   public List<ZWaveVersion> getFirmwareVersions() {
      return firmwareVersions;
   }
   
   @Override
   public int byteLength() {
      int firmwareVersionsCount = firmwareVersions != null ? firmwareVersions.size() : 0;
      return BASE_LENGTH + (Math.max(firmwareVersionsCount - 1, 0) * 2);
   }

   @Override
   public byte[] bytes() {
      
      int firmwareVersionsCount = firmwareVersions != null ? firmwareVersions.size() : 0;
      
      ZWaveVersion ver0 = firmwareVersionsCount > 0 ? firmwareVersions.get(0) : null;
      
      byte[] baseBytes = new byte[baseLength];
      baseBytes[0] = (byte)cmdClass;
      baseBytes[1] = (byte)cmd;
      baseBytes[2] = (byte)protocolLibraryType;
      baseBytes[3] = (byte)protocolVersion;
      baseBytes[4] = (byte)protocolSubversion;
      baseBytes[5] = (byte)(ver0 != null ? ver0.getVersion() : 0);
      baseBytes[6] = (byte)(ver0 != null ? ver0.getSubversion() : 0);
      baseBytes[7] = (byte)hardwareVersion;
      baseBytes[8] = (byte)(firmwareVersionsCount > 1 ? firmwareVersionsCount - 1 : 0);
      
      if (firmwareVersionsCount > 1) {
         byte[] firmwares = new byte[(firmwareVersionsCount - 1) * 2];
         for (int i = 1; i < firmwareVersionsCount; i++) {
            ZWaveVersion ver = firmwareVersions.get(i);
            firmwares[(i - 1) * 2] = (byte)ver.getVersion();
            firmwares[((i - 1) * 2) + 1] = (byte)ver.getSubversion();
         }
         return ByteUtils.concat(baseBytes, firmwares);
      }
      else {
         return baseBytes;
      }
   }
   
   public static CmdVersionReportDecoder decoder() {
      return DECODER;
   }
   
   public static class CmdVersionReportDecoder implements Decoder {
      
      private CmdVersionReportDecoder() {}

      @Override
      public Decoded decode(byte[] bytes, int initialOffset) {
         int offset = initialOffset + 2;
         int protocolLibraryType = 0x00FF & bytes[offset];
         offset++;
         int protocolVersion = 0x00FF & bytes[offset];
         offset++;
         int protocolSubversion = 0x00FF & bytes[offset];
         offset++;
         int firmware0Version = 0x00FF & bytes[offset];
         offset++;
         int firmware0Subversion = 0x00FF & bytes[offset];
         offset++;
         int hardwareVersion = 0x00FF & bytes[offset];
         offset++;
         int firmwareCount = Math.max(0x00FF & bytes[offset], 0);
         offset++;
                  
         List<ZWaveVersion> firmwareVersions = new ArrayList<>(firmwareCount + 1);
         firmwareVersions.add(new ZWaveVersion(firmware0Version, firmware0Subversion));
         
         if (firmwareCount > 0) {
            for (int i = 0; i < firmwareCount; i++) {
               int firmwareNVersion = 0x00FF & bytes[offset];
               offset++;
               int firmwareNSubversion = 0x00FF & bytes[offset];
               offset++;
               firmwareVersions.add(new ZWaveVersion(firmwareNVersion, firmwareNSubversion));
            }
         }
         
         CmdVersionReport cmdVersionReport = new CmdVersionReport(protocolLibraryType,
               protocolVersion,
               protocolSubversion,
               hardwareVersion,
               firmwareVersions);
         
         return new Decoded(CmdClasses.VERSION.intId(), VersionCmdClass.CMD_VERSION_REPORT, cmdVersionReport.byteLength(), cmdVersionReport);
      }
      
   }

   public static class ZWaveVersion {
      private final int version;
      private final int subversion;
      
      ZWaveVersion(int version, int subversion) {
         this.version = version;
         this.subversion = subversion;
      }
      
      public int getVersion() {
         return version;
      }
      
      public int getSubversion() {
         return subversion;
      }
      
      public String asString() {
         return String.format("%d.%d", version, subversion);
      }
   }
   
}



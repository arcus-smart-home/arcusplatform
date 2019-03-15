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
package com.iris.agent.os.serial;

import io.netty.channel.ChannelConfig;

public interface UartChannelConfig extends ChannelConfig {
   public enum StopBits {
      STOPBITS_1,
      STOPBITS_2,
   };

   public enum ParityBit {
      PARITY_NONE,
      PARITY_ODD,
      PARITY_EVEN,
   };

   public enum DataBits {
      DATABITS_5,
      DATABITS_6,
      DATABITS_7,
      DATABITS_8,
   }

   public enum FlowControl {
      FLOW_NONE,
      FLOW_XONXOFF,
      FLOW_RTSCTS,
   }

   public UartChannelConfig setBaudRate(int baudRate);
   public UartChannelConfig setDataBits(DataBits bits);
   public UartChannelConfig setStopBits(StopBits bits);
   public UartChannelConfig setParityBit(ParityBit bits);
   public UartChannelConfig setFlowControl(FlowControl flow);
   public UartChannelConfig setVTime(int vtime);
   public UartChannelConfig setVMin(int vmin);

   public int getBaudRate();
   public DataBits getDataBits();
   public StopBits getStopBits();
   public ParityBit getParityBit();
   public FlowControl getFlowControl();
   public int getVTime();
   public int getVMin();
}


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

import static com.iris.agent.os.serial.UartChannelOption.BAUDRATE;
import static com.iris.agent.os.serial.UartChannelOption.DATABITS;
import static com.iris.agent.os.serial.UartChannelOption.FLOWCONTROL;
import static com.iris.agent.os.serial.UartChannelOption.PARITYBIT;
import static com.iris.agent.os.serial.UartChannelOption.STOPBITS;
import static com.iris.agent.os.serial.UartChannelOption.VMIN;
import static com.iris.agent.os.serial.UartChannelOption.VTIME;

import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;

import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultChannelConfig;

public class DefaultUartChannelConfig extends DefaultChannelConfig implements UartChannelConfig {
   private int baudRate = 115200;
   private FlowControl flowControl = FlowControl.FLOW_NONE;
   private DataBits dataBits = DataBits.DATABITS_8;
   private StopBits stopBits = StopBits.STOPBITS_1;
   private ParityBit parityBit = ParityBit.PARITY_NONE;
   private int vtime = 1;
   private int vmin = 0;

   public DefaultUartChannelConfig(Channel channel) {
      super(channel);
   }

    @Override
    public Map<ChannelOption<?>, Object> getOptions() {
        return getOptions(super.getOptions(), BAUDRATE, FLOWCONTROL, STOPBITS, DATABITS, PARITYBIT);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getOption(@Nullable ChannelOption<T> option) {
        if (option == BAUDRATE) {
            return (T) Integer.valueOf(getBaudRate());
        }

        if (option == FLOWCONTROL) {
            return (T) getFlowControl();
        }

        if (option == DATABITS) {
            return (T) getDataBits();
        }

        if (option == STOPBITS) {
            return (T) getStopBits();
        }

        if (option == PARITYBIT) {
            return (T) getParityBit();
        }

        return super.getOption(option);
    }

    @Override
    public <T> boolean setOption(@Nullable ChannelOption<T> option, @Nullable T value) {
      try {
         validate(option, value);
         if (value == null) {
            throw new NullPointerException();
         }

         if (option == BAUDRATE) {
            setBaudRate((Integer) value);
         } else if (option == FLOWCONTROL) {
            setFlowControl((FlowControl) value);
         } else if (option == DATABITS) {
            setDataBits((DataBits) value);
         } else if (option == STOPBITS) {
            setStopBits((StopBits) value);
         } else if (option == PARITYBIT) {
            setParityBit((ParityBit) value);
         } else if (option == VTIME) {
            setVTime((Integer) value);
         } else if (option == VMIN) {
            setVMin((Integer) value);
         } else {
            return super.setOption(option, value);
         }
       } catch (Exception ex) {
          throw new RuntimeException(ex);
       }

        return true;
    }

   @Override
   public UartChannelConfig setBaudRate(int baudRate) {
      this.baudRate = baudRate;
      return this;
   }

   @Override
   public UartChannelConfig setDataBits(DataBits bits) {
      this.dataBits = bits;
      return this;
   }

   @Override
   public UartChannelConfig setStopBits(StopBits bits) {
      this.stopBits = bits;
      return this;
   }

   @Override
   public UartChannelConfig setParityBit(ParityBit bits) {
      this.parityBit = bits;
      return this;
   }

   @Override
   public UartChannelConfig setFlowControl(FlowControl flow) {
      this.flowControl = flow;
      return this;
   }

   @Override
   public UartChannelConfig setVTime(int vtime) {
      this.vtime = vtime;
      return this;
   }

   @Override
   public UartChannelConfig setVMin(int vmin) {
      this.vmin = vmin;
      return this;
   }

   @Override
   public int getBaudRate() {
      return baudRate;
   }

   @Override
   public DataBits getDataBits() {
      return dataBits;
   }

   @Override
   public StopBits getStopBits() {
      return stopBits;
   }

   @Override
   public ParityBit getParityBit() {
      return parityBit;
   }

   @Override
   public FlowControl getFlowControl() {
      return flowControl;
   }

   @Override
   public int getVTime() {
      return vtime;
   }

   @Override
   public int getVMin() {
      return vmin;
   }

   @Override
   public String toString() {
      StringBuilder bld = new StringBuilder();
      bld.append(getClass().getSimpleName()).append(" [");
      bld.append("baudRate=").append(baudRate);
      bld.append(",flowControl=").append(flowControl);
      bld.append(",dataBits=").append(dataBits);
      bld.append(",stopBits=").append(stopBits);
      bld.append(",parityBit=").append(parityBit);

      bld.append("]");
      return bld.toString();
   }
}


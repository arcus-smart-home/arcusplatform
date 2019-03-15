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

import java.net.SocketAddress;

import org.eclipse.jdt.annotation.Nullable;

import com.google.common.base.Preconditions;

import io.netty.channel.Channel;
import io.netty.channel.oio.OioByteStreamChannel;

public class UartOioChannel extends OioByteStreamChannel {
   private static final UartAddress LOCALADDR = new UartAddress("localhost");
   private final UartChannelConfig config;
   private boolean open;

   @Nullable
   private UartAddress address;

   @Nullable
   private UartNative.SerialPort port;

   public UartOioChannel() {
      super(null);
      this.config = new DefaultUartChannelConfig(this);
      this.open = true;
   }

   public UartOioChannel(Channel parent) {
      super(parent);
      this.config = new DefaultUartChannelConfig(this);
      this.open = true;
   }

   @Override
   public UartChannelConfig config() {
      return config;
   }

   @Override
   public boolean isOpen() {
      return open;
   }

   @Override
   protected void doConnect(@Nullable SocketAddress remoteAddress, @Nullable SocketAddress localAddress) throws Exception {
      UartAddress uartAddress = (UartAddress)remoteAddress;
      if (uartAddress == null) {
         throw new NullPointerException("remote address cannot be null");
      }

      this.address = uartAddress;
      String port = uartAddress.getPort();

      UartNative.SerialPort serialPort = UartNative.create(port);
      UartChannelConfig config = config();
      switch (config.getStopBits()) {
      case STOPBITS_1:
         serialPort.setStopBits(UartNative.StopBits.STOP1);
         break;

      case STOPBITS_2:
         serialPort.setStopBits(UartNative.StopBits.STOP2);
         break;

      default:
         throw new RuntimeException("unknown number of stop bits");
      }

      switch (config.getParityBit()) {
      case PARITY_NONE:
         serialPort.setParityBit(UartNative.ParityBit.NONE);
         break;

      case PARITY_ODD:
         serialPort.setParityBit(UartNative.ParityBit.ODD);
         break;

      case PARITY_EVEN:
         serialPort.setParityBit(UartNative.ParityBit.EVEN);
         break;

      default:
         throw new RuntimeException("unknown parity");
      }

      switch (config.getDataBits()) {
      case DATABITS_5:
         serialPort.setDataBits(UartNative.DataBits.DATA5);
         break;

      case DATABITS_6:
         serialPort.setDataBits(UartNative.DataBits.DATA6);
         break;

      case DATABITS_7:
         serialPort.setDataBits(UartNative.DataBits.DATA7);
         break;

      case DATABITS_8:
         serialPort.setDataBits(UartNative.DataBits.DATA8);
         break;

      default:
         throw new RuntimeException("unknown number of data bits");
      }

      switch (config.getFlowControl()) {
      case FLOW_NONE:
         serialPort.setFlowControl(UartNative.FlowControl.NONE);
         break;

      case FLOW_XONXOFF:
         serialPort.setFlowControl(UartNative.FlowControl.XONXOFF);
         break;

      case FLOW_RTSCTS:
         serialPort.setFlowControl(UartNative.FlowControl.RTSCTS);
         break;

      default:
         throw new RuntimeException("unknown flow control");
      }

      serialPort.setVTime(config.getVTime());
      serialPort.setVMin(config.getVMin());
      serialPort.setBaudRate(config.getBaudRate());
      serialPort.open();
      this.port = serialPort;

      activate(serialPort.getInputStream(), serialPort.getOutputStream());
   }

   protected void initFromUnsafe() throws Exception {
      UartNative.SerialPort pt = port;

      Preconditions.checkNotNull(pt);
      activate(pt.getInputStream(), pt.getOutputStream());
   }

   @Override
   protected SocketAddress localAddress0() {
      return LOCALADDR;
   }

   @Nullable
   @Override
   protected SocketAddress remoteAddress0() {
      return address;
   }

   @Override
   protected void doBind(@Nullable SocketAddress localAddress) throws Exception {
      throw new UnsupportedOperationException();
   }

   @Override
   protected void doDisconnect() throws Exception {
      doClose();
   }

   @Override
   protected void doClose() throws Exception {
      try {
         open = false;
         super.doClose();
      } finally {
         if (port != null) {
            port.close();
            port = null;
         }
      }
   }
}


/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2019 Arcus Project
 *
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
package com.iris.agent.zigbee;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.agent.os.serial.UartNative;
import com.zsmartsystems.zigbee.transport.ZigBeePort;

/**
 * ZigBeePort implementation backed by the Arcus JNA-based UartNative serial port.
 * This avoids the jSSC dependency used by zsmartsystems' ZigBeeSerialPort, which
 * requires libstdc++ on the target system.
 *
 * Thread safety: the ASH frame handler reads and writes from multiple threads.
 * The write methods are synchronized since the underlying OutputStream (from
 * Channels.newOutputStream) is not thread-safe.
 */
public class NativeZigBeePort implements ZigBeePort {
   private static final Logger logger = LoggerFactory.getLogger(NativeZigBeePort.class);

   private final String portName;
   private final int baudRate;
   private final FlowControl flowControl;

   private volatile UartNative.SerialPort serialPort;
   private volatile InputStream inputStream;
   private volatile OutputStream outputStream;
   private volatile boolean isOpen = false;

   public NativeZigBeePort(String portName, int baudRate, FlowControl flowControl) {
      this.portName = portName;
      this.baudRate = baudRate;
      this.flowControl = flowControl;
   }

   @Override
   public boolean open() {
      return open(baudRate, flowControl);
   }

   @Override
   public boolean open(int baudRate) {
      return open(baudRate, flowControl);
   }

   @Override
   public boolean open(int baudRate, FlowControl flowControl) {
      try {
         logger.info("Opening native serial port {} at {} baud, flow control {}",
               portName, baudRate, flowControl);

         UartNative.SerialPort sp = UartNative.create(portName);
         sp.setBaudRate(baudRate);
         sp.setDataBits(UartNative.DataBits.DATA8);
         sp.setStopBits(UartNative.StopBits.STOP1);
         sp.setParityBit(UartNative.ParityBit.NONE);

         switch (flowControl) {
            case FLOWCONTROL_OUT_XONOFF:
               sp.setFlowControl(UartNative.FlowControl.XONXOFF);
               break;
            case FLOWCONTROL_OUT_RTSCTS:
               sp.setFlowControl(UartNative.FlowControl.RTSCTS);
               break;
            case FLOWCONTROL_OUT_NONE:
            default:
               sp.setFlowControl(UartNative.FlowControl.NONE);
               break;
         }

         // VMIN=1, VTIME=0: read() blocks until at least 1 byte arrives.
         // VMIN=0 causes the kernel to return 0 bytes on timeout, which
         // UartInputStream converts to 0x00 data bytes, corrupting ASH framing.
         sp.setVTime(0);
         sp.setVMin(1);

         sp.open();

         serialPort = sp;
         inputStream = sp.getInputStream();
         outputStream = sp.getOutputStream();
         isOpen = true;

         logger.info("Native serial port {} opened successfully (fd={})", portName, sp.getFd());
         return true;
      } catch (Exception e) {
         logger.error("Failed to open native serial port {}", portName, e);
         isOpen = false;
         return false;
      }
   }

   @Override
   public void close() {
      if (!isOpen) {
         return;
      }
      isOpen = false;

      // Close only the underlying serial port — it owns the FD.
      // Don't close the streams separately since they share the same FD
      // via FileChannel; closing one would invalidate the other.
      UartNative.SerialPort sp = serialPort;
      if (sp != null) {
         try {
            sp.close();
         } catch (IOException e) {
            logger.warn("Error closing native serial port {}: {}", portName, e.getMessage());
         }
      }

      serialPort = null;
      inputStream = null;
      outputStream = null;
      logger.info("Native serial port {} closed", portName);
   }

   @Override
   public synchronized void write(int value) {
      OutputStream os = outputStream;
      if (!isOpen || os == null) {
         return;
      }
      try {
         os.write(value);
      } catch (IOException e) {
         logger.error("Error writing to serial port ({}: {})", e.getClass().getSimpleName(), e.getMessage(), e);
      }
   }

   @Override
   public synchronized void write(int[] values) {
      OutputStream os = outputStream;
      if (!isOpen || os == null) {
         return;
      }
      try {
         byte[] data = new byte[values.length];
         for (int i = 0; i < values.length; i++) {
            data[i] = (byte) values[i];
         }
         os.write(data);
      } catch (IOException e) {
         logger.error("Error writing to serial port ({}: {})", e.getClass().getSimpleName(), e.getMessage(), e);
      }
   }

   @Override
   public int read() {
      InputStream is = inputStream;
      if (!isOpen || is == null) {
         return -1;
      }
      try {
         return is.read();
      } catch (IOException e) {
         if (isOpen) {
            logger.error("Error reading from serial port ({}: {})", e.getClass().getSimpleName(), e.getMessage(), e);
         }
         return -1;
      }
   }

   @Override
   public int read(int timeout) {
      if (!isOpen) {
         return -1;
      }
      if (timeout <= 0) {
         return read();
      }
      InputStream is = inputStream;
      if (is == null) {
         return -1;
      }
      try {
         long endTime = System.currentTimeMillis() + timeout;
         while (isOpen && System.currentTimeMillis() < endTime) {
            if (is.available() > 0) {
               return is.read();
            }
            Thread.sleep(1);
         }
         return -1;
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
         return -1;
      } catch (IOException e) {
         if (isOpen) {
            logger.error("Error reading from serial port ({}: {})", e.getClass().getSimpleName(), e.getMessage(), e);
         }
         return -1;
      }
   }

   @Override
   public void purgeRxBuffer() {
      InputStream is = inputStream;
      if (!isOpen || is == null) {
         return;
      }
      try {
         while (is.available() > 0) {
            is.read();
         }
      } catch (IOException e) {
         logger.warn("Error purging RX buffer: {}", e.getMessage());
      }
   }
}

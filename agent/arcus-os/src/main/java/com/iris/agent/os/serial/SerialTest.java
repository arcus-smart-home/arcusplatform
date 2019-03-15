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

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SerialTest {
   private static final Logger log = LoggerFactory.getLogger("serial");

   private SerialTest() {
   }

   public static void main(String[] args) throws Exception {
      if (args.length < 3) {
         log.info("Usage: SerialText <port> <baud> <flow>");
         log.info("where:");
         log.info("    baud = the baud rate of the serial device");
         log.info("    flow = flow control [none,xonxoff,rtscts]");
      }

      int baud = Integer.parseInt(args[1]);

      UartNative.FlowControl fc;
      switch (args[2]) {
      case "none": fc = UartNative.FlowControl.NONE; break;
      case "xonxoff": fc = UartNative.FlowControl.XONXOFF; break;
      case "rtscts": fc = UartNative.FlowControl.RTSCTS; break;
      default: throw new Exception("unknown flow control: " + args[2]);
      }

      try (UartNative.SerialPort port = UartNative.create(args[0])) {
         port.setBaudRate(baud);
         port.setDataBits(UartNative.DataBits.DATA8);
         port.setParityBit(UartNative.ParityBit.NONE);
         port.setStopBits(UartNative.StopBits.STOP1);
         port.setFlowControl(fc);

         port.open();
         final InputStream is = port.getInputStream();
         final OutputStream os = port.getOutputStream();
         final FileChannel ch = port.getChannel();

         log.info("INPUT STREAM:  {}", is.getClass());
         log.info("OUTPUT STREAM: {}", os.getClass());
         log.info("FILE CHANNEL:  {}", ch);

         Thread readThread = new Thread(new Runnable() {
            @Override
            public void run() {
               try {
                  byte[] buffer = new byte[8192];
                  while (true) {
                     int available = is.available();
                     if (available <= 0) {
                        Thread.sleep(100);
                        continue;
                     }

                     log.info("{} bytes available to read", available);
                     int num = is.read(buffer);
                     if (num < 0) break;
                     if (num == 0) {
                        log.warn("SERIAL PORT RETURNED EMPTY DATA");
                        continue;
                     }

                     log.info("read {} bytes: {}", num, toHex(buffer, num));
                  }
               } catch (Exception ex) {
                  log.error("Error reading serial data", ex);
               }
            }
         });

         Thread writeThread = new Thread(new Runnable() {
            @Override
            public void run() {
               try {
                  byte[] buffer = new byte[5];
                  while (true) {
                     Thread.sleep(5000);
                     buffer[0] = ((byte)0x1A);
                     buffer[1] = ((byte)0xC0);
                     buffer[2] = ((byte)0x38);
                     buffer[3] = ((byte)0xBC);
                     buffer[4] = ((byte)0x7E);

                     log.info("writing bytes: {}", toHex(buffer, buffer.length));
                     os.write(buffer);
                     os.flush();
                     log.info("wrote {} bytes", buffer.length);
                  }
               } catch (Exception ex) {
                  log.error("Error writing serial data", ex);
               }
            }
         });

         readThread.setDaemon(true);
         writeThread.setDaemon(true);
         readThread.start();
         writeThread.start();

         Thread.sleep(60000);

         log.info("Closing serial port...");
         port.close();
      }
   }

   private static String toHex(@Nullable byte[] data, int num) {
      if (data == null || num == 0) {
         return "";
      }

      StringBuilder bld = new StringBuilder();
      for (int i = 0; i < num; ++i) {
         if (bld.length() != 0) bld.append(' ');

         byte value = data[i];
         if ((value & 0xFF) < 0x10) bld.append('0');
         bld.append(Integer.toHexString(value & 0xFF));
      }

      return bld.toString();
   }
}




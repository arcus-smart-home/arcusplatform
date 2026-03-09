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
package com.iris.agent.zwave.engine;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.agent.os.serial.UartNative;

/**
 * Manages the serial port connection to the Z-Wave controller chip.
 *
 * Uses direct blocking I/O via UartNative, following the same pattern as
 * NativeZigBeePort in the ZigBee controller.  Streams are obtained via
 * FileInputStream/FileOutputStream on the raw FileDescriptor to avoid
 * InterruptibleChannel issues (FileChannel's ClosedByInterruptException
 * permanently kills the channel if any thread is interrupted).
 *
 * A dedicated reader thread decodes Z-Wave serial frames and places
 * them into a blocking queue for the engine to consume.
 */
public class ZWaveSerialPort {
   private static final Logger logger = LoggerFactory.getLogger(ZWaveSerialPort.class);

   private final String portPath;
   private final BlockingQueue<Object> inboundQueue = new LinkedBlockingQueue<>();

   private volatile UartNative.SerialPort serialPort;
   private volatile InputStream inputStream;
   private volatile OutputStream outputStream;
   private volatile boolean open;
   private Thread readerThread;

   public ZWaveSerialPort(String portPath) {
      this.portPath = portPath;
   }

   /**
    * Open the serial port and start the reader thread.
    */
   public void open() throws Exception {
      logger.info("Opening Z-Wave serial port: {}", portPath);

      UartNative.SerialPort sp = UartNative.create(portPath);
      sp.setBaudRate(ZWaveSerialConstants.ZWAVE_BAUD_RATE);
      sp.setDataBits(UartNative.DataBits.DATA8);
      sp.setStopBits(UartNative.StopBits.STOP1);
      sp.setParityBit(UartNative.ParityBit.NONE);
      sp.setFlowControl(UartNative.FlowControl.NONE);
      // VMIN=1, VTIME=0: read() blocks until at least 1 byte arrives.
      // This matches the ZigBee NativeZigBeePort configuration and avoids
      // the 0-byte timeout reads that corrupt framing.
      sp.setVMin(1);
      sp.setVTime(0);
      sp.open();

      serialPort = sp;
      inputStream = sp.getInputStream();
      outputStream = sp.getOutputStream();
      open = true;

      readerThread = new Thread(this::readLoop, "zwave-serial-port-reader");
      readerThread.setDaemon(true);
      readerThread.start();

      logger.info("Z-Wave serial port opened successfully (fd={})", sp.getFd());
   }

   /**
    * Close the serial port.
    */
   public void close() {
      if (!open) {
         return;
      }
      logger.info("Closing Z-Wave serial port");
      open = false;

      // Close only the underlying serial port — it owns the FD.
      // Don't close the streams separately since they share the same FD.
      UartNative.SerialPort sp = serialPort;
      if (sp != null) {
         try {
            sp.close();
         } catch (IOException e) {
            logger.warn("Error closing serial port: {}", e.getMessage());
         }
      }

      serialPort = null;
      inputStream = null;
      outputStream = null;
   }

   /**
    * Send raw bytes to the serial port.
    */
   public synchronized void write(byte[] data) {
      OutputStream os = outputStream;
      if (!open || os == null) {
         logger.warn("Cannot write to serial port - not open");
         return;
      }
      try {
         os.write(data);
         os.flush();
      } catch (IOException e) {
         logger.error("Z-Wave serial write failed ({}: {})", e.getClass().getSimpleName(), e.getMessage(), e);
      }
   }

   /**
    * Send a single byte (ACK, NAK, CAN).
    */
   public void writeByte(byte b) {
      write(new byte[] { b });
   }

   /**
    * Send ACK to the Z-Wave controller.
    */
   public void sendAck() {
      writeByte(ZWaveSerialConstants.ACK);
   }

   /**
    * Send NAK to the Z-Wave controller.
    */
   public void sendNak() {
      writeByte(ZWaveSerialConstants.NAK);
   }

   /**
    * Poll the inbound queue for the next message. Returns a ZWaveSerialFrame
    * for data frames, a Byte for single-byte signals, or null on timeout.
    */
   public Object poll(long timeout, TimeUnit unit) throws InterruptedException {
      return inboundQueue.poll(timeout, unit);
   }

   /**
    * Drain the inbound queue.
    */
   public void drain() {
      inboundQueue.clear();
   }

   public boolean isOpen() {
      return open;
   }

   /**
    * Reader thread that continuously reads from the serial port input
    * stream and decodes Z-Wave frames.  With VMIN=1/VTIME=0, read()
    * blocks until at least one byte arrives.
    */
   private void readLoop() {
      logger.debug("Z-Wave serial port reader started");
      byte[] buf = new byte[256];
      byte[] frameBuf = new byte[512];
      int framePos = 0;

      while (open) {
         try {
            InputStream is = inputStream;
            if (is == null) break;

            int n = is.read(buf);
            if (n < 0) {
               if (open) {
                  logger.warn("Z-Wave serial port returned EOF");
               }
               break;
            }
            if (n == 0) {
               continue;
            }

            // Append to frame buffer
            for (int i = 0; i < n; i++) {
               if (framePos >= frameBuf.length) {
                  logger.warn("Z-Wave frame buffer overflow, discarding {} bytes", framePos);
                  framePos = 0;
               }
               frameBuf[framePos++] = buf[i];
            }

            // Decode complete frames from the buffer
            framePos = decodeFrames(frameBuf, framePos);

         } catch (IOException e) {
            if (open) {
               logger.error("Z-Wave serial port read error ({}: {})",
                     e.getClass().getSimpleName(), e.getMessage(), e);
            }
            break;
         }
      }
      logger.debug("Z-Wave serial port reader stopped");
   }

   /**
    * Decode complete frames from the buffer. Returns the number of
    * unconsumed bytes remaining (shifted to the start of the buffer).
    */
   private int decodeFrames(byte[] buf, int len) {
      int pos = 0;

      while (pos < len) {
         byte b = buf[pos];

         switch (b) {
            case ZWaveSerialConstants.ACK:
            case ZWaveSerialConstants.NAK:
            case ZWaveSerialConstants.CAN:
               inboundQueue.offer(Byte.valueOf(b));
               pos++;
               break;

            case ZWaveSerialConstants.SOF:
               if (pos + 1 >= len) {
                  shift(buf, pos, len);
                  return len - pos;
               }
               int frameLen = 0xFF & buf[pos + 1];
               if (frameLen < 3) {
                  logger.warn("Invalid Z-Wave frame length: {}", frameLen);
                  pos += 2;
                  break;
               }
               int totalLen = frameLen + 2; // SOF + length byte + frame content
               if (pos + totalLen > len) {
                  shift(buf, pos, len);
                  return len - pos;
               }
               byte[] raw = new byte[totalLen];
               System.arraycopy(buf, pos, raw, 0, totalLen);
               ZWaveSerialFrame frame = ZWaveSerialFrame.parse(raw, 0, raw.length);
               if (frame != null) {
                  inboundQueue.offer(frame);
               } else {
                  logger.warn("Failed to parse Z-Wave frame, bad checksum");
               }
               pos += totalLen;
               break;

            default:
               logger.debug("Discarding unexpected byte: 0x{}", String.format("%02X", 0xFF & b));
               pos++;
               break;
         }
      }

      return 0;
   }

   private static void shift(byte[] buf, int from, int len) {
      if (from > 0 && from < len) {
         System.arraycopy(buf, from, buf, 0, len - from);
      }
   }
}

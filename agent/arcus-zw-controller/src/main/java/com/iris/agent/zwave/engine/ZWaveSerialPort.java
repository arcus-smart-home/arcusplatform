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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.agent.os.serial.UartAddress;
import com.iris.agent.os.serial.UartChannelConfig;
import com.iris.agent.os.serial.UartChannelOption;
import com.iris.agent.os.serial.UartOioChannel;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.oio.OioEventLoopGroup;
import io.netty.handler.codec.ByteToMessageDecoder;

/**
 * Manages the serial port connection to the Z-Wave controller chip.
 * Uses the existing arcus-os UartOioChannel for Netty-based serial I/O.
 *
 * Inbound frames and single-byte signals (ACK/NAK/CAN) are placed into
 * a blocking queue for the engine to consume.
 */
public class ZWaveSerialPort {
   private static final Logger logger = LoggerFactory.getLogger(ZWaveSerialPort.class);

   private final String portPath;
   private final BlockingQueue<Object> inboundQueue = new LinkedBlockingQueue<>();

   private OioEventLoopGroup eventLoopGroup;
   private Channel channel;

   public ZWaveSerialPort(String portPath) {
      this.portPath = portPath;
   }

   /**
    * Open the serial port and start reading frames.
    */
   public void open() throws Exception {
      logger.info("Opening Z-Wave serial port: {}", portPath);
      eventLoopGroup = new OioEventLoopGroup();

      Bootstrap bootstrap = new Bootstrap();
      bootstrap.group(eventLoopGroup)
         .channel(UartOioChannel.class)
         .option(UartChannelOption.BAUDRATE, ZWaveSerialConstants.ZWAVE_BAUD_RATE)
         .option(UartChannelOption.DATABITS, UartChannelConfig.DataBits.DATABITS_8)
         .option(UartChannelOption.STOPBITS, UartChannelConfig.StopBits.STOPBITS_1)
         .option(UartChannelOption.PARITYBIT, UartChannelConfig.ParityBit.PARITY_NONE)
         .option(UartChannelOption.FLOWCONTROL, UartChannelConfig.FlowControl.FLOW_NONE)
         .handler(new ChannelInitializer<UartOioChannel>() {
            @Override
            protected void initChannel(UartOioChannel ch) {
               ch.pipeline().addLast(new ZWaveFrameDecoder());
               ch.pipeline().addLast(new ZWaveFrameHandler());
            }
         });

      ChannelFuture future = bootstrap.connect(new UartAddress(portPath)).sync();
      channel = future.channel();
      logger.info("Z-Wave serial port opened successfully");
   }

   /**
    * Close the serial port.
    */
   public void close() {
      logger.info("Closing Z-Wave serial port");
      if (channel != null) {
         try {
            channel.close().sync();
         } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
         }
         channel = null;
      }
      if (eventLoopGroup != null) {
         eventLoopGroup.shutdownGracefully();
         eventLoopGroup = null;
      }
   }

   /**
    * Send raw bytes to the serial port.
    */
   public void write(byte[] data) {
      if (channel != null && channel.isActive()) {
         ByteBuf buf = channel.alloc().buffer(data.length);
         buf.writeBytes(data);
         channel.writeAndFlush(buf);
      } else {
         logger.warn("Cannot write to serial port - channel not active");
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
      return channel != null && channel.isActive();
   }

   /**
    * Netty decoder that parses the Z-Wave serial framing protocol.
    * Handles SOF frames and single-byte signals (ACK/NAK/CAN).
    */
   private class ZWaveFrameDecoder extends ByteToMessageDecoder {
      @Override
      protected void decode(ChannelHandlerContext ctx, ByteBuf in, java.util.List<Object> out) {
         while (in.isReadable()) {
            in.markReaderIndex();
            byte first = in.readByte();

            switch (first) {
               case ZWaveSerialConstants.ACK:
               case ZWaveSerialConstants.NAK:
               case ZWaveSerialConstants.CAN:
                  out.add(Byte.valueOf(first));
                  break;

               case ZWaveSerialConstants.SOF:
                  if (!in.isReadable()) {
                     in.resetReaderIndex();
                     return;
                  }
                  int frameLen = 0xFF & in.readByte();
                  if (frameLen < 3) {
                     // Invalid, discard
                     logger.warn("Invalid Z-Wave frame length: {}", frameLen);
                     break;
                  }
                  // Need frameLen - 1 more bytes (we already read the length byte)
                  if (in.readableBytes() < frameLen - 1) {
                     in.resetReaderIndex();
                     return;
                  }
                  // Read the entire frame including SOF and length for parsing
                  byte[] raw = new byte[frameLen + 2]; // SOF + length + frame content
                  raw[0] = ZWaveSerialConstants.SOF;
                  raw[1] = (byte) frameLen;
                  in.readBytes(raw, 2, frameLen);

                  ZWaveSerialFrame frame = ZWaveSerialFrame.parse(raw, 0, raw.length);
                  if (frame != null) {
                     out.add(frame);
                  } else {
                     logger.warn("Failed to parse Z-Wave frame, bad checksum");
                  }
                  break;

               default:
                  // Unexpected byte, discard
                  logger.debug("Discarding unexpected byte: 0x{}", String.format("%02X", 0xFF & first));
                  break;
            }
         }
      }
   }

   /**
    * Netty handler that receives decoded frames and signals and queues them.
    */
   private class ZWaveFrameHandler extends SimpleChannelInboundHandler<Object> {
      @Override
      protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
         inboundQueue.offer(msg);
      }

      @Override
      public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
         logger.error("Error in Z-Wave serial handler", cause);
      }
   }
}

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

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Platform;
import com.sun.jna.ptr.IntByReference;

public final class UartNative {
   private static final Logger log = LoggerFactory.getLogger(UartNative.class);

   private static final Impl IMPL;
   private static final byte XON = 0x11;
   private static final byte XOFF = 0x13;

   static {
      Impl impl = null;

      if (Platform.isLinux()) {
         impl = UartNativeLinux.INSTANCE;
      } else if (Platform.isMac()) {
         impl = UartNativeOsx.INSTANCE;
      }

      if (impl == null) {
         throw new RuntimeException("architecture is not supported");
      }

      IMPL = impl;
   }

   public static SerialPort create(String port) {
      return new SerialPort(port);
   }

   private static FileChannel toFileChannel(FileDescriptor fd, String path) {
      return sun.nio.ch.FileChannelImpl.open(fd, path, true, true, false, null);
   }

   private static FileDescriptor toFileDescriptor(int fd) {
      try {
         FileDescriptor fdesc = new FileDescriptor();
         sun.misc.SharedSecrets.getJavaIOFileDescriptorAccess().set(fdesc, fd);
         return fdesc;
      } catch (Exception ex) {
         throw new UnsupportedOperationException(ex);
      }
   }

   private static void available(Integer fd, Object[] args) {
      IMPL.ioctl(fd, IMPL.constants().fionread(), args);
   }

   public enum ParityBit {
      NONE,
      ODD,
      EVEN,
   }

   public enum StopBits {
      STOP1,
      STOP2,
   }

   public enum DataBits {
      DATA5,
      DATA6,
      DATA7,
      DATA8,
   }

   public enum FlowControl {
      NONE,
      XONXOFF,
      RTSCTS,
   }

   // IMPLEMENTATION NODE:
   // A serial port is a streaming interface that never reaches its end, however
   // if a read timeout is set on the serial port using VTIME then the value -1
   // will be returned from the delegate's read method. This implementation
   // converts that to a 0 return value since it is just a timeout not a
   // end of stream.
   public static final class UartInputStream extends InputStream {
      private final Integer fd;
      private final InputStream delegate;
      private final IntByReference availableIntRef;
      private final Object[] availableArgs;

      public UartInputStream(int fd, InputStream delegate) {
         this.fd = fd;
         this.delegate = delegate;

         IntByReference ref = null;
         while (ref == null) {
            try {
               ref = new IntByReference();
            } catch (Throwable ex) {
               log.error("could not allocate new int by reference: {}", ex.getMessage(), ex);
            }
         }

         this.availableIntRef = ref;
         this.availableArgs = new Object[] { availableIntRef };
      }

      @Override
      public int read() throws IOException {
         int result = delegate.read();
         return (result < 0) ? 0 : result;
      }

      @Override
      public int read(@Nullable byte[] b) throws IOException {
         int result = delegate.read(b);
         return (result < 0) ? 0 : result;
      }

      @Override
      public int read(@Nullable byte[] b, int off, int len) throws IOException {
         int result = delegate.read(b, off, len);
         return (result < 0) ? 0 : result;
      }

      @Override
      public int available() throws IOException {
         UartNative.available(fd, availableArgs);
         return availableIntRef.getValue();
      }

      @Override
      public void close() throws IOException {
         delegate.close();
      }
   }

   public static class SerialPort implements Closeable {
      private final String port;

      private final Object oldtio;
      private final Object newtio;
      private int fd = -1;

      @Nullable
      private FileDescriptor filedesc;

      private int inputBaudRate = 115200;
      private int outputBaudRate = 115200;
      private int vtime = 0;
      private int vmin = 1;
      private FlowControl flow = FlowControl.RTSCTS;
      private ParityBit parity = ParityBit.NONE;
      private StopBits stop = StopBits.STOP1;
      private DataBits data = DataBits.DATA8;

      private SerialPort(String port) {
         this.port = port;
         this.oldtio = IMPL.newTermios();
         this.newtio = IMPL.newTermios();
      }

      public FileChannel getChannel() {
         if (fd >= 0 && filedesc != null) {
            return UartNative.toFileChannel(filedesc, port);
         }

         throw new RuntimeException("serial port must be opened first");
      }

      public InputStream getInputStream() {
         if (fd >= 0 && filedesc != null) {
            FileChannel channel = UartNative.toFileChannel(filedesc, port);
            InputStream delegate = Channels.newInputStream(channel);
            return new UartInputStream(fd, delegate);
         }

         throw new RuntimeException("serial port must be opened first");
      }

      public OutputStream getOutputStream() {
         if (fd >= 0 && filedesc != null) {
            FileChannel channel = UartNative.toFileChannel(filedesc, port);
            return Channels.newOutputStream(channel);
         }

         throw new RuntimeException("serial port must be opened first");
      }

      @SuppressWarnings("deprecation")
      public void open() throws IOException {
         Native.setPreserveLastError(true);

         int ofd = IMPL.open(port, IMPL.constants().getOpenFlags());
         if (ofd < 0) {
            throw new IOException("cannot open serial port: " + port);
         }

         try {
            int result;
            this.fd = ofd;
            this.filedesc = toFileDescriptor(fd);

            result = IMPL.fcntl(ofd, IMPL.constants().getfl());
            result = IMPL.fcntl(ofd, IMPL.constants().setfl(), result & ~IMPL.constants().nonblock());
            if (result < 0) {
               throw new IOException("cannot set serial port to non-blocking: " + Native.getLastError());
            }

            result = IMPL.tcgetattr(ofd, oldtio);
            if (result < 0) {
               throw new IOException("cannot get current serial port settings: " + Native.getLastError());
            }

            IMPL.cfmakeraw(newtio);

            result = IMPL.cfsetispeed(newtio, inputBaudRate);
            if (result < 0) {
               throw new IOException("cannot set serial port input speed: " + Native.getLastError());
            }

            result = IMPL.cfsetospeed(newtio, outputBaudRate);
            if (result < 0) {
               throw new IOException("cannot set serial port output speed: " + Native.getLastError());
            }

            IMPL.setTiosCc(newtio, IMPL.constants().vmin(), (byte)vmin);
            IMPL.setTiosCc(newtio, IMPL.constants().vtime(), (byte)vtime);
            IMPL.setTiosCc(newtio, IMPL.constants().vstart(), (byte)XON);
            IMPL.setTiosCc(newtio, IMPL.constants().vstop(), (byte)XOFF);

            long c_cflag = IMPL.getTiosCFlags(newtio);
            long c_iflag = IMPL.getTiosIFlags(newtio);

            c_cflag |= IMPL.constants().cread();
            c_cflag |= IMPL.constants().clocal();
            c_cflag &= ~IMPL.constants().csize();

            c_iflag |= IMPL.constants().ignpar();

            switch (data) {
            case DATA5: c_cflag |= IMPL.constants().cs5(); break;
            case DATA6: c_cflag |= IMPL.constants().cs6(); break;
            case DATA7: c_cflag |= IMPL.constants().cs7(); break;
            case DATA8: c_cflag |= IMPL.constants().cs8(); break;
            default: throw new IOException("unknown char size");
            }

            switch (stop) {
            case STOP1: break;
            case STOP2: c_cflag |= IMPL.constants().cstopb(); break;
            default: throw new IOException("unknown number of stop bits");
            }

            switch (parity) {
            case NONE: break;
            case EVEN: c_cflag |= IMPL.constants().parenb(); break;
            case ODD:  c_cflag |= IMPL.constants().parenb() | IMPL.constants().parodd(); break;
            default: throw new IOException("unknown parity");
            }

            switch (flow) {
            case NONE: break;
            case XONXOFF: c_iflag |= IMPL.constants().ixon() | IMPL.constants().ixoff(); break;
            case RTSCTS: c_cflag |= IMPL.constants().crtscts(); break;
            default: throw new IOException("unknown flow control");
            }

            IMPL.setTiosCFlags(newtio, c_cflag);
            IMPL.setTiosIFlags(newtio, c_iflag);

            IMPL.tcflush(ofd, IMPL.constants().tciflush());
            result = IMPL.tcsetattr(ofd, IMPL.constants().tcsanow(), newtio);
            if (result < 0) {
               throw new IOException("cannot setup serial port settings");
            }
         } catch (Throwable th) {
            IMPL.close(ofd);
            this.fd = -1;
            this.filedesc = null;
            throw th;
         }
      }

      @Override
      public void close() throws IOException {
         int cfd = this.fd;
         if (cfd > 0) {
            IMPL.tcflush(cfd, IMPL.constants().tciflush());
            IMPL.tcsetattr(cfd, IMPL.constants().tcsanow(), oldtio);
            IMPL.close(cfd);
         }

         this.filedesc = null;
         this.fd = -1;
      }

      public String getPort() {
         return port;
      }

      @Nullable
      public FileDescriptor getFiledesc() {
         return filedesc;
      }

      public int getFd() {
         return fd;
      }

      public int getInputBaudRate() {
         return inputBaudRate;
      }

      public void setInputBaudRate(int inputBaudRate) {
         this.inputBaudRate = inputBaudRate;
      }

      public int getOutputBaudRate() {
         return outputBaudRate;
      }

      public void setOutputBaudRate(int outputBaudRate) {
         this.outputBaudRate = outputBaudRate;
      }

      public void setBaudRate(int baudRate) {
         setInputBaudRate(baudRate);
         setOutputBaudRate(baudRate);
      }

      public int getVTime() {
         return vtime;
      }

      public void setVTime(int vtime) {
         this.vtime = vtime;
      }

      public int getVMin() {
         return vmin;
      }

      public void setVMin(int vmin) {
         this.vmin = vmin;
      }

      public FlowControl getFlowControl() {
         return flow;
      }

      public void setFlowControl(FlowControl flow) {
         this.flow = flow;
      }

      public ParityBit getParityBit() {
         return parity;
      }

      public void setParityBit(ParityBit parity) {
         this.parity = parity;
      }

      public StopBits getStopBits() {
         return stop;
      }

      public void setStopBits(StopBits stop) {
         this.stop = stop;
      }

      public DataBits getDataBits() {
         return data;
      }

      public void setDataBits(DataBits data) {
         this.data = data;
      }
   }

   interface Impl {
      Object newTermios();
      CConstants constants();

      int open(String path, int flags);
      int close(int fd);
      int fcntl(int fd, int cmd, Object... args);
      int ioctl(int fd, NativeLong request, Object... args);

      int tcsendbreak(int fd, int duration);
      int tcdrain(int fd);
      int tcflush(int fd, int queueSelector);
      int tcflow(int fd, int action);

      int tcgetattr(int fd, Object termios);
      int tcsetattr(int fd, int optionalActions, Object termios);

      void cfmakeraw(Object termios);
      long cfgetispeed(Object termios);
      long cfgetospeed(Object termios);

      int cfsetispeed(Object termios, long speed);
      int cfsetospeed(Object termios, long speed);

      long getTiosIFlags(Object termios);
      long getTiosOFlags(Object termios);
      long getTiosCFlags(Object termios);
      long getTiosLFlags(Object termios);

      void setTiosIFlags(Object termios, long value);

      void setTiosOFlags(Object termios, long value);

      void setTiosCFlags(Object termios, long value);

      void setTiosLFlags(Object termios, long value);

      byte getTiosCc(Object termios, int idx);

      void setTiosCc(Object termios, int idx, byte value);
   }

   interface CConstants {
      int getOpenFlags();

      int getfl();

      int setfl();

      int nonblock();

      NativeLong fionread();

      long clocal();

      long csize();

      long cread();

      long ignpar();

      long cs5();

      long cs6();

      long cs7();

      long cs8();

      long cstopb();

      long parenb();

      long parodd();

      long ixon();

      long ixoff();

      long crtscts();

      int vmin();

      int vtime();

      int vstart();

      int vstop();

      int tciflush();

      int tcsanow();
   }
}


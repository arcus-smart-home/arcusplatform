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

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Structure;

public enum UartNativeLinux implements UartNative.Impl {
   INSTANCE;

   @Override
   public Object newTermios() {
      return new LINCLibrary.Termios();
   }

   @Override
   public UartNative.CConstants constants() {
      return LinuxConstants.INSTANCE;
   }

   @Override
   public int open(String path, int flags) {
      return LINCLibrary.INSTANCE.open(path, flags);
   }

   @Override
   public int close(int fd) {
      return LINCLibrary.INSTANCE.close(fd);
   }

   @Override
   public int fcntl(int fd, int cmd, Object... args) {
      return LINCLibrary.INSTANCE.fcntl(fd, cmd, args);
   }

   @Override
   public int ioctl(int fd, NativeLong request, Object... args) {
      return LINCLibrary.INSTANCE.ioctl(fd, request, args);
   }

   @Override
   public int tcsendbreak(int fd, int duration) {
      return LINCLibrary.INSTANCE.tcsendbreak(fd, duration);
   }

   @Override
   public int tcdrain(int fd) {
      return LINCLibrary.INSTANCE.tcdrain(fd);
   }

   @Override
   public int tcflush(int fd, int queueSelector) {
      return LINCLibrary.INSTANCE.tcflush(fd, queueSelector);
   }

   @Override
   public int tcflow(int fd, int action) {
      return LINCLibrary.INSTANCE.tcflow(fd, action);
   }

   @Override
   public int tcgetattr(int fd, Object termios) {
      return LINCLibrary.INSTANCE.tcgetattr(fd, (LINCLibrary.Termios)termios);
   }

   @Override
   public int tcsetattr(int fd, int optionalActions, Object termios) {
      return LINCLibrary.INSTANCE.tcsetattr(fd, optionalActions, (LINCLibrary.Termios)termios);
   }

   @Override
   public void cfmakeraw(Object termios) {
      LINCLibrary.INSTANCE.cfmakeraw((LINCLibrary.Termios)termios);
   }

   private long fromSpeed(int speed) {
      switch (speed) {
      case LinuxConstants.B0: return 0;
      case LinuxConstants.B50: return 50;
      case LinuxConstants.B75: return 75;
      case LinuxConstants.B110: return 110;
      case LinuxConstants.B134: return 134;
      case LinuxConstants.B150: return 150;
      case LinuxConstants.B200: return 200;
      case LinuxConstants.B300: return 300;
      case LinuxConstants.B600: return 600;
      case LinuxConstants.B1200: return 1200;
      case LinuxConstants.B1800: return 1800;
      case LinuxConstants.B2400: return 2400;
      case LinuxConstants.B4800: return 4800;
      case LinuxConstants.B9600: return 9600;
      case LinuxConstants.B19200: return 19200;
      case LinuxConstants.B38400: return 38400;
      case LinuxConstants.B57600: return 57600;
      case LinuxConstants.B115200: return 115200;
      case LinuxConstants.B230400: return 230400;
      case LinuxConstants.B460800: return 460800;
      case LinuxConstants.B500000: return 500000;
      case LinuxConstants.B576000: return 576000;
      case LinuxConstants.B921600: return 921600;
      case LinuxConstants.B1000000: return 1000000;
      case LinuxConstants.B1152000: return 1152000;
      case LinuxConstants.B1500000: return 1500000;
      case LinuxConstants.B2000000: return 2000000;
      case LinuxConstants.B2500000: return 2500000;
      case LinuxConstants.B3000000: return 3000000;
      case LinuxConstants.B3500000: return 3500000;
      case LinuxConstants.B4000000: return 4000000;
      default: throw new RuntimeException("unknown baud rate: " + speed);
      }
   }

   @Override
   public long cfgetispeed(Object termios) {
      return fromSpeed(LINCLibrary.INSTANCE.cfgetispeed((LINCLibrary.Termios)termios));
   }

   @Override
   public long cfgetospeed(Object termios) {
      return fromSpeed(LINCLibrary.INSTANCE.cfgetospeed((LINCLibrary.Termios)termios));
   }

   private int toSpeed(long speed) {
      switch ((int)speed) {
      case 0: return LinuxConstants.B0;
      case 50: return LinuxConstants.B50;
      case 75: return LinuxConstants.B75;
      case 110: return LinuxConstants.B110;
      case 134: return LinuxConstants.B134;
      case 150: return LinuxConstants.B150;
      case 200: return LinuxConstants.B200;
      case 300: return LinuxConstants.B300;
      case 600: return LinuxConstants.B600;
      case 1200: return LinuxConstants.B1200;
      case 1800: return LinuxConstants.B1800;
      case 2400: return LinuxConstants.B2400;
      case 4800: return LinuxConstants.B4800;
      case 9600: return LinuxConstants.B9600;
      case 19200: return LinuxConstants.B19200;
      case 38400: return LinuxConstants.B38400;
      case 57600: return LinuxConstants.B57600;
      case 115200: return LinuxConstants.B115200;
      case 230400: return LinuxConstants.B230400;
      case 460800: return LinuxConstants.B460800;
      case 500000: return LinuxConstants.B500000;
      case 576000: return LinuxConstants.B576000;
      case 921600: return LinuxConstants.B921600;
      case 1000000: return LinuxConstants.B1000000;
      case 1152000: return LinuxConstants.B1152000;
      case 1500000: return LinuxConstants.B1500000;
      case 2000000: return LinuxConstants.B2000000;
      case 2500000: return LinuxConstants.B2500000;
      case 3000000: return LinuxConstants.B3000000;
      case 3500000: return LinuxConstants.B3500000;
      case 4000000: return LinuxConstants.B4000000;
      default: throw new RuntimeException("unknown baud rate: " + speed);
      }
   }

   @Override
   public int cfsetispeed(Object termios, long speed) {
      return LINCLibrary.INSTANCE.cfsetispeed((LINCLibrary.Termios)termios, toSpeed(speed));
   }

   @Override
   public int cfsetospeed(Object termios, long speed) {
      return LINCLibrary.INSTANCE.cfsetospeed((LINCLibrary.Termios)termios, toSpeed(speed));
   }

   @Override
   public long getTiosIFlags(Object termios) {
      return ((LINCLibrary.Termios)termios).c_iflag;
   }

   @Override
   public long getTiosOFlags(Object termios) {
      return ((LINCLibrary.Termios)termios).c_oflag;
   }

   @Override
   public long getTiosCFlags(Object termios) {
      return ((LINCLibrary.Termios)termios).c_cflag;
   }

   @Override
   public long getTiosLFlags(Object termios) {
      return ((LINCLibrary.Termios)termios).c_lflag;
   }

   @Override
   public void setTiosIFlags(Object termios, long value) {
      ((LINCLibrary.Termios)termios).c_iflag = (int)value;
   }

   @Override
   public void setTiosOFlags(Object termios, long value) {
      ((LINCLibrary.Termios)termios).c_oflag = (int)value;
   }

   @Override
   public void setTiosCFlags(Object termios, long value) {
      ((LINCLibrary.Termios)termios).c_cflag = (int)value;
   }

   @Override
   public void setTiosLFlags(Object termios, long value) {
      ((LINCLibrary.Termios)termios).c_lflag = (int)value;
   }

   @Override
   public byte getTiosCc(Object termios, int idx) {
      return ((LINCLibrary.Termios)termios).c_cc[idx];
   }

   @Override
   public void setTiosCc(Object termios, int idx, byte value) {
      ((LINCLibrary.Termios)termios).c_cc[idx] = value;
   }

   public static enum LinuxConstants implements UartNative.CConstants {
      INSTANCE;

      public static final int O_NOCTTY   = 0x00000100;
      public static final int O_NONBLOCK = 0x00000800;
      public static final int O_RDWR     = 0x00000002;

      public static final NativeLong FIONREAD = new NativeLong(0x0000541BL);

      public static final int TCSANOW = 0;
      public static final int TCSADRAIN = 1;
      public static final int TCSAFLUSH = 2;

      public static final int B0      = 0x00000000;
      public static final int B50     = 0x00000001;
      public static final int B75     = 0x00000002;
      public static final int B110    = 0x00000003;
      public static final int B134    = 0x00000004;
      public static final int B150    = 0x00000005;
      public static final int B200    = 0x00000006;
      public static final int B300    = 0x00000007;
      public static final int B600    = 0x00000008;
      public static final int B1200   = 0x00000009;
      public static final int B1800   = 0x0000000A;
      public static final int B2400   = 0x0000000B;
      public static final int B4800   = 0x0000000C;
      public static final int B9600   = 0x0000000D;
      public static final int B19200  = 0x0000000E;
      public static final int B38400  = 0x0000000F;
      public static final int B57600  = 0x00001001;
      public static final int B115200 = 0x00001002;
      public static final int B230400 = 0x00001003;
      public static final int B460800 = 0x00001004;
      public static final int B500000 = 0x00001005;
      public static final int B576000 = 0x00001006;
      public static final int B921600 = 0x00001007;
      public static final int B1000000 = 0x00001008;
      public static final int B1152000 = 0x00001009;
      public static final int B1500000 = 0x0000100A;
      public static final int B2000000 = 0x0000100B;
      public static final int B2500000 = 0x0000100C;
      public static final int B3000000 = 0x0000100D;
      public static final int B3500000 = 0x0000100E;
      public static final int B4000000 = 0x0000100F;
      public static final int EXTA    = B19200;
      public static final int EXTB    = B38400;

      public static final int TCIFLUSH    = 0;
      public static final int TCOFLUSH    = 2;
      public static final int TCIOFLUSH   = 2;
      public static final int TCOOFF      = 0;
      public static final int TCOON       = 1;
      public static final int TCIOFF      = 2;
      public static final int TCION       = 3;

      public static final long CSIZE       = 0x00000030;
      public static final long CS5         = 0x00000000;
      public static final long CS6         = 0x00000010;
      public static final long CS7         = 0x00000020;
      public static final long CS8         = 0x00000030;
      public static final long CSTOPB      = 0x00000040;
      public static final long CREAD       = 0x00000080;
      public static final long PARENB      = 0x00000100;
      public static final long PARODD      = 0x00000200;
      public static final long HUPCL       = 0x00000400;
      public static final long CLOCAL      = 0x00000800;
      public static final long CRTSCTS     = 0x80000000;

      public static final long IGNBRK  = 0x00000001;
      public static final long BRKINT  = 0x00000002;
      public static final long IGNPAR  = 0x00000004;
      public static final long PARMRK  = 0x00000008;
      public static final long INPCK   = 0x00000010;
      public static final long ISTRIP  = 0x00000020;
      public static final long INLCR   = 0x00000040;
      public static final long IGNCR   = 0x00000080;
      public static final long ICRNL   = 0x00000100;
      public static final long IUCLC   = 0x00000200;
      public static final long IXON    = 0x00000400;
      public static final long IXANY   = 0x00000800;
      public static final long IXOFF   = 0x00001000;
      public static final long IMAXBEL = 0x00002000;
      public static final long IUTF8   = 0x00004000;

      public static final long OPOST   = 0x00000001;
      public static final long ONLCR   = 0x00000004;

      public static final int VINTR = 0;
      public static final int VQUIT = 1;
      public static final int VERASE = 2;
      public static final int VKILL = 3;
      public static final int VEOF = 4;
      public static final int VTIME = 5;
      public static final int VMIN = 6;
      public static final int VSWTC = 7;
      public static final int VSTART = 8;
      public static final int VSTOP = 9;
      public static final int VSUSP = 10;
      public static final int VEOL = 11;
      public static final int VREPRINT = 12;
      public static final int VDISCARD = 13;
      public static final int VWERASE = 14;
      public static final int VLNEXT = 15;
      public static final int VEOL2 = 16;

      public static final int F_GETFL = 3;
      public static final int F_SETFL = 4;

      @Override
      public int getOpenFlags() {
         return O_NOCTTY | O_RDWR | O_NONBLOCK;
      }

      @Override
      public int getfl() {
         return F_GETFL;
      }

      @Override
      public int setfl() {
         return F_SETFL;
      }

      @Override
      public int nonblock() {
         return O_NONBLOCK;
      }

      @Override
      public NativeLong fionread() {
         return FIONREAD;
      }

      @Override
      public long clocal() {
         return CLOCAL;
      }

      @Override
      public long cread() {
         return CREAD;
      }

      @Override
      public long csize() {
         return CSIZE;
      }

      @Override
      public long ignpar() {
         return IGNPAR;
      }

      @Override
      public long cs5() {
         return CS5;
      }

      @Override
      public long cs6() {
         return CS6;
      }

      @Override
      public long cs7() {
         return CS7;
      }

      @Override
      public long cs8() {
         return CS8;
      }

      @Override
      public long cstopb() {
         return CSTOPB;
      }

      @Override
      public long parenb() {
         return PARENB;
      }

      @Override
      public long parodd() {
         return PARODD;
      }

      @Override
      public long ixon() {
         return IXON;
      }

      @Override
      public long ixoff() {
         return IXOFF;
      }

      @Override
      public long crtscts() {
         return CRTSCTS;
      }

      @Override
      public int vmin() {
         return VMIN;
      }

      @Override
      public int vtime() {
         return VTIME;
      }

      @Override
      public int vstart() {
         return VSTART;
      }

      @Override
      public int vstop() {
         return VSTOP;
      }

      @Override
      public int tciflush() {
         return TCIFLUSH;
      }

      @Override
      public int tcsanow() {
         return TCSANOW;
      }
   }

   public interface LINCLibrary extends Library {
      LINCLibrary INSTANCE = (LINCLibrary)Native.loadLibrary("c", LINCLibrary.class);

      public int open(String path, int flags);
      public int close(int fd);
      public int fcntl(int fd, int cmd, Object... args);
      public int ioctl(int fd, NativeLong reqeust, Object... args);

      public int tcsendbreak(int fd, int duration);
      public int tcdrain(int fd);
      public int tcflush(int fd, int queue_selector);
      public int tcflow(int fd, int action);

      public int tcgetattr(int fd, Termios termios);
      public int tcsetattr(int fd, int optional_actions, Termios termios);

      public void cfmakeraw(Termios termios);
      public int cfgetispeed(Termios termios);
      public int cfgetospeed(Termios termios);

      public int cfsetispeed(Termios termios, int speed);
      public int cfsetospeed(Termios termios, int speed);
      public int cfsetspeed(Termios termios, int speed);

      public static class Termios extends Structure {
         public static final int NCCS = 32;

         public int c_iflag;
         public int c_oflag;
         public int c_cflag;
         public int c_lflag;
         public byte c_line;
         public byte[] c_cc;
         public int c_ispeed;
         public int c_ospeed;

         public Termios() {
            this.c_cc = new byte[NCCS];
         }

         @Override
         protected List getFieldOrder() {
            return Arrays.asList("c_iflag", "c_oflag", "c_cflag", "c_lflag", "c_line", "c_cc", "c_ispeed", "c_ospeed");
         }

         @Override
         public String toString() {
            return "Termios [" +
                "c_iflag=" + Long.toHexString((long)c_iflag & 0xFFFFFFFF) +
               ",c_oflag=" + Long.toHexString((long)c_oflag & 0xFFFFFFFF) +
               ",c_cflag=" + Long.toHexString((long)c_cflag & 0xFFFFFFFF) +
               ",c_lflag=" + Long.toHexString((long)c_lflag & 0xFFFFFFFF) +
               ",c_line=" + Integer.toHexString((int)c_line & 0xFF) +
               ",c_cc=" + Arrays.toString(c_cc) +
               ",c_ispeed=" + c_ispeed +
               ",c_ospeed=" + c_ospeed + "]";
         }
      }
   }
}


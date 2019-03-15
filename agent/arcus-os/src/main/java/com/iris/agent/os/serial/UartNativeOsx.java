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
import com.sun.jna.Platform;
import com.sun.jna.Structure;

public enum UartNativeOsx implements UartNative.Impl {
   INSTANCE;

   @Override
   public Object newTermios() {
      return new OSXCLibrary.Termios();
   }

   @Override
   public UartNative.CConstants constants() {
      return OsxConstants.INSTANCE;
   }

   @Override
   public int open(String path, int flags) {
      return OSXCLibrary.INSTANCE.open(path, flags);
   }

   @Override
   public int close(int fd) {
      return OSXCLibrary.INSTANCE.close(fd);
   }

   @Override
   public int fcntl(int fd, int cmd, Object... args) {
      return OSXCLibrary.INSTANCE.fcntl(fd, cmd, args);
   }

   @Override
   public int ioctl(int fd, NativeLong request, Object... args) {
      return OSXCLibrary.INSTANCE.ioctl(fd, request, args);
   }

   @Override
   public int tcsendbreak(int fd, int duration) {
      return OSXCLibrary.INSTANCE.tcsendbreak(fd, duration);
   }

   @Override
   public int tcdrain(int fd) {
      return OSXCLibrary.INSTANCE.tcdrain(fd);
   }

   @Override
   public int tcflush(int fd, int queueSelector) {
      return OSXCLibrary.INSTANCE.tcflush(fd, queueSelector);
   }

   @Override
   public int tcflow(int fd, int action) {
      return OSXCLibrary.INSTANCE.tcflow(fd, action);
   }

   @Override
   public int tcgetattr(int fd, Object termios) {
      return OSXCLibrary.INSTANCE.tcgetattr(fd, (OSXCLibrary.Termios)termios);
   }

   @Override
   public int tcsetattr(int fd, int optionalActions, Object termios) {
      return OSXCLibrary.INSTANCE.tcsetattr(fd, optionalActions, (OSXCLibrary.Termios)termios);
   }

   @Override
   public void cfmakeraw(Object termios) {
      OSXCLibrary.INSTANCE.cfmakeraw((OSXCLibrary.Termios)termios);
   }

   @Override
   public long cfgetispeed(Object termios) {
      return OSXCLibrary.INSTANCE.cfgetispeed((OSXCLibrary.Termios)termios);
   }

   @Override
   public long cfgetospeed(Object termios) {
      return OSXCLibrary.INSTANCE.cfgetospeed((OSXCLibrary.Termios)termios);
   }

   @Override
   public int cfsetispeed(Object termios, long speed) {
      return OSXCLibrary.INSTANCE.cfsetispeed((OSXCLibrary.Termios)termios, new NativeLong(speed));
   }

   @Override
   public int cfsetospeed(Object termios, long speed) {
      return OSXCLibrary.INSTANCE.cfsetospeed((OSXCLibrary.Termios)termios, new NativeLong(speed));
   }

   @Override
   public long getTiosIFlags(Object termios) {
      return ((OSXCLibrary.Termios)termios).c_iflag.longValue();
   }

   @Override
   public long getTiosOFlags(Object termios) {
      return ((OSXCLibrary.Termios)termios).c_oflag.longValue();
   }

   @Override
   public long getTiosCFlags(Object termios) {
      return ((OSXCLibrary.Termios)termios).c_cflag.longValue();
   }

   @Override
   public long getTiosLFlags(Object termios) {
      return ((OSXCLibrary.Termios)termios).c_lflag.longValue();
   }

   @Override
   public void setTiosIFlags(Object termios, long value) {
      ((OSXCLibrary.Termios)termios).c_iflag = new NativeLong(value);
   }

   @Override
   public void setTiosOFlags(Object termios, long value) {
      ((OSXCLibrary.Termios)termios).c_oflag = new NativeLong(value);
   }

   @Override
   public void setTiosCFlags(Object termios, long value) {
      ((OSXCLibrary.Termios)termios).c_cflag = new NativeLong(value);
   }

   @Override
   public void setTiosLFlags(Object termios, long value) {
      ((OSXCLibrary.Termios)termios).c_lflag = new NativeLong(value);
   }

   @Override
   public byte getTiosCc(Object termios, int idx) {
      return ((OSXCLibrary.Termios)termios).c_cc[idx];
   }

   @Override
   public void setTiosCc(Object termios, int idx, byte value) {
      ((OSXCLibrary.Termios)termios).c_cc[idx] = value;
   }

   public static enum OsxConstants implements UartNative.CConstants {
      INSTANCE;

      public static final int O_NOCTTY = 0x00020000;
      public static final int O_NONBLOCK = 0x00000004;
      public static final int O_RDWR = 0x00000002;

      public static final NativeLong FIONREAD = new NativeLong(0x4004667FL);

      public static final int TCSANOW = 0;
      public static final int TCSADRAIN = 1;
      public static final int TCSAFLUSH = 2;
      public static final int TCSASOFT = 0x10;

      public static final int B0      = 0;
      public static final int B50     = 50;
      public static final int B75     = 75;
      public static final int B110    = 110;
      public static final int B134    = 134;
      public static final int B150    = 150;
      public static final int B200    = 200;
      public static final int B300    = 300;
      public static final int B600    = 600;
      public static final int B1200   = 1200;
      public static final int B1800   = 1800;
      public static final int B2400   = 2400;
      public static final int B4800   = 4800;
      public static final int B9600   = 9600;
      public static final int B19200  = 19200;
      public static final int B38400  = 38400;
      public static final int B7200   = 7200;
      public static final int B14400  = 14400;
      public static final int B28800  = 28800;
      public static final int B57600  = 57600;
      public static final int B76800  = 76800;
      public static final int B115200 = 115200;
      public static final int B230400 = 230400;
      public static final int EXTA    = 19200;
      public static final int EXTB    = 38400;

      public static final int TCIFLUSH    = 1;
      public static final int TCOFLUSH    = 2;
      public static final int TCIOFLUSH   = 3;
      public static final int TCOOFF      = 1;
      public static final int TCOON       = 2;
      public static final int TCIOFF      = 3;
      public static final int TCION       = 4;

      public static final long CIGNORE     = 0x00000001;
      public static final long CSIZE       = 0x00000300;
      public static final long CS5         = 0x00000000;
      public static final long CS6         = 0x00000100;
      public static final long CS7         = 0x00000200;
      public static final long CS8         = 0x00000300;
      public static final long CSTOPB      = 0x00000400;
      public static final long CREAD       = 0x00000800;
      public static final long PARENB      = 0x00001000;
      public static final long PARODD      = 0x00002000;
      public static final long HUPCL       = 0x00004000;
      public static final long CLOCAL      = 0x00008000;
      public static final long CCTS_OFLOW  = 0x00010000;
      public static final long CRTS_IFLOW  = 0x00020000;
      public static final long CDTR_IFLOW  = 0x00040000;
      public static final long CDSR_OFLOW  = 0x00080000;
      public static final long CCAR_OFLOW  = 0x00100000;
      public static final long MDMBUF      = 0x00100000;
      public static final long CRTSCTS     = (CCTS_OFLOW | CRTS_IFLOW);

      public static final long IGNBRK      = 0x00000001;
      public static final long BRKINT      = 0x00000002;
      public static final long IGNPAR      = 0x00000004;
      public static final long PARMRK      = 0x00000008;
      public static final long INPCK       = 0x00000010;
      public static final long ISTRIP      = 0x00000020;
      public static final long INLCR       = 0x00000040;
      public static final long IGNCR       = 0x00000080;
      public static final long ICRNL       = 0x00000100;
      public static final long IXON        = 0x00000200;
      public static final long IXOFF       = 0x00000400;
      public static final long IXANY       = 0x00000800;
      public static final long IMAXBEL     = 0x00002000;
      public static final long IUTF8       = 0x00004000;

      public static final long OPOST       = 0x00000001;
      public static final long ONLCR       = 0x00000002;
      public static final long OXTABS      = 0x00000004;
      public static final long ONOEOT      = 0x00000008;

      public static final int VEOF        = 0;
      public static final int VEOL        = 1;
      public static final int VEOL2       = 2;
      public static final int VERASE      = 3;
      public static final int VWERASE     = 4;
      public static final int VKILL       = 5;
      public static final int VREPRINT    = 6;
      public static final int VINTR       = 8;
      public static final int VQUIT       = 9;
      public static final int VSUSP       = 10;
      public static final int VDSUSP      = 11;
      public static final int VSTART      = 12;
      public static final int VSTOP       = 13;
      public static final int VLNEXT      = 14;
      public static final int VDISCARD    = 15;
      public static final int VMIN        = 16;
      public static final int VTIME       = 17;
      public static final int VSTATUS     = 18;

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

   public interface OSXCLibrary extends Library {
      OSXCLibrary INSTANCE = (OSXCLibrary)Native.loadLibrary((Platform.isWindows() ? "msvcrt" : "c"), OSXCLibrary.class);

      public int open(String path, int flags);
      public int close(int fd);
      public int fcntl(int fd, int cmd, Object... args);
      public int ioctl(int fd, NativeLong reqeust, Object... args);

      public int tcsendbreak(int fd, int duration);
      public int tcdrain(int fd);
      public int tcflush(int fd, int queue_selector);
      public int tcflow(int fd, int action);

      int tcgetattr(int fd, Termios termios);
      int tcsetattr(int fd, int optional_actions, Termios termios);

      void cfmakeraw(Termios termios);
      long cfgetispeed(Termios termios);
      long cfgetospeed(Termios termios);

      int cfsetispeed(Termios termios, NativeLong speed);
      int cfsetospeed(Termios termios, NativeLong speed);
      int cfsetspeed(Termios termios, NativeLong speed);

      public static class Termios extends Structure {
         public static final int NCCS = 20;

         public NativeLong c_iflag;
         public NativeLong c_oflag;
         public NativeLong c_cflag;
         public NativeLong c_lflag;
         public byte[] c_cc;
         public NativeLong c_ispeed;
         public NativeLong c_ospeed;

         public Termios() {
            this.c_iflag = new NativeLong();
            this.c_oflag = new NativeLong();
            this.c_cflag = new NativeLong();
            this.c_lflag = new NativeLong();
            this.c_ispeed = new NativeLong();
            this.c_ospeed = new NativeLong();
            this.c_cc = new byte[NCCS];
         }

         @Override
         protected List getFieldOrder() {
            return Arrays.asList("c_iflag", "c_oflag", "c_cflag", "c_lflag", "c_cc", "c_ispeed", "c_ospeed");
         }

         @Override
         public String toString() {
            return "Termios [" +
                "c_iflag=" + Long.toHexString(c_iflag.longValue()) +
               ",c_oflag=" + Long.toHexString(c_oflag.longValue()) +
               ",c_cflag=" + Long.toHexString(c_cflag.longValue()) +
               ",c_lflag=" + Long.toHexString(c_lflag.longValue()) +
               ",c_cc=" + Arrays.toString(c_cc) +
               ",c_ispeed=" + c_ispeed.longValue() +
               ",c_ospeed=" + c_ospeed.longValue() + "]";
         }
      }
   }
}


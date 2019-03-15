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
package com.iris.log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IrisStdioHijack {
   private final PrintStream oldOut;
   private final PrintStream oldErr;

   public IrisStdioHijack() {
      this.oldOut = System.out;
      this.oldErr = System.err;
   }

   public PrintStream getRealOutputStream() {
      return oldOut;
   }

   public PrintStream getRealErrorStream() {
      return oldErr;
   }

   public void start() {
      String layoutType = System.getenv("IRIS_LOGTYPE");
      if ("json".equalsIgnoreCase(layoutType) || "jsonstr".equalsIgnoreCase(layoutType)) {
         hijackStdio();
      }
   }

   public void stop() {
      String layoutType = System.getenv("IRIS_LOGTYPE");
      if ("json".equalsIgnoreCase(layoutType) || "jsonstr".equalsIgnoreCase(layoutType)) {
         unhijackStdio();
      }
   }

   private void hijackStdio() {
      Logger stdoutLogger = LoggerFactory.getLogger("stdout");
      Logger stderrLogger = LoggerFactory.getLogger("stderr");

      PrintStream newOut = new PrintStream(new StdioOutputStream(stdoutLogger));
      PrintStream newErr = new PrintStream(new StdioOutputStream(stderrLogger));

      System.setOut(newOut);
      System.setErr(newErr);
   }

   private void unhijackStdio() {
      System.setOut(oldOut);
      System.setErr(oldErr);
   }

   private static final class StdioOutputStream extends OutputStream {
      private final Logger logger;
      private final ByteArrayOutputStream baos;

      private StdioOutputStream(Logger logger) {
         this.logger = logger;
         this.baos = new ByteArrayOutputStream();
      }

      @Override
      public void write(int b) throws IOException {
         if (b == '\n') {
            flush();
         } else {
            baos.write(b);
         }
      }

      @Override
      public void write(byte[] b) throws IOException {
         write(b, 0, b.length);
      }

      @Override
      public void write(byte[] b, int off, int len) throws IOException {
         if (off < 0 || (len + off) > b.length) {
            throw new IndexOutOfBoundsException();
         }

         for (int i = off, e = off + len; i < e; ++i) {
            write(b[i]);
         }
      }

      @Override
      public void flush() throws IOException {
         String out = baos.toString();
         baos.reset();

         if (out != null && !out.isEmpty()) {
            logger.info(out);
         }
      }

      @Override
      public void close() throws IOException {
         try {
            flush();
         } catch (IOException ex) {
            // ignore
         }

         baos.close();
      }
   }
}


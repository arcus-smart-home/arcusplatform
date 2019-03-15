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
package com.iris.agent.storage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.iris.agent.test.SystemTestCase;

@Ignore
@RunWith(JUnit4.class)
public class TestStorageService extends SystemTestCase {
   @Test
   public void testFileContentMonitor() throws Exception {
      URI uri = StorageService.createTemp("test", ".tmp");
      File file = StorageService.getFile(uri);
      file.deleteOnExit();
      file.delete();

      List<String> test = new ArrayList<>();
      for (int i = 0; i < 5; ++i) {
         test.add("test" + i);
      }

      FileContentMonitor monitor = StorageService.getFileMonitor(uri);
      ContentListener listener = new ContentListener();
      monitor.addListener(listener);

      for (String next : test) {
         write(file, next);
         Thread.sleep(1000);
      }

      Assert.assertEquals("unexpected results: " + iterableDiffString(test, listener.getContents()), test, listener.getContents());
   }

   @Test
   @Ignore
   public void testFileWatch() throws Exception {
      URI uri = StorageService.createTemp("test", ".tmp");
      File file = StorageService.getFile(uri);
      file.deleteOnExit();
      file.delete();

      List<String> test = new ArrayList<>();
      for (int i = 0; i < 5; ++i) {
         test.add("test" + i);
      }

      List<MonitorEvent> expected = new ArrayList<>();
      expected.add(new MonitorEvent(EventType.FILE_CREATED, new FileData(file, test.get(0))));
      expected.add(new MonitorEvent(EventType.FILE_CHANGED, new FileData(file, test.get(1))));
      expected.add(new MonitorEvent(EventType.FILE_CHANGED, new FileData(file, test.get(2))));
      expected.add(new MonitorEvent(EventType.FILE_CHANGED, new FileData(file, test.get(3))));
      expected.add(new MonitorEvent(EventType.FILE_CHANGED, new FileData(file, test.get(4))));

      WatchHandle handle = StorageService.watch(uri);
      LoggingListener listener = new LoggingListener(true,false);
      handle.addWatchListener(listener);

      for (String next : test) {
         write(file, next);
         Thread.sleep(1000);
      }

      Assert.assertEquals("unexpected results: " + iterableDiffString(expected, listener.getContents()), expected, listener.getContents());
   }

   @Test
   @Ignore
   public void testDirectoryWatch() throws Exception {
      // This test is not working right now because some times the directory creation
      // events are delivered in unexpected order, though all of the events seem to
      // be delivered.
      URI uri = StorageService.createTemp("test", ".tmp");
      File file = StorageService.getFile(uri);
      file.delete();
      file.mkdirs();
      Thread.sleep(1000);

      File dir1 = new File(file, "test.dir1");
      File dir2 = new File(file, "test.dir2");
      File fl1 = new File(dir1, "test.fl1");

      List<MonitorEvent> expected = new ArrayList<>();
      //expected.add(new MonitorEvent(EventType.DIR_CREATED, file));
      expected.add(new MonitorEvent(EventType.DIR_CREATED, dir1)); // create dir1
      expected.add(new MonitorEvent(EventType.DIR_CHANGED, dir1)); // create
      expected.add(new MonitorEvent(EventType.DIR_CREATED, dir2)); // create dir2
      expected.add(new MonitorEvent(EventType.DIR_CHANGED, dir1)); // delete fl1
      expected.add(new MonitorEvent(EventType.DIR_DELETED, dir1)); // delete dir1
      expected.add(new MonitorEvent(EventType.DIR_DELETED, dir2)); // delete dir2

      WatchHandle handle = StorageService.watch(uri);
      LoggingListener listener = new LoggingListener(false,true);
      handle.addWatchListener(listener);

      try {
         dir1.mkdirs();
         Thread.sleep(1000);

         write(fl1, "data");
         Thread.sleep(1000);

         dir2.mkdirs();
         Thread.sleep(1000);

         fl1.delete();
         Thread.sleep(1000);

         FileUtils.deleteDirectory(dir1);
         Thread.sleep(1000);

         FileUtils.deleteDirectory(dir2);
         Thread.sleep(1000);
      } finally {
         FileUtils.deleteDirectory(file);
      }

      Assert.assertEquals("unexpected results:\n" + iterableDiffString(expected, listener.getContents()), expected, listener.getContents());
   }

   private static void write(File file, String contents) throws IOException {
      try (OutputStream os = new BufferedOutputStream(new FileOutputStream(file))) {
         IOUtils.write(contents, os, StandardCharsets.UTF_8);
      }
   }

   @Nullable
   private static String read(File file) {
      try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
         return IOUtils.toString(is, StandardCharsets.UTF_8);
      } catch (Exception ex) {
         return null;
      }
   }

   @Nullable
   private static String iterableDiffString(Iterable<?> it1, Iterable<?> it2) {
      StringBuilder result = new StringBuilder();

      Iterator<?> i1 = it1.iterator();
      Iterator<?> i2 = it2.iterator();

      int idx = 0;
      while (i1.hasNext() && i2.hasNext()) {
         Object o1 = i1.next();
         Object o2 = i2.next();
         if (o1 == o2) {
            continue;
         }

         if (o1 == null || !o1.equals(o2)) {
            result.append("at idx ").append(idx).append(":\n");
            result.append("   ").append(o1).append(" != ").append("\n");
            result.append("   ").append(o2).append("\n");
         }

         idx++;
      }

      while (i1.hasNext()) {
         Object o1 = i1.next();

         result.append("at idx ").append(idx).append(":\n");
         result.append("   ").append(o1).append(" != ").append("\n");
         result.append("   <none>\n");

         idx++;
      }

      while (i2.hasNext()) {
         Object o2 = i2.next();

         result.append("at idx ").append(idx).append(":\n");
         result.append("   <none> !=\n");
         result.append("   ").append(o2).append("\n");

         idx++;
      }

      result.append("\n");
      return result.toString();
   }

   private static final class ContentListener implements FileContentListener {
      private final List<String> contents = new ArrayList<>();

      public List<String> getContents() {
         return contents;
      }

      @Override
      public void fileContentsModified(FileContentMonitor monitor) {
         contents.add(monitor.getContents());
      }
   }

   private static final class LoggingListener implements WatchListener {
      private List<MonitorEvent> contents = new ArrayList<>();
      private final boolean fl;
      private final boolean dir;

      public LoggingListener(boolean fl, boolean dir) {
         this.fl = fl;
         this.dir = dir;
      }

      public List<MonitorEvent> getContents() {
         return contents;
      }

      @Override
      public void onDirectoryCreate(File directory) {
         if (dir) contents.add(new MonitorEvent(EventType.DIR_CREATED, directory));
      }

      @Override
      public void onDirectoryChange(File directory) {
         if (dir) contents.add(new MonitorEvent(EventType.DIR_CHANGED, directory));
      }

      @Override
      public void onDirectoryDelete(File directory) {
         if (dir) contents.add(new MonitorEvent(EventType.DIR_DELETED, directory));
      }

      @Override
      public void onFileCreate(File file) {
         if (fl) contents.add(new MonitorEvent(EventType.FILE_CREATED, new FileData(file,read(file))));
      }

      @Override
      public void onFileChange(File file) {
         if (fl) contents.add(new MonitorEvent(EventType.FILE_CHANGED, new FileData(file,read(file))));
      }

      @Override
      public void onFileDelete(File file) {
         if (fl) contents.add(new MonitorEvent(EventType.FILE_DELETED, new FileData(file,read(file))));
      }
   }

   private static enum EventType {
      DIR_CREATED,
      DIR_CHANGED,
      DIR_DELETED,
      FILE_CREATED,
      FILE_CHANGED,
      FILE_DELETED,
   }

   private static final class FileData {
      private final File file;

      @Nullable
      private final String contents;

      public FileData(File file, @Nullable String contents) {
         this.file = file;
         this.contents = contents;
      }

      @Override
      @SuppressWarnings("null")
      public int hashCode() {
         final int prime = 31;
         int result = 1;
         result = prime * result + ((contents == null) ? 0 : contents.hashCode());
         result = prime * result + ((file == null) ? 0 : file.hashCode());
         return result;
      }

      @Override
      @SuppressWarnings({ "null", "unused" })
      public boolean equals(Object obj) {
         if (this == obj)
            return true;
         if (obj == null)
            return false;
         if (getClass() != obj.getClass())
            return false;
         FileData other = (FileData) obj;
         if (contents == null) {
            if (other.contents != null)
               return false;
         } else if (!contents.equals(other.contents))
            return false;
         if (file == null) {
            if (other.file != null)
               return false;
         } else if (!file.equals(other.file))
            return false;
         return true;
      }

      @Override
      public String toString() {
         return "FileData [file=" + file + ", contents=" + contents + "]";
      }
   }

   private static final class MonitorEvent {
      private final EventType type;
      private final Object contents;

      public MonitorEvent(EventType type, Object contents) {
         this.type = type;
         this.contents = contents;
      }

      @Override
      @SuppressWarnings("null")
      public int hashCode() {
         final int prime = 31;
         int result = 1;
         result = prime * result + ((contents == null) ? 0 : contents.hashCode());
         result = prime * result + ((type == null) ? 0 : type.hashCode());
         return result;
      }

      @Override
      @SuppressWarnings({ "null", "unused" })
      public boolean equals(Object obj) {
         if (this == obj)
            return true;
         if (obj == null)
            return false;
         if (getClass() != obj.getClass())
            return false;
         MonitorEvent other = (MonitorEvent) obj;
         if (contents == null) {
            if (other.contents != null)
               return false;
         } else if (!contents.equals(other.contents))
            return false;
         if (type != other.type)
            return false;
         return true;
      }

      @Override
      public String toString() {
         return "MonitorEvent [type=" + type + ", contents=" + contents + "]";
      }
   }
}


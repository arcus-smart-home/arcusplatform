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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.Nullable;

import com.iris.agent.storage.impl.DefaultFileContentMonitor;
import com.iris.agent.storage.impl.DefaultWatchService;

public final class StorageService {
   private static final URI TMP = URI.create("tmp:///");
   private static final Object START_LOCK = new Object();

   @Nullable
   private static ConcurrentMap<String,File> rootMappingInternal;

   @Nullable
   private static DefaultWatchService watchInternal;

   private StorageService() {
   }

   public static void start(long watchInterval, TimeUnit unit) {
      synchronized (START_LOCK) {
         if (rootMappingInternal != null || watchInternal != null) {
            throw new IllegalStateException("storage service already started");
         }

         try {
            ConcurrentMap<String,File> rootMap = new ConcurrentHashMap<>();
            DefaultWatchService defWatch = new DefaultWatchService(TimeUnit.MILLISECONDS.convert(watchInterval, unit));

            rootMappingInternal = rootMap;
            watchInternal = defWatch;
         } catch (Exception ex) {
            throw new RuntimeException("could not startup storage service", ex);
         }
      }
   }

   public static void shutdown() {
      synchronized (START_LOCK) {
         rootMappingInternal = null;
         watchInternal = null;
      }
   }

   public static void addRootMapping(String protocol, String location) {
      String loc = location;
      if (loc.startsWith("file://")) {
         loc = loc.substring("file://".length());
      } else if (loc.startsWith("file:")) {
         loc = loc.substring("file:".length());
      }

      if (!loc.startsWith("/")) {
         throw new RuntimeException("invalid root path, must be absolute: " + location);
      }

      addRootMapping(protocol, new File(loc));
   }

   public static void addRootMapping(String protocol, File location) {
      String prot = protocol;
      if (prot.endsWith("://")) {
         prot = prot.substring(0, prot.length() - 3);
      }

      location.mkdirs();
      getRootMapping().putIfAbsent(prot, location);
   }

   public static URI createTemp(String prefix, String suffix) throws IOException {
      File parent = getFile(TMP);
      File result = File.createTempFile(prefix, suffix, parent);

      String path = result.getAbsolutePath();
      String parentPath = parent.getAbsolutePath();

      String uriPath = path.substring(parentPath.length());
      return URI.create("tmp://" + uriPath);
   }

   public static File createTempFile(String prefix, String suffix) throws IOException {
      File parent = getFile(TMP);
      return File.createTempFile(prefix, suffix, parent);
   }

   public static WatchHandle watch(URI uri) throws IOException {
      return getWatch().watch(getFile(uri));
   }

   public static WatchHandle watch(File file) throws IOException {
      return getWatch().watch(file);
   }

   public static FileContentMonitor getFileMonitor(String path) throws IOException {
       return getFileMonitor(URI.create(path));
   }
   
   
   public static FileContentMonitor getFileMonitor(URI uri) throws IOException {
      return getFileMonitor(getFile(uri));
   }

   public static FileContentMonitor getFileMonitor(File file) throws IOException {
      if (file.isDirectory()) {
         throw new IOException("file content monitor cannot monitor directories");
      }

      WatchHandle handle = watch(file);
      DefaultFileContentMonitor monitor = new DefaultFileContentMonitor(handle);
      monitor.setup(file);

      return monitor;
   }

   private static DefaultWatchService getWatch() {
      DefaultWatchService service = watchInternal;
      if (service == null) {
         throw new IllegalStateException("storage service not running");
      }

      return service;
   }

   private static ConcurrentMap<String,File> getRootMapping() {
      ConcurrentMap<String,File> rootMap = rootMappingInternal;
      if (rootMap == null) {
         throw new IllegalStateException("storage service not running");
      }

      return rootMap;
   }

   /////////////////////////////////////////////////////////////////////////////
   // Utility methods
   /////////////////////////////////////////////////////////////////////////////

   public static File getFile(String uri) {
      return getFile(URI.create(uri));
   }

   public static File getFile(URI uri) {
      return resolve(uri);
   }

   public static InputStream getInputStream(String uri) throws IOException {
      return getInputStream(URI.create(uri));
   }

   public static InputStream getInputStream(URI uri) throws IOException {
      return getInputStream(resolve(uri));
   }

   public static InputStream getInputStream(File file) throws IOException {
      return new FileInputStream(file);
   }

   public static OutputStream getOutputStream(String uri) throws IOException {
      return getOutputStream(URI.create(uri), false);
   }

   public static OutputStream getOutputStream(URI uri) throws IOException {
      return getOutputStream(uri, false);
   }

   public static OutputStream getOutputStream(String uri, boolean append) throws IOException {
      return getOutputStream(URI.create(uri), append);
   }

   public static OutputStream getOutputStream(URI uri, boolean append) throws IOException {
      return getOutputStream(resolve(uri), append);
   }

   public static OutputStream getOutputStream(File file) throws IOException {
      return getOutputStream(file, false);
   }

   public static OutputStream getOutputStream(File file, boolean append) throws IOException {
      return new FileOutputStream(file, append);
   }

   public static Reader getReader(String uri) throws IOException {
      return getReader(URI.create(uri));
   }

   public static Reader getReader(URI uri) throws IOException {
      return new InputStreamReader(getInputStream(uri));
   }

   public static Reader getReader(String uri, Charset cs) throws IOException {
      return getReader(URI.create(uri), cs);
   }

   public static Reader getReader(URI uri, Charset cs) throws IOException {
      return new InputStreamReader(getInputStream(uri), cs);
   }

   public static Reader getReader(String uri, CharsetDecoder decoder) throws IOException {
      return getReader(URI.create(uri), decoder);
   }

   public static Reader getReader(URI uri, CharsetDecoder decoder) throws IOException {
      return new InputStreamReader(getInputStream(uri), decoder);
   }

   public static Writer geteWriter(String uri) throws IOException {
      return getWriter(URI.create(uri));
   }

   public static Writer getWriter(URI uri) throws IOException {
      return new OutputStreamWriter(getOutputStream(uri));
   }

   public static Writer getWriter(String uri, Charset cs) throws IOException {
      return getWriter(URI.create(uri), cs);
   }

   public static Writer getWriter(URI uri, Charset cs) throws IOException {
      return new OutputStreamWriter(getOutputStream(uri), cs);
   }

   public static Writer getWriter(String uri, CharsetEncoder encoder) throws IOException {
      return getWriter(URI.create(uri), encoder);
   }

   public static Writer getWriter(URI uri, CharsetEncoder encoder) throws IOException {
      return new OutputStreamWriter(getOutputStream(uri), encoder);
   }

   /////////////////////////////////////////////////////////////////////////////
   // Implementation details
   /////////////////////////////////////////////////////////////////////////////

   /*
   private static FileObject resolveObject(URI uri) throws IOException {
      FileObject root = getVfs().get(uri.getScheme());
      if (root == null) {
         throw new FileNotFoundException(uri.toString());
      }

      FileObject file = root.resolveFile(uri.getPath());
      if (file == null) {
         throw new FileNotFoundException(uri.toString());
      }

      return file;
   }

   private static FileContent resolveContent(URI uri) throws IOException {
      FileObject file = resolveObject(uri);
      FileContent content = file.getContent();
      if (content == null) {
         throw new FileNotFoundException(uri.toString());
      }

      return content;
   }
   */

   private static File resolve(URI uri) {
      ConcurrentMap<String,File> rootMap = getRootMapping();
      File base = rootMap.get(uri.getScheme());
      if (base == null) {
         throw new RuntimeException("no root storage service mapping for: " + uri);
      }

      return new File(base, uri.getPath());
   }
}


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
package com.iris.bridge.server.http.impl.responder;

import static io.netty.handler.codec.http.HttpHeaders.Names.IF_MODIFIED_SINCE;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

import javax.activation.MimetypesFileTypeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;

import com.google.common.collect.ImmutableMap;
import com.iris.bridge.server.http.HttpException;
import com.iris.bridge.server.http.HttpSender;
import com.iris.bridge.server.http.Responder;

public class DirectoryResponder implements Responder {
   private final static Logger logger = LoggerFactory.getLogger(DirectoryResponder.class);
   private static final Pattern INSECURE_URI = Pattern.compile(".*[<>&\"].*");

   protected final String rootFilePath;
   protected final MimetypesFileTypeMap mimeTypes;
   protected final String urlRoot;
   protected final HttpSender httpSender;

   public DirectoryResponder(String rootFilePath, HttpSender httpSender, MimetypesFileTypeMap mimeTypes) {
      this(rootFilePath, null, httpSender, mimeTypes);
   }

   public DirectoryResponder(String rootFilePath, String urlRoot, HttpSender httpSender, MimetypesFileTypeMap mimeTypes) {
      this.rootFilePath = rootFilePath;
      this.urlRoot = urlRoot;
      this.mimeTypes = mimeTypes;
      this.httpSender = httpSender;
   }

   @Override
   public void sendResponse(FullHttpRequest req, ChannelHandlerContext ctx) throws Exception {
      String path = sanitizeUri(req.getUri());
      if (path == null) {
         throw new HttpException(HttpResponseStatus.FORBIDDEN.code());
      }
      File file = new File(path);

      if (file.isHidden() || !file.exists() || file.isDirectory()) {
         throw new HttpException(HttpResponseStatus.NOT_FOUND.code());
      }

      if (!file.isFile()) {
         throw new HttpException(HttpResponseStatus.FORBIDDEN.code());
      }

      String contentType = mimeTypes.getContentType(file.getPath());

      // Cache Validation
      String ifModifiedSince = req.headers().get(IF_MODIFIED_SINCE);
      if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
         SimpleDateFormat dateFormatter = new SimpleDateFormat(HttpSender.HTTP_DATE_FORMAT, Locale.US);
         Date ifModifiedSinceDate = dateFormatter.parse(ifModifiedSince);

         // Only compare up to the second because the datetime format we send to the client
         // does not have milliseconds
         long ifModifiedSinceDateSeconds = ifModifiedSinceDate.getTime() / 1000;
         long fileLastModifiedSeconds = file.lastModified() / 1000;
         if (ifModifiedSinceDateSeconds <= fileLastModifiedSeconds) {
            throw new HttpException(HttpResponseStatus.NOT_MODIFIED.code());
         }
      }

      httpSender.sendFile(file, ImmutableMap.of(HttpHeaders.Names.CONTENT_TYPE, contentType), HttpHeaders.isKeepAlive(req), ctx);
   }

   protected String sanitizeUri(String uri) {
      // Chop off the url root
      if (urlRoot != null && uri.startsWith(urlRoot)) {
         uri = uri.substring(urlRoot.length());
      }

      // Decode the path.
      try {
         uri = URLDecoder.decode(uri, "UTF-8");
      } catch (UnsupportedEncodingException e) {
         throw new Error(e);
      }

      if (uri.isEmpty() || uri.charAt(0) != '/') {
         return null;
      }

      if(uri.contains("?")) {
         uri = uri.substring(0, uri.indexOf('?'));
      }

      // Convert file separators.
      uri = uri.replace('/', File.separatorChar);

      // Simplistic dumb security check.
      // You will have to do something serious in the production environment.
      if (uri.contains(File.separator + '.') ||
            uri.contains('.' + File.separator) ||
            uri.charAt(0) == '.' || uri.charAt(uri.length() - 1) == '.' ||
            INSECURE_URI.matcher(uri).matches()) {
         return null;
      }

      // Convert to absolute path.
      String path = rootFilePath + uri;
      if (logger.isTraceEnabled()) {
         logger.trace("current dir is {}", Paths.get(".").toAbsolutePath().normalize().toString());
         logger.trace("path to current file is '{}'", path);
      }

      return path;
   }

}


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
package com.iris.video.previewupload.server.handlers;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType;

import java.net.URLDecoder;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Timer;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.bridge.server.http.Responder;
import com.iris.bridge.server.http.annotation.HttpPost;
import com.iris.bridge.server.http.impl.RequestHandlerImpl;
import com.iris.bridge.server.http.impl.auth.AlwaysAllow;
import com.iris.core.dao.DeviceDAO;
import com.iris.messages.model.Device;
import com.iris.video.storage.PreviewStorage;

import static com.iris.video.previewupload.server.VideoPreviewUploadMetrics.*;

@Singleton
@HttpPost("/upload*")
public class UploadHandler extends RequestHandlerImpl {
   private static final Logger logger = LoggerFactory.getLogger(UploadHandler.class);

   @Inject
   public UploadHandler(AlwaysAllow auth, PreviewStorage storage, DeviceDAO devDao) {
      super(auth, new UploadHandlerResponder(storage, devDao));
   }

   private static class UploadHandlerResponder implements Responder {
      private final PreviewStorage storage;
      private final DeviceDAO devDao;

      private UploadHandlerResponder(PreviewStorage storage, DeviceDAO devDao) {
         this.storage = storage;
         this.devDao = devDao;
      }

      @Override
      public void sendResponse(FullHttpRequest req, ChannelHandlerContext ctx) throws Exception {
         long startTime = System.nanoTime();

         HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(req);
         try {
            String place = null;
            int num = 0;
            while(decoder.hasNext()) {
               num++;

               InterfaceHttpData httpData = decoder.next();
               if(httpData.getHttpDataType() == HttpDataType.Attribute && httpData.getName().equalsIgnoreCase("place")) {
                  place = ((Attribute) httpData).getValue();
               } else if(httpData.getHttpDataType() == HttpDataType.FileUpload) {
                  String camProtAddr = URLDecoder.decode(httpData.getName(), "UTF-8");
                  Device d = findCamera(camProtAddr);
                  if(d == null) {
                     UPLOAD_UNKNOWN.inc();
                     logger.warn("ignoring preview upload for non-existent camera {}", camProtAddr);
                     continue;
                  }
                  write(place, d, (FileUpload) httpData);
               }
            }
            HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            ChannelFuture future = ctx.writeAndFlush(response);
            if(!HttpHeaders.isKeepAlive(req)) {
               future.addListener(ChannelFutureListener.CLOSE);
            }

            UPLOAD_NUM.update(num);
            UPLOAD_SUCCESS.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
         } catch (Exception ex) {
            UPLOAD_FAIL.update(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
         } finally {
            decoder.cleanFiles();
         }
      }

      private void write(String placeId, Device d, FileUpload file) throws Exception {
         if (file.isCompleted()) {
            byte[] imageData = file.get();
            // TODO:  we should add check to make sure the jpg isn't corrupt
            if(imageData != null && imageData.length > 0) {
               UPLOAD_SIZE.update(imageData.length);
               try (Timer.Context context = UPLOAD_WRITE.time()) {
                  storage.write(d.getId().toString(), imageData);
               }
            } else {
               UPLOAD_EMPTY.inc();
               logger.info("got an upload for camera {} at {} with a null or zero-length file", d.getId(), placeId);
            }
         } else {
            UPLOAD_INCOMPLETE.inc();
            logger.warn("got an incomplete upload for camera {}", d.getId());;
         }
      }

      private Device findCamera(String protAddr) {
         return devDao.findByProtocolAddress(protAddr);
      }
   }
}


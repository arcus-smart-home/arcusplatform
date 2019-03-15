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
package com.iris.media;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.rtsp.RtspHeaders;

public final class IrisRtspSdp {
   private static final Logger log = LoggerFactory.getLogger(IrisRtspSdp.class);

   // Reference: https://tools.ietf.org/html/rfc4566
   // Reference: http://en.wikipedia.org/wiki/Session_Description_Protocol
   //
   // Session Description (mandatory)
   //    v=  (protocol version number, currently only 0)
   //    o=  (originator and session identifier : username, id, version number, network address)
   //    s=  (session name : mandatory with at least one UTF-8-encoded character)
   //    i=* (session title or short information)
   //    u=* (URI of description)
   //    e=* (zero or more email address with optional name of contacts)
   //    p=* (zero or more phone number with optional name of contacts)
   //    c=* (connection information not required if included in all media)
   //    b=* (zero or more bandwidth information lines)
   //    One or more Time descriptions ("t=" and "r=" lines; see below)
   //    z=* (time zone adjustments)
   //    k=* (encryption key)
   //    a=* (zero or more session attribute lines)
   //    Zero or more Media descriptions (each one starting by an "m=" line; see below)
   //
   // Time description (mandatory)
   //    t=  (time the session is active)
   //    r=* (zero or more repeat times)
   //
   // Media description (optional)
   //    m=  (media name and transport address)
   //    i=* (media title or information field)
   //    c=* (connection information optional if included at session level)
   //    b=* (zero or more bandwidth information lines)
   //    k=* (encryption key)
   //    a=* (zero or more media attribute lines overriding the Session attribute lines)
   //
   // Attributes take two forms:
   //    a=flag (boolean property)
   //    a=attribute:value (named parameter)
   private final Map<Character,Object> data;
   private final Map<String,Object> attrs;
   private final List<Media> media;

   private IrisRtspSdp(Map<Character,Object> data, Map<String,Object> attrs, List<Media> media) {
      this.data = data;
      this.attrs = attrs;
      this.media = media;
   }

   public <T> T get(Character ch, Class<T> type) {
      return (T)data.get(ch);
   }

   public <T> T getAttr(String key, Class<T> type) {
      return (T)attrs.get(key);
   }

   public Collection<Media> getMedia() {
      return Collections.unmodifiableList(media);
   }

   public static IrisRtspSdp parse(FullHttpResponse rsp) {
      try {
         return parseByteBuf(rsp.content(), getContentCharset(rsp));
      } catch (Exception ex) {
         throw new RuntimeException(ex);
      }
   }

   public static IrisRtspSdp parse(String data) {
      try {
         return parseStr(data);
      } catch (Exception ex) {
         throw new RuntimeException(ex);
      }
   }

   @Override
   public String toString() {
      StringBuilder bld = new StringBuilder();
      bld.append("sdp [");

      boolean first = true;
      for (Map.Entry<Character,Object> entry : data.entrySet()) {
         if (!first) bld.append(", ");
         bld.append(entry.getKey());
         bld.append("=");
         bld.append(entry.getValue());

         first = false;
      }

      if (!first) bld.append(", ");
      bld.append("attrs=").append(attrs);
      bld.append(",media=").append(media);

      bld.append("]");
      return bld.toString();
   }

   /////////////////////////////////////////////////////////////////////////////
   /////////////////////////////////////////////////////////////////////////////

   public static IrisRtspSdp parseByteBuf(ByteBuf content, Charset cs) throws IOException {
      try (ByteBufInputStream bbis = new ByteBufInputStream(content);
           BufferedReader reader = new BufferedReader(new InputStreamReader(bbis))) {
         return parse(reader);
      }
   }

   private static IrisRtspSdp parseStr(String str) throws IOException {
      try (BufferedReader reader = new BufferedReader(new StringReader(str))) {
         return parse(reader);
      }
   }

   private static IrisRtspSdp parse(BufferedReader reader) throws IOException {
      Map<Character,Object> result = new LinkedHashMap<>();
      Map<String,Object> attrs = new LinkedHashMap<>();
      List<Media> media = new ArrayList<>();

      String next = reader.readLine();
      while (next != null) {
         if (next.length() <= 2 || next.charAt(1) != '=') {
            continue;
         }

         char name = next.charAt(0);
         String value = next.substring(2);

         try {
            switch (name) {
            case 'm': parseMedia(value, media); break;
            case 'b': parseBandwidth(value, media); break;
            case 'a': parseAttribute(value, attrs, media); break;
            default: result.put(name, value); break;
            }
         } catch (Exception ex) {
            log.warn("error parsing sdp: {}", ex.getMessage(), ex);
         }


         next = reader.readLine();
      }

      return new IrisRtspSdp(result, attrs, media);
   }

   private static void parseAttribute(String value, Map<String,Object> attrs, List<Media> media) {
      try {
         int idx = value.indexOf(':');
         String key = value.substring(0, idx).trim().toLowerCase();
         String val = value.substring(idx+1).trim();
         switch(key) {
         case "rtpmap": parseRtpMap(val, media); break;
         case "framerate": parseFrameRate(val, media); break;
         case "x-framerate": parseFrameRate(val, media); break;
         case "fmtp": parseFormatParameters(val, media); break;

         default:
            if (media.isEmpty()) {
               attrs.put(key, val);
            } else {
               log.trace("ignoring unknown attribute on media: {}", value);
            }
            break;
         }
      } catch (Exception ex) {
      }
   }

   private static void parseMedia(String value, List<Media> media) {
      String[] parts = value.split("\\s+");
      if (parts == null || parts.length < 4) {
         throw new RuntimeException("unknown format for media descriptor: " + value);
      }

      Media md = new Media(parts[0], parts[1], parts[2], parts[3]);
      log.trace("parsed media descriptor: {}", md);

      media.add(md);
   }

   private static void parseBandwidth(String value, List<Media> media) {
      if (media.isEmpty()) {
         log.warn("bandwidth attribute without preceding media attribute: {}", value);
         return;
      }

      Media md = media.get(media.size() - 1);

      int idx = value.indexOf(':');
      String type = value.substring(0, idx).trim().toLowerCase();
      String band = value.substring(idx+1).trim();
      md.setBandwidth(type, Integer.valueOf(band));
   }

   private static void parseRtpMap(String value, List<Media> media) {
      String[] formatValue = value.split("\\s+");
      String format = formatValue[0];
      String fvalue = formatValue[1];

      String[] encoding = fvalue.split("/");
      String encodingName = encoding[0];
      String encodingClock = encoding[1];

      for (Media md : media) {
         if (md.format.equals(format)) {
            md.setEncodingInfo(encodingName, Integer.valueOf(encodingClock));
         }
      }
   }

   private static void parseFrameRate(String value, List<Media> media) {
      if (media.isEmpty()) {
         log.warn("frame rate attribute without preceding media attribute: {}", value);
         return;
      }

      Media md = media.get(media.size() - 1);
      md.setFrameRate(Double.valueOf(value));
   }

   private static void parseFormatParameters(String value, List<Media> media) {
      int idx = value.indexOf(" ");
      String format = value.substring(0, idx);
      String paramString = value.substring(idx + 1);

      String[] params = paramString.split(";");

      Builder<String> fvalue = ImmutableList.builder();
      for(int i = 1; i < params.length; i++) {
         fvalue.add(params[i].trim());
      }

      for (Media md : media) {
         if (md.format.equals(format)) {
            md.setFormatParameters(fvalue.build());
         }
      }
   }

   private static Charset getContentCharset(HttpResponse rsp) {
      return getContentCharset(rsp.headers().get(RtspHeaders.Names.CONTENT_ENCODING));
   }

   private static Charset getContentCharset(HttpRequest req) {
      return getContentCharset(req.headers().get(RtspHeaders.Names.CONTENT_ENCODING));
   }

   private static Charset getContentCharset(String cs) {
      if (cs == null || cs.isEmpty()) {
         return StandardCharsets.UTF_8;
      }

      try {
         return Charset.forName(cs);
      } catch (Exception ex) {
         return StandardCharsets.UTF_8;
      }
   }

   public static final class Media {
      private final String type;
      private final String port;
      private final String protocol;
      private final String format;

      private String bandwidthType = "";
      private int bandwidth = -1;

      private String encodingName = "";
      private int encodingClockRate = -1;

      private double frameRate = -1.0;

      private int width = -1;
      private int height = -1;

      private List<String> formatParameters = ImmutableList.of();

      public Media(String type, String port, String protocol, String format) {
         this.type = type;
         this.port = port;
         this.protocol = protocol;
         this.format = format;
      }

      public String getType() {
         return type;
      }

      public boolean isVideo() {
         return "video".equals(type);
      }

      public String getPort() {
         return port;
      }

      public String getProtocol() {
         return protocol;
      }

      public String getFormat() {
         return format;
      }

      public boolean hasBandwidth() {
         return !bandwidthType.isEmpty() && bandwidth >= 0;
      }

      public String getBandwithType() {
         return bandwidthType;
      }

      public int getBandwith() {
         return bandwidth;
      }

      public boolean hasEncodingInfo() {
         return !encodingName.isEmpty() && encodingClockRate >= 0;
      }

      public String getEncodingName() {
         return encodingName;
      }

      public int getEncodingClockRate() {
         return encodingClockRate;
      }

      public boolean hasFrameRate() {
         return frameRate >= 0;
      }

      public double getFrameRate() {
         return frameRate;
      }

      public boolean hasResolution() {
         return !(width <= 0 || height <= 0);
      }

      public int getWidth() {
         return width;
      }

      public int getHeight() {
         return height;
      }

      public void setResolution(int width, int height) {
         this.width = width;
         this.height = height;
      }

      public boolean hasFormatParameters() {
         return !formatParameters.isEmpty();
      }

      public List<String> getFormatParameters() {
         return formatParameters;
      }

      private void setBandwidth(String bandwidthType, int bandwidth) {
         this.bandwidthType = bandwidthType;
         this.bandwidth = bandwidth;
      }

      private void setEncodingInfo(String encodingName, int encodingClockRate) {
         this.encodingName = encodingName;
         this.encodingClockRate = encodingClockRate;
      }

      public void setFrameRate(double frameRate) {
         this.frameRate = frameRate;
      }

      private void setFormatParameters(List<String> formatParameters) {
         this.formatParameters = formatParameters;
      }

      @Override
      public String toString() {
         StringBuilder bld = new StringBuilder();

         bld.append("sdp media descriptor [")
            .append("type=").append(type)
            .append(",port=").append(port)
            .append(",protocol=").append(protocol)
            .append(",format=").append(format);

         if (hasBandwidth()) {
            bld.append(",bwtype=").append(bandwidthType);
            bld.append(",bw=").append(bandwidth);
         }

         if (hasEncodingInfo()) {
            bld.append(",encname=").append(encodingName);
            bld.append(",encclk=").append(encodingClockRate);
         }

         if (hasFrameRate()) {
            bld.append(",framerate=").append(frameRate);
         }

         if (hasResolution()) {
            bld.append(",width=").append(width);
            bld.append(",height=").append(height);
         }

         if (hasFormatParameters()) {
            bld.append("fmtp=").append(formatParameters);
         }

         bld.append("]");
         return bld.toString();
      }
   }
}


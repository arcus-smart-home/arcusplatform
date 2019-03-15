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
package com.iris.agent.upnp;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.iris.agent.http.HttpServer;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ChannelFactory;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.InternetProtocolFamily;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.util.concurrent.GenericFutureListener;

final class IrisUpnpServer {
   private static final Logger log = LoggerFactory.getLogger(IrisUpnpServer.class);
   private static final Splitter usnSplitter = Splitter.on(':').trimResults();

   private static final long MINIMUM_CACHE_CONTROL = TimeUnit.NANOSECONDS.convert(5, TimeUnit.MINUTES);
   private static final long DEFAULT_CACHE_CONTROL = TimeUnit.NANOSECONDS.convert(60, TimeUnit.MINUTES);

   private final UpnpHandler handler = new UpnpHandler();
   private final UpnpInitializer initializer = new UpnpInitializer();
   private final String inet;
   private final NetworkInterface nif;
   private final String addr;
   private final String localUuid;
   private boolean isShutdown;

   IrisUpnpServer(String inet) throws SocketException {
      this.inet = inet;
      this.nif = NetworkInterface.getByName(inet);

      InetAddress best = null;
      Enumeration<InetAddress> addrs = nif.getInetAddresses();
      while (addrs.hasMoreElements()) {
         InetAddress addr = addrs.nextElement();
         if (best == null) {
            best = addr;
         } else if (addr instanceof Inet4Address) {
            best = addr;
         }
      }

      this.addr = best.getHostAddress();
      this.localUuid = "uuid:" + IrisUpnpService.uuid;
   }

   void start(EventLoopGroup evl) {
      try {
         InetAddress mcast = InetAddress.getByName("239.255.255.250");
         InetSocketAddress local = new InetSocketAddress(1900);
         final InetSocketAddress remote = new InetSocketAddress(mcast, 1900);

         Bootstrap bootstrap = new Bootstrap()
            .group(evl)
            .handler(initializer)
            .channelFactory(new ChannelFactory<NioDatagramChannel>() {
               @Override
               public NioDatagramChannel newChannel() {
                  return new NioDatagramChannel(InternetProtocolFamily.IPv4);
               }
            })
            .option(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator.DEFAULT)
            .option(ChannelOption.SO_BROADCAST, true)
            .option(ChannelOption.SO_REUSEADDR, true)
            // Need a fairly large buffer to handle all the UPnP messages in large networks
            .option(ChannelOption.SO_RCVBUF, 128 * 1024)
            .option(ChannelOption.IP_MULTICAST_TTL, 255)
            .option(ChannelOption.IP_MULTICAST_IF, nif);

         bootstrap.bind(local).addListener(new GenericFutureListener<ChannelFuture>() {
            @Override
            public void operationComplete(@Nullable ChannelFuture future) throws Exception {
               try {
                  future.get();
                  final DatagramChannel channel = (DatagramChannel)future.channel();
                  channel.joinGroup(remote, nif).addListener(new GenericFutureListener<ChannelFuture>() {
                     @Override
                     public void operationComplete(@Nullable ChannelFuture future) throws Exception {
                        try {
                           future.get();
                           log.info("upnp on interface {} now active", inet);
                        } catch (Exception ex) {
                           IrisUpnpService.restartAfterDelay(1, TimeUnit.MINUTES);
                        }
                     }
                  });
               } catch (Exception ex) {
                  IrisUpnpService.restartAfterDelay(1, TimeUnit.MINUTES);
               }
            }
         });
      } catch (Throwable t) {
         log.error("upnp server failed to start:", t);
         IrisUpnpService.restartAfterDelay(1, TimeUnit.MINUTES);
      }
   }

   void shutdown() {
      isShutdown = true;
      ChannelHandlerContext ctx = handler.ctx;
      if (ctx != null) {
         ctx.close();
      }
   }

   void search() {
      handler.msearch();
   }

   void alive(String uuid) {
      handler.alive(uuid);
   }

   private final class UpnpInitializer extends ChannelInitializer<DatagramChannel> {
      @Override
      protected void initChannel(@Nullable DatagramChannel ch) throws Exception {
         Preconditions.checkNotNull(ch);
         ch.pipeline().addLast(handler);
      }
   }

   private final class UpnpHandler extends ChannelDuplexHandler {
      final InetAddress mcast;
      final InetSocketAddress remote;
      UniqueServiceName usn;
      ChannelHandlerContext ctx;

      @SuppressWarnings("null")
      UpnpHandler() {
         try {
            this.mcast = InetAddress.getByName("239.255.255.250");
            this.remote = new InetSocketAddress(mcast, 1900);
            this.usn = new UniqueServiceName();
         } catch (Exception ex) {
            throw new RuntimeException(ex);
         }
      }

      @Override
      public void channelActive(@Nullable ChannelHandlerContext ctx) throws Exception {
         Preconditions.checkNotNull(ctx);
         this.ctx = ctx;
      }

      @Override
      public void channelInactive(@Nullable ChannelHandlerContext ctx) throws Exception {
         if (!isShutdown) {
            IrisUpnpService.restartAfterDelay(1, TimeUnit.SECONDS);
         }
      }

      @Override
      public void channelRead(@Nullable ChannelHandlerContext ctx, @Nullable Object msg) throws Exception {
         DatagramPacket packet = (DatagramPacket)msg;
         if (packet == null) {
            return;
         }

         try {
            ByteBuf content = packet.content();
            if (log.isTraceEnabled()) {
               log.trace("recv upnp message: {}", content.toString(StandardCharsets.UTF_8));
            }

            if (content.readableBytes() > 5 &&
                content.getByte(content.readerIndex()) == 'H' &&
                content.getByte(content.readerIndex()+1) == 'T' &&
                content.getByte(content.readerIndex()+2) == 'T' &&
                content.getByte(content.readerIndex()+3) == 'P' &&
                content.getByte(content.readerIndex()+4) == '/') {
               handleResponse(ctx, packet);
            } else {
               handleRequest(ctx, packet);
            }
         } catch (Throwable th) {
            log.debug("error processing upnp packet:", th);
         } finally {
            if (packet.refCnt() > 0) {
               packet.release(packet.refCnt());
            }
         }
      }

      private void handleRequest(@Nullable ChannelHandlerContext ctx, DatagramPacket packet) throws Exception {
         EmbeddedChannel http = new EmbeddedChannel(new HttpRequestDecoder());

         try {
            http.writeInbound(Unpooled.unreleasableBuffer(packet.content()));
            http.finish();

            while (true) {
               Object result = http.readInbound();
               if (result == null) {
                  break;
               }

               if (result instanceof HttpRequest) {
                  HttpRequest req = (HttpRequest)result;
                  switch (req.getMethod().name()) {
                  case "NOTIFY": handleUpnpNotify(packet, req); break;
                  case "M-SEARCH": handleUpnpMsearch(packet, req); break;
                  default: log.debug("unknown upnp message: {}", req.getMethod()); break;
                  }
               } 
            }
         } finally {
            http.finishAndReleaseAll();
         }
      }

      private void handleResponse(@Nullable ChannelHandlerContext ctx, DatagramPacket packet) throws Exception {
         EmbeddedChannel http = new EmbeddedChannel(new HttpResponseDecoder());

         try {
            http.writeInbound(Unpooled.unreleasableBuffer(packet.content()));
            http.finish();

            while (true) {
               Object result = http.readInbound();
               if (result == null) {
                  break;
               }

               if (result instanceof HttpResponse) {
                  HttpResponse res = (HttpResponse)result;
                  switch (res.getStatus().code()) {
                  case 200: handleUpnpMsearchResponse(packet, res); break;
                  default: log.debug("unknown upnp response: {}", res.getStatus().code()); break;
                  }
               } 
            }
         } finally {
            http.finishAndReleaseAll();
         }
      }

      private void handleUpnpMsearch(DatagramPacket packet, HttpRequest request) {
         HttpHeaders headers = request.headers();
         String man = headers.get("MAN");
         // MAN must contain ssdp:discover, with or without quotes
         if (!"\"ssdp:discover\"".equals(man) && !"ssdp:discover".equals(man)) {
            log.trace("bad msearch MAN: {}", request);
            return;
         }

         try {
            int mx;
            // If there is no MX value, assume a default of 10 rather than dropping packet
            try {
               String mxVal = headers.get("MX");
               if (mxVal == null) {
                  mx = 10;
               } else {
                  mx = Integer.parseInt(mxVal);
               }
            } catch (NumberFormatException ex) {
               log.info("bad msearch MX: {}", request);
               return;
            }
            if (mx < 0) mx = 0;
            if (mx > 120) mx = 120;

            String st = headers.get("ST");
            if (st == null) {
               log.trace("bad msearch ST: {}", request);
               return;
            }

            int delay = ThreadLocalRandom.current().nextInt(0, mx+1);
            if (st.equals(localUuid)) {
               ctx.channel().eventLoop().schedule(new Responder(packet.sender()) {
                  @Override
                  public ChannelFuture send(Channel ch, InetSocketAddress to, String date) {
                     log.trace("sending msearch response for local device: {}", to);
                     respondDevice(to, ch, date);
                     return respondDevice(to, ch, date);
                  }
               }, delay, TimeUnit.SECONDS);
               return;
            }

            switch (st) {
            case "ssdp:all":
               ctx.channel().eventLoop().schedule(new Responder(packet.sender()) {
                  @Override
                  public ChannelFuture send(Channel ch, InetSocketAddress to, String date) {
                     log.trace("sending msearch response for all devices: {}", to);
                     respondRootDevice(to, ch, date);
                     respondRootDevice(to, ch, date);

                     respondDevice(to, ch, date);
                     respondDevice(to, ch, date);

                     respondBasicDevice(to, ch, date);
                     return respondBasicDevice(to, ch, date);
                  }
               }, delay, TimeUnit.SECONDS);
               break;
            case "upnp:rootdevice":
               ctx.channel().eventLoop().schedule(new Responder(packet.sender()) {
                  @Override
                  public ChannelFuture send(Channel ch, InetSocketAddress to, String date) {
                     log.trace("sending msearch response for root device: {}", to);
                     respondRootDevice(to, ch, date);
                     return respondRootDevice(to, ch, date);
                  }
               }, delay, TimeUnit.SECONDS);
               break;
            case "urn:schemas-upnp-org:device:Basic:1":
               ctx.channel().eventLoop().schedule(new Responder(packet.sender()) {
                  @Override
                  public ChannelFuture send(Channel ch, InetSocketAddress to, String date) {
                     log.trace("sending msearch response for basic device: {}", to);
                     respondBasicDevice(to, ch, date);
                     return respondBasicDevice(to, ch, date);
                  }
               }, delay, TimeUnit.SECONDS);
               break;
            default:
               // ignore
               break;
            }
         } catch (Exception ex) {
            log.info("upnp failed to process msearch:", ex);
         }
      }

      private void handleUpnpNotify(DatagramPacket packet, HttpRequest request) {
         HttpHeaders headers = request.headers();
         if (!parseUsnHeader(headers)) {
            log.trace("dropping upnp m-search response with bad usn");
            return;
         }

         String url = headers.get("LOCATION");
         String nts = headers.get("NTS");
         nts = (nts == null) ? "ssdp:alive" : nts;
         long maxAge = parseCacheControlHeader(headers);

         if (log.isTraceEnabled()) {
            log.trace("upnp notify: cache={}s, sender={}, uuid={}, class={}, namespace={}, type={}, version={}, nts={}",
               TimeUnit.SECONDS.convert(maxAge,TimeUnit.NANOSECONDS),
               packet.sender(),
               usn.deviceUuid,
               usn.clazz,
               usn.namespace,
               usn.type,
               usn.version,
               nts);
         }

         switch (nts.toLowerCase()) {
         case "ssdp:alive":
            IrisUpnpService.poke(packet.sender(), usn, maxAge, url, headers);
            break;

         case "ssdp:byebye":
            // Ignore bye-bye messages except from cameras which need to keep track of this info
            if (IrisUpnpService.upnpCameraDeviceTypeWhiteList.contains(usn.type)) {
               IrisUpnpService.remove(usn.deviceUuid);
            } else {
               log.trace("upnp ignoring ssdp:byebye sent by device: {}", usn.deviceUuid);
            }
            break;

         default:
            log.warn("upnp ignoring unknown notify ssdp type: {}", nts);
            break;
         }
      }

      private void handleUpnpMsearchResponse(DatagramPacket packet, HttpResponse response) {
         HttpHeaders headers = response.headers();
         if (!parseUsnHeader(headers)) {
            log.trace("dropping upnp m-search response with bad usn");
            return;
         }

         String url = headers.get("LOCATION");
         long maxAge = parseCacheControlHeader(headers);

         if (log.isTraceEnabled()) {
            log.trace("upnp msearch response: cache={}s, sender={}, uuid={}, class={}, namespace={}, type={}, version={}\n{}",
               TimeUnit.SECONDS.convert(maxAge,TimeUnit.NANOSECONDS),
               packet.sender(),
               usn.deviceUuid,
               usn.clazz,
               usn.namespace,
               usn.type,
               usn.version, response);
         }

         IrisUpnpService.poke(packet.sender(), usn, maxAge, url, headers);
      }

      private void msearch() {
         ByteBuf data = Unpooled.buffer();
         ByteBufUtil.writeUtf8(data, 
            "M-SEARCH * HTTP/1.1\r\n" +
            "HOST: 239.255.255.250:1900\r\n" +
            "MAN: \"ssdp:discover\"\r\n" +
            "MX: 10\r\n" +
            "ST: ssdp:all\r\n" + 
            "USER-AGENT: Iris OS/2.0 UPnP/1.1 Iris/2.0\r\n\r\n"
         );

         log.trace("sending upnp msearch on {}", inet);
         ctx.writeAndFlush(new DatagramPacket(data, remote));
      }

      private ChannelFuture respondRootDevice(InetSocketAddress to, Channel ch, String date) {
         ByteBuf data = Unpooled.buffer();
         ByteBufUtil.writeUtf8(data, 
            "HTTP/1.1 200 OK\r\n" +
            "CACHE-CONTROL: max-age=1800\r\n" +
            "DATE: " + date + "\r\n" +
            "EXT:\r\n" +
            "LOCATION: http://" + addr + ":" + HttpServer.PORT + "/upnp/device.xml\r\n" +
            "ST: upnp:rootdevice\r\n" +
            "USN: uuid:" + IrisUpnpService.uuid + "::upnp:rootdevice\r\n" +
            "SERVER: Iris OS/2.0 UPnP/1.0 Iris/2.0\r\n\r\n"
         );

         return ch.writeAndFlush(new DatagramPacket(data,to));
      }

      private ChannelFuture respondDevice(InetSocketAddress to, Channel ch, String date) {
         ByteBuf data = Unpooled.buffer();
         ByteBufUtil.writeUtf8(data, 
            "HTTP/1.1 200 OK\r\n" +
            "CACHE-CONTROL: max-age=1800\r\n" +
            "DATE: " + date + "\r\n" +
            "EXT:\r\n" +
            "LOCATION: http://" + addr + ":" + HttpServer.PORT + "/upnp/device.xml\r\n" +
            "ST: uuid:" + IrisUpnpService.uuid + "\r\n" +
            "USN: uuid:" + IrisUpnpService.uuid + "\r\n" +
            "SERVER: Iris OS/2.0 UPnP/1.0 Iris/2.0\r\n\r\n"
         );

         return ch.writeAndFlush(new DatagramPacket(data,to));
      }

      private ChannelFuture respondBasicDevice(InetSocketAddress to, Channel ch, String date) {
         ByteBuf data = Unpooled.buffer();
         ByteBufUtil.writeUtf8(data, 
            "HTTP/1.1 200 OK\r\n" +
            "CACHE-CONTROL: max-age=1800\r\n" +
            "DATE: " + date + "\r\n" +
            "EXT:\r\n" +
            "LOCATION: http://" + addr + ":" + HttpServer.PORT + "/upnp/device.xml\r\n" +
            "ST: urn:schemas-upnp-org:device:Basic:1\r\n" +
            "USN: uuid:" + IrisUpnpService.uuid + "::urn:schemas-upnp-org:device:Basic:1\r\n" +
            "SERVER: Iris OS/2.0 UPnP/1.0 Iris/2.0\r\n\r\n"
         );

         return ch.writeAndFlush(new DatagramPacket(data,to));
      }

      private void aliveRootDevice(String uuid) {
         ByteBuf data = Unpooled.buffer();
         ByteBufUtil.writeUtf8(data, 
            "NOTIFY * HTTP/1.1\r\n" +
            "USN: uuid:" + uuid + "::upnp:rootdevice\r\n" +
            "NT: upnp:rootdevice\r\n" +
            "CACHE-CONTROL: max-age=1800\r\n" +
            "HOST: 239.255.255.250:1900\r\n" +
            "LOCATION: http://" + addr + ":" + HttpServer.PORT + "/upnp/device.xml\r\n" +
            "NTS: ssdp:alive\r\n" +
            "SERVER: Iris OS/2.0 UPnP/1.0 Iris/2.0\r\n\r\n"
         );

         log.trace("sending upnp alive notify on {}", inet);
         ctx.writeAndFlush(new DatagramPacket(data, remote));
      }

      private void aliveDevice(String uuid) {
         ByteBuf data = Unpooled.buffer();
         ByteBufUtil.writeUtf8(data, 
            "NOTIFY * HTTP/1.1\r\n" +
            "USN: uuid:" + uuid + "\r\n" +
            "NT: uuid:" + uuid + "\r\n" +
            "CACHE-CONTROL: max-age=1800\r\n" +
            "HOST: 239.255.255.250:1900\r\n" +
            "LOCATION: http://" + addr + ":" + HttpServer.PORT + "/upnp/device.xml\r\n" +
            "NTS: ssdp:alive\r\n" +
            "SERVER: Iris OS/2.0 UPnP/1.0 Iris/2.0\r\n\r\n"
         );

         log.trace("sending upnp alive notify on {}", inet);
         ctx.writeAndFlush(new DatagramPacket(data, remote));
      }

      private void aliveBasicDevice(String uuid) {
         ByteBuf data = Unpooled.buffer();
         ByteBufUtil.writeUtf8(data, 
            "NOTIFY * HTTP/1.1\r\n" +
            "USN: uuid:" + uuid + "::urn:schemas-upnp-org:device:Basic:1\r\n" +
            "NT: urn:schemas-upnp-org:device:Basic:1\r\n" +
            "CACHE-CONTROL: max-age=1800\r\n" +
            "HOST: 239.255.255.250:1900\r\n" +
            "LOCATION: http://" + addr + ":" + HttpServer.PORT + "/upnp/device.xml\r\n" +
            "NTS: ssdp:alive\r\n" +
            "SERVER: Iris OS/2.0 UPnP/1.0 Iris/2.0\r\n\r\n"
         );

         log.trace("sending upnp alive notify on {}", inet);
         ctx.writeAndFlush(new DatagramPacket(data, remote));
      }

      private void alive(String uuid) {
         aliveRootDevice(uuid);
         aliveRootDevice(uuid);

         aliveDevice(uuid);
         aliveDevice(uuid);

         aliveBasicDevice(uuid);
         aliveBasicDevice(uuid);
      }

      private long parseCacheControlHeader(HttpHeaders headers) {
         String cache = headers.get(HttpHeaders.Names.CACHE_CONTROL);
         if (cache == null) {
            log.trace("cache control header missing");
            return DEFAULT_CACHE_CONTROL;
         }

         int idx = cache.indexOf("max-age=");
         if (idx < 0) {
            log.trace("cache control header missing max-age directive");
            return DEFAULT_CACHE_CONTROL;
         }

         int start = idx + "max-age=".length();
         int end = start+1;
         for (int len = cache.length(); end < len; ++end) {
            if (!Character.isDigit(cache.charAt(end))) {
               break;
            }
         }

         try {
            long maxAge = TimeUnit.NANOSECONDS.convert(Long.parseLong(cache.substring(start,end).trim()), TimeUnit.SECONDS);
            return (maxAge < MINIMUM_CACHE_CONTROL) ? MINIMUM_CACHE_CONTROL : maxAge;
         } catch (NumberFormatException ex) {
            log.trace("cache control header max-age directive invalid", ex);
            return DEFAULT_CACHE_CONTROL;
         }
      }

      private boolean parseUsnHeader(HttpHeaders headers) {
         String usnHeader = headers.get("USN");
         if (usnHeader == null) {
            log.trace("usn missing");
            return false;
         }

         List<String> parts = usnSplitter.splitToList(usnHeader);
         int size = parts.size();

         if (size != 2 && size != 5 && size != 8) {
            log.trace("usn bad size: {}", size);
            return false;
         }

         String uuidPart = parts.get(0);
         if (!"uuid".equals(uuidPart)) {
            log.trace("usn bad uuid");
            return false;
         }

         String deviceUuid = parts.get(1);
         if (deviceUuid == null || deviceUuid.length() != 36) {
            log.trace("usn bad device uuid: {}", deviceUuid);
            return false;
         }

         String namespace = null;
         String clazz = null;
         String type = null;
         Integer version = null;
         if ((size == 5 || size == 8) && !parts.get(2).isEmpty()) {
            log.trace("usn bad seperator");
            return false;
         }

         if (size == 5) {
            if (!"upnp".equals(parts.get(3)) || !"rootdevice".equals(parts.get(4))) {
               log.trace("usn bad root device");
               return false;
            }

            clazz = "root";
            namespace = "upnp";
            type = "rootdevice";
            version = 1;
         }

         if (size == 8) {
            if (!"urn".equals(parts.get(3))) {
               log.trace("usn bad urn");
               return false;
            }

            namespace = parts.get(4);
            clazz = parts.get(5);
            type = parts.get(6);
            try {
               version = Integer.parseInt(parts.get(7));
            } catch (NumberFormatException ex) {
               log.trace("usn bad version", ex);
               return false;
            }
         }

         usn.deviceUuid = deviceUuid;
         usn.namespace = namespace;
         usn.clazz = clazz;
         usn.type = type;
         usn.version = version;
         return true;
      }
   }

   private static String getResponseDate() {
      Calendar calendar = Calendar.getInstance();
      SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
      dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
      return dateFormat.format(calendar.getTime());
   }

   static final class UniqueServiceName {
      @Nullable String deviceUuid;
      @Nullable String namespace;
      @Nullable String clazz;
      @Nullable String type;
      @Nullable Integer version;
   }
    
   abstract class Responder implements Runnable {
      final InetSocketAddress to;

      public Responder(InetSocketAddress to) {
         this.to = to;
      }

      @Override
      public void run() {
         send(handler.ctx.channel(), to, getResponseDate());
      }

      protected abstract ChannelFuture send(Channel ch, InetSocketAddress to, String date);
   }
}


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
package com.iris.agent.router;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.iris.agent.addressing.HubAddr;
import com.iris.agent.addressing.HubBridgeAddress;
import com.iris.agent.addressing.HubServiceAddress;
import com.iris.messages.PlatformMessage;
import com.iris.protocol.ProtocolMessage;

public final class Router {
   private static final Logger log = LoggerFactory.getLogger(Router.class);

   private final Set<PortInternal> ports;
   private final Set<PortInternal> snoopers;

   private final Map<String,PortInternal> service;
   private final Map<String,PortInternal> protocol;

   private final ExecutorService executor;
   private @Nullable MessageDispatcher dispatcher;

   public Router() {
      this.executor = Executors.newCachedThreadPool(new RouterThreadFactory());
      this.ports = Collections.newSetFromMap(new ConcurrentHashMap<PortInternal,Boolean>());
      this.snoopers = Collections.newSetFromMap(new ConcurrentHashMap<PortInternal,Boolean>());

      this.service = new ConcurrentHashMap<>();
      this.protocol = new ConcurrentHashMap<>();
   }

   @PostConstruct
   public void start() {
      log.info("starting up iris hub ring buffer router");

      this.dispatcher = new MessageDispatcher(new LinkedTransferQueue<>());
      executor.submit(dispatcher);
   }

   @PreDestroy
   public void shutdown() {
      MessageDispatcher dispatcher = this.dispatcher;
      this.dispatcher = null;

      if (dispatcher != null) {
         dispatcher.shutdown();
      }

      executor.shutdown();
      while (!executor.isShutdown()) {
         try {
            if (executor.awaitTermination(30, TimeUnit.SECONDS)) {
               break;
            }
         } catch (InterruptedException ex) {
            // retry
         }
      }
   }

   private BlockingQueue<Message> createQueue() {
      return new LinkedTransferQueue<>();
   }

   public Port injector(String name) {
      return new InjectingPort(this, name, createQueue());
   }

   public Port connect(String name, SnoopingPortHandler handler) {
      return createConnection(new SnoopingPort(this, handler, null, name, createQueue()));
   }

   public Port connect(String name, SnoopingPortHandler handler, HubAddr address, PortHandler addressableHandler) {
      AddressMatchingPort addressablePort = createAddressMatchingPort(address, addressableHandler, name, null);
      return createConnection(new SnoopingPort(this, handler, addressablePort, name, createQueue()));
   }

   public Port gateway(String name, SnoopingPortHandler handler, HubAddr address, PortHandler addressableHandler) {
      AddressMatchingPort addressablePort = createAddressMatchingPort(address, addressableHandler, name, null);
      return createConnection(new SnoopingPort(this, handler, addressablePort, name, createQueue(), true));
   }

   public Port connect(String name, HubAddr address, PortHandler handler) {
      Preconditions.checkNotNull(address, "port address cannot be null");

      AbstractPort port = createAddressMatchingPort(address, handler, name, createQueue());
      return createConnection(port);
   }

   private AddressMatchingPort createAddressMatchingPort(HubAddr address, PortHandler handler, String name, @Nullable BlockingQueue<Message> queue) {
      Preconditions.checkNotNull(address, "port address cannot be null");

      if (address instanceof HubServiceAddress) {
         return new ServicePort((HubServiceAddress)address, this, handler, name, queue);
      } else if (address instanceof HubBridgeAddress) {
         return new BridgePort((HubBridgeAddress)address, this, handler, name, queue);
      } else {
         throw new IllegalStateException("hub router does not know how to route messages to address: " + address);
      }
   }

   private Port createConnection(AbstractPort port) {
      this.ports.add(port);
      if (port.isListenAll()) {
         snoopers.add(port);
      }

      String svcid = port.getServiceId();
      if (svcid != null) {
         service.put(svcid, port);
      }

      String prtid = port.getProtocolId();
      if (prtid != null) {
         protocol.put(prtid, port);
      }

      executor.submit(port);
      return port;
   }

   public void disconnect(Port port) {
      send(new Message(Message.Type.POISON, null, port, null, false));
   }

   void sendPlatform(HubAddr addr, PlatformMessage message, boolean forward) {
      send(new Message(Message.Type.PLATFORM, message.getSource(), addr, message, forward));
   }

   void sendProtocol(HubAddr addr, ProtocolMessage message, boolean forward) {
      send(new Message(Message.Type.PROTOCOL, message.getSource(), addr, message, forward));
   }

   void sendCustom(Object addr, Object message) {
      send(new Message(Message.Type.CUSTOM, null, addr, message, false));
   }

   private void send(Message message) {
      try {
         MessageDispatcher dispatcher = this.dispatcher;
         if (dispatcher != null) {
            dispatcher.queue.put(message);
         } else {
            log.warn("message dispatcher shutdown, could not dispatch message: {}", message);
         }
      } catch (InterruptedException ex) {
         Thread.currentThread().interrupt();
         throw new RuntimeException(ex);
      }
   }

   private final class MessageDispatcher implements Runnable {
      private final BlockingQueue<Message> queue;

      public MessageDispatcher(BlockingQueue<Message> queue) {
         this.queue = queue;
      }

      void shutdown() {
         try {
            queue.put(new Message(Message.Type.POISON, null, null, null, false));
         } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
         }
      }

      @Override
      public void run() {
         try {
            while (true) {
               Message msg = queue.take();

               try {
                  Object dst = msg.getDestination();
                  switch (msg.getType()) {
                  case PLATFORM:
                  case PROTOCOL:
                     HubAddr addr = (HubAddr)dst;
                     String svcid = addr.getServiceId();
                     String prtid = addr.getProtocolId();

                     PortInternal svcpt = (svcid != null) ? service.get(svcid) : null;
                     PortInternal prtpt = (prtid != null) ? protocol.get(prtid) : null;

                     if (svcpt != null) {
                        svcpt.enqueue(addr,msg,false);
                        if (prtpt != null && prtpt != svcpt) {
                           prtpt.enqueue(addr,msg,false);
                        }
                     } else if (prtpt != null) {
                        prtpt.enqueue(addr,msg,false);
                     }

                     if (!msg.isForwarded()) {
                        for (PortInternal port : snoopers) {
                           port.enqueue(addr,msg,true);
                        }
                     }
                     break;

                  case CUSTOM:
                     if (ports.contains(dst)) {
                        ((PortInternal)dst).enqueue(null,msg,false);
                     } else {
                        log.warn("dropping custom message not address to anyone: {}", msg);
                     }
                     break;

                  case POISON:
                     if (dst == null) {
                        for (PortInternal port : ports) {
                           port.enqueue(null,msg,false);
                        }
                        
                        return;
                     } else {
                        ((PortInternal)dst).enqueue(null,msg,false);
                     }
                     break;

                  default:
                     log.warn("unknown message type, ignoring: {}", msg.getType());
                     break;
                  }

                  /*
                  for (PortInternal port : ports) {
                     if (port.canHandleMessage(msg)) {
                        port.getQueue().put(msg);
                     }
                  }
                  */
               } catch (Exception ex) {
                  log.warn("exception while dispatching message, dropping: {}", msg, ex);
               }
            }
         } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
         } finally {
            log.info("message dispatcher shutting down...");
         }
      }
   }

   private static final class RouterThreadFactory implements ThreadFactory {
      private final AtomicLong num = new AtomicLong(0);

      @Override
      public Thread newThread(@Nullable Runnable r) {
         Thread thr = new Thread(r);
         thr.setName("rtr" + num.getAndIncrement());
         thr.setDaemon(false);
         return thr;
      }
   }
}


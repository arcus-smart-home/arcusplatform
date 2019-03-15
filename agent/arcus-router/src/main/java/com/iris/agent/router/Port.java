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

import org.eclipse.jdt.annotation.Nullable;

import com.iris.agent.addressing.HubAddr;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.protocol.Protocol;
import com.iris.protocol.ProtocolMessage;

public interface Port {
   public static final Object HANDLED = new Object();

   /**
    * Creates a delegate port that will handle the given types of
    * messages. This delegate port can send any kind of message,
    * but will only receive messages matching the filter.
    *
    * Any given message will only be delievered to a single
    * handler. The selected handler will be the first matching
    * where the following rules define the matching order:
    *    * Delegate handlers are checked in the order they
    *      we registered via calls to this method.
    *    * The top-level handler will be selected as the
    *      default if no other handler is present.
    */
   Port delegate(PortHandler handler, String... messageTypes);

   /**
    * Creates a delegate port that will handle the given types of
    * messages. This delegate port can send any kind of message,
    * but will only receive messages matching the filter.
    *
    * Any given message will only be delievered to a single
    * handler. The selected handler will be the first matching
    * where the following rules define the matching order:
    *    * Delegate handlers are checked in the order they
    *      we registered via calls to this method.
    *    * The top-level handler will be selected as the
    *      default if no other handler is present.
    */
   Port delegate(PortHandler handler, Class<?>... messageTypes);

   /**
    * Replies to a request with a response. The port will ensure
    * that the correlation id and other request/response parameters
    * are correctly copied into the reply.
    *
    * If the request does not require a response then an uncorrelated
    * message will be sent back to the requester.
    */
   void reply(PlatformMessage req, MessageBody rsp);

   /**
    * Replies to a request with an error response. The port will ensure
    * that the correlation id and other request/response parameters
    * are correctly copied into the reply.
    *
    * If the request does not require a response then an uncorrelated
    * message will be sent back to the requester.
    */
   void errorReply(PlatformMessage req, String code, String msg);

   /**
    * Replies to a request with an error response. The port will ensure
    * that the correlation id and other request/response parameters
    * are correctly copied into the reply.
    *
    * If the request does not require a response then an uncorrelated
    * message will be sent back to the requester.
    */
   void errorReply(PlatformMessage req, Throwable cause);

   /**
    * Forwards a message from one port to another port.
    */
   Object forward(HubAddr destination, PlatformMessage message);

   /**
    * Forwards a message from one port to another port.
    */
   void forward(HubAddr destination, ProtocolMessage message);

   /**
    * Sends an error event to the original requster. The error event
    * will not have the correlation id and other request/response
    * parameters copied, as this is an error event not response.
    */
   void error(PlatformMessage req, String code, String msg);

   /**
    * Sends an error event to the original requster. The error event
    * will not have the correlation id and other request/response
    * parameters copied, as this is an error event not response.
    */
   void error(PlatformMessage req, Throwable cause);

   /**
    * Sends a platform message from this port to the destination address.
    */
   void send(HubAddr destination, MessageBody message);
   void send(HubAddr destination, MessageBody message, int ttl);
   void send(HubAddr destination, PlatformMessage message);

   void send(Address destination, MessageBody message);
   void send(Address destination, MessageBody message, int ttl);
   void send(PlatformMessage message);

   /**
    * Sends a platform message request from this port to the destination address.
    */
   void sendRequest(HubAddr destination, MessageBody message);
   void sendRequest(Address destination, MessageBody message);
   void sendRequest(HubAddr destination, MessageBody message, int ttl);
   void sendRequest(Address destination, MessageBody message, int ttl);

   /**
    * Sends a platform message event from this port.
    */
   void sendEvent(MessageBody message);
   void sendEvent(MessageBody message, int ttl);

   /**
    * Sends a platform message event from the given address.
    */
   void sendEvent(Address source, MessageBody message);
   void sendEvent(Address source, MessageBody message, int ttl);

   /**
    * Sends a protocol message from this port to the destination address.
    */
   <T> void send(HubAddr destination, Protocol<T> protocol, T message);
   void send(HubAddr destination, String protocol, byte[] message);
   void send(HubAddr addr, ProtocolMessage message);

   <T> void send(Address destination, Protocol<T> protocol, T message);
   void send(Address destination, String protocol, byte[] message);
   void send(ProtocolMessage message);

   /**
    * Places a platform message on the queue.
    */
   void queue(PlatformMessage msg);

   /**
    * Places a protocol message on the queue.
    */
   void queue(ProtocolMessage msg);

   /**
    * Queues up a custom message from this port back to itself. This is used to
    * turn asynchronous events into synchronous events.
    */
   void queue(Object custom);

   Address getPlatformAddress();
   Address getProtocolAddress();

   Address getSendPlatformAddress();
}


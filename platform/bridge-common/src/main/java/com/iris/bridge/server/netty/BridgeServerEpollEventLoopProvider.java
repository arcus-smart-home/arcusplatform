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
package com.iris.bridge.server.netty;

import java.util.concurrent.ThreadFactory;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.socket.ServerSocketChannel;

public class BridgeServerEpollEventLoopProvider implements BridgeServerEventLoopProvider {
   @Override
   public EventLoopGroup create(int threads, ThreadFactory factory) {
      return new EpollEventLoopGroup(threads, factory);
   }

   @Override
   public Class<? extends ServerSocketChannel> getServerSocketChannelClass() {
      return EpollServerSocketChannel.class;
   }
}


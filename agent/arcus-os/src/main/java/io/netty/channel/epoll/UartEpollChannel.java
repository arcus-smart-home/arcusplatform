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
package io.netty.channel.epoll;

import io.netty.channel.epoll.AbstractEpollStreamChannel;
import io.netty.channel.unix.Socket;

import java.net.SocketAddress;

import com.iris.agent.os.serial.UartAddress;

public final class UartEpollChannel extends AbstractEpollStreamChannel {
    private static final UartAddress REMOTEADDR = new UartAddress("localhost");
    private final UartEpollChannelConfig config;

    public UartEpollChannel(Socket fd, boolean active) {
        super(fd, active);
        config = new UartEpollChannelConfig(this);
    }

    @Override
    protected SocketAddress localAddress0() {
		return null;
    }

    @Override
    protected SocketAddress remoteAddress0() {
		return REMOTEADDR;
    }

    @Override
    protected void doBind(SocketAddress local) throws Exception {
		throw new UnsupportedOperationException();
    }

    @Override
    public UartEpollChannelConfig config() {
        return config;
    }

	 @Override
    protected boolean doConnect(SocketAddress remoteAddress, SocketAddress localAddress) throws Exception {
		return true;
	 }
}


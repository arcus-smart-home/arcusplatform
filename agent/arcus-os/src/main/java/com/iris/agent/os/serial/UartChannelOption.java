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
package com.iris.agent.os.serial;

import static io.netty.channel.ChannelOption.valueOf;

import io.netty.channel.ChannelOption;

public final class UartChannelOption {
   public static final ChannelOption<Integer> BAUDRATE = valueOf("BAUDRATE");
   public static final ChannelOption<UartChannelConfig.FlowControl> FLOWCONTROL = valueOf("FLOWCONTROL");
   public static final ChannelOption<UartChannelConfig.StopBits> STOPBITS = valueOf("STOPBITS");
   public static final ChannelOption<UartChannelConfig.DataBits> DATABITS = valueOf("DATABITS");
   public static final ChannelOption<UartChannelConfig.ParityBit> PARITYBIT = valueOf("PARITYBIT");
   public static final ChannelOption<Integer> VTIME = valueOf("VTIME");
   public static final ChannelOption<Integer> VMIN = valueOf("VMIN");

   private UartChannelOption() { }
}


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
package com.iris.protocol.zwave.message;

import com.iris.protocol.zwave.model.ZWaveCommand;
import com.iris.protocol.zwave.model.ZWaveNode;

public class ZWaveCommandMessage implements ZWaveMessage {
   public final static String TYPE = "Command";
	private static final long serialVersionUID = -8289798556070224937L;

	private ZWaveCommand	command = null;		// Command that we want to send on the
	private ZWaveNode		node	= null;

	public ZWaveCommandMessage() {
	}

	public ZWaveCommandMessage(ZWaveNode node, ZWaveCommand command) {
	   this.node = node;
	   this.command = command;
	}

	@Override
   public String getMessageType() {
      return TYPE;
   }

	public void setDevice ( ZWaveNode node ) {
		this.node = node;
	}

	public ZWaveNode getDevice() {
		return node;
	}

	public void setCommand (ZWaveCommand command) {
		this.command = command;
	}

	public ZWaveCommand getCommand() {
		return command;
	}

   /* (non-Javadoc)
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      return "ZWaveCommandMessage [command=" + command + ", node=" + node + "]";
   }

}


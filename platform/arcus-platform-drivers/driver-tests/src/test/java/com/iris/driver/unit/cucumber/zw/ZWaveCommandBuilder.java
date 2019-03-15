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
package com.iris.driver.unit.cucumber.zw;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.mutable.MutableByte;

import com.iris.driver.service.executor.DriverExecutors;
import com.iris.driver.unit.cucumber.CommandBuilder;
import com.iris.driver.unit.cucumber.DataTypeUtilities;
import com.iris.driver.unit.cucumber.DriverTestContext;
import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.zwave.message.ZWaveCommandMessage;
import com.iris.protocol.zwave.message.ZWaveMessage;
import com.iris.protocol.zwave.message.ZWaveNodeInfoMessage;
import com.iris.protocol.zwave.model.ZWaveAllCommandClasses;
import com.iris.protocol.zwave.model.ZWaveCommand;
import com.iris.protocol.zwave.model.ZWaveCommandClass;
import com.iris.protocol.zwave.model.ZWaveNode;


public class ZWaveCommandBuilder extends CommandBuilder {

	ZWaveDriverTestCase context;
	ZWaveCommandMessage message = new ZWaveCommandMessage();

	
	ZWaveCommandBuilder (ZWaveDriverTestCase context) {
		this.context = context;
		message.setDevice(context.getNode());
		message.setCommand(new ZWaveCommand());
	}


	@Override
	public CommandBuilder commandName (String commandClassName, String commandName) {
		ZWaveCommandClass commandClass = ZWaveAllCommandClasses.getClass(commandClassName);
		if (commandClass == null) 
			throw new IllegalArgumentException("No command class found with the name "+ commandClassName+". Should be one of "+ getAllCommandClassNames());
		ZWaveCommand command;
		try {
			command =new ZWaveCommand(commandClass.commandsByName.get(commandName));
		} catch(NullPointerException npe) { 
			throw new IllegalArgumentException("No command name found with the name " + commandName);
		}
		message.setCommand(command);
		return this;
	}
	
	@Override
	public CommandBuilder addProtocolMessageData(String parameterName, String parameterValue){
			message.getCommand().setRecv(parameterName, DataTypeUtilities.byteValueOf(parameterValue));
			return this;
	}


	@Override
	public void buildAndSend() {
		message.setCommand(new ZWaveTestCommand(message.getCommand()));
		ProtocolMessage protocolMsg = ProtocolMessage.builder()
		.from(context.getClientAddress())
		.to(context.getDriverAddress())
		.withPayload(context.getProtocol(), message)
		.create();
		DriverExecutors.dispatch(protocolMsg, context.getDriverExecutor());		
	}
	
	private Set<String> getAllCommandClassNames() {
		return ZWaveAllCommandClasses.allClasses.classesByName.keySet();
	}

	public void nodeinfo(){
		ZWaveNodeInfoMessage message = new ZWaveNodeInfoMessage((byte)0, (byte)0, (byte)0, (byte)(0), (byte)0);
		message.setNodeId(context.getNode().getNumber());
		ProtocolMessage protocolMsg = ProtocolMessage.builder()
				.from(context.getClientAddress())
				.to(context.getDriverAddress())
				.withPayload(context.getProtocol(), message)
				.create();
		DriverExecutors.dispatch(protocolMsg, context.getDriverExecutor());		

	}
	
	@Override
	public CommandBuilder addPayload(String[] actualStrings) {
		// TODO Auto-generated method stub
		return null;
	}

	//ZWaveCommand.payload() returns the send bytes, and we are trying to load receive bytes.
	private class ZWaveTestCommand extends ZWaveCommand{
	private static final long serialVersionUID = 1L;
	private LinkedHashMap<String, MutableByte> receiveVariables;
	
	   public ZWaveTestCommand(ZWaveCommand clone) {
		      this.commandName = clone.commandName;
		      this.commandClass = clone.commandClass;
		      this.commandNumber = clone.commandNumber;
		      this.response_id = clone.response_id;

		      if (clone.sendVariables != null) {
		         for (Map.Entry<String,MutableByte> entry : clone.sendVariables.entrySet()) {
		            this.sendVariables.put(entry.getKey(), new MutableByte(entry.getValue()));
		         }
		      }

		      this.receiveVariables = new LinkedHashMap<String, MutableByte>();
		      if (clone.receiveVariables != null) {
		         for (Map.Entry<String,MutableByte> entry : clone.receiveVariables.entrySet()) {
		            this.receiveVariables.put(entry.getKey(), new MutableByte(entry.getValue()));
		         }
		      }

		      this.responseStatus = clone.responseStatus;
		   }

	   	//loads the receive variables into the payload vs send variable in parentclass
		public byte[] payload() {
			      
			byte[] payload = new byte[receiveVariables.size()];
			
			int idx = 0;
			for (Entry<String,MutableByte> entry : receiveVariables.entrySet()) {
				payload[idx++] = entry.getValue().byteValue();
			}

			return payload;
	   }
	
	}
}


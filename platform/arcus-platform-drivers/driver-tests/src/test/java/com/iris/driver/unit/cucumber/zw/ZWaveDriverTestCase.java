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

import com.iris.driver.groovy.GroovyProtocolPluginModule;
import com.iris.driver.unit.cucumber.AbstractDriverTestCase;
import com.iris.driver.unit.cucumber.CommandBuilder;
import com.iris.driver.unit.cucumber.DataTypeUtilities;
import com.iris.driver.unit.cucumber.DriverTestContext;
import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.zwave.Protocol.Command;
import com.iris.protocol.zwave.Protocol.Message;
import com.iris.protocol.zwave.ZWaveExternalProtocol;
import com.iris.protocol.zwave.ZWaveProtocol;
import com.iris.protocol.zwave.message.ZWaveMessage;
import com.iris.protocol.zwave.model.ZWaveAllCommandClasses;
import com.iris.protocol.zwave.model.ZWaveCommand;
import com.iris.protocol.zwave.model.ZWaveCommandClass;
import com.iris.protocol.zwave.model.ZWaveNode;
import com.iris.test.Modules;

import io.netty.buffer.Unpooled;

import java.io.IOException;
import java.util.LinkedHashMap;

import org.apache.commons.lang3.mutable.MutableByte;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Modules({ GroovyProtocolPluginModule.class })
public class ZWaveDriverTestCase extends AbstractDriverTestCase implements DriverTestContext<ZWaveMessage> {

    private final ZWaveNode node;

	private final Logger logger = LoggerFactory.getLogger(ZWaveDriverTestCase.class) ; 	
	
	
	private ZWaveCommand command;
	
    public ZWaveDriverTestCase() {
        node = new ZWaveNode((byte)10);
    }
    
    public ZWaveNode getNode(){
       return node;
    }

    @Override
    public com.iris.protocol.Protocol<ZWaveMessage> getProtocol() {
        return ZWaveProtocol.INSTANCE;
    }
    
    @Override
    public void validateProtocolMessage(ProtocolMessage protocolMsg, String type, String subType){
		this.command = null;
		Message zwaveProtocolMsg = (Message)protocolMsg.getValue();
		logger.debug("\n ZWave Protocol Message = "+zwaveProtocolMsg);
		byte[] zwaveMsgByte = ZWaveExternalProtocol.INSTANCE.createSerializer().serialize(zwaveProtocolMsg);
		switch (zwaveProtocolMsg.getType()){
			case 1:
				Command pcmd=null;
				try {
					pcmd = Command.serde().nettySerDe().decode( Unpooled.wrappedBuffer(zwaveProtocolMsg.getPayload()) );
				} catch (Exception e) {
					assert false: e.getMessage();
				}
				ZWaveCommandClass cc = ZWaveAllCommandClasses.allClasses.get(pcmd.rawCommandClassId());
				if (cc == null) {
				   assert false: "unknown command class: " + pcmd.getCommandClassId();
				}
				command = cc.get(pcmd.rawCommandId());
				if (command == null) {
				   assert false: "unknown command " + pcmd.getCommandId() + " in command class " + pcmd.getCommandClassId();
				}
				logger.trace("Command "+pcmd);
				try{
					command.set(pcmd.getPayload());
				} catch (Exception e){}
				//intentionally swallowing exception to get more meaningful errors below.
				logger.trace("\n Validating ZWave Command payload "+ command);
				assert ZWaveCommandClass.scrub(cc.name).equalsIgnoreCase(type): "expected ["+type + "] but found ["+cc.name +"] instead";
				assert command.commandName.equalsIgnoreCase(subType): "expected ["+subType + "] but found ["+command.commandName +"] instead";
				break;
			case 4:
				logger.trace("\n zwave message bytes = {}", zwaveMsgByte);
				break;
			default:
				logger.warn("received unknown type zwave message {}", zwaveProtocolMsg);
				break;
		}

    }
    
    @Override
    public void checkTimeoutSeconds(ProtocolMessage message, Integer expectedTimeoutSeconds )throws IOException{
    	com.iris.protocol.zwave.Protocol.Message protocol = (com.iris.protocol.zwave.Protocol.Message)message.getValue();
 		assert protocol.getType() == com.iris.protocol.zwave.Protocol.SetOfflineTimeout.ID: "Not a timeout message";
 		com.iris.protocol.zwave.Protocol.SetOfflineTimeout ptout = com.iris.protocol.zwave.Protocol.SetOfflineTimeout.serde().nettySerDe().decode( Unpooled.wrappedBuffer(protocol.getPayload()) );
 		logger.trace("Deserialized Timeout Message: {} ", ptout);
 		if ( null != expectedTimeoutSeconds){
 		assert expectedTimeoutSeconds == ptout.getSeconds(): "Expected "+expectedTimeoutSeconds+ " seconds and instead got "+ptout.getSeconds();
 		}
    }
    
    public void checkPoll(ProtocolMessage message, String cmdClass, String command, int period)throws IOException{
    	com.iris.protocol.zwave.Protocol.Message protocol = (com.iris.protocol.zwave.Protocol.Message)message.getValue();
 		assert protocol.getType() == com.iris.protocol.zwave.Protocol.SetSchedule.ID: "Not a poll setup message";
 		com.iris.protocol.zwave.Protocol.SetSchedule psched = com.iris.protocol.zwave.Protocol.SetSchedule.serde().nettySerDe().decode( Unpooled.wrappedBuffer(protocol.getPayload()) );
		logger.trace("Parsed the set schedule: {} ", psched);
		assert psched.getNumSchedules() == 1 : "Only support 1 schedule, received "+psched.getNumSchedules();
		//get commands from message
		com.iris.protocol.zwave.Protocol.Schedule schedule[] = psched.getSchedule();
		byte payload[] = schedule[0].getPayload();
		ZWaveCommandClass ccActual = ZWaveAllCommandClasses.allClasses.get(payload[0]);
		assert ccActual != null: "unknown command class: " + payload[0];
		ZWaveCommand commandActual = ccActual.get(payload[1]);
		assert commandActual != null : "unknown command " + payload[0] + " in command class " + payload[1];
		assert ZWaveCommandClass.scrub(ccActual.name).equalsIgnoreCase(cmdClass): "Unexpected command class "+ccActual.name +", expecting "+ cmdClass;
		assert commandActual.commandName.equalsIgnoreCase(command): " Unexpected command of "+commandActual.commandName+", expecting "+command;
		assert psched.getSeconds() == period: "Unexpected polling rate of "+psched.getSeconds()+", Expected " + period;
				
    }
    
    @Override
    public void checkSentParameter(String parameterName, String parameterValue){
		Byte actualParameter = null;
		logger.debug("\nParsing parameter: "+ parameterName);
		try{
			try{
				actualParameter = command.getSend(command.getSendNames().get(DataTypeUtilities.byteValueOf(parameterName)));
			} catch (NumberFormatException ex){
				actualParameter = command.getSend(parameterName);			
			}
		} catch (Exception e){
			assert false: "Unable to find parameter "+ parameterName;
		}
		assert actualParameter == DataTypeUtilities.byteValueOf(parameterValue): "Expected value "+ parameterValue + " but instead found " + actualParameter;
				
    }

	@Override
	public CommandBuilder getCommandBuilder() {
		return new ZWaveCommandBuilder(this);
	}

}


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
package com.iris.driver.unit.cucumber.ipcd;

import java.util.Map;

import com.iris.driver.groovy.GroovyProtocolPluginModule;
import com.iris.driver.unit.cucumber.AbstractDriverTestCase;
import com.iris.driver.unit.cucumber.CommandBuilder;
import com.iris.driver.unit.cucumber.DriverTestContext;
import com.iris.protocol.Protocol;
import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.ipcd.IpcdProtocol;
import com.iris.protocol.ipcd.message.IpcdMessage;
import com.iris.protocol.ipcd.message.model.IpcdCommand;

import com.iris.test.Modules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Modules({ GroovyProtocolPluginModule.class })
public class IpcdDriverTestCase extends AbstractDriverTestCase implements DriverTestContext<IpcdMessage> {

	private final Logger logger = LoggerFactory.getLogger(IpcdDriverTestCase.class) ; 	

	private Map ipcdProtocolMsg;
	
    @Override
    public Protocol<IpcdMessage> getProtocol() {
        return IpcdProtocol.INSTANCE;
    }
    
    @Override
    public void validateProtocolMessage(ProtocolMessage protocolMsg, String type, String subtype){
		ipcdProtocolMsg = ((IpcdCommand)protocolMsg.getValue()).mapify();
		String command = (String)ipcdProtocolMsg.get("command");
		assert command.equalsIgnoreCase(type): "unable to find command ["+ type +"] in IPCD prototcol message" + ipcdProtocolMsg +", instead found "+command;
    }
    
    @Override
    public void checkTimeoutSeconds(ProtocolMessage message, Integer expectedTimeoutSeconds )throws java.io.IOException{

    	assert false:"checkTimeoutSeconds not implemented in IpcdDriverTestCase";
    }
    
    @Override
    public void checkSentParameter( String parameterName, String parameterValue){
		String command = (String)ipcdProtocolMsg.get("command");
		logger.trace("\n The command is [{}] \n", command);
		switch (command){
			case "Download":
				break;
			case "SetParameterValues":
				ipcdProtocolMsg = (Map)ipcdProtocolMsg.get("values");
				break;
			default:
				assert false : "Unknown IPCD Command: "+ipcdProtocolMsg.get("command");
				break;
		}
		assert ipcdProtocolMsg.containsKey(parameterName): "unable to find key ["+ parameterName +"] in IPCD prototcol message" + ipcdProtocolMsg;
		assert parameterValue.equalsIgnoreCase((String)ipcdProtocolMsg.get(parameterName)): parameterName +"not equal to ["+ parameterValue +"] in IPCD prototcol message" + ipcdProtocolMsg;
    }
    
    public CommandBuilder getCommandBuilder(){
    	return new IpcdCommandBuilder(this);
    }
}    


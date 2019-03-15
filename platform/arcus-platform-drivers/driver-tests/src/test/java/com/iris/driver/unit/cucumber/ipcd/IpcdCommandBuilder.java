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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.iris.driver.unit.cucumber.CommandBuilder;
import com.iris.driver.unit.cucumber.DriverTestContext;
import com.iris.driver.unit.cucumber.ipcd.IpcdDriverTestCase;
import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.ipcd.message.IpcdMessage;
import com.iris.protocol.ipcd.message.model.Device;
import com.iris.protocol.ipcd.message.model.DeviceInfo;
import com.iris.protocol.ipcd.message.model.GetDeviceInfoCommand;
import com.iris.protocol.ipcd.message.model.GetDeviceInfoResponse;
import com.iris.protocol.ipcd.message.model.GetParameterValuesCommand;
import com.iris.protocol.ipcd.message.model.GetParameterValuesResponse;
import com.iris.protocol.ipcd.message.model.IpcdEvent;
import com.iris.protocol.ipcd.message.model.IpcdReport;
import com.iris.protocol.ipcd.message.model.IpcdResponse;
import com.iris.protocol.ipcd.message.model.Status;
import com.iris.protocol.ipcd.message.model.StatusType;
import com.iris.protocol.ipcd.message.model.ValueChange;
import com.iris.driver.service.executor.DriverExecutors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IpcdCommandBuilder extends CommandBuilder{

	private IpcdDriverTestCase context;
	IpcdMessage message;
	Device device;
	IpcdCommandBuilder() {}
	List<ValueChange> changes;
	Map<String,Object> valueMap;
	DeviceInfo deviceInfo;

	private final Logger logger = LoggerFactory.getLogger(IpcdDriverTestCase.class) ; 	

	IpcdCommandBuilder(DriverTestContext<IpcdMessage> context) {
		this.context = (IpcdDriverTestCase)context;
		assert null != context: "Context was null";
		assert null != this.context: "Context was null";
		device = new Device();
		device.setVendor("Test");
		device.setModel("Made-Up");
		device.setSn("1A2B3C4D5E6F");
		device.setIpcdver("0.3");
	}

	@Override
	public IpcdCommandBuilder commandName (String type, String subType) {
		logger.trace("Switching on type:"+type);
		switch (type){
			case "event":
				message = new IpcdEvent();
				ArrayList<String> events = new ArrayList<>();
				events.add(subType);
				((IpcdEvent)message).setEvents(events);
				events.add(subType);
				((IpcdEvent)message).setDevice(device);
				changes = new ArrayList<>();
				((IpcdEvent) message).setDevice(device);
				break;
			case "response":
				switch (subType){
					case "GetParameterValuesResponse":
						message = new GetParameterValuesResponse();
						((GetParameterValuesResponse)message).setRequest(new GetParameterValuesCommand());
						Status status = new Status();
						status.setResult(StatusType.fromString("success"));
						((GetParameterValuesResponse)message).setStatus(status);
						((GetParameterValuesResponse)message).setDevice(device);
						valueMap = new HashMap<>();
						break;
					case "GetDeviceInfoResponse":
						message = new GetDeviceInfoResponse();
						((GetDeviceInfoResponse)message).setRequest(new GetDeviceInfoCommand());
						Status status2 = new Status();
						status2.setResult(StatusType.fromString("success"));
						((GetDeviceInfoResponse)message).setStatus(status2);
						((GetDeviceInfoResponse)message).setDevice(device);
						deviceInfo = new DeviceInfo();
						break;
					default:
						logger.warn("Unrecognized SubType - {}",subType);
						return null;
				}
				((IpcdResponse) message).setDevice(device);
				Status status = new Status();
				break;
			case "report":
				logger.trace("Populating a report");
				message = new IpcdReport();
				valueMap = new HashMap<>();
				((IpcdReport) message).setDevice(device);
				break;
			default:
				assert false: "Unrecognized command type: "+ type;
				break;
		}
		logger.trace("built command "+message.mapify());
		return this;
	}

	public IpcdCommandBuilder addProtocolMessageData(String parameterName, String parameterValue){
		if (message.getClass() == IpcdEvent.class ){
				ValueChange change = new ValueChange();
				change.setParameter( parameterName);
				change.setValue(parameterValue);
				changes.add(change);
		} else if (message.getClass() == GetParameterValuesResponse.class || message.getClass() == IpcdReport.class){
				valueMap.put(parameterName, parameterValue);
		} else if (message.getClass() == GetDeviceInfoResponse.class){
				switch(parameterName.trim()){
					case "fwver":
						deviceInfo.setFwver(parameterValue);
						break;
					case "connection":
						deviceInfo.setConnection(parameterValue);
						break;
					case "connectUrl":
						deviceInfo.setConnectUrl(parameterValue);
						break;
					case "commands":
						parameterValue.replace("[","");
						parameterValue.replace("]","");
						deviceInfo.setCommands(Arrays.asList(parameterValue.split(",")));
						break;
					case "actions":
						parameterValue.replace("[","");
						parameterValue.replace("]","");
						deviceInfo.setActions(Arrays.asList(parameterValue.split(",")));
						break;
					default:
						assert false :"Unrecognized parameter name - "+parameterName;
				}
		} else {
				assert false:"Unrecognized Class - "+message.getClass();
		}
		return this;
	}	
	
	public void buildAndSend() {
		if (message.getClass() ==  IpcdEvent.class){
				((IpcdEvent)message).setValueChanges(changes);
		} else if (message.getClass() == GetParameterValuesResponse.class){
				((GetParameterValuesResponse)message).setResponse(valueMap);
		} else if (message.getClass() ==  GetDeviceInfoResponse.class){
				((GetDeviceInfoResponse)message).setResponse(deviceInfo);
		} else if (message.getClass() == IpcdReport.class){
				  ((IpcdReport)message).setReport(valueMap);
					logger.trace("added value Map "+message.mapify());
		} else {
			assert false :"Unrecognized Class - "+ message.getClass();
		}
		assert null != context: "Context was null in IcpdCommandBuilder.buildAndSend";
		assert null != message: "Message was null in IcpdCommandBuilder.buildAndSend";
		logger.trace("Serializing Message"+this.message.mapify());
		ProtocolMessage protocolMsg = ProtocolMessage.builder()
		.from(this.context.getClientAddress())
		.to(this.context.getDriverAddress())
		.withPayload(context.getProtocol(), this.message)
		.create();
		assert (null != protocolMsg): "protocolMsg null in IcpdCommandBuilder.buildAndSend";
		assert null != context.getDriverExecutor(): "DriverExecutor is null";
		logger.trace("Dispatching protocol Message"+protocolMsg);
		logger.trace("With event:"+protocolMsg.getValue());
		byte[] buffer = protocolMsg.getBuffer();
		logger.trace("With Buffer"+ buffer);
		StringBuilder contents = new StringBuilder("[ ");
		for ( byte b: buffer) {
			contents.append(b);
			contents.append(" , ");
		}
		contents.append("]");

		logger.trace("Buffer contents are :"+contents);
		
		DriverExecutors.dispatch(protocolMsg, context.getDriverExecutor());
	}

	@Override
	public CommandBuilder addPayload(String[] actualStrings) {
		// TODO Auto-generated method stub
		return null;
	}

}


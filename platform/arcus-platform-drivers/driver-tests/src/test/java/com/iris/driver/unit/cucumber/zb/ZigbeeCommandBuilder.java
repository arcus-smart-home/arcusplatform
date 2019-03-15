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
package com.iris.driver.unit.cucumber.zb;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.driver.unit.cucumber.DataTypeUtilities;
import com.iris.driver.groovy.zigbee.ClusterBinding;
import com.iris.driver.groovy.zigbee.ClusterDescriptor;
import com.iris.driver.groovy.zigbee.MessageDescriptor;
import com.iris.driver.groovy.zigbee.cluster.alertme.ZigbeeAlertmeClusters;
import com.iris.driver.groovy.zigbee.cluster.zcl.GeneralBinding;
import com.iris.driver.groovy.zigbee.cluster.zcl.ZigbeeZclClusters;
import com.iris.driver.service.executor.DriverExecutors;
import com.iris.driver.unit.cucumber.CommandBuilder;
import com.iris.driver.unit.cucumber.DriverTestContext;
import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.zigbee.ZclData;
import com.iris.protocol.zigbee.ZigbeeProtocol;
import com.iris.protocol.zigbee.msg.ZigbeeMessage;
import com.iris.protocol.zigbee.zcl.General;
import com.iris.protocol.zigbee.zcl.General.ZclAttributeReport;
import com.iris.protocol.zigbee.zcl.General.ZclReadAttributeRecord;
import com.iris.protocol.zigbee.zcl.General.ZclReadAttributesResponse;
import com.iris.protocol.zigbee.zcl.General.ZclReportAttributes;

import groovy.lang.MissingPropertyException;

public class ZigbeeCommandBuilder extends CommandBuilder {

	private DriverTestContext<ZigbeeMessage.Protocol> context;
	private int clusterId=0, manufacturerCode=0;
	private byte flags=0,  endpoint=1;
	private short messageId=0, profileId=0x0104;
	private byte[] payload;
	private Map<Short, ZclData> zclDataPayload = new LinkedHashMap<>();
	private Map<String, String> protocMessageAttribute = new LinkedHashMap<>();
	private ResponseType responseType;
	private String clusterName, messageType; 
	private ClusterBinding binding;
	
	private static Logger logger = LoggerFactory.getLogger(ZigbeeCommandBuilder.class);
	
	public ZigbeeCommandBuilder(DriverTestContext<ZigbeeMessage.Protocol> context){
		this.context = context;
		logger.trace("Zigbee Command Builder created");
	}
	
	@Override
	public CommandBuilder commandName(String type, String subType) {
		logger.trace("Building Zigbee command {} with value {}", type, subType);
			if(type.startsWith("0x")) {
				setClusterId(DataTypeUtilities.parseHex(type));
			}else if (StringUtils.isNumeric(type)){
				setClusterId(Short.parseShort(type));
			}else if(!"alertme".equalsIgnoreCase(type)){
				setClusterName(type);
			}
			
			if(subType.startsWith("0x")) {
				messageId = DataTypeUtilities.parseHex(subType);
			} else if (StringUtils.isNumeric(subType)){
				try {
				messageId = Short.parseShort(subType);
				}  catch (NumberFormatException e) {}
			} else if("alertme".equalsIgnoreCase(type)){
				setAlertMeClusterName(subType);
			} else {
				setMessage(subType);
			}
			return this;
	}

	@Override
	public CommandBuilder addProtocolMessageData(String parameterName, String parameterValue) {
		try {
			 logger.trace("Add ZCL Data {}", parameterName);
			this.addZclData(parameterName, parameterValue);
		} catch (MissingPropertyException e) {
			 logger.trace("Add ProtocMessage Data {}",parameterName);
			this.addProtocMessageData(parameterName, parameterValue);
		}
		return this;
	}

	public void setClusterId(short clusterId)  {
		for(String key : ZigbeeZclClusters.descriptorsByName.keySet()) {
			if(ZigbeeZclClusters.descriptorsByName.get(key).getId() == clusterId) {
				setClusterName(key);
				break;
			}
		}
		if(binding == null) {
			logger.warn("Unsupported Zigbee cluster {}", clusterId);
			logger.trace("Look under AlertMe");
			for(String key : ZigbeeAlertmeClusters.descriptorsByName.keySet()) {
				if(ZigbeeAlertmeClusters.descriptorsByName.get(key).getId() == clusterId) {
					setAlertMeClusterName(key);
					break;
				}
			}
			logger.debug("Creating new cluster [0x{%h}]", clusterId);
			setMfgSpecificClusterName(clusterId);
		}
	}

	MfgClusterBinding mfgClusterBinding;

	private ZigbeeCommandBuilder setMfgSpecificClusterName(short clusterId) {
		mfgClusterBinding = new MfgClusterBinding(clusterId);
		this.binding = mfgClusterBinding;
		this.clusterName = binding.getName();
		this.clusterId = binding.getId();
		return this;
	}

	public ZigbeeCommandBuilder setClusterName(String clusterName)  {
		this.clusterName = clusterName;
		ClusterDescriptor clusterDescriptor = ZigbeeZclClusters.descriptorsByName.get(clusterName);
		Class<?> bindingClass = clusterDescriptor.getClass().getEnclosingClass();
		try {
			logger.trace("attempting to set binding on cluster:"+clusterName);
			binding = (GeneralBinding) bindingClass.newInstance();
			clusterId = binding.getId();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return this;
	}
	
	private ZigbeeCommandBuilder setAlertMeClusterName(String clusterName) {
		this.clusterName = clusterName;
		ClusterDescriptor clusterDescriptor = ZigbeeAlertmeClusters.descriptorsByName.get(clusterName);
		Class<?> bindingClass = clusterDescriptor.getClass().getEnclosingClass();
		profileId = (short)49686;
		try {
			logger.trace("attempting to set binding on AlertMe cluster:"+clusterName);
			binding = (ClusterBinding) bindingClass.newInstance();
			clusterId = binding.getId();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return this;
		
	}
		
	// may need to add more due to test case arise.
	public ZigbeeCommandBuilder addZclData(String attributeName, String value){
		Object attributeId = null;
		logger.trace("In addZclData; Adding attribute "+ attributeName);
		if(attributeName.matches("[A-Z_]+")) {
			try{
				logger.trace("Looking for property on binding"+binding);
				attributeId = binding.getProperty(attributeName);			
			} catch (MissingPropertyException e) {
				throw e;
			}			
		} else
		if(attributeName.matches("0x[0-9A-F]+")) {
			int attributeIdInt = Integer.parseInt(attributeName.substring(2), 16);			
			if(Short.MAX_VALUE < (int)attributeIdInt) {
				attributeIdInt = -(65536 - (int)attributeIdInt);
			}
			attributeId = (short)attributeIdInt;
		} else
		if(attributeName.matches("[0-9]+")) {
			attributeId = Short.parseShort(attributeName);
		} else {
			throw new MissingPropertyException("Does not support String literal attributeName "+attributeName);
		}
		
		
		if(StringUtils.isNumeric(value) || value.matches("(-?)\\d")){
			long valueNum = Long.parseLong(value);
			logger.trace("Adding data ["+value+"]");
			if(Byte.MAX_VALUE >= valueNum && Byte.MIN_VALUE <= valueNum) {
				ZclData data = ZclData.builder().set8Bit(Byte.parseByte(value)).create();
				zclDataPayload.put((Short)attributeId, data);				
			} else
			if(Short.MAX_VALUE >= valueNum && Short.MIN_VALUE <= valueNum) {
				ZclData data = ZclData.builder().set16Bit(Short.parseShort(value)).create();
				zclDataPayload.put((Short)attributeId, data);				
			} else
			if(Integer.MAX_VALUE >= valueNum && Integer.MIN_VALUE <= valueNum) {
				ZclData data = ZclData.builder().set32Bit(Integer.parseInt(value)).create();
				zclDataPayload.put((Short)attributeId, data);				
			} else
			if(Long.MAX_VALUE >= valueNum && Long.MIN_VALUE <= valueNum) {
				ZclData data = ZclData.builder().set64Bit(Long.parseLong(value)).create();
				zclDataPayload.put((Short)attributeId, data);				
			}
		} else if (value.matches("(-?)\\d.\\d")){
			ZclData data = ZclData.builder().setFloat(Float.parseFloat(value)).create();
			zclDataPayload.put((Short)attributeId, data);
		} else if (value.startsWith("0x")) {
			
			long decimal = Long.parseLong(value.substring(2), 16);
			ZclData data = null;
			if(decimal >= Byte.MIN_VALUE && decimal <= Byte.MAX_VALUE ) {
				data = ZclData.builder().set8Bit((byte)decimal).create();
			} else			
			if(decimal >= Short.MIN_VALUE && decimal <= Short.MAX_VALUE ) {
				data = ZclData.builder().set16Bit((short)decimal).create();
			} else 
			if(decimal >= Integer.MIN_VALUE && decimal <= Integer.MAX_VALUE ) {
				data = ZclData.builder().set32Bit((int)decimal).create();
			} else 
			if(decimal >= Long.MIN_VALUE && decimal <= Long.MAX_VALUE ) {
				data = ZclData.builder().set64Bit(decimal).create();
			} else {
				throw new UnsupportedOperationException("Unable to parse data value "+value);
			}
			zclDataPayload.put((Short)attributeId, data);
		} else { 
			// not a number.
			ZclData data = ZclData.builder().setLongString(value).create();
			zclDataPayload.put((Short)attributeId, data);
		} 
		return this;
	}
	
	/**
	 * This is to collect the 
	 * @param propertyName
	 * @param value
	 */
	public ZigbeeCommandBuilder addProtocMessageData(String propertyName, String value){
		protocMessageAttribute.put(propertyName.toUpperCase(), value);
		if(flags != 0)
			flags = 3;
		return this;
	}
	
	public ZigbeeCommandBuilder setFlags(byte flags) {
		this.flags = flags;
		return this;
	}
	
	public ZigbeeCommandBuilder setManufacturerCode(int manufacturerCode) {
		this.manufacturerCode = manufacturerCode;
		return this;
	}
	
	public CommandBuilder addPayload (String[] actualStrings) throws Exception {
		this.payload = ZigbeeUtil.parsePayload(actualStrings, this.clusterId );
		return this;
	}
	
	/**
	 * This will build the message and send it to the protocol bus
	 * @throws IOException
	 */
	public void buildAndSend() {
		logger.trace("In Build and Send");
		ZigbeeMessage.Zcl.Builder zclBuilder = ZigbeeMessage.Zcl.builder();
		ZigbeeMessage.Zcl origZclMessage = zclBuilder
					.setClusterId(clusterId)
					.setFlags(flags)
					.setZclMessageId(messageId)
					.setProfileId(profileId)
					.setEndpoint(endpoint)
					.setManufacturerCode(manufacturerCode)
					.setPayload(buildPayload())
					.create();

		ZigbeeMessage.Protocol origMessage = ZigbeeProtocol.packageMessage(origZclMessage);
		ProtocolMessage protcolMsg = ProtocolMessage.builder()
				.from(context.getClientAddress())
				.to(context.getDriverAddress())
				.withPayload(context.getProtocol(), origMessage)
				.create();
		
		DriverExecutors.dispatch(protcolMsg, context.getDriverExecutor());
		//ProtocolMessage  prootcolMessage 
		//context.getDeviceDriver().handleProtocolMessage(build(), context.getDeviceDriverContext());
	}
	
	private byte[] buildPayload() {
		byte[] payloadBytes = new byte[0];
		if(payload != null && payload.length > 0) {
			payloadBytes = payload;
			logger.trace("Loaded Payload Bytes");
		} else 
		if(!zclDataPayload.isEmpty() ) { 
			logger.trace("We Have ZCL Payload");
			if (ResponseType.READ == responseType) {
				//ZclData -> ZclReadAttributeRecord -> ZclReadAttributesResponse -> payload
				List<ZclReadAttributeRecord> records = new ArrayList<>();
				int payloadSize = 0;
				for(Short attributeId : zclDataPayload.keySet()) {
					ZclData data = zclDataPayload.get(attributeId);
					ZclReadAttributeRecord record = ZclReadAttributeRecord.builder()
							.setAttributeData(data)
							.setAttributeIdentifier(attributeId)
							.create();
					records.add(record);
					payloadSize += record.getByteSize();
				}
				ZclReadAttributeRecord[] recordsArray = records.toArray(new ZclReadAttributeRecord[records.size()]);
				ZclReadAttributesResponse response = ZclReadAttributesResponse.builder().setAttributes(recordsArray).create();
				ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[payloadSize]).order(ByteOrder.LITTLE_ENDIAN);
				try {
					ZclReadAttributesResponse.serde().nioSerDe().encode(byteBuffer, response);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				payloadBytes = byteBuffer.array();				
			} else
			if (ResponseType.REPORT == responseType) {
				// ZclData -> ZclAttributeReport -> ZclReportAttributes -> payload
				List<ZclAttributeReport> records = new ArrayList<>();
				int payloadSize = 0;
				for(Short attributeId : zclDataPayload.keySet()) {
					ZclData data = zclDataPayload.get(attributeId);
					ZclAttributeReport record = ZclAttributeReport.builder()
							.setAttributeData(data)
							.setAttributeIdenifier(attributeId.shortValue())
							.create();
					records.add(record);
					payloadSize += record.getByteSize();
				}
				ZclAttributeReport[] recordsArray = records.toArray(new ZclAttributeReport[records.size()]);
				ZclReportAttributes response = ZclReportAttributes.builder().setAttributes(recordsArray).create();
				ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[payloadSize]).order(ByteOrder.LITTLE_ENDIAN);
				try {
					ZclReportAttributes.serde().nioSerDe().encode(byteBuffer, response);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				payloadBytes = byteBuffer.array();				
			}
		} else if(!protocMessageAttribute.isEmpty() ) { 
			logger.trace("We have a protocol Message Attribute");
			try{
				logger.trace("Processsing :"+ payloadBytes.toString());
				logger.trace("Binding :"+binding.toString());
				// build the message
				String classFullName = String.format("com.iris.protocol.zigbee.zcl.%s", binding.getName());
				for(Class<?> messageTypeClass : Class.forName(classFullName).getDeclaredClasses()){
					if(messageTypeClass.getSimpleName().equalsIgnoreCase(messageType)) {
						// encode the message
						Object builderObj = messageTypeClass.getMethod("builder").invoke(null);
						for(Method m : builderObj.getClass().getDeclaredMethods()){
							String methodName = m.getName();
							// find matching setter with the value collected with protocMessageAttribute
							if(methodName.startsWith("set")) {
								String propertyName = StringUtils.substringAfter(methodName, "set");
								String key = propertyName.toUpperCase();
								if(protocMessageAttribute.containsKey(key)){
									String value = protocMessageAttribute.get(key);
									Class<?> parameterType = m.getParameterTypes()[0];
									// invoke the setter
									Object valueParam = null;
									if(parameterType.isArray()){
										Class<?> arrayComponentType = parameterType.getComponentType();										
										String[] values = value.split(",");										
										valueParam = Array.newInstance(arrayComponentType, values.length);										
										for(int i=0, j=values.length; i<j; i++) {
											Array.set(valueParam, i, DataTypeUtilities.parsePrimitive(arrayComponentType, values[i]));
										}
									} else {
										valueParam = DataTypeUtilities.parsePrimitive(parameterType, value);
									}
									if(valueParam != null)
										m.invoke(builderObj, valueParam);
								}
							} 
						}
		
						Object messageObj = builderObj.getClass().getMethod("create").invoke(builderObj);
						Object serdeObj = messageTypeClass.getMethod("serde").invoke(null);
						
						// get payload size
						int payloadSize = (Integer)messageTypeClass.getMethod("getByteSize").invoke(messageObj);
						// setup for serialization
						ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[payloadSize]).order(ByteOrder.LITTLE_ENDIAN);
						// serialize 
						Method encodeMethod = serdeObj.getClass().getMethod("encode", ByteBuffer.class, messageTypeClass);
						encodeMethod.setAccessible(true);
						encodeMethod.invoke(serdeObj, byteBuffer, messageObj);
						// extract as byte[]
						payloadBytes = byteBuffer.array();	
						break;
					}
				}				
			} catch (Exception e){
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}

		return payloadBytes;
	}

	/**
	 * @param newValue
	 */
	private void setMessage(Object newValue) {
		if(General.ZclReadAttributesResponse.class.getSimpleName().equalsIgnoreCase((String)newValue)){
			messageId = General.ZclReadAttributesResponse.ID;
			responseType = ResponseType.READ;
			return;
		}
		if(General.ZclReportAttributes.class.getSimpleName().equalsIgnoreCase((String)newValue)){
			messageId = General.ZclReportAttributes.ID;
			responseType = ResponseType.REPORT;
			return;
		}	
		if(General.ZclWriteAttributesResponse.class.getSimpleName().equalsIgnoreCase((String)newValue)){
			messageId = General.ZclWriteAttributesResponse.ID;
			responseType = ResponseType.WRITE_RESPONSE;
			return;
		}
		
		if(null == GeneralBinding.Descriptor.instance().getMessageByName(((String)newValue).toLowerCase())) {
			// non general message type ( iaszone.zonestatuschangenotification etc)
			flags = 3;
		}
		
		for(Class<?> c : binding.getClass().getDeclaredClasses()){
			if(c.getName().endsWith("Descriptor")){
				try {	
					// get the inner class Descriptor from the Zigbee binding (GeneralBinding, IasZoneBinding .. etc)
					Object descriptorObj = c.getMethod("instance")
							.invoke(binding);
					
					// get the inner class MessageDescriptor from the binding's Descriptor instance
					Object messageDescriptorObj = descriptorObj
							.getClass()
							.getMethod("getMessageByName", String.class)
							.invoke(descriptorObj, newValue.toString().toLowerCase());
					messageId = ((MessageDescriptor)messageDescriptorObj).getIdAsShort();
					messageType = (String)newValue;
					return;
				} catch (Exception e) {
					break;
				}
			}
		}
	}
		
	private enum ResponseType {
		REPORT, READ, WRITE_RESPONSE
	}

	@Override
	public String toString() {
		return "ZigbeeCommandBuilder [clusterId=" + clusterId + ", flags=" + flags + ", messageId=" + messageId
				+ ", clusterName=" + clusterName + ", messageType=" + messageType + "]";
	}

}


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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.beanutils.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.iris.device.attributes.AttributeKey;
import com.iris.device.attributes.AttributeMap;
import com.iris.driver.groovy.GroovyProtocolPluginModule;
import com.iris.driver.groovy.zigbee.ClusterBinding;
import com.iris.driver.groovy.zigbee.ClusterDescriptor;
import com.iris.driver.groovy.zigbee.MessageDecoder;
import com.iris.driver.groovy.zigbee.MessageDescriptor;
import com.iris.driver.groovy.zigbee.cluster.zcl.GeneralBinding;
import com.iris.driver.groovy.zigbee.cluster.zcl.ZigbeeZclClusters;
import com.iris.driver.unit.cucumber.AbstractDriverTestCase;
import com.iris.driver.unit.cucumber.CommandBuilder;
import com.iris.driver.unit.cucumber.DriverTestContext;
import com.iris.messages.model.Device;
import com.iris.protocol.Protocol;
import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.constants.ZigbeeConstants;
import com.iris.protocol.zigbee.ZclData;
import com.iris.protocol.zigbee.ZigbeeProtocol;
import com.iris.protocol.zigbee.msg.ZigbeeMessage;
import com.iris.protocol.zigbee.msg.ZigbeeMessage.SetOfflineTimeout;
import com.iris.protocol.zigbee.msg.ZigbeeMessage.Zcl;
import com.iris.protocol.zigbee.zcl.Constants;
import com.iris.protocol.zigbee.zcl.General.ZclAttributeRecord;
import com.iris.protocol.zigbee.zcl.General.ZclAttributeReportingConfigurationRecord;
import com.iris.protocol.zigbee.zcl.General.ZclConfigureReporting;
import com.iris.protocol.zigbee.zcl.General.ZclReadAttributes;
import com.iris.protocol.zigbee.zcl.General.ZclReadReportingConfiguration;
import com.iris.protocol.zigbee.zcl.General.ZclWriteAttributeRecord;
import com.iris.protocol.zigbee.zcl.General.ZclWriteAttributes;
import com.iris.test.Modules;


@Modules({ GroovyProtocolPluginModule.class })
public class ZigbeeDriverTestCase extends AbstractDriverTestCase implements DriverTestContext<ZigbeeMessage.Protocol> {
	 
	private final Logger logger = LoggerFactory.getLogger(ZigbeeDriverTestCase.class) ; 	
	
	private Zcl zcl;
	
	@Override
    public Protocol<ZigbeeMessage.Protocol> getProtocol() {
        return ZigbeeProtocol.INSTANCE;
    }

   @Override
   public void initializeDriver(String driverScriptResource) throws Exception {
      super.initializeDriver(driverScriptResource);

      // setup a mock hub ID for IAS closure test case.
      addProtocolAttribute(AttributeKey.createMapOf(ZigbeeConstants.ATTR_HUB, Object.class), ImmutableMap.of(ZigbeeProtocol.ATTR_HUB_EUI64, 1L));
   }

    @Override
    public void validateProtocolMessage(ProtocolMessage protocolMsg, String type, String subType){
		ZigbeeMessage.Protocol protocol = (ZigbeeMessage.Protocol) protocolMsg.getValue();
		byte[] protocolPayload = protocol.getPayload();
		logger.debug("\n protocol = "+ protocol);
		int zigbeeType = protocol.getType();
		if(zigbeeType == 2) {
			zcl = ZigbeeMessage.Zcl.serde().fromBytes(ByteOrder.LITTLE_ENDIAN, protocolPayload);
			assert zcl != null: "Unable to parse Zigbee ZCL from queue";
			int cluster = zcl.getClusterId();
			logger.trace("\n Got the message :"+zcl+", cluster "+cluster);
			// assert the message
			// =============================================
			// assert the cluster
			// =============================================
			try {
				logger.trace("Comparing decimal "+type+" to "+cluster );
				assert cluster == Integer.parseInt(type): "Expected cluster "+type+", instead found "+cluster;
			} catch (NumberFormatException e) {
				ClusterDescriptor descriptor = ZigbeeZclClusters.descriptorsByName.get(type.toLowerCase());
				logger.trace("\n the descriptor is :"+ descriptor);
				if (descriptor != null) {
					assert cluster == descriptor.getId(): "Expected cluster "+type+", instead found "+cluster;
				} else if(type.startsWith("0x")) {
					// 0x0000 Hex
					logger.trace("Parsing type "+type);
					int expectedCluster = Integer.parseInt(type.substring(2), 16); 
					assert cluster == expectedCluster: "Expected cluster "+expectedCluster+", instead found "+cluster;
				} else {
					assert false: "Unable to validate cluster "+cluster+" with type "+type;
				}
			}
			// =============================================
			// assert the message
			// =============================================
			int messageId = zcl.rawZclMessageId();
			if (messageId < 0) messageId +=256;
			try {
				logger.trace("Comparing decimal "+subType+" to "+messageId );
				assert messageId == Integer.parseInt(subType): "Expected cluster "+subType+", instead found "+messageId;
			} catch (NumberFormatException nfe) {
				if(subType.startsWith("0x")) {
				assert messageId == Short.parseShort(subType.substring(2), 16): "Expected Hex Message "+ subType +", instead found " + messageId;
				} else if (1 == (zcl.getFlags() & 0x01)){
					logger.debug("This is a cluster Specific message id:"+messageId );
					ClusterDescriptor clusterDescriptor = ZigbeeZclClusters.descriptorsByName.get(type.toLowerCase());
					logger.debug("DescriptorId "+clusterDescriptor+"matches cluster"+zcl.rawClusterId());
					MessageDescriptor messageDescriptor = clusterDescriptor.getMessageByName(subType.toLowerCase());
					logger.debug("\n message descriptor ="+ messageDescriptor);
					if (messageDescriptor != null) {
						assert zcl.rawZclMessageId() == messageDescriptor.getIdAsShort():"Expected Message "+ subType +", instead found" + messageId;
					} else {
						assert false: "Unable to validate "+messageId+" with subType "+subType;
					}
								
					// de-serialize the message to theCommand binding
					MessageDecoder clusterDecoder = ZigbeeZclClusters.decodersById.get(zcl.rawClusterId());
					if(clusterDecoder != null) {
						// deserialze the message to facilitate property level assertion
						Object theCommand = clusterDecoder.decodeClientMessage(zcl.rawZclMessageId(), zcl.getPayload(), ByteOrder.LITTLE_ENDIAN);
					}
					
				}else {
					logger.debug("This is a general message of raw type[{}]", zcl.rawZclMessageId() );
					// de-serialize the message to theCommand binding
					Object theCommand = GeneralBinding.Decoder.instance().decodeGeneralMessage(zcl.getZclMessageId(), zcl.getPayload(), ByteOrder.LITTLE_ENDIAN);				
					String commandName = getCommandName(theCommand);
					logger.debug ("Determined Command Name is {} compating to {}", commandName, subType);
					assert commandName.equalsIgnoreCase(subType):"Expected message "+subType+" and instead got message "+ commandName;
				}
			}
		} else if(zigbeeType == 4) {
			// CONTROL
			ZigbeeMessage.Control zcontrol = ZigbeeProtocol.getControlMessage(protocol); 		
			// assert the message
			assert zcontrol != null: "Unable to parse Zigbee Control message from queue";
			logger.trace("\n\nFound a control Message:"+zcontrol+"\n\n");
		} else {
		    assert false: "Unable to process Zigbee message type:"+ zigbeeType;
		}
    
    }

   private String getCommandName(Object message){
	   if(message.getClass() == ZclReadAttributes.class ){
		   return ((ZclReadAttributes)message).toString().substring(0,17);
	   }else if (message.getClass() ==ZclWriteAttributes.class){
		   return((ZclWriteAttributes)message).toString().substring(0,18);
	   }else if (message.getClass() == ZclReadReportingConfiguration.class){
		   return((ZclReadReportingConfiguration)message).toString().substring(0, 29);
	   }else if (message.getClass() == ZclConfigureReporting.class){
		   return((ZclConfigureReporting)message).toString().substring(0,21);
	   }else{
		  return message.toString();
	   }

   }
    
    public void checkTimeoutSeconds(ProtocolMessage message, Integer expectedTimeoutSeconds)throws java.io.IOException{
		ZigbeeMessage.Protocol protocol = (ZigbeeMessage.Protocol) message.getValue();
		assert ZigbeeMessage.SetOfflineTimeout.ID == protocol.getType() : "Not a timeout message";
		byte[] protocolPayload = protocol.getPayload();		
		logger.debug ("Zigbee timeout message"+ protocol);
		SetOfflineTimeout ptout = ZigbeeMessage.SetOfflineTimeout.serde().fromBytes(ByteOrder.LITTLE_ENDIAN, protocolPayload);
		logger.debug("\n Got the message :"+ptout);
		if ( null != expectedTimeoutSeconds){
 		assert expectedTimeoutSeconds == ptout.getSeconds(): "Expected "+expectedTimeoutSeconds+ " seconds and instead got "+ptout.getSeconds();
		}
	}
    /**
     * 
     * @param message a Protocol Message that containss a ZclReadAttributes
     * @param attribute STRING name of attributein ALL_CAPS to see see if contained in message
     */
    public void checkReadAttributes (String attribute){
		Object theRawCommand = GeneralBinding.Decoder.instance().decodeGeneralMessage(zcl.getZclMessageId(), zcl.getPayload(), ByteOrder.LITTLE_ENDIAN);				
		logger.debug("the raw command class is {}", theRawCommand.getClass());
		short actual[] = null;
		if (theRawCommand.getClass() == ZclReadAttributes.class){
	    	ZclReadAttributes theCommand = (ZclReadAttributes)theRawCommand;
	    	actual = theCommand.getAttributes();
		} else if (theRawCommand.getClass() == ZclReadReportingConfiguration.class){
			ZclReadReportingConfiguration theCommand = (ZclReadReportingConfiguration)theRawCommand;
			logger.trace("Got the command: {}", theCommand);
	    	ZclAttributeRecord[] records = theCommand.getAttributes();
	    	actual= new short[records.length];
	    	int count = 0;
	    	for (ZclAttributeRecord record : records){
	    		logger.trace("Added attribute [{}] to list",record);
	    		actual[count++] = record.rawAttributeIdentifier();
	    	}
		} else if (theRawCommand.getClass() == ZclWriteAttributes.class){
			ZclWriteAttributes theCommand = (ZclWriteAttributes)theRawCommand;
			logger.trace("Got the command: {}", theCommand);
	    	ZclWriteAttributeRecord[] records = theCommand.getAttributes();
	    	actual= new short[records.length];
	    	int count = 0;
	    	for (ZclWriteAttributeRecord record : records){
	    		logger.trace("Added attribute [{}] to list",record);
	    		actual[count++] = record.rawAttributeIdentifier();
	    	}
		} else if (theRawCommand.getClass() == ZclConfigureReporting.class){
			ZclConfigureReporting theCommand = (ZclConfigureReporting)theRawCommand;
			logger.trace("Got the command: {}", theCommand);
	    	ZclAttributeReportingConfigurationRecord[] records = theCommand.getAttributes();
	    	actual= new short[records.length];
	    	int count = 0;
	    	for (ZclAttributeReportingConfigurationRecord record : records){
	    		logger.trace("Added attribute [{}] to list",record);
	    		actual[count++] = record.rawAttributeIdentifier();
	    	}
		} 
		Short expected=null;
		try {
			expected = Short.parseShort(attribute);
		} catch(NumberFormatException e) {
			try { 
				expected = Short.parseShort(attribute.substring(2), 16);
			} catch (NumberFormatException nfe){
				ClusterBinding binding = TestZigbeeZclClusters.bindingsByID.get((short)zcl.getClusterId() );
				expected = (Short)binding.getProperty(attribute);
			}
		}
		logger.trace("checking "+ attribute + " with a value of "+expected);
		assert null != actual:"Unable to find attributes in "+ theRawCommand;
		Short [] attributes = new Short[actual.length];
		for( int i = 0; i < actual.length; i++) {
				attributes[i] = new Short(actual[i]);
		}
		assert  Arrays.asList(attributes).contains(expected): "Unable to find attribute: "+attribute+" with expected value: "+expected+" in response: "+theRawCommand;
    }

	/**
	 * @param  message a ProtocolMessage that contains a ZCL message to parse 
	 * 
	 * @return Zcl Zigbee Cluster Library Message
	 */
	private Zcl getZclFromProtocolMessage(ProtocolMessage message) {
		ZigbeeMessage.Protocol protocol = (ZigbeeMessage.Protocol) message.getValue();
		byte[] protocolPayload = protocol.getPayload();
		logger.trace("\n protocol = "+ protocol);
		Zcl zcl = ZigbeeMessage.Zcl.serde().fromBytes(ByteOrder.LITTLE_ENDIAN, protocolPayload);
		logger.trace("\n Got the message :"+zcl);
		return zcl;
	}
    /**
     * 
     * @param message a ProtocolMessage that contains a single Write Attributes
S    * @param attribute name in ALL_CAPS to check if it was written
     * @param value  String Name or Decimal value that was written.
     */
    public void checkWriteAttribute(String attribute, String value){
    	ZclWriteAttributes theCommand = (ZclWriteAttributes)GeneralBinding.Decoder.instance().decodeGeneralMessage(zcl.getZclMessageId(), zcl.getPayload(), ByteOrder.LITTLE_ENDIAN);
		logger.trace("\n Got the Command "+ theCommand);
		ZclWriteAttributeRecord record[] = theCommand.getAttributes();
		logger.trace("\n Attribute ="+ record[0]);
		int actualAttribute = record[0].getAttributeIdentifier();
		logger.trace("\n checking for attribute "+actualAttribute);
		ClusterBinding binding = TestZigbeeZclClusters.bindingsByID.get((short)zcl.getClusterId() );
		Short expectedAttribute = (Short)binding.getProperty(attribute);
		assert expectedAttribute == actualAttribute;
		ZclData data = record[0].getAttributeData();
		logger.debug ("\n ZclData = "+data);
		checkZclData(data, value, binding);
}

    /**
     * 
     * @param data a ZCL data object that consists of a data type, and an object that holds the actual data
     * @param value the String containing the value we want to compare to the ZCL, either decimal or Constant
     * @param binding the Zcl Cluster, for dereferencing strings for Enums, Constants
     */
    public void checkZclData(ZclData data, String value, ClusterBinding binding){
      	int type = data.getDataType();
		if (Byte.MAX_VALUE < type){
			type = type - 256;
		}
		logger.trace("Looking for data type: "+type);
    	switch(type){
    		case Constants.ZB_TYPE_SIGNED_16BIT:
    			Short expectedShort = Short.parseShort(value);
    			Short actualShort = (Short)data.getDataValue();
				assert  0 == Short.compare(expectedShort, actualShort): "Was expecting data "+expectedShort+", and found: "+actualShort;    			
    			break;
    		case Constants.ZB_TYPE_8BIT:
    		case Constants.ZB_TYPE_BITMAP_8BIT:
    		case Constants.ZB_TYPE_ENUM_8BIT:
    			 Byte actualByte = (Byte)data.getDataValue();
    			 Byte expectedByte;
    			 try {
    				 expectedByte = Byte.parseByte(value);
    			 } catch (NumberFormatException e) {
    				 expectedByte = (Byte)binding.getProperty(value);
    			 }
 				assert expectedByte == actualByte : "Was expecting data "+expectedByte+", and found: "+actualByte;    			
 				break;
    		case Constants.ZB_TYPE_IEEE:
    	    	Long expectedData = Long.parseUnsignedLong(value);
    			long actualData=0;
    		  	final ByteBuffer bb = ByteBuffer.wrap((byte[])data.getDataValue());
    	    	bb.order(ByteOrder.LITTLE_ENDIAN);
    			for (int i = 0; i< bb.capacity(); i++){
    				actualData +=(bb.get(i) & (byte)0xFF);     				
    			}
				assert 0 == Long.compareUnsigned(expectedData, actualData): "Was expecting data "+expectedData+", and found: "+actualData;    			
    			break;
    		default:
				assert false: "Unexpected data type "+ data.getDataType();
    			break;
    	}
    }
 
 
    	public void checkSentParameter( String parameterName, String parameterValue){
			MessageDecoder clusterDecoder = ZigbeeZclClusters.decodersById.get(zcl.rawClusterId());
			Object theCommand = null;
			if(clusterDecoder != null) {
				// deserialze the message to facilitate property level assertion
				theCommand = clusterDecoder.decodeClientMessage(zcl.rawZclMessageId(), zcl.getPayload(), ByteOrder.LITTLE_ENDIAN);
			}
			assert theCommand != null: "Zcl message did not deserialize";
    		logger.trace("\n Got the Command "+ theCommand);
			//Get a number for parameterValue
			int expected = -1000000;
			try {
				expected = parameterValue.startsWith("0x")
				? Integer.parseInt(parameterValue.substring(2), 16)
				: Integer.parseInt(parameterValue);
			} catch (Exception e) {
				logger.trace(e.getMessage());
			}
			
			Object actual = null;
			try{
			actual = PropertyUtils.getProperty(theCommand, parameterName);
			} catch (Exception e){
				logger.trace(e.getMessage());
			}
			logger.trace("\n checking property "+ parameterName +" for value "+actual);
			logger.trace("\n actual property class is "+ actual.getClass());
			if (actual.getClass() == Integer.class) {
					assert expected == (int)actual;
					return;
			}
					assert false: "parameter "+ parameterName + " has unsupported value type "+actual.getClass();
    	}
    	/**
    	 * This validates generic payloads, can be used for MSP or commands not supported more specifically.
    	 * 
    	 * @param zcl
    	 * @param actualStrings
    	 */
    	public void validatePayload( String[] actualStrings){
    		try {
    			byte[] actual = zcl.getPayload(); 
    			byte[] expected = ZigbeeUtil.parsePayload(actualStrings, zcl.getClusterId() );
    			assert actual.length == expected.length: "Expected an array length "+ expected.length + ", instead found length "+actual.length;
    			for (int i = 0; i< actual.length; i++){
    				assert actual[i] == expected[i]: "Byte "+i+ " expecting "+expected[i]+", instead found "+actual[i];
    			}
    		} catch (Exception e){
    			assert false:"Exception validating payload:"+e.getMessage();
    		}
    	}

   private final static String PROTOCOL_ATTRIBUTES_ENDPOINTS_KEY = "hubzbprofile:endpoints";
   private final static String PROTOCOL_ATTRIBUTES_ENDPOINT_ID_KEY = "hubzbendpoint:id";
   private final static String PROTOCOL_ATTRIBUTES_PROFILE_ID_KEY = "hubzbprofile:id";

   public void setEndpoint(int endpointId){
      Device device = (Device) this.getDevice();
      AttributeMap map = device.getProtocolAttributes();

      AttributeKey<Set<Object>> attrKey = AttributeKey.createSetOf(ZigbeeProtocol.ATTR_PROFILES, Object.class);
      Set<Object> attrValue = new HashSet<Object>();
      Map<String, Object> profile = new HashMap<String, Object>();
      List<Object> endpoints = new ArrayList<Object>();

      Map<String, Object> endpointIdEntry = new HashMap<String, Object>();
      endpointIdEntry.put(PROTOCOL_ATTRIBUTES_ENDPOINT_ID_KEY, endpointId);
      endpoints.add(endpointIdEntry);
      profile.put(PROTOCOL_ATTRIBUTES_ENDPOINTS_KEY, endpoints);
      profile.put(PROTOCOL_ATTRIBUTES_PROFILE_ID_KEY, endpointId);
      attrValue.add(profile);
      map.set(attrKey, attrValue);
      device.setProtocolAttributes(map);
   }

	@Override
    public CommandBuilder getCommandBuilder(){
    	return new ZigbeeCommandBuilder(this);
    }
}
    


package com.iris.driver.unit

import java.nio.ByteOrder
import java.util.Map.Entry;

import org.apache.commons.lang3.mutable.MutableByte;
import org.easymock.EasyMock;
import org.junit.After;

import com.iris.capability.attribute.Attributes
import com.iris.capability.definition.CapabilityDefinition;
import com.iris.device.attributes.AttributeKey
import com.iris.driver.unit.cucumber.groovy.DeviceCommandBuilder;
import com.iris.driver.unit.cucumber.groovy.DeviceMessageValidator;
import com.iris.driver.unit.cucumber.groovy.DriverEventBuilder;
import com.iris.driver.unit.cucumber.MockGroovyDriverModule.CapturedScheduledEvent;
import com.iris.driver.unit.cucumber.ipcd.IpcdDriverTestCase
import com.iris.driver.unit.cucumber.CommandBuilder
import com.iris.driver.unit.cucumber.zb.ZigbeeDriverTestCase
import com.iris.driver.unit.cucumber.zw.ZWaveDriverTestCase
import com.iris.io.json.JSON
import com.iris.messages.capability.ClasspathDefinitionRegistry;
import com.iris.messages.model.Device
import com.iris.protocol.zwave.Protocol

import io.netty.buffer.Unpooled;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

this.metaClass.mixin(cucumber.api.groovy.Hooks)
this.metaClass.mixin(cucumber.api.groovy.EN)

final class Constant {
	enum Direction { 	IN, OUT		}
}

class TestEnvironment {
	private final Logger logger = LoggerFactory.getLogger(TestEnvironment.class) ; 	
}

final int recentTestDelay = 20000;

World { 
	return new TestEnvironment() 
}

Before("@Zigbee") {
    context = new ZigbeeDriverTestCase()
    context.setUp()
}

Before("@ZWave") {
    context = new ZWaveDriverTestCase()
    context.setUp()
}

Before("@IPCD"){
	context = new IpcdDriverTestCase()
	context.setUp()
}

After {
    if (context == null) {
        throw new IllegalStateException("This feature is not tagged with a protocol. Please add '@ZWave', '@Zigbee' or @IPCD to the first line of the feature file.")
    }

    context.tearDown()
}


/* ====================================================================================
 * managing capability attribute setup
 * ====================================================================================
 */

/**
 * Initializes a device driver in the test runtime environment.
 * Accepts:
 * 		The name of a classpath driver script.
 *
 * EXAMPLE:
 * 		Given the ZWaveSwitch.driver has been initialized
 * 		Given the ZWave_Fibaro_MotionSensor.driver has been initialized
 */
Given(~/^the (.+) has been initialized$/) { String driverScriptResource ->
    context.initializeDriver(driverScriptResource)
}


/**
 * Initializes capability attributes doe a device driver in the test runtime environment with the 
 * 
 *
 * EXAMPLE:
 * 		Given the capability devpow:battery is 100
 *      Given the driver attribute devconn:state is ONLINE
 */
Given(~/^the capability (\w+):(\w+) is (.+)$/) { namespace, attributeName, attributeValue ->
	// lookup for the attribute definition
	logger.trace(" initializing "+namespace+":"+attributeName + " to "+attributeValue);
	CapabilityDefinition capability = ClasspathDefinitionRegistry.instance().getCapability(namespace);
	def isInstance = attributeName.contains(".")
	def attributeDefinition = capability.attributes.find { 
		it.name == attributeName || (isInstance && it.name == attributeName.substring(0, attributeName.indexOf(".")))  
	}
	def attributeType = attributeDefinition.type
	
	// a closure to parse the attribute key
	def parseAttributeKey = { javaType ->
		if(isInstance) {
			def index = attributeName.indexOf(".")+1;
			Attributes.createKey(namespace, attributeName.substring(0, index-1), javaType)
			.instance(attributeName.substring(index))
		} else {
			AttributeKey.createType(namespace + ":" + attributeName, javaType)
		}
	}
	
	// setting the attribute value
	if(attributeType.javaType == String) {	
		if ('""' == attributeValue){
			context.getDeviceDriverContext().setAttributeValue(parseAttributeKey(String),"")
		}	else {
		context.getDeviceDriverContext().setAttributeValue(parseAttributeKey(String),attributeValue)
		}
	} else
	if(attributeType.javaType == Boolean) {
		context.getDeviceDriverContext().setAttributeValue(parseAttributeKey(Boolean),Boolean.parseBoolean(attributeValue))
	} else
	if(attributeType.javaType == Double) {
		context.getDeviceDriverContext().setAttributeValue(parseAttributeKey(Double),Double.parseDouble(attributeValue))
	} else
	if(attributeType.javaType == Integer) {
		context.getDeviceDriverContext().setAttributeValue(parseAttributeKey(Integer),Integer.parseInt(attributeValue))
	} else
	if(attributeType.isCollection()) {
		if(attributeType.rawType.javaType == Map) {
			Map<String, Object> valueMap = (Map<String, Object>)JSON.fromJson(attributeValue, Map.class)
			context.getDeviceDriverContext().setAttributeValue(
				parseAttributeKey(attributeType.javaType),
				valueMap)
		}
		else {
			List<Object> values = (List<Object>)JSON.fromJson(attributeValue, List.class)
			context.getDeviceDriverContext().setAttributeValue(
				parseAttributeKey(attributeType.javaType),
				values)
		}
	} else
	if (attributeType.javaType == Date){
		 context.getDeviceDriverContext().setAttributeValue(parseAttributeKey(Date), new Date(attributeValue))
	} else{	 
		 logger.warn("Unregonized attribute type " + attributeType.javaType);
	}
}

/**
 * Initializes a driver attribute to a value.  This will NOT produce a value change.
 * 
 *
 * EXAMPLE:
 *      Given the driver attribute devconn:state is ONLINE
 */
Given(~/^the driver attribute (.+):(.+) is (.+)$/) { namespace, attributeName, attributeValue ->
	// lookup for the attribute definition
	CapabilityDefinition capability = ClasspathDefinitionRegistry.instance().getCapability(namespace);
	def isInstance = attributeName.contains(".")
	def attributeDefinition = capability.attributes.find { 
		it.name == attributeName || (isInstance && it.name == attributeName.substring(0, attributeName.indexOf(".")))  
	}
	def attributeType = attributeDefinition.type
	
	// a closure to parse the attribute key
	def parseAttributeKey = { javaType ->
		if(isInstance) {
			def index = attributeName.indexOf(".")+1;
			Attributes.createKey(namespace, attributeName.substring(0, index-1), javaType)
			.instance(attributeName.substring(index))
		} else {
			AttributeKey.createType(namespace + ":" + attributeName, javaType)
		}
	}
	
	// setting the attribute value
	if(attributeType.isCollection()) {
		if(attributeType.rawType.javaType == Map) {
			Map<String, Object> valueMap = (Map<String, Object>)JSON.fromJson(attributeValue, Map.class)
			context.getDeviceDriverContext().setAttributeValue(
				parseAttributeKey(attributeType.javaType),
				valueMap)
		}
		else {
			List<Object> values = (List<Object>)JSON.fromJson(attributeValue, List.class)
			context.getDeviceDriverContext().setAttributeValue(
				parseAttributeKey(attributeType.javaType),
				values)
		}
	}
	else {
		context.getDeviceDriverContext().setAttributeValue(parseAttributeKey(attributeType.javaType),attributeValue)
	}
	context.getDeviceDriverContext().clearDirty() 
}

/**
 * Initialize groovy binding "vars" use in the driver code
 * 
 * EXAMPLE:
 *  Given the driver variable test is hello
 */
Given(~/^the driver variable (.+) is (.+)$/) { String variableName, String value ->
	if(value.isNumber()) {
		if(value.isInteger()) {
			context.getDeviceDriverContext().setVariable(variableName, Integer.parseInt(value))
		} else
		if(value.isLong()) {
			context.getDeviceDriverContext().setVariable(variableName, Long.parseLong(value))
		} else
		if(value.isDouble()) {
			context.getDeviceDriverContext().setVariable(variableName, Double.parseDouble(value))
		} else
		if(value.isFloat()) {
			context.getDeviceDriverContext().setVariable(variableName, Float.parseFloat(value))	
		}
	} else if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
		context.getDeviceDriverContext().setVariable(variableName, Boolean.valueOf(value))
	} else  if ("null".equalsIgnoreCase(value)){
		context.getDeviceDriverContext().setVariable(variableName, null)
	} else if("now".equalsIgnoreCase(value)) {	
			context.getDeviceDriverContext().setVariable(variableName, new Date( System.currentTimeMillis()))
	} else {
		try {
			def v = JSON.fromJson(value, Object.class);
			context.getDeviceDriverContext().setVariable(variableName, v);
			logger.trace("Set variable "+ variableName + " to " +v);
			return;
		}
		catch(e) {
			// not json
		}

			context.getDeviceDriverContext().setVariable(variableName, value)
	}	
	
}

/**
 * Initialize groovy binding "vars" use in the driver code for time variables,
 *
 * EXAMPLE:
 *  Given the time driver variable test is 60 seconds ago
 *  
 */
Given(~/^the (time|millisecond) driver variable (.+) is (\d+) (\w+) (?:ago)?$/) { String type, String variableName, int delay, String units  ->
	long shift = convertToMs(delay, units)
	long now = System.currentTimeMillis();
	long newValue = System.currentTimeMillis()-shift
	if ("time" == type){
		context.getDeviceDriverContext().setVariable(variableName, new Date(newValue))
	} else {
		context.getDeviceDriverContext().setVariable(variableName, newValue)
	}
}
/**
 * The Zigbee cluster is access via Zigbee.endpoint. 
 * This step is to setup the endpoint so that Zigbee drivers can be loaded.
 */
Given(~/^the device has endpoint (.+)$/) {int endpointId ->
	context.setEndpoint(endpointId);
} 

/**
 * TODO docment me
 */
Given(~/^the device has tag (.+)$/) {String tag ->
	context.device.tags.add(tag)
}

Given(~/^the pin (\w*) is valid$/) { String pin ->
	String actor = UUID.randomUUID().toString()
	EasyMock.reset(context.pinManager)
	EasyMock
		.expect(context.pinManager.validatePin(pin))
		.andReturn(actor)
		.anyTimes()
	context.pinManager.setActor(actor)
	EasyMock.expectLastCall().anyTimes()
	EasyMock.replay(context.pinManager)
}

Given(~/^the pin (\w+) is invalid$/) { String pin ->
	EasyMock.reset(context.pinManager)
	EasyMock
		.expect(context.pinManager.validatePin(pin))
		.andReturn(null)
	EasyMock.replay(context.pinManager)
}

/* ====================================================================================
 * Driver life-cycle
 * ====================================================================================
 */
/**
 * This allows to execute the device life-cycle closure.
 * 
 * added - onAdded
 * connected - onConnected
 * disconnected - onDisconnected
 * removed - onRemoved.
 * 
 * STEP - EXECUTION_LIFECYCLE
 */
When(~/^the device is (added|connected|disconnected|removed|upgraded)$/) { String action->
	switch(action){
		case "added":
			new DriverEventBuilder(context).deviceAssociated().buildAndSend()
			break;
		case "connected":
			new DriverEventBuilder(context).deviceConnected().buildAndSend()
			break;
		case "disconnected":
			new DriverEventBuilder(context).deviceDisconnected().buildAndSend()
			break;
		case "removed":
			new DriverEventBuilder(context).deviceDisassociated().buildAndSend()
			break;
		case "upgraded":
			new DriverEventBuilder(context).driverUpgraded().buildAndSend()
			break;
		default:
			break;
	};
}

/**
 * Emulates a device connecting to the platform.
 * 
 * This covers onConnect closure.
 *
 * EXAMPLE:
 * 		When the device connects to the platform
 * 		When a binary switch connects to the platform
 * 		When a new motion sensor connects to the platform
 */
// Deprecated - TBD :: Replace with EXECUTION_LIFECYCLE
When(~/^(?:a|the) .+ connects to the platform$/) {
    ->
    new DriverEventBuilder(context).deviceConnected().buildAndSend()
}

/**
 * Emulates a device disconnecting from the platform
 *
 * EXAMPLE:
 * 		When the device disconnects from the platform
 * 		When a binary switch disconnects from the platform
 * 		When a new motion sensor disconnects from the platform
 */
// Deprecated - TBD :: Replace with EXECUTION_LIFECYCLE
When(~/^(?:a|the) .+ disconnects from the platform$/) {
    ->
    new DriverEventBuilder(context).deviceDisconnected().buildAndSend()
}

/**
 * Emulates a device disassociating from the platform
 *
 * EXAMPLE:
 * 		When the device disassociates from the platform
 * 		When a binary switch disassociates from the platform
 * 		When a new motion sensor disassociates from the platform
 */
// Deprecated - TBD :: Replace with EXECUTION_LIFECYCLE
When(~/^(?:a|the) .+ disassociates from the platform$/) {
    ->
    new DriverEventBuilder(context).deviceDisassociated().buildAndSend()
}

/**
 * Emulates a trigger to execute a scheduled event
 *
 * EXAMPLE:
 * 		When event DelayedRead triggers 
 */
When(~/^(?:the )?event (.+) triggers?(?: with(.+))?$/) { String eventName, String json ->
	java.util.LinkedHashMap data = json != null ? JSON.fromJson(json, Object.class) : null;
	 new DriverEventBuilder(context).scheduledNowEvent(eventName, data).buildAndSend()
}

/**
 * Emulates a client (or other Iris entity) placing a device command message with no arguments on the platform bus.
 * Accepts:
 * 		The name of the command in the form {namespace}:{command-name}.
 *
 * EXAMPLE:
 * 		When a get capabilities command is placed on the platform bus
 * 		When a swit:switch command is placed on the platform bus
 */
// deprecated - TBD :: Replace with SETUP_CAPABILITY_METHOD
When(~/^a (.+) command is placed on the platform bus$/) { String messageType ->
    new DeviceCommandBuilder(context).command(messageType).buildAndSend()
}


/**
 * Emulates a client (or other Iris entity) placing a device command message with one attribute/value on the platform bus.
 * Accepts:
 * 		The name of the command in the form {namespace}:{command-name}
 * 		The attribute namespace (see namespace definitions inside classes of com.iris.capability.models)
 * 		The attribute name (see name definitions inside classes of com.iris.capability.models)
 * 		The attribute value (specific to the attribute)
 *
 * EXAMPLE:
 * 		When a set attribute command with the value swit state ON is placed on the platform bus
 */
// deprecated - TBD :: Replace with SETUP_CAPABILITY_METHOD
When(~/^a (.+) command with the value of (\S+) (\S+) is placed on the platform bus$/) { messageType, attribute, value ->
    new DeviceCommandBuilder(context).command(messageType).withStringAttribute(attribute, value).buildAndSend()
}

/**
 * Emulates a client (or other Iris entity) placing a device command message with one attribute/value on the platform bus.
 * Accepts:
 * 		The name of the command in the form {namespace}:{command-name}
 * 		The attribute namespace (see namespace definitions inside classes of com.iris.capability.models)
 * 		The attribute name (see name definitions inside classes of com.iris.capability.models)
 * 		The quoted attribute value (to allow spaces, etc.)
 *
 * EXAMPLE:
 * 		When a base:SetAttributes command with the value of dev:name "Bob & Sue's Device" is placed on the platform bus
 */
When(~/^a (.+) command with the value of (\S+) "([^"]*)" is placed on the platform bus$/) { messageType, attribute, value ->
    new DeviceCommandBuilder(context).command(messageType).withStringAttribute(attribute, value).buildAndSend()
}


/**
 * Emulates a client (or other Iris entity) placing a device command message with one argument/value on the platform bus.
 * Accepts:
 * 		The name of the command in the form {namespace}:{command-name}
 * 		The argument name
 * 		The argument value (specific to the argument)
 *
 * EXAMPLE:
 * 		When a therm:SetIdealTemperature command with argument temperature of value 25.0 is placed on the platform bus
 */
When(~/^a (.+) command with argument (\S+) of value (\S+) is placed on the platform bus$/) { messageType, argument, value ->
    new DeviceCommandBuilder(context).command(messageType).withStringArgument(argument, value).buildAndSend()
}


/**
 * Setup a client to call capability method
 * 
 * CAVEAT:
 * This will not send, till step IN-FINAL. 
 * This is to allow setting of any number of capability attributes before sending.
 * 
 * EXAMPLE:
 * When the capability ident:Identify
 * When the capability base:GetAttributes
 * 
 * STEP : SETUP_CAPABILITY_METHOD
 */
When(~/^the capability method (.+)$/) { String messageType ->
	direction = Constant.Direction.IN
		if ("N/A" == messageType) return
	commandBuilder = new DeviceCommandBuilder(context).command(messageType)
}

/**
 * Setup a client capability method to be call with additional capabaility attributes.
 * 
 * PRE-CONDITION
 * Step CAPABILITY_METHOD need to takes place, before this step.
 * 
 * CAVEAT:
 * This will not send, till step IN-FINAL. 
 * This is to allow setting of any number of capability attributes before sending.*
 * 
 * STEP : SETUP_CAPABILITY_ATTRIBUTE 
 * 
 */
When(~/^with capability (.+) is (.+)$/) { String attribute, value ->
	if("N/A" == attribute) return	
	if(attribute.indexOf(":") > -1) {
		commandBuilder.withStringAttribute(attribute, value)
	} else {
		commandBuilder.withStringArgument(attribute,value)
	}
}


/**
 * Setup a client capability method to be call with additional capabaility attributes.
 * 
 * PRE-CONDITION
 * Step CAPABILITY_METHOD need to takes place, before this step.
 * 
 * CAVEAT:
 * This will not send, till step IN-FINAL. 
 * This is to allow setting of any number of capability attributes before sending.*
 * 
 * STEP : SETUP_CAPABILITY_ATTRIBUTE 
 * 
 */
When(~/^attribute (.+) is (.+)$/) { String attribute, value ->	
	if(attribute.indexOf(":") > -1) {
		commandBuilder.withAttribute(attribute, JSON.fromJson(value, Object.class))
	} else {
		commandBuilder.withArgument(attribute, JSON.fromJson(value, Object.class))
	}
}


/* ====================================================================================
 * Validating platform message 
 * ====================================================================================
 */
/**
 * Validates that the driver placed a device message on the platform bus.
 * Accepts:
 * 		The name of the message type (see the event definitions inside classes in com.iris.capability.models) in the form
 * 			{namespace}:{message-type}
 *
 * EXAMPLE:
 * 		The driver should place a base:ValueChange event on the platform bus
 * 
 * STEP : ASSERT_PLATFORM_MESSAGE_POLL
 */
Then(~/^the driver should( not)? place a (.+) message on the platform bus$/) { negative, messageName ->
	if (null == negative){
		theMessage = context.getPlatformBus().take().getValue()
		deviceMessageValidator = new DeviceMessageValidator(theMessage)
		assert new DeviceMessageValidator(theMessage).eventName().is(messageName)
	}
}	

/**
 * Validates that the message in context has an attribute whose value is a list of elements matching
 * an expected value.
 * PRE-CONDITION:
 * 		May only be used after a step that validates that the driver placed a device message on the platform bus
 * 		(see STEP : ASSERT_PLATFORM_MESSAGE_POLL)
 *
 * Accepts:
 * 		The name of the message attribute in form {namespace}:{name}
 * 		The expected list of values expressed as a valid Groovy array (i.e., [1, 2, 3], ['a', 'b', 'c'])
 *
 * EXAMPLE:
 * 		Then the message's base:caps attribute list should be ['dev', 'devadv', 'devpow']
 * 
 * STEP : ASSERT_CAPABILITY_ATTRIBUTE_1
 */
// deprecated - TBD :: Replace with ASSERT_CAPABILITY
Then(~/^the message\'?s (.+) attribute list should be (.+)$/) { attributeName, expectedValue ->
    assert new DeviceMessageValidator(theMessage)
	.attribute(attributeName)
	.hasSameElementsAs(Eval.me(expectedValue))
}

/**
 * Validates that the message in context has an attribute whose value matches an expected value.
 * PRE-CONDITION:
 * 		May only be used after a step that validates that the driver placed a device message on the platform bus
 *		(see STEP : PLATFORM_MESSAGE_POLL)
 *
 * Accepts:
 * 		The name of the attribute (see the event definitions inside classes in com.iris.capability.models) in the form
 * 			{namespace}:{event-name}
 * 		The expected list of values expressed as a valid Groovy array (i.e., [1, 2, 3], ['a', 'b', 'c'])
 *
 * EXAMPLE:
 * 		Then the message's devpow:source attribute should be LINE
 * 
 * STEP : ASSERT_CAPABILITY_ATTRIBUTE_2
 */
// deprecated - TBD :: Replace with ASSERT_CAPABILITY
Then(~/^the message\'?s (.+) attribute should be (.+)$/) { attributeName, expectedValue ->    
	assert new DeviceMessageValidator(theMessage)
	.attribute(attributeName)
	.is(expectedValue)
}

/**
 * Validates that the message in context has an attribute whose value is approximately an expected value.
 * PRE-CONDITION:
 * 		May only be used after a step that validates that the driver placed a device message on the platform bus
 *
 * Accepts:
 * 		The name of the attribute 
 *      The the delta value allowed
 * 		The expected approximate value
 *
 * EXAMPLE:
 * 		Then the message's idealTempSet attribute numeric value should be within delta 0.1 of 25.0
 * 
 * STEP : ASSERT_ATTRIBUTE_1
 */
Then(~/^the message\'?s (.+) attribute numeric value should be within delta ([\d.]+) of (.+)$/) { attributeName, delta, expectedValue ->
	float value
	float fDelta
	try{
		value = Float.parseFloat(expectedValue)
		fDelta = Float.parseFloat(delta)
	} catch (Exception e) {
		assert false, "Test Values must have a numeric value"
	}
	float actual
	try{
		def receivedValue = theMessage.getAttributes().get(attributeName)
		actual = (float) Double.valueOf(receivedValue)
	} catch (Exception e) {
		assert false, "Attribute must have a numeric value"
	}
	assert (Math.abs(actual - value) < fDelta)
}

/* ====================================================================================
 * ???
 * ====================================================================================
 */
/**
 * Validates that one or both busses have no messages remaining on them.
 *
 * Accepts:
 * 		The bus selection, one of: 'platform', 'protocol' or 'both'
 *
 * EXAMPLE:
 * 		Then the plafform bus should be empty
 * 		Then the protocol bus should be empty
 * 		Then both busses should be empty
 */
/* deprecated - TBD :: Replace with STEP : nothing else should happen*/
Then(~/^(?:the )?(platform|protocol|both) (?:bus|busses) should be empty$/) { busSelection ->
    if (busSelection == 'platform' || busSelection == 'both')
        assert context.getPlatformBus().getMessageQueue().peek() == null
    if (busSelection == 'protocol' || busSelection == 'both')
        assert context.getProtocolBus().getMessageQueue().peek() == null
}

/**
 * Validates that both busses have no messages remaining on them.
 * Same as explicitly stating that both busses should be empty, but more readable
 * Also confirms no additional scheduled events
 * 
 * EXAMPLE:
 * 		Then nothing should happen
 * 		Then nothing else should happen
 */
Then(~/^nothing (?:else )?should happen$/) { ->
		assert null == context.getPlatformBus().getMessageQueue().peek()
		assert null == context.getProtocolBus().getMessageQueue().peek()
		assert null == context.pollScheduledEvent()
}

/**
 * Validates that the driver set the specified platform attribute (i.e., platform state data) to the given value.
 * Accepts:
 * 		The name if the platform attribute in the form {namespace}:{attribute}
 * 		The expected value as a sequence of digits
 *
 * EXAMPLE:
 * 		Then the driver attribute devpow:battery should change to 33
 */
/* deprecated - TBD :: Replace with STEP : ASSERT_CAPABILITY*/
Then(~/^the (?:device|platform|driver) attribute (.+) should (?:change to|be|equal) (.+)$/) { String attributeName, String value ->
	def attributeDef = ClasspathDefinitionRegistry.instance().getAttribute(attributeName)
	if(attributeDef == null) {
		throw new IllegalArgumentException("Unrecognized attribute " + attributeName);
	}
	def attributeKey = AttributeKey.createType(attributeName, attributeDef.type.javaType)
	Object expectedValue;
	try {
		expectedValue = JSON.fromJson(value, Object.class)
	}
	catch(e) {
		// not JSON
		expectedValue = value;
	}
	expectedValue = attributeKey.coerceToValue(expectedValue).value
    assert expectedValue == context.getDeviceDriverContext().getAttributeValue(attributeKey)
}



/**
 * Validates the capability attribute value
 * 
 * STEP : ASSERT_CAPABILITY
 */
Then(~/^the capability (.+):(.+) should be (.+)$/) { namespace, attributeName, String expectedValue ->	

	def isInstance = attributeName.contains(".");
	
	def rawAttributeName = isInstance
	? attributeName.substring(0, attributeName.lastIndexOf("."))
	: attributeName

	def key = null
	if(isInstance) {
		def instanceId = attributeName.substring(attributeName.lastIndexOf(".")+1)
		key = context.deviceDriverContext.attributeKeys.find { it.namespace == namespace && it.id == rawAttributeName && it.instance == instanceId}
	} else {
		key = context.deviceDriverContext.attributeKeys.find { it.namespace == namespace && it.id == rawAttributeName }
	}
	
	context.deviceDriverContext.attributeKeys.find {
		if(isInstance) {
			return it.namespace == namespace && it.id == rawAttributeName && it.instance == rawAttributeName
		} else {
			return it.namespace == namespace && it.id == rawAttributeName
		}
	}
	
	if(key) {
		def actualAttribute = context.deviceDriverContext.getAttributeValue(key)
				if(key.type == Date.class){
						if ( "recent" == expectedValue) {
							assert actualAttribute.getTime() > System.currentTimeMillis() - recentTestDelay;
							}  else {
							assert actualAttribute == new Date (expectedValue)
							}
		} else if ('""' == expectedValue) {
		   assert "" == actualAttribute
		}else {
			try { 
				def expectedNumber = Double.valueOf(expectedValue);
				def actualNumber = Double.valueOf(actualAttribute);
				
				assert expectedNumber == actualNumber;
				
			} catch ( NumberFormatException e) {
				
			assert expectedValue == actualAttribute.toString()
			}
		}
	}else {
		assert false, "Unable to read capability ${namespace}:${attributeName}"
	}
	
}
/**
 * Validates the capability attribute value
 *
 * STEP : ASSERT_CAPABILITY
 */
Then(~/^the numeric capability (.+):(.+) should be within ([\d.]+)% of ([\d.]+)$/) { namespace, attributeName, double range, double expectedValue ->

	def isInstance = attributeName.contains(".");
	
	def rawAttributeName = isInstance
	? attributeName.substring(0, attributeName.lastIndexOf("."))
	: attributeName

	def key = null
	if(isInstance) {
		def instanceId = attributeName.substring(attributeName.lastIndexOf(".")+1)
		key = context.deviceDriverContext.attributeKeys.find { it.namespace == namespace && it.id == rawAttributeName && it.instance == instanceId}
	} else {
		key = context.deviceDriverContext.attributeKeys.find { it.namespace == namespace && it.id == rawAttributeName }
	}
	
	context.deviceDriverContext.attributeKeys.find {
		if(isInstance) {
			return it.namespace == namespace && it.id == rawAttributeName && it.instance == rawAttributeName
		} else {
			return it.namespace == namespace && it.id == rawAttributeName
		}
	}
	
	if(key) {
		def actualAttribute = context.deviceDriverContext.getAttributeValue(key)
		def actualNumber
			try {
				actualNumber = Double.valueOf(actualAttribute);

			} catch ( NumberFormatException e) {
				
			assert false, "Actual attribute is not a number"
			}
				double epsilon = range*expectedValue/100
				assert (Math.abs(expectedValue - actualNumber) < epsilon);
	}else {
		assert false, "Unable to read capability ${namespace}:${attributeName}"
	}
	
}
/**
 * This will JSON decode and validate the driver variable
 * 
 * EXAMPLE:
 * 	Then the driver variable should be 'xyz'
 * 
 * STEP : ASSERT_CAPABILITY
 */
Then(~/^the driver variable (\S+) should be (.*)$/) { variableName, json ->
	def value = JSON.fromJson(json, Object.class)
	if ("recent" == value){   
		def actual = context.getDeviceDriverContext().getVariable(variableName)
		try {
			actual = actual.getTime()
		} catch (groovy.lang.MissingMethodException e){}
		assert  actual > System.currentTimeMillis() - recentTestDelay;
	} else {
	assert value == context.getDeviceDriverContext().getVariable(variableName)
	}
}


/**
 * This will decode and validate numeric driver variables
 *
 * EXAMPLE:
 * 	Then the driver variable should be within 1% of 25
 *
 * STEP : ASSERT_CAPABILITY
 */
Then(~/^the numeric driver variable (\S+) should be within ([\d.]+)% of ([\d.]+)$/) { variableName,  float range, target ->
	float value;
	try{
		value = Float.parseFloat(target)
	} catch (Exception e){
	assert false, "Variable must have a numeric value"
	}	
	float actual = context.getDeviceDriverContext().getVariable(variableName)
	float delta = range*value/100
	assert (Math.abs(actual - value) < delta) 
}



/**
 * Validates that the driver has place the matching number of messages in the protocol bus.
 * 
 * EXAMPLE:
 * 		Then protocol message count is 5 
 */
Then(~/^protocol message count is (\d+)$/) { int expected ->
	def actual = context.protocolBus.messageQueue.size()
	assert actual == expected
}
 
 
/**
 * Validates the scheduled events.
 * 
 * EXAMPLE:
 * 	Then the driver should schedule event Timeout
 * 	Then the driver should defer event DeferredRead
 * 
 * STEP : ASSERT_CAPABILITY
 */
Then(~/^the driver should (schedule|defer) event (\w+|N\\A)$/) { String method, String event ->
	verifySchedule(method, event, -1, null, -1, null)
}

/**
 * Validates a scheduled or defered event with data.  The data should be JSON encoded.
 * 
 * EXAMPLE:
 * 	Then the driver should schedule event Timeout with {'retryCount': 10}
 * 	Then the driver should defer event DeferredRead with "A string"
 * 
 * STEP : ASSERT_CAPABILITY
 */
Then(~/^the driver should (schedule|defer) event (\w+|N\/A) with (.*)$/) { String method, String event, String data ->
	verifySchedule(method, event, -1, null, -1, data)
}

Then(~/^the driver should schedule event (\w+|N\/A) in (\d+) (sec|second|seconds|ms|milliseconds)$/) { String event, long delay, String unit ->
	verifySchedule('scheduleIn', event, delay, unit, -1, null)
}

Then(~/^the driver should schedule event (\w+|N\/A) in (\d+) (sec|second|seconds|ms|milliseconds) with (.*)$/) { String event, long delay, String unit, String data ->
	verifySchedule('scheduleIn', event, delay, unit, -1, data)
}

Then(~/^the driver should schedule event (\w+|N\/A) (?:in|every) (\d+) (sec|second|seconds|ms|milliseconds) (\d+) times$/) { String event, long delay, String unit, int maxRepetitions ->
	verifySchedule('scheduleRepeating', event, delay, unit, maxRepetitions, null)
}

Then(~/^the driver should schedule event (\w+|N\/A) (?:in|every) (\d+) (sec|second|seconds|ms|milliseconds) (\d+) times with (.*)$/) { String event, long delay, String unit, int maxRepetitions, String data ->
	verifySchedule('scheduleRepeating', event, delay, unit, repetitions, data)
}

Then(~/^the driver should cancel event (\w+|N\/A)$/) { String event ->
	verifySchedule('cancel', event, -1, null, -1, null)
}

Then(~/^there should be no more scheduled events$/) { ->
	assert null == context.pollScheduledEvent()
}

void verifySchedule(String method, String event, long delay, String unit, int repetitions, String json) {
	if("schedule".equals(method)) {
		method = "scheduleIn"
	}
	if ("N/A" == event) return
    long delayMs = delay >= 0 ? convertToMs(delay, unit) : -1;
    Object data = json != null ? JSON.fromJson(json, Object.class) : null;
	CapturedScheduledEvent e = new CapturedScheduledEvent(method, event, delayMs, repetitions, data)
	assert e.matches( context.pollScheduledEvent() )
}

//================================================================
// DRIVER <= DEVICE [IN]
//================================================================
/**
 * This mock inbound ZWave or Zigbee message from the device. 
 * The mock is create with the 2 arguments.
 * 
 * ARGUMENT:
 * The argument values must be Zigbee and Z-Wave supported in this project. 
 *  
 * 	ZWave (refer to ZWaveCommandClasses.json)
 * 		(type) command class name : meter, 
 * 		(subType) command name : get
 *  
 *  Zigbee (refer to ZCL binding classes)
 *  	(type) cluster name : iaszone,
 *      (subType) message type name : ZoneStatusChangeNotification
 * 
 * EXAMPLE:
 * 		When the device response with meter get
 * 		When the device response with basic zclreadattributesresponse
 * 
 * STEP : IN-MOCK
 */
When(~/^the device (?:response with|responds with|sends) (.+) (.+)$/) { String type, String subType ->
	direction = Constant.Direction.IN 
		commandBuilder = context.getCommandBuilder().commandName(type, subType)
}

/**
 *  Special case handler for the nodeinfo message
 */
When(~/^the device sends nodeInfo$/) { ->
	direction = Constant.Direction.IN
		commandBuilder = context.getCommandBuilder().nodeinfo()
}


/**
 * This signal the mock ZWave or Zigbee message is complete and is good for sending.
 * 
 * PRE-CONDITION:
 * 	This required IN-MOCK to be setup first
 * 	
 * EXAMPLE:
 * 	And send to driver
 * 
 * STEP : IN-FINAL
 */
When(~/^send to driver$/) { ->
	commandBuilder.buildAndSend()
}


When(~/^with header flags (\d+)$/) { byte flags ->
	commandBuilder.setFlags(flags)
}

When(~/^with header flags 0x([0-9A-F]{2})$/) { String flags ->
	commandBuilder.setFlags(Byte.decode("0x" + flags))
}

When(~/^with manufacturer code (\d+)$/) { int code ->
	commandBuilder.setManufacturerCode(code)
}

When(~/^with manufacturer code 0x([0-9A-F]{4})$/) { String code ->
	commandBuilder.setManufacturerCode(Integer.decode("0x" + code))
}


//================================================================
// DRIVER <=> DEVICE [BI-DIRECTION]
//================================================================
/**
 * This is to set the ZWave receive variable or Zigbee attribute with value.
 * The value is use for mocking the IN messages or OUT messages.
 * 
 * When it is IN, parameterName is the ZWave receive variable name or the Zigbee attribute name, IPCD 'value' Json.
 * 
 * When it is OUT, parameterName is the ZWave send variable name. It is not been applied to Zigbee, IPCD.
 * 
 * The argument parameterValue is the value to be set.
 * 
 * PRE-CONDITION:
 * 	This required IN-MOCK or OUT-MOCK
 * 
 * EXAMPLE:
 * 	And with parameter type <setpoint>
 * 	And with parameter ATTR_POWER_SOURCE <source>
 * 
 * STEP : PARAMETER
 */
When(~/^with parameter (.+) (.+)$/) { String parameterName, String parameterValue ->
	if (parameterValue == "N/A") {
		return
	}
	if(direction == Constant.Direction.IN) {
		if ( "null" != parameterValue){
			// Setup the input values for respective protocol
			commandBuilder.addProtocolMessageData(parameterName, parameterValue)		
		}
	} else {
		context.checkSentParameter( parameterName, parameterValue)
	}
}


/**
 * This populate the mock Zigbee message with a payload.
 * It is only for zigbee.
 * There are cluster, which are not formally supported in this project,
 * but receive as incoming messages
 * This provide a series of bytes to be set in payload.
 *
 * PRE-CONDITION:
 * 		This required IN-MOCK to be setup first.
 *
 * EXAMPLE:
 * 		And with payload 13,0,0,0,0, 10,10,10,10,10, 20,20,20
 *
 * STEP : PAYLOAD
 * AlertMe Care, Contact, KeyPad, RangeExtender
 * Centralite Contact, Help, Keypad, Motion, Smart button, Smart Plug
 * Nyce Tilt, Waxman Leak, Waxman SmartValve
 * 
 */
When(~/^with payload (.+)$/) { String payload->
		String[] actualStrings = payload.tokenize(",")
		if(direction == Constant.Direction.IN) {
			commandBuilder.addPayload(actualStrings);
		} else {
			context.validatePayload(actualStrings);
		}
}

/**
 * This may be used to set the endpoint on a Zigbee device message
 * or validate the endpoint on a Zigbee driver message
 *
 * PRE-CONDITION:
 * 		This required IN-MOCK to be setup first.
 *
 * EXAMPLE:
 * 		And with endpoint 2
 *
 * STEP : PAYLOAD
 */
When(~/^with endpoint (\d+)$/) { byte endpoint ->
		if(direction == Constant.Direction.IN) {
			commandBuilder.endpoint = endpoint
		} else {
			assert false: "Outbound check for endpoint not implemented";
			assert endpoint == zcl.endpoint
		}
}

//================================================================
// DRIVER => DEVICE [OUT]
//================================================================
/**
 * This is specifically for a ZigBee read attributes, because they are an array.
 */
When(~/^with attribute (\w+)$/) {String attribute ->
	if(direction == Constant.Direction.IN){
		throw new Exception("Not Implemented for IN direction")
	} else {
		context.checkReadAttributes(attribute)
	}
}

/**
 * This is specifically for a ZigBee write attributes, because they are an object.
 */
When(~/^with attribute (\w+) value (\w+)$/) {String attribute, String value ->
	if(direction == Constant.Direction.IN){
			throw new Exception("Not Implemented for IN direction")
	} else {
		context.checkWriteAttribute(attribute, value);
	}
}


/**
 *  The driver may produce ZWave or Zigbee messages send to protocol bus.
 *  This is to consume top message from protocol bus and de-serialized it for assertion.
 * The 2 arguments are for asserting the message type correctness.
 * For example, asserting an expected Zigbee onoff of message type off.
 * 
 * CAVEAT: 
 * 	The assertion is done by message order in the queue.
 *  The goal is to assert that the driver send the messages.  
 * 	It is not meant to assert that the protocol bus contain certain type of messages.
 * 
 * ARGUMENT:
 * The argument values must be the Zigbee cluster or Z-Wave commands supported in this project. 
 *  
 * 	ZWave (refer to ZWaveCommandClasses.json)
 * 		(type) string command class name : meter, 
 * 		(subType) string command name : get
 *  
 *  Zigbee (refer to generated ZCL binding classes) 
 *  	It supports text name, number or hex cluster representation.
 *  	(type) cluster name : onoff, 6, 0x0006
 *      (subType) message type name : ZoneStatusChangeNotification
 *      
 *  IPCD ( refer to IpcdProtocol.java)
 *  	(type) IPCD Command
 *  	(subType) unused arguement filled with a dummy word
 * 
 * EXAMPLE:
 * 	(ZWave)
 * 	Then the driver should send meter get
 * 	
 *  (Zigbee)
 * 	Then the driver should send onoff 0
 * 	Then the driver should send onoff zclReadAttributes
 * 	Then the driver should send 0x0006 zclReadAttributes
 * 	Then the driver should send 0x0006 1
 * 
 *  (IPCD)
 *  Then the driver should send onEvent command
 *  Then the driver should send onFactoryReset command
 * 
 * STEP : OUT-MESSAGE
 */
Then(~/^the driver should send (.+) (.+)$/) {String type, String subType ->
	if(("N/A" == type) || ("N/A" == subType)) return
	direction = Constant.Direction.OUT
	type = type.trim()
	protocolMsg = context.getProtocolBus().take()
	context.validateProtocolMessage(protocolMsg, type, subType);	
}

/**
 * This will consume the top protocol bus message and assert the driver setup timeout.
 * 
 * EXAMPLE:
 * 	Then the driver should set timeout
 * 
 * STEP : OUT-TIMEOUT
 */
Then(~/^the driver should set timeout$/) { ->
	def reflexDefinition = context.getDeviceDriver().definition.getReflexes()
	def timeout = reflexDefinition.getOfflineTimeout();
	if (Long.MAX_VALUE != timeout){
		//timeout was set and we don't have to check value Victory!
	} else {
	def protocolMsg = context.getProtocolBus().take()
    context.checkTimeoutSeconds(protocolMsg, null)
	}
}

/*
* First this will check if the timeout was set in the reflex.  If there is no timeout set then
* it will be at it's default value of Long.MAX value.  If it was set it will run the comparison.
* 
* If not set, this will consume the top protocol bus message and assert the driver setup timeout.
*
* This means the test step will not have to change on a switch to a reflex.
* Includes testing duration.
*
* EXAMPLE:
* 	Then the driver should set timeout at 10 minutes
*
* STEP : OUT-TIMEOUT
*/

Then(~/^the driver should set timeout at (\d+) (min|mins|minute|minutes|hr|hour|hrs|hours)$/) { int duration , String units ->
	def reflexDefinition = context.getDeviceDriver().definition.getReflexes()
	def timeout = reflexDefinition.getOfflineTimeout();
	if (Long.MAX_VALUE != timeout){
//		if( duration != null && units != units) {
			assert timeout == convertToSeconds(duration, units);
//		}
	} else {
   def protocolMsg = context.getProtocolBus().take()
   int expectedTimeout = convertToSeconds(duration,units)
   context.checkTimeoutSeconds(protocolMsg, expectedTimeout)
	}
}

/**
 * This will consume the top protocol bus message and assert the driver setup polling.
 *
 * EXAMPLE:
 * 	Then the driver should poll basic.get every 20 minutes
 *
 */
Then(~/^the driver should poll (\w+)\.(\w+) every (\d+) (\w+)$/) { String type, String subType, int period, String units ->
	def protocolMsg = context.getProtocolBus().take()
	int expectedPeriod = convertToSeconds(period,units)
	context.checkPoll(protocolMsg, type, subType, expectedPeriod)
}

//================================================================
// Utility
//================================================================


/**
 *
 * @param delay  Long, expect >= 0
 * @param unit  ms, sec, seconds, milliseconds
 * @return delay time in milliseconds
 */
long convertToMs(long delay, String unit){
	if (0 > delay){
		return -1
	}
	switch (unit){
		case 'sec':
		case 'second':
		case 'seconds':
			delay = delay*1000
			break;
		case 'ms':
		case 'milliseconds':
			break;
		default:
			delay = convertToMs(convertToSeconds(delay, unit), 'sec')
			break;
	}
	return delay	
}

/**
 *
 * @param delay  Long, expect >= 0
 * @param unit  ms, sec, seconds, milliseconds
 * @return delay time in milliseconds
 */
long convertToSeconds(long delay, String unit){
	if (0 > delay){
		return -1
	}
	switch (unit){
		case 'min':
		case 'minute':
		case 'min':
		case 'minutes':
			delay = delay*60
			break;
		case 'hr':
		case 'hrs':
		case 'hour':
		case 'hours':
			delay = delay * 3600
			break;
		case 'sec':
		case 'second':
		case 'seconds':
			break;
		default:
			logger.warn("received unrecognized unit: "+unit)
			break;
	}
	return delay
}



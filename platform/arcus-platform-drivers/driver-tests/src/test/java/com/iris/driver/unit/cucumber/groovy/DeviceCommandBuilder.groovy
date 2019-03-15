package com.iris.driver.unit.cucumber.groovy

import com.iris.device.attributes.AttributeValue
import com.iris.device.model.CommandDefinition
import com.iris.device.model.AttributeDefinition
import com.iris.device.model.CapabilityDefinition
import com.iris.device.model.CommandDefinition
import com.iris.device.model.CommandDefinition.CommandDefinitionBuilder
import com.iris.driver.unit.cucumber.DriverTestContext
import com.iris.messages.PlatformMessage
import com.iris.driver.service.executor.DriverExecutors

class DeviceCommandBuilder {

	def DriverTestContext<?> context;
	def CommandDefinitionBuilder builder;
	def CommandDefinition command;

	def DeviceCommandBuilder () {}

	def DeviceCommandBuilder (DriverTestContext<?> context) {
		this.context = context;
	}

	def DeviceCommandBuilder command (String messageTypeIdentifier) {
		def (commandNamespace, commandName) = messageTypeIdentifier.tokenize(":")

		CapabilityDefinition capability = context.getCapabilityRegistry().getCapabilityDefinitionByNamespace(commandNamespace);
		if (capability == null)
			throw new IllegalArgumentException("No capability exists with the namespace ${commandNamespace}. Possible namespaces are ${getCapabilityNamespaces()}");

		command = capability.getCommands().get(commandName);		
		if (command == null)
			throw new IllegalArgumentException("No command named ${commandName} is defined for the capability ${capability.getCapabilityName()}. Possible commands are ${getCommandNames(commandNamespace)}");

		builder = command.builder();
		return this;
	}

	def DeviceCommandBuilder withStringAttribute (String name, String value) {
		withAttribute(name, value)
			 
		return this;
	}

	def DeviceCommandBuilder withAttribute (String name, Object value) {
		def (attributeNamespace, attributeName) = name.tokenize(":")
		
		CapabilityDefinition capabilityDefinition = context.getCapabilityRegistry().getCapabilityDefinitionByNamespace(attributeNamespace);
		if (capabilityDefinition == null)
			throw new IllegalArgumentException("No capability exists with the namespace ${attributeNamespace}. Possible namespaces are ${getCapabilityNamespaces()}");
		
		def isInstance = attributeName.contains(".")
				
		def rawAttributeName = isInstance 
		? attributeName.substring(0, attributeName.lastIndexOf("."))
		: attributeName 	
		
		def attributeDefinitionObj = capabilityDefinition.getAttributes()
		def attributeDefinition = null//attributeDefinitionObj;				
		if(attributeDefinitionObj instanceof Map) {
			attributeDefinition = attributeDefinitionObj[attributeNamespace+":"+rawAttributeName]
		}
		
		if (attributeDefinition == null)
			throw new IllegalArgumentException("No attribute named ${name} exists for the capability ${attributeNamespace}. Possible attributes are: ${getAttributes(attributeNamespace)}");
		
		if(isInstance) {
			def instanceName = attributeName.substring(attributeName.lastIndexOf(".")+1)
			def attributeKey = attributeDefinition.key.instance(instanceName)
			// using the AttributeDefinition to enforce definition rule on the value
			value = attributeDefinition.coerceToValue(value).value
			// add the value with instance key
			builder.add(new AttributeValue(attributeKey, value))
		} else {
			builder.add(attributeDefinition.coerceToValue(value));
		}
			 
		return this;
	}

	def DeviceCommandBuilder withStringArgument (String name, String value) {
		withArgument(name, value)
	}
	
	def DeviceCommandBuilder withArgument (String name, Object value) {
		AttributeDefinition attributeDefinition = command.getInputArguments().get(name);
		if (attributeDefinition == null)
			throw new IllegalArgumentException("No argument named ${name} exists for command ${command.getName()}. Possible arguments are: ${getCommandArguments()}");
	
		builder.add(attributeDefinition.coerceToValue(value));
			
		return this;
	}
	
	def PlatformMessage build () {
		return PlatformMessage.builder().from(context.getClientAddress()).to(context.getDriverAddress()).withPayload(builder.create()).create();
	}

	def buildAndSend() {
		def platformMsg = build()
		DriverExecutors.dispatch(platformMsg, context.getDriverExecutor());
	}

	private List<String> getCommandArguments () {
		def args = [];
		for (String thisArgument : command.getInputArguments().keySet()) {
			args.push(thisArgument);
		}
		
		return args;
	}
	
	private List<String> getAttributes (String capabilityNamespace) {
		def attributes = [];
		
		def definitionObj = context.getCapabilityRegistry().getCapabilityDefinitionByNamespace(capabilityNamespace).getAttributes();
		
		if(definitionObj instanceof Map) {
			definitionObj.each { key, value -> attributes.push(key)}
		} else {
			for (AttributeDefinition thisDefinition : definitionObj.getAttributes()) {
				attributes.push(thisDefinition.getName());
			}
		}
		return attributes;
	}
	
	private List<String> getCommandNames (String capabilityNamespace) {
		def names = [];
		for (String key : context.getCapabilityRegistry().getCapabilityDefinitionByNamespace(capabilityNamespace).getCommands().keySet()) {
			names.push(key);
		}

		return names;
	}

	private List<String> getCapabilityNamespaces () {
		def namespaces = [];
		for (CapabilityDefinition thisCapability : context.getCapabilityRegistry().listCapabilityDefinitions()) {
			namespaces.push(thisCapability.getNamespace());
		}

		return namespaces;
	}
}

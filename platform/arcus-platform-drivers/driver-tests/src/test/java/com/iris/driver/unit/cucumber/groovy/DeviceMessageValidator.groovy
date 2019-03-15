package com.iris.driver.unit.cucumber.groovy

import com.iris.driver.unit.cucumber.DataTypeUtilities
import com.iris.messages.MessageBody
import com.iris.driver.unit.cucumber.DataTypeUtilities

class DeviceMessageValidator {

	private final MessageBody message;
	def receivedValue;
	
	def DeviceMessageValidator() {}
	
	def DeviceMessageValidator(MessageBody message) {
		this.message = message;
	}
	
	def DeviceMessageValidator hasSameElementsAs (List<?> expectedValue) {
		assert expectedValue != null
		assert receivedValue != null
		assert expectedValue.sort() == receivedValue.sort() 
		return this;
	}
			
	def DeviceMessageValidator is (String expectedValue) {
		try {
			def expectedNumber = Double.valueOf(expectedValue);
			def actualNumber = Double.valueOf(receivedValue);
			
			assert expectedNumber == actualNumber;
			return this;
			
		} catch ( NumberFormatException e) {

		assert DataTypeUtilities.stringValueOf(expectedValue) == DataTypeUtilities.stringValueOf(receivedValue)
		return this;
		}
	}
		
	def DeviceMessageValidator eventName () {
		receivedValue = message.getMessageType();
		return this;
	}
	
	def DeviceMessageValidator attribute (name) {
		receivedValue = message.getAttributes().get(name);
		return this;
	}
	
	def DeviceMessageValidator isCurrent() {
		double left = ( System.currentTimeMillis() / 60000 )
		double right = ( receivedValue / 60000 )
		assert left.round() == right.round()
		return this
	}
}

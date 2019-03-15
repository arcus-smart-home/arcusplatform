package com.iris.driver.unit.cucumber.groovy

import com.iris.driver.event.DriverEvent
import com.iris.driver.event.ScheduledDriverEvent
import com.iris.driver.unit.cucumber.DriverTestContext;
import com.iris.messages.model.DriverId
import com.iris.model.Version

class DriverEventBuilder {

	def DriverTestContext<?> context;
	def DriverEvent theEvent;
	
	def DriverEventBuilder () {}
	
	def DriverEventBuilder (DriverTestContext<?> context) {
		this.context = context;
	}
	private final int minReflexVersion = 0
	
	def DriverEventBuilder event (String eventIdentifier) {
		switch (eventIdentifier) {
		case "device connected": return deviceConntected();
		case "device disconnected": return deviceDisconnected();
		case "device disassociated": return deviceDisassociated();
		default: throw new IllegalArgumentException("No such driver event of type " + eventIdentifier);
		}
		
		return this;
	}

	def DriverEventBuilder deviceAssociated() {
		theEvent = DriverEvent.createAssociated(context.getDeviceDriverContext().getDevice().getProtocolAttributes());
		return this;
	}
	
	def DriverEventBuilder deviceDisassociated() {
		theEvent = DriverEvent.createDisassociated();
		return this;
	}
		
	def DriverEventBuilder deviceDisconnected() {
		theEvent = DriverEvent.createDisconnected(minReflexVersion);
		return this;
	}
	
	def DriverEventBuilder deviceConnected() {
		theEvent = DriverEvent.createConnected(minReflexVersion);
		return this;
	}
	
	def DriverEventBuilder driverUpgraded() {
		theEvent = DriverEvent.driverUpgraded(new DriverId("Fallback", new Version(1,0)));
		return this;
		
	}
	
	def DriverEventBuilder scheduledNowEvent(String eventName, Object data = null) {
		theEvent = DriverEvent.createScheduledEvent(eventName, data, null, new Date());
		return this;
	}
	
	def DriverEvent build () {
		return theEvent;
	}
	
	def void buildAndSend () throws Exception {
		context.getDeviceDriver().handleDriverEvent(build(), context.getDeviceDriverContext());
	}
}

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
package com.iris.driver.unit.cucumber;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Set;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.iris.capability.registry.CapabilityRegistry;
import com.iris.capability.registry.CapabilityRegistryModule;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.dao.PersonDAO;
import com.iris.core.dao.PersonPlaceAssocDAO;
import com.iris.core.driver.DeviceDriverStateHolder;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.core.messaging.memory.InMemoryProtocolMessageBus;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.core.protocol.ProtocolMessageBus;
import com.iris.device.attributes.AttributeKey;
import com.iris.device.attributes.AttributeMap;
import com.iris.driver.DeviceDriver;
import com.iris.driver.DeviceDriverContext;
import com.iris.driver.PlatformDeviceDriverContext;
import com.iris.driver.groovy.GroovyDriverFactory;
import com.iris.driver.groovy.GroovyDriverModule;
import com.iris.driver.groovy.pin.PinManagementContext;
import com.iris.driver.service.executor.DefaultDriverExecutor;
import com.iris.driver.service.executor.DriverExecutor;
import com.iris.driver.unit.cucumber.MockGroovyDriverModule.CapturedScheduledEvent;
import com.iris.driver.unit.cucumber.MockGroovyDriverModule.CapturingSchedulerContext;
import com.iris.messages.address.Address;
import com.iris.messages.address.ClientAddress;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceConnectionCapability;
import com.iris.messages.capability.PresenceCapability;
import com.iris.messages.model.Device;
import com.iris.messages.model.Fixtures;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

import groovy.util.GroovyScriptEngine;

@Mocks({DeviceDAO.class, PersonDAO.class, PlacePopulationCacheManager.class})
@Modules({ InMemoryMessageModule.class, CapabilityRegistryModule.class, MockGroovyDriverModule.class })
public class AbstractDriverTestCase extends IrisMockTestCase {

	private final static Logger logger = LoggerFactory.getLogger(AbstractDriverTestCase.class) ;

    @Inject
    protected PlatformMessageBus platformBus;
    
    @Inject
    protected PlacePopulationCacheManager mockPopulationCacheMgr;

    @Inject
    protected ProtocolMessageBus protocolBus;

    @Inject
    protected CapabilityRegistry capabilityRegistry;

    @Inject
    protected GroovyDriverFactory factory;
    
   @Inject
   protected DeviceDAO deviceDAO;

   @Inject
   protected CapturingSchedulerContext scheduler;

   @Inject
   private PinManagementContext pinManager;

    private final ClientAddress clientAddress = Fixtures.createClientAddress();
    private final Address driverAddress = Fixtures.createDeviceAddress();
    private final Address protocolAddress = Fixtures.createProtocolAddress();

    protected DeviceDriver deviceDriver;
    protected PlatformDeviceDriverContext deviceDriverContext;
    protected DriverExecutor executor;

    @Inject
    public void cacheScripts(GroovyScriptEngine engine) throws MalformedURLException {
        engine.getConfig().setRecompileGroovySource(false);
        engine.getConfig().setTargetDirectory("build/drivers");
    }
    
    @Provides @Named(GroovyDriverModule.NAME_GROOVY_DRIVER_DIRECTORIES)
    public Set<URL> groovyDriverUrls() throws MalformedURLException {
       return ImmutableSet.of(
             new File("src/test/resources").toURI().toURL(),
             new File("../../arcus-containers/driver-services/src/main/resources").toURI().toURL(),
             new File("build/drivers").toURI().toURL()
       );

    }
    
    @Provides
    public PersonPlaceAssocDAO providesPersonPlaceAssocDao() {
       return EasyMock.createMock(PersonPlaceAssocDAO.class);
    }
    
    public Address getProtocolAddress() {
        return protocolAddress;
    }

    public Address getDriverAddress() {
        return driverAddress;
    }

    public Device getDevice() {
       Preconditions.checkState(deviceDriverContext != null, "Must call initializeDriver first");
       return deviceDriverContext.getDevice();
    }
    
    public DeviceDriverContext getDeviceDriverContext() {
        return deviceDriverContext;
    }

    public DeviceDriver getDeviceDriver() {
        return deviceDriver;
    }

    public ClientAddress getClientAddress() {
        return clientAddress;
    }
    
    public DriverExecutor getDriverExecutor() {
		return executor;
	}

	public InMemoryPlatformMessageBus getPlatformBus() {
        return (InMemoryPlatformMessageBus) platformBus;
    }

    public InMemoryProtocolMessageBus getProtocolBus() {
        return (InMemoryProtocolMessageBus) protocolBus;
    }

    public CapabilityRegistry getCapabilityRegistry() {
        return capabilityRegistry;
    }
    
    public PinManagementContext getPinManager() {
       return pinManager;
    }
    
    @Nullable
    public CapturedScheduledEvent pollScheduledEvent() {
       return scheduler.events().poll();
    }

    public void initializeDriver(String driverScriptResource) throws Exception {
       logger.info("Initializing driver: {}", driverScriptResource);
       assert null != factory: "Factory null";
       deviceDriver = factory.load(driverScriptResource);
       Device device = createDeviceFromDriver(deviceDriver);
       deviceDriverContext = new PlatformDeviceDriverContext(device, deviceDriver, mockPopulationCacheMgr);
       // initialize online state so that it doesn't generate a ValueChange in the test cases
       deviceDriverContext.setAttributeValue(DeviceConnectionCapability.KEY_STATE, DeviceConnectionCapability.STATE_ONLINE);
       deviceDriverContext.setAttributeValue(DeviceConnectionCapability.KEY_LASTCHANGE, new Date());
       if(deviceDriverContext.getAttributeValue(Capability.KEY_CAPS).contains(PresenceCapability.NAMESPACE)) {
          deviceDriverContext.setAttributeValue(PresenceCapability.KEY_PRESENCE, PresenceCapability.PRESENCE_PRESENT);
          deviceDriverContext.setAttributeValue(PresenceCapability.KEY_PRESENCECHANGED, new Date());
       }
       deviceDriverContext.clearDirty();
       
       executor = new DefaultDriverExecutor(deviceDriver, deviceDriverContext, null, 10);

       /* adding mock behavior for mock DeviceDAO
        * This will make the PlatformDeviceDriverContext to think that the device is dirty and trigger deviceDao.save.
        * It is mock to return a not null value to avoid NullPointerException.
        * 
        * The plumbing is needed to set endpoint to the AttributeMap. ( see ~/^the Zigbee device has endpoint (.+)$/)
        * Likewise, it is apply to the deviceDao.updateDriverState
        */
        Capture<Device> deviceRef = EasyMock.newCapture(CaptureType.LAST);
        expect(deviceDAO.save(EasyMock.capture(deviceRef))).andAnswer(() -> deviceRef.getValue().copy()).anyTimes();
        deviceDAO.updateDriverState(anyObject(Device.class), anyObject(DeviceDriverStateHolder.class));
        EasyMock.expectLastCall().anyTimes();
        EasyMock.replay(deviceDAO);

    }

    @SuppressWarnings("unchecked")
    protected Device createDeviceFromDriver(DeviceDriver driver) {

        Device device = Fixtures.createDevice();
        for (AttributeKey<?> thisKey : driver.getBaseAttributes().keySet()) {
            Object thisValue = driver.getBaseAttributes().get(thisKey);

            switch (thisKey.getName()) {
            case "devadv:drivername":
                device.setDrivername((String) thisValue);
                break;
            case "devadv:protocol":
                device.setProtocol((String) thisValue);
                break;
            case "dev:devtypehint":
                device.setDevtypehint((String) thisValue);
                break;
            case "base:caps":
                device.setCaps((Set<String>) thisValue);
                break;
            case "dev:vendor":
                device.setVendor((String) thisValue);
                break;
            case "dev:model":
                device.setModel((String) thisValue);
                break;
            }
        }

        device.setDriverversion(driver.getDefinition().getVersion());
        return device;
    }
    
    protected <V> void addProtocolAttribute(AttributeKey<V> key, V value) {
       AttributeMap copy = getDevice().getProtocolAttributes();
       copy.set(key, value);
       getDevice().setProtocolAttributes(copy);
    }
 
}


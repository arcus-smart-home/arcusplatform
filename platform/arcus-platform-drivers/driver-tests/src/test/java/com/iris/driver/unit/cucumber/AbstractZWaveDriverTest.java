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

import groovy.util.GroovyScriptEngine;

import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.iris.capability.registry.CapabilityRegistry;
import com.iris.capability.registry.CapabilityRegistryModule;
import com.iris.core.dao.DeviceDAO;
import com.iris.core.messaging.memory.InMemoryMessageModule;
import com.iris.core.messaging.memory.InMemoryPlatformMessageBus;
import com.iris.core.messaging.memory.InMemoryProtocolMessageBus;
import com.iris.core.platform.PlatformMessageBus;
import com.iris.core.protocol.ProtocolMessageBus;
import com.iris.device.attributes.AttributeKey;
import com.iris.driver.DeviceDriver;
import com.iris.driver.DeviceDriverContext;
import com.iris.driver.PlatformDeviceDriverContext;
import com.iris.driver.groovy.ClasspathResourceConnector;
import com.iris.driver.groovy.GroovyDriverFactory;
import com.iris.driver.groovy.customizer.DriverCompilationCustomizer;
import com.iris.driver.groovy.GroovyProtocolPluginModule;
import com.iris.messages.address.Address;
import com.iris.messages.address.ClientAddress;
import com.iris.messages.model.Device;
import com.iris.messages.model.Fixtures;
import com.iris.population.PlacePopulationCacheManager;
import com.iris.protocol.Protocol;
import com.iris.protocol.zwave.ZWaveProtocol;
import com.iris.protocol.zwave.message.ZWaveMessage;
import com.iris.protocol.zwave.model.ZWaveNode;
import com.iris.test.IrisMockTestCase;
import com.iris.test.Mocks;
import com.iris.test.Modules;

@Mocks({DeviceDAO.class, PlacePopulationCacheManager.class})
@Modules({ InMemoryMessageModule.class, CapabilityRegistryModule.class, GroovyProtocolPluginModule.class })
public abstract class AbstractZWaveDriverTest extends IrisMockTestCase implements DriverTestContext<ZWaveMessage> {

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

    private final ClientAddress clientAddress = Fixtures.createClientAddress();
    private final Address driverAddress = Fixtures.createDeviceAddress();
    private final Address protocolAddress = Fixtures.createProtocolAddress();

    private Device device;
    private DeviceDriver deviceDriver;
    private DeviceDriverContext deviceDriverContext;

    @Provides
    public GroovyScriptEngine scriptEngine(CapabilityRegistry capabilityRegistry) {
        GroovyScriptEngine engine = new GroovyScriptEngine(new ClasspathResourceConnector());
        engine.getConfig().addCompilationCustomizers(new DriverCompilationCustomizer(capabilityRegistry));
        return engine;
    }

    @Override
    public Address getProtocolAddress() {
        return protocolAddress;
    }

    @Override
    public Address getDriverAddress() {
        return driverAddress;
    }

    @Override
    public Protocol<ZWaveMessage> getProtocol() {
        return ZWaveProtocol.INSTANCE;
    }

    @Override
    public DeviceDriverContext getDeviceDriverContext() {
        return deviceDriverContext;
    }

    @Override
    public DeviceDriver getDeviceDriver() {
        return deviceDriver;
    }

    @Override
    public ClientAddress getClientAddress() {
        return clientAddress;
    }

    @Override
    public InMemoryPlatformMessageBus getPlatformBus() {
        return (InMemoryPlatformMessageBus) platformBus;
    }

    @Override
    public InMemoryProtocolMessageBus getProtocolBus() {
        return (InMemoryProtocolMessageBus) protocolBus;
    }

    @Override
    public CapabilityRegistry getCapabilityRegistry() {
        return capabilityRegistry;
    }

    @Override
    public ZWaveNode getDevice() {
        ZWaveNode node = new ZWaveNode((byte)10);
        return node;
    }

    public void initializeDriver(String driverScriptResource) throws Exception {
        deviceDriver = factory.load(driverScriptResource);
        device = createDeviceFromDriver(deviceDriver);
        deviceDriverContext = new PlatformDeviceDriverContext(device, deviceDriver, mockPopulationCacheMgr);
    }

    @SuppressWarnings("unchecked")
    private Device createDeviceFromDriver(DeviceDriver driver) {

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
}


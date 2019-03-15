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
/**
 * 
 */
package com.iris.driver.unit.cucumber.zb;

import java.util.HashMap;
import java.util.Map;

import com.iris.driver.groovy.zigbee.ClusterBinding;
import com.iris.driver.groovy.zigbee.cluster.zcl.*;
import com.iris.protocol.zigbee.zcl.*;

/**
 * @author Finch
 *
 */
public class TestZigbeeZclClusters {
	   public final static Map<Short, ClusterBinding> bindingsByID = new HashMap<>();

	   static {
		   bindingsByID.put(DeviceTemperature.CLUSTER_ID, new DeviceTemperatureBinding());
		      bindingsByID.put(Basic.CLUSTER_ID, new BasicBinding());
		      bindingsByID.put(Scenes.CLUSTER_ID, new ScenesBinding());
		      bindingsByID.put(Diagnostics.CLUSTER_ID, new DiagnosticsBinding());
		      bindingsByID.put(ApplianceAlerts.CLUSTER_ID, new ApplianceAlertsBinding());
		      bindingsByID.put(Ballast.CLUSTER_ID, new BallastBinding());
		      bindingsByID.put(Time.CLUSTER_ID, new TimeBinding());
		      bindingsByID.put(IlluminanceSensing.CLUSTER_ID, new IlluminanceSensingBinding());
		      bindingsByID.put(ThermostatUi.CLUSTER_ID, new ThermostatUiBinding());
		      bindingsByID.put(Shade.CLUSTER_ID,new ShadeBinding());
		      bindingsByID.put(OnOffSwitch.CLUSTER_ID, new OnOffSwitchBinding());
		      bindingsByID.put(IasAce.CLUSTER_ID, new IasAceBinding());
		      bindingsByID.put(Groups.CLUSTER_ID, new GroupsBinding());
		      bindingsByID.put(PowerProfile.CLUSTER_ID, new PowerProfileBinding());
		      bindingsByID.put(IasWd.CLUSTER_ID, new IasWdBinding());
		      bindingsByID.put(FlowMeasurement.CLUSTER_ID, new FlowMeasurementBinding());
		      bindingsByID.put(Metering.CLUSTER_ID, new MeteringBinding());
		      bindingsByID.put(DoorLock.CLUSTER_ID, new DoorLockBinding());
		      bindingsByID.put(Thermostat.CLUSTER_ID, new ThermostatBinding());
		      bindingsByID.put(PollControl.CLUSTER_ID, new PollControlBinding());
		      bindingsByID.put(OccupancySensing.CLUSTER_ID, new OccupancySensingBinding());
		      bindingsByID.put(Dehumidification.CLUSTER_ID, new DehumidificationBinding());
		      bindingsByID.put(Color.CLUSTER_ID, new ColorBinding());
		      bindingsByID.put(Alarms.CLUSTER_ID, new AlarmsBinding());
		      bindingsByID.put(WindowCovering.CLUSTER_ID, new WindowCoveringBinding());
		      bindingsByID.put(IasZone.CLUSTER_ID,new IasZoneBinding());
		      bindingsByID.put(PressureMeasurement.CLUSTER_ID, new PressureMeasurementBinding());
		      bindingsByID.put(Identify.CLUSTER_ID, new IdentifyBinding());
		      bindingsByID.put(Pump.CLUSTER_ID, new PumpBinding());
		      bindingsByID.put(TemperatureMeasurement.CLUSTER_ID, new TemperatureMeasurementBinding());
		      bindingsByID.put(Fan.CLUSTER_ID, new FanBinding());
		      bindingsByID.put(IlluminanceMeasurement.CLUSTER_ID, new IlluminanceMeasurementBinding());
		      bindingsByID.put(HumidityMeasurement.CLUSTER_ID, new HumidityMeasurementBinding());
		      bindingsByID.put(Level.CLUSTER_ID, new LevelBinding());
		      bindingsByID.put(OnOff.CLUSTER_ID, new OnOffBinding());
		      bindingsByID.put(ElectricalMeasurement.CLUSTER_ID, new ElectricalMeasurementBinding());
		      bindingsByID.put(Ota.CLUSTER_ID, new OtaBinding());
		      bindingsByID.put(Power.CLUSTER_ID, new PowerBinding());
	   }
	   
}


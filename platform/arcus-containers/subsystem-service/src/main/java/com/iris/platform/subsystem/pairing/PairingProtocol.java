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
package com.iris.platform.subsystem.pairing;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import com.iris.common.subsystem.SubsystemContext;
import com.iris.messages.capability.PairingSubsystemCapability.StartPairingResponse;
import com.iris.prodcat.ProductCatalogEntry;

public enum PairingProtocol {
	/** Z-Wave devices */
	ZWAV(Flags.HUB, Flags.MESH),
	/** ZigBee HA, subset of ZigBee, reserved for future use */
	ZBHA(Flags.HUB, Flags.MESH),
	/** ZigBee AlertMe, subset of ZigBee, reserved for future use */
	ZBAM(Flags.HUB, Flags.MESH),
	/** ZigBee devices */
	ZIGB(Flags.HUB, Flags.MESH),
	/** Sercomm cameras */
	SCOM(Flags.HUB, Flags.ETHERNET),
	/** Anything unrecognized that requires the hub to pair (not the hub itself) */
	HUB(Flags.HUB),
	/** Direct ip connected devices, not cloud-to-cloud, does include AOSmith */
	IPCD(Flags.IPCD),
	/** Generic cloud-to-cloud connections */
	CLOUD(Flags.OAUTH),
	/** Alexa integration */
	ALXA(Flags.ASST, Flags.OAUTH),
	/** Google integration */
	GOOG(Flags.ASST, Flags.OAUTH),
	/** Generic assistant integration */
	ASST(Flags.ASST, Flags.OAUTH);
	
	public static PairingProtocol forProduct(ProductCatalogEntry entry) {
		String protocolFamily = entry.getProtoFamily();
		if(protocolFamily == null) {
			protocolFamily = "";
		}
		else {
			protocolFamily = protocolFamily.toLowerCase();
		}
		// null-safe true check, default false
		boolean hubRequired = Boolean.TRUE.equals(entry.getHubRequired());
		
		switch(protocolFamily) {
		case "z-wave":
			return PairingProtocol.ZWAV;
		case "zigbee":
			return PairingProtocol.ZIGB;
		case "cloud-to-cloud":
			return CLOUD;
		case "ip":
			if("Sercomm".equalsIgnoreCase(entry.getManufacturer())) {
				return PairingProtocol.SCOM;
			}
			else if(hubRequired) {
				return PairingProtocol.HUB;
			}
			else {
				return PairingProtocol.IPCD;
			}
		case "ipcd":
			return PairingProtocol.IPCD;
		case "proprietary":
			if("amazon".equals(entry.getManufacturer())) {
				return PairingProtocol.ALXA;
			}
			else if("google".equals(entry.getManufacturer())) {
				return PairingProtocol.GOOG;
			}
			else if(hubRequired) {
				return PairingProtocol.HUB;
			}
			else {
				return PairingProtocol.ASST;
			}
		default:
			if(hubRequired) {
				return PairingProtocol.HUB;
			}
			else {
				return PairingProtocol.CLOUD;
			}
		}
	}
	
	Set<Flags> flags;
	
	PairingProtocol(Flags... flags) {
		this.flags = EnumSet.copyOf(Arrays.asList(flags));
	}
	
	public boolean isHub() { return flags.contains(Flags.HUB); }
	public boolean isIpcd() { return flags.contains(Flags.IPCD); }
	public boolean isOAuth() { return flags.contains(Flags.OAUTH); }
	public boolean isAssistant() { return flags.contains(Flags.ASST); }
	public boolean isWirelessMesh() { return flags.contains(Flags.MESH); }
	public boolean isEthernetPairing() { return flags.contains(Flags.ETHERNET); }
	
	public StartPairingResponse.Builder addOAuthInfo(StartPairingResponse.Builder builder, SubsystemContext<?> context) {
		return builder;
	}
	
	public enum Flags {
		/** Hub based pairing */
		HUB,
		/** Pairing over a wireless mesh network */
		MESH,
		/** Should be paired while connected to ethernet */
		ETHERNET,
		/** IPCD based pairing */
		IPCD,
		/** External OAuth control (a device(s) to control) */
		OAUTH,
		/** Internal OAuth control (expose our interfaces to an assistant) */
		ASST;
	}
}


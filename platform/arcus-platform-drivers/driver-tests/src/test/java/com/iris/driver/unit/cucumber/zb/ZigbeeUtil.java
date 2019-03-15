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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.iris.driver.groovy.zigbee.MessageDecoder;
import com.iris.driver.groovy.zigbee.cluster.zcl.GeneralBinding;
import com.iris.driver.groovy.zigbee.cluster.zcl.ZigbeeZclClusters;

public class ZigbeeUtil {

	public static void main (String[] arg) {
		boolean x = "A".matches("(-)?\\d");
		System.out.println(x);
	}
	
	/**
	 * 
	 * @param text
	 * @param cluster
	 * @throws Exception
	 */
	public static byte[] parsePayload(String[] text, int cluster) throws Exception {
		List<Byte> collector = new ArrayList<>();
		for(String i : text) {
			String item = i.trim(); 
			if(item.matches("(-)?\\d") || StringUtils.isNumeric(item)){
				collector.add(Byte.parseByte(item));
			} else if (item.startsWith("0x")) {				
				int decimal = Integer.parseInt(item.substring(2), 16);
				if(Byte.MAX_VALUE > decimal) {
					decimal =  -(256 - decimal);
				}
				collector.add((byte)decimal);
			} else {
				// ATTR_SOMETHING
				MessageDecoder decoder = ZigbeeZclClusters.decodersById.get((short)cluster);
				if(decoder != null) {
					Class<?> bindingClass = decoder.getClass().getEnclosingClass();
					GeneralBinding b = (GeneralBinding)bindingClass.newInstance();
					Class<?> clusterClass = Class.forName("com.iris.protocol.zigbee.zcl."+b.getName());
					Object o = clusterClass.getField(item).get(null);
					if(o instanceof Short) {
						collector.add((byte)((Short)o & 0xff));
						collector.add((byte)((Short)o>>8 & 0xff));
					}
				} else {
					// There are cases of Zigbee cluster which are not from auto generated.
					collector.add(Byte.parseByte(item));
				}

			}
		}

		byte[] copy = new byte[collector.size()];
		for(int i=0, j=collector.size(); i<j; i++) {
			copy[i] = collector.get(i);
		}
		
		return copy;
	}

}


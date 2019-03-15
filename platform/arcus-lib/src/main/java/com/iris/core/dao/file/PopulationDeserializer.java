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
package com.iris.core.dao.file;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.iris.io.xml.BaseJAXPDeserializer;
import com.iris.messages.type.Population;
import com.iris.resource.manager.ResourceParser;

public class PopulationDeserializer extends BaseJAXPDeserializer<List<Population>> implements ResourceParser<List<Population>>{

	private static final String ROOT_ELEMENT_NAME = "populations";
	private static final String MAIN_ELEMENT_NAME = "population";
	private static final String ATTRIB_NAME = "name";
	private static final String ATTRIB_DESCRIPTION = "description";
	private static final String ATTRIB_MIN_HUB_V2_VERSION = "minhubv2version";
	private static final String ATTRIB_MIN_HUB_V3_VERSION = "minhubv3version";
	
	private static final String ANY_NAMESPACE = "*";
	private static final String XSD = "classpath:/schema/population/population.xsd";
	   
	   
	@Override
	public List<Population> fromJAXP(Document rootElement) {
		List<Population> populations = new ArrayList<Population>();
        
		NodeList populationsElement = rootElement.getElementsByTagNameNS(ANY_NAMESPACE, ROOT_ELEMENT_NAME);
		NodeList populationElements = ((Element) populationsElement.item(0)).getElementsByTagNameNS(ANY_NAMESPACE, MAIN_ELEMENT_NAME);
  
  
		for(int i=0;i<populationElements.getLength();i++){
			Element el = (Element)populationElements.item(i);
			Population curPopulation = new Population();
			curPopulation.setName(el.getAttribute(ATTRIB_NAME));
			curPopulation.setDescription(el.getAttribute(ATTRIB_DESCRIPTION));
			curPopulation.setMinHubV2Version(el.getAttribute(ATTRIB_MIN_HUB_V2_VERSION));			
			curPopulation.setMinHubV3Version(el.getAttribute(ATTRIB_MIN_HUB_V3_VERSION));			
			populations.add(curPopulation);
		}
		return populations;
	}

	@Override
	public String schemaResource() {
		return XSD;
	}

	@Override
	public List<Population> parse(InputStream is) {
		try {
			return deserialize(is);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

}


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
package com.iris.client.nws;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.iris.resource.manager.ResourceParser;
import com.opencsv.CSVParser;
import com.opencsv.CSVReader;

/*
 * The {@code StateCodeMappingResourceLoader} loads NWS SAME Code State Name Mappings for each two character US Postal
 * state code abbreviation that is references in the NWS SAME Code data file.
 * 
 * Each mapping will be stored in a {@link java.util.Map Map} on a state by state basis.
 * 
 * For SAME Code mappings by County and State source data:
 * See {@linktourl http://www.nws.noaa.gov/nwr/data/SameCode.txt}
 * 
 * For Specific County Coverage Listings by State source data:
 * See {@linktourl http://www.nws.noaa.gov/nwr/data/SameCode.txt}
 */

public class StateCodeMappingResourceLoader implements ResourceParser<Map<String, SameState>> {
	private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(StateCodeMappingResourceLoader.class);

	/*
	 * @see com.iris.resource.manager.ResourceParser#parse(java.io.InputStream)
	 */	
	@Override
	public Map<String, SameState> parse(InputStream is) {
		LOGGER.debug("Parsing NWS SAME Code State Name Mapping input data file");

		Map<String, SameState> sameStates = new HashMap<String,SameState>();

		try (CSVReader reader = new CSVReader(new InputStreamReader(is), 0, new CSVParser())) {
			String[] nextLine;
			while ((nextLine = reader.readNext()) != null) {
				if (nextLine.length < 2) { // error in input file, skip the record and continue
					continue;
				}

            /*
             * The NWS SAME code data file can contain whitespace around state
             * codes on load, strip this out
             */
				String stateCode = nextLine[0].trim(); 
				String state = nextLine[1];

				if (StringUtils.isEmpty(stateCode) || StringUtils.isEmpty(state)) {
					LOGGER.warn(
							"Invalid NWS SAME Code State Name mapping file record, null value(s) found while parsing input data file with values: State Code:{} and State Name:{}",
							stateCode, state);
					continue;
				}

				sameStates.put(stateCode, new SameState(stateCode, state));
			}
		} catch (Exception e) {
			LOGGER.warn("Error parsing NWS SAME Code State Name Mapping input data file", e);
			throw new IllegalStateException(e);
		}
		
		if (sameStates.isEmpty()) {
			return null;
		}
		
		return sameStates;
	}
}


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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Table;
import com.iris.Utils;
import com.iris.resource.manager.ResourceParser;
import com.opencsv.CSVParser;
import com.opencsv.CSVReader;

/*
 * The {@code SameCodeResourceLoader} loads NWS SAME Code mapping as derived from the National Weather Service
 * SAME CODE datafile.
 * 
 * Note that a county can be repeated on a state by state basis. As such the codes will be stored in a 
 * {@link java.util.Map Map} on a state by state basis. Each Map entry will contain a {@link java.util.List List}
 * of Same Code entries for the given state or territory.
 * 
 * For SAME Code mappings by County and State source data:
 * See {@linktourl http://www.nws.noaa.gov/nwr/data/SameCode.txt}
 * 
 * For Specific County Coverage Listings by State source data:
 * See {@linktourl http://www.nws.noaa.gov/nwr/data/SameCode.txt}
 * 
 * @see com.iris.resource.manager.ResourceParser#parse(java.io.InputStream)
 */

public class SameCodeResourceLoader implements ResourceParser<SameCodeRegistry> {
   private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(SameCodeResourceLoader.class);

   // Map stateCode -> sameState
   private final Map<String, SameState> sameStates;

   public SameCodeResourceLoader(Map<String, SameState> sameStates) {
      Utils.assertNotEmpty(sameStates, String.format("%s: State Name Mappings cannot be empty or null", this.getClass().getSimpleName()));
      this.sameStates = sameStates;
   }

   /*
    * @see com.iris.resource.manager.ResourceParser#parse(java.io.InputStream)
    */
   @Override
   public SameCodeRegistry parse(InputStream is) {
      LOGGER.debug("Parsing NWS SAME Code input data file");

      /*
       * Create a guava table. These tables have two keys, each called a vertex.
       * This table will be indexed as follows stateCode, county, samecode
       * Conceptually it is the equivalent of Map<stateCode, Map<county,
       * sameCode>>
       * 
       * Provides for easy lookup of SameCode objects by both keys
       * stateCode,county
       */
      Table<String, String, SameCode> sameCodes;

      ImmutableTable.Builder<String, String, SameCode> sameCodeBuilder = new ImmutableTable.Builder<String, String, SameCode>();

      // ListMultimap conceptually provides Map<stateCode, List<counties>>
      ListMultimap<String, String> sameCounties = ArrayListMultimap.create();

      try (CSVReader reader = new CSVReader(new InputStreamReader(is), 0, new CSVParser())){
         String[] nextLine;
         while ((nextLine = reader.readNext()) != null){
            if (nextLine.length < 3) { // error in input file, skip
               continue;
            }

            String code = nextLine[0];
            String county = nextLine[1];

            /*
             * The NWS SAME code data file can contain whitespace around state
             * codes on load, strip this out
             */
            String stateCode = nextLine[2].trim();

            if (StringUtils.isEmpty(code) || StringUtils.isEmpty(county) || StringUtils.isEmpty(stateCode)) {
               LOGGER.warn(
                     "Invalid NWS SAME Code Record, null value(s) found while parsing input date file for values Code:{} County:{} and State Code:{}",
                     code, county, stateCode);
               continue; // skip
            }

            SameState sameState = sameStates.get(stateCode);

            if (sameState == null) {
               LOGGER.warn("Missing NWS SAME Code State Name mapping for NWS State Code: {}", stateCode);
               continue; // skip
            }else{
               String state = sameStates.get(stateCode).getState();

               sameCodeBuilder.put(stateCode, county, new SameCode(code, county, stateCode, state));
               sameCounties.put(stateCode, county);
            }

         }
      }catch (Exception e){
         LOGGER.warn("Error parsing NWS SAME Code input data file", e);
         throw new IllegalStateException(e);
      }

      sameCodes = sameCodeBuilder.build();

      /*
       * Sort all lists before insertion into registry
       */

      // For each state: sort counties by name
      sameCounties.keySet().stream().forEach(stateCode -> Collections.sort(sameCounties.get(stateCode)));

      // Sort SameStates before insertion into registry (
      List<SameState> sameStateList = new ArrayList<SameState>(sameStates.values());
      // Comparable sorts on stateCode
      Collections.sort(sameStateList);

      if (sameCodes.isEmpty()) {
         return null;
      }

      return new SameCodeRegistry(sameCodes, sameStateList, sameCounties);
   }
}


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
package com.iris.client.eas;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.iris.resource.manager.ResourceParser;

/*
 * The {@code EasCodeResourceLoader} loads EAS Event Codes as compiled by NOAA and provided as a text file
 * for input.
 * 
 * For a Listing of EAS Code Mappings:
 * See {@linktourl http://www.nws.noaa.gov/nwr/info/eventcodes.html}
 * 
 * @see com.iris.resource.manager.ResourceParser#parse(java.io.InputStream)
 */
public class EasCodeResourceLoader implements ResourceParser<Map<String, EasCode>> {
   private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(EasCodeResourceLoader.class);
   
   private static final String EAS = "eas";
   private static final String NAME = "name";
   private static final String GROUP = "group";
   
   private static final String REGEX = String.format("^(?<%s>.{3})(?:\\s+)(?<%s>.*[^\\s]+)(?:\\s+)(?<%s>[^\\s]+)$", EAS, NAME, GROUP);
   
   private static final Pattern PATTERN = Pattern.compile(REGEX);

   /*
    * @see com.iris.resource.manager.ResourceParser#parse(java.io.InputStream)
    */
   @Override
   public Map<String, EasCode> parse(InputStream is) {
      LOGGER.debug("Parsing NOAA EAS Event Code input data file");

      Map<String, EasCode> easCodes = new TreeMap<String, EasCode>();
      
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))){
         
         reader.lines().map(EasCodeResourceLoader::stringToEas).forEach(easCode -> addToMap(easCodes, easCode));

      }catch (Exception e){
         LOGGER.warn("Error parsing NOAA EAS Event Code input data file", e);
         throw new IllegalStateException(e);
      }

      if (easCodes.isEmpty()) {
         return null;
      }

      return easCodes;
   }
   
   private void addToMap(Map<String, EasCode> easCodes, EasCode easCode) {
      if (easCode == null) {
         return; // skip         
      }
      
      easCodes.put(easCode.getEas(), easCode);
   }
   
   /*
    * This conversion is accessed externally. However package scope suffices to keep its visibility minimal.
    */
   static EasCode stringToEas(String input) {
      Matcher matcher = PATTERN.matcher(input);
      
      if (matcher.find()) {
         return new EasCode(matcher.group(EAS), matcher.group(NAME), matcher.group(GROUP));
      }
      
      LOGGER.warn(
            "Non-Fatal Condition: An invalid EAS Event Code was found while parsing the input data file. The record {} will be skipped.", input);   
      
      return null;
   }
}


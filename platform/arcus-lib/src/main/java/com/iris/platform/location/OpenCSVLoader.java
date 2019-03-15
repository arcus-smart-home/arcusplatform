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
package com.iris.platform.location;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.iris.common.sunrise.GeoLocation;
import com.iris.resource.manager.ResourceParser;
import com.opencsv.CSVParser;
import com.opencsv.CSVReader;

public class OpenCSVLoader implements ResourceParser<Map<String,GeoLocation>> {
   private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(OpenCSVLoader.class);
   
   @Override
   public Map<String,GeoLocation> parse(InputStream is) {
      LOGGER.debug("parsing geocode file");
      CSVReader reader=null;
      Map<String,GeoLocation>geopointsByZip = new HashMap<String, GeoLocation>(40000);
      try{
         reader = new CSVReader(new InputStreamReader(is), 1, new CSVParser());
         String[] nextLine;
         while ((nextLine = reader.readNext()) != null){
            if (nextLine.length < 3) {
               continue;
            }
            String zip = nextLine[0];
            String latitude = nextLine[1];
            String longitude = nextLine[2];
            if(StringUtils.isEmpty(zip)||StringUtils.isEmpty(latitude)||StringUtils.isEmpty(longitude)){
               LOGGER.warn("invalid geocode location for record zip:{} lat:{} long:{}",zip,latitude,longitude);
               continue;
            }
            geopointsByZip.put(zip.trim(), GeoLocation.fromCoordinates(latitude, longitude));
         }
         return geopointsByZip;
      }catch (Exception e){
         LOGGER.warn("Error parsing location CSV file", e);
         throw new IllegalStateException(e);
      }finally{
         IOUtils.closeQuietly(reader);
      }
   }

}


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
package com.iris.common.sunrise;


import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.iris.common.sunrise.SunriseSunsetCalc.ZENITH;

public class TestSunriseSunsetCalculator extends Assert {
   
   private SunriseSunsetCalc calc=null;
   private Calendar forDate=null;
   
   private String latitude="38.9693464546265";
   private String longitude="-95.2360020273613";
   
   private GeoLocation location = GeoLocation.fromCoordinates(latitude, longitude);
   
   private static final SimpleDateFormat DF=new SimpleDateFormat("yyyy-MM-dd z");
   private Calendar staticDate;
   private Date date;
   
   @Before
   public void setUp() throws Exception{
      calc=SunriseSunsetCalcFactory.getCalculator();
      forDate = Calendar.getInstance();
      date = DF.parse("2016-03-29 Central Standard Time");
      staticDate = Calendar.getInstance(TimeZone.getTimeZone("US/Central"));
      staticDate.setTime(date);
   }
   
   @Test
   public void testCalculateSunriseSunset() throws Exception {
      
      SunriseSunsetInfo results= calc.calculateSunriseSunset(staticDate, location);
      
      assertEquals(2,results.getSunrise().get(Calendar.MONTH));
      assertEquals(29,results.getSunrise().get(Calendar.DAY_OF_MONTH));
      assertEquals(7,results.getSunrise().get(Calendar.HOUR_OF_DAY));
      assertEquals(9,results.getSunrise().get(Calendar.MINUTE));

      assertEquals(2,results.getSunset().get(Calendar.MONTH));
      assertEquals(29,results.getSunset().get(Calendar.DAY_OF_MONTH));
      assertEquals(19,results.getSunset().get(Calendar.HOUR_OF_DAY));
      assertEquals(43,results.getSunset().get(Calendar.MINUTE));
   }
   
   @Test
   public void testTargetTimezone() throws Exception {
      TimeZone easternTz = TimeZone.getTimeZone("US/Eastern");
      staticDate.setTimeZone(easternTz);
      SunriseSunsetInfo results= calc.calculateSunriseSunset(staticDate, location);
      assertEquals(easternTz,results.getSunrise().getTimeZone());
   }
   
   @Test
   public void testZenithOverload() throws Exception {
      TimeZone easternTz = TimeZone.getTimeZone("US/Eastern");
      staticDate = Calendar.getInstance(easternTz);
      staticDate.setTime(date);
      SunriseSunsetInfo results= calc.calculateSunriseSunset(staticDate, location,ZENITH.CIVIL);
      assertEquals(7,results.getSunrise().get(Calendar.HOUR));
      assertEquals(42,results.getSunrise().get(Calendar.MINUTE));
   }
}


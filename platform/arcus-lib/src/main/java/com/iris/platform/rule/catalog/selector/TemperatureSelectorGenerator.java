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
package com.iris.platform.rule.catalog.selector;

import java.math.BigDecimal;
import java.text.DecimalFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.util.UnitConversion;

public class TemperatureSelectorGenerator extends MinMaxSelectorGenerator {
   
   private static final Logger logger = LoggerFactory.getLogger(TemperatureSelectorGenerator.class);   
   private final DecimalFormat df = new DecimalFormat("#");
   
   public TemperatureSelectorGenerator(double min, double max, double inc,
			Unit unit) {
		super(min, max, inc, unit);
	}

   @Override
   protected Option createOption(double curValue) {
	   String formattedValue = BigDecimal.valueOf(UnitConversion.tempFtoC(curValue)).setScale(1, BigDecimal.ROUND_HALF_UP).toString();
	   return new Option(df.format(curValue), formattedValue );
   }

   

}


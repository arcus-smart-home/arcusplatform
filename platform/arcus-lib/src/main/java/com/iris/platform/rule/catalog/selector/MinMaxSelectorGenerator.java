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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.iris.common.rule.RuleContext;

public class MinMaxSelectorGenerator implements SelectorGenerator {
   private static final Logger logger = LoggerFactory.getLogger(MinMaxSelectorGenerator.class);

   public static enum Unit {CELSIUS, FAHRENHEIT, WATTS, NONE}
   
   private final double min;
   private final double max;
   private final double increment;
   private final Unit unit;
   private final DecimalFormat df = new DecimalFormat("#");
   
   public MinMaxSelectorGenerator(double min, double max, double inc, Unit unit) {
	   Preconditions.checkArgument(min <= max, "min value should not exceed max value.");
	   Preconditions.checkArgument(inc > 0, "increment value should be greater than 0.");
	   this.min = min;
	   this.max = max;
	   this.increment = inc;
	   this.unit = unit;
   }

   @Override
   public boolean isSatisfiable(RuleContext context) {
      return true;
   }

   @Override
   public Selector generate(RuleContext context) {
      List<Option> options = new ArrayList<Option>();
      double cur = min;
      while (cur < max) {
    	  options.add(createOption(cur));
    	  cur += increment;
      }  
      options.add(createOption(max));

      ListSelector selector = new ListSelector();
      selector.setOptions(options);
      //logger.debug(options.toString());
      return selector;
   }
   
   protected Option createOption(double curValue) {
	   String formattedValue = df.format(curValue);
	   return new Option(formattedValue, formattedValue );
   }

   

}


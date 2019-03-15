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
/**
 *
 */
package com.iris.platform.rule.catalog.serializer;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.Attributes;

import com.iris.capability.definition.AttributeDefinition;
import com.iris.messages.capability.AlarmSubsystemCapability;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.DeviceCapability;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.capability.SceneCapability;
import com.iris.messages.capability.SubsystemCapability;
import com.iris.model.predicate.Predicates;
import com.iris.model.query.expression.ExpressionCompiler;
import com.iris.model.type.EnumType;
import com.iris.platform.rule.catalog.selector.ConstantListSelectorGenerator;
import com.iris.platform.rule.catalog.selector.ConstantSelectorGenerator;
import com.iris.platform.rule.catalog.selector.FilteringListSelectorGenerator;
import com.iris.platform.rule.catalog.selector.ListSelectorGenerator;
import com.iris.platform.rule.catalog.selector.MinMaxSelectorGenerator;
import com.iris.platform.rule.catalog.selector.MinMaxSelectorGenerator.Unit;
import com.iris.platform.rule.catalog.selector.Option;
import com.iris.platform.rule.catalog.selector.PresenceSelectorGenerator;
import com.iris.platform.rule.catalog.selector.SelectorGenerator;
import com.iris.platform.rule.catalog.selector.SelectorType;
import com.iris.platform.rule.catalog.selector.TemperatureSelectorGenerator;
import com.iris.platform.rule.catalog.selector.TextSelectorGenerator;
import com.iris.platform.rule.catalog.template.TemplatedValue;
import com.iris.serializer.sax.TagProcessor;
import com.iris.validators.Validator;

/**
 *
 */
public class SelectorProcessor extends BaseCatalogProcessor {
   public static final String TAG = "selector";

   public static final String TYPE_TIME = "time-of-day";
   public static final String TYPE_DAY = "day-of-week";
   public static final String TYPE_DEVICE = "device";
   public static final String TYPE_AVAILABLE_ALERT = "available-alert";
   public static final String TYPE_PERSON = "person";
   public static final String TYPE_SCENE = "scene";
   public static final String TYPE_PRESENCE = "presence";
   public static final String TYPE_DURATION = "duration";
   public static final String TYPE_TIME_RANGE = "time-range";
   public static final String TYPE_ATTRIBUTE = "attribute";
   public static final String TYPE_CONSTANT = "constant";
   public static final String TYPE_TEXT = "text";
   public static final String TYPE_TEMPERATURE = "temperature";
   public static final String TYPE_MIN_MAX = "min-max";

   private SelectorGenerator generator;

   private String type;
   private String name;
   private String query;
   private String attribute;
   private List<Option> options;
   private String minStr;
   private String maxStr;
   private String incrementStr;


   /**
    * @param validator
    */
   public SelectorProcessor(Validator validator) {
      super(validator);
   }

   public String getName() {
      return name;
   }

   public SelectorGenerator getSelectorGenerator() {
      return generator;
   }


   @Override
   public TagProcessor getHandler(String qName, Attributes attributes) {
      if(OptionsProcessor.TAG.equals(qName)) {
         return new OptionsProcessor(getValidator());
      }
      return super.getHandler(qName, attributes);
   }

   /* (non-Javadoc)
    * @see com.iris.platform.rule.catalog.serializer.BaseTagHandler#enterTag(java.lang.String, org.xml.sax.Attributes)
    */
   @Override
   public void enterTag(String qName, Attributes attributes) {
      super.enterTag(qName, attributes);
      if(TAG.equals(qName)) {
         type = getValue("type", attributes);
         name = getValue("name", attributes);
         query = getValue("query", null, attributes);
         attribute = getValue("attribute", null, attributes);
         minStr = getValue("min", null, attributes);
         maxStr = getValue("max", null, attributes);
         incrementStr = getValue("increment", "1", attributes);
         
      }
      // TODO query handlers
   }

   /* (non-Javadoc)
    * @see com.iris.platform.rule.catalog.serializer.BaseTagHandler#exitTag(java.lang.String)
    */
   @SuppressWarnings("unchecked")
   @Override
   public void exitTag(String qName) {
      if(TAG.equals(qName)) {
         if(StringUtils.isEmpty(type)) {
            getValidator().error("Missing type tag on <" + TAG + ">");
            return;
         }
         if(TYPE_TIME.equalsIgnoreCase(type)) {
            generator = new ConstantSelectorGenerator(SelectorType.TIME_OF_DAY);
         }
         else if(TYPE_DAY.equalsIgnoreCase(type)) {
            generator = new ConstantSelectorGenerator(SelectorType.DAY_OF_WEEK);
         }
         else if(TYPE_DURATION.equalsIgnoreCase(type)) {
            generator = new ConstantSelectorGenerator(SelectorType.DURATION);
         }
         else if(TYPE_TIME_RANGE.equalsIgnoreCase(type)) {
            generator = new ConstantSelectorGenerator(SelectorType.TIME_RANGE);
         }
         else if(TYPE_ATTRIBUTE.equalsIgnoreCase(type)) {
            if(attribute == null) {
               getValidator().error("Invalid selector [" + name +"]: No attribute specified");
            }
            generator = createAttributeGenerator();
         }
         else if(TYPE_CONSTANT.equalsIgnoreCase(type)) {
            generator = new ConstantListSelectorGenerator(options);
         }
         else if(TYPE_TEXT.equalsIgnoreCase(type)) {
            generator = new TextSelectorGenerator();
         }
         else if(TYPE_DEVICE.equalsIgnoreCase(type)) {
            ListSelectorGenerator listGenerator = new ListSelectorGenerator(options);
            listGenerator.setLabel(TemplatedValue.named(DeviceCapability.ATTR_NAME, String.class));
            listGenerator.setValue(TemplatedValue.named(Capability.ATTR_ADDRESS));
            try {
               listGenerator.setMatcher(
                     com.google.common.base.Predicates.and(
                           Predicates.isA(DeviceCapability.NAMESPACE),
                           ExpressionCompiler.compile(query)
                     )
               );
               this.generator = listGenerator;
            }
            catch(Exception e) {
               getValidator().equals("Invalid query [" + query + "]: " + e.getMessage());
            }
         }
         else if(TYPE_AVAILABLE_ALERT.equalsIgnoreCase(type)) {
            FilteringListSelectorGenerator listGenerator = new FilteringListSelectorGenerator(options);
            listGenerator.setMatcher(
               com.google.common.base.Predicates.and(
                  Predicates.isA(AlarmSubsystemCapability.NAMESPACE),
                  Predicates.attributeEquals(SubsystemCapability.ATTR_AVAILABLE, true)));
            listGenerator.setFilterCollectionTemplate(
               TemplatedValue.named(AlarmSubsystemCapability.ATTR_AVAILABLEALERTS));
            this.generator = listGenerator;
         }
         else if(TYPE_PERSON.equalsIgnoreCase(type)) {
            ListSelectorGenerator listGenerator = new ListSelectorGenerator();
            // Fall back to e-mail if first name isn't available.
            listGenerator.setLabel(TemplatedValue.named(PersonCapability.ATTR_FIRSTNAME, String.class),
                  TemplatedValue.named(PersonCapability.ATTR_EMAIL, String.class));
            listGenerator.setValue(TemplatedValue.named(Capability.ATTR_ADDRESS));
            listGenerator.setMatcher(Predicates.isA(PersonCapability.NAMESPACE));
            this.generator = listGenerator;
         }
         else if(TYPE_SCENE.equalsIgnoreCase(type)) {
            ListSelectorGenerator listGenerator = new ListSelectorGenerator();
            listGenerator.setLabel(TemplatedValue.named(SceneCapability.ATTR_NAME, String.class));
            listGenerator.setValue(TemplatedValue.named(Capability.ATTR_ADDRESS));
            listGenerator.setMatcher(Predicates.isA(SceneCapability.NAMESPACE));
            this.generator = listGenerator;
         }
         else if(TYPE_PRESENCE.equalsIgnoreCase(type)) {
            this.generator = new PresenceSelectorGenerator();
         }
         else if(TYPE_TEMPERATURE.equals(type)) {
        	 this.generator = createMinMaxGenerator(Unit.FAHRENHEIT);
         }else if(TYPE_MIN_MAX.equals(type)) {
        	 this.generator = createMinMaxGenerator(Unit.NONE);
         }
         else {
            getValidator().error("Unrecognized option type: " + type);
         }
      }
   }

   

@Override
   public void exitChildTag(String qName, TagProcessor handler) {
      if(OptionsProcessor.TAG.equals(qName)) {
         options = ((OptionsProcessor) handler).getOptions();
      }
   }

   private SelectorGenerator createAttributeGenerator() {
      if(attribute == null) {
         getValidator().error("Invalid selector [" + name +"]: No attribute specified");
         return null;
      }
      AttributeDefinition attrDef = getRegistry().getAttribute(attribute);
      if(attrDef == null) {
         getValidator().error("Invalid selector [" + name + "]: No attribute [" + attribute + "] defined");
         return null;
      }
      if(!(attrDef.getType() instanceof EnumType)) {
         getValidator().error("Invalid selector [" + name +"]: Only attributes with an enumeration type are supported");
         return null;
      }
      EnumType enumtype = (EnumType) attrDef.getType();
      List<Option> options = enumtype.getValues().stream()
            .map((s) -> createOption(s))
            .collect(Collectors.toList());
      return new ConstantListSelectorGenerator(options);
   }
   
   private SelectorGenerator createMinMaxGenerator(Unit curUnit) {
	   if(minStr == null) {
         getValidator().error("Invalid selector [" + name +"]: No min value specified");
         return null;
       }
	   if(maxStr == null) {
		   getValidator().error("Invalid selector [" + name +"]: No max value specified");
	       return null;
	   }
	   if(incrementStr == null) {
		   getValidator().error("Invalid selector [" + name +"]: No increment value specified");
	       return null;
	   }
	   double min = Double.parseDouble(minStr);
	   double max = Double.parseDouble(maxStr);
	   double inc = Double.parseDouble(incrementStr);
	   if(min > max) {
		   getValidator().error("Invalid selector [" + name +"]: min value can not be greater than max value specified");
	       return null;
	   }
	   if(inc <= 0) {
		   getValidator().error("Invalid selector [" + name +"]: increment value can not be non positive value");
	       return null;
	   }
	   if(Unit.FAHRENHEIT.equals(curUnit)) {
		   return new TemperatureSelectorGenerator(min, max, inc, Unit.FAHRENHEIT);
	   }else {
		   return new MinMaxSelectorGenerator(min, max, inc, Unit.NONE);
	   }
	   
	}

   private Option createOption(String value) {
      Option option = new Option();
      option.setLabel(value);
      option.setValue(value);
      return option;
   }
}


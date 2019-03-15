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
package com.iris.capability.attribute.transform;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableSet;
import com.iris.capability.definition.AttributeTypes;
import com.iris.capability.registry.CapabilityRegistry;
import com.iris.device.model.AttributeDefinition;
import com.iris.messages.capability.Capability;
import com.iris.messages.capability.ProductCapability;
import com.iris.prodcat.ExternalApplication;
import com.iris.prodcat.ExternalApplication.PlatformType;
import com.iris.prodcat.Input;
import com.iris.prodcat.Input.InputType;
import com.iris.prodcat.ProductCatalogEntry;
import com.iris.prodcat.Step;
import com.iris.prodcat.Step.StepType;
import com.iris.util.IrisCollections;

public class ProductCatalogEntryTransformer extends
      ReflectiveBeanAttributesTransformer<ProductCatalogEntry> {
	
	private static class StepAttribute {
      final static String TYPE = "type";
      final static String IMG = "img";
      final static String TEXT = "text";
      final static String SUBTEXT = "subText";
      final static String TARGET = "target";
      final static String MESSAGE = "message";
      final static String INPUTS = "inputs";
      final static String APPS = "apps";
      final static String ORDER = "order";
	}
	
	private static class InputAttribute {
		final static String NAME = "name";
		final static String LABEL = "label";
		final static String TYPE = "type";
		final static String VALUE = "value";
		final static String REQUIRED = "required";
		final static String MAXLEN = "maxlen";
		final static String MINLEN = "minlen";
	}
	
	private static class AppAttribute {
		final static String PLATFORM = "platform";
		final static String APPURL = "appUrl";		
	}

	private static final String EMPTY_SPACES = "           ";


   public ProductCatalogEntryTransformer(CapabilityRegistry registry) {
      super(
            registry, 
            ImmutableSet.of(ProductCapability.NAMESPACE, Capability.NAMESPACE), 
            ProductCatalogEntry.class
      );
   }

   /* (non-Javadoc)
    * @see com.iris.capability.attribute.transform.ReflectiveBeanAttributesTransformer#getValue(java.lang.Object, com.iris.device.model.AttributeDefinition)
    */
   @Override
   protected Object getValue(ProductCatalogEntry bean, AttributeDefinition definition)
         throws Exception {
      if(ProductCapability.ATTR_PAIR.equals(definition.getName())) {
         return transformStepsToListOfMap(bean.getPair());
      }
      else if(ProductCapability.ATTR_REMOVAL.equals(definition.getName())) {
          return transformRemovalStepsToListOfMap(bean.getRemoval());
       }
      else if(ProductCapability.ATTR_RESET.equals(definition.getName())) {
         return transformStepsToListOfMap(bean.getReset());
      }
      else if(ProductCapability.ATTR_RECONNECT.equals(definition.getName())) {
          return transformStepsToListOfMap(bean.getReconnect());
       }
      else {
         return super.getValue(bean, definition);
      }
   }

   private Object transformStepsToListOfMap(List<Step> steps) {
      List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
      if(steps != null) {
         for(Step step: steps) {
            result.add(transformOneStepToMap(step));
         }
      }
      return result;
   }
   
   private Map<String, Object> transformOneStepToMap(Step step) {
   	return IrisCollections
	      .<String, Object>map()
	      .put(StepAttribute.TYPE, AttributeTypes.coerceString(step.getType()))
         .put(StepAttribute.IMG, AttributeTypes.coerceString(step.getImg()))
         .put(StepAttribute.TEXT, AttributeTypes.coerceString(step.getText()))
         .put(StepAttribute.SUBTEXT, AttributeTypes.coerceString(step.getSubText()))
         .put(StepAttribute.TARGET, AttributeTypes.coerceString(step.getTarget()))
         .put(StepAttribute.MESSAGE, AttributeTypes.coerceString(step.getMessage()))
         .put(StepAttribute.INPUTS, transformInputsToListOfMap(step.getInputs()))
         .put(StepAttribute.APPS, transformAppsToListOfMap(step.getExternalApplications()))
	      .put(StepAttribute.ORDER, AttributeTypes.coerceInt(step.getOrder()))
	      .create();
   }
   
   /**
    * If there are multiple removal steps, they will be concatenated into a single step.  This is to make it
    * backward compatible for existing mobile apps.
    * @param steps
    * @return
    */
   private Object transformRemovalStepsToListOfMap(List<Step> steps) {
   	
   	if(steps == null || steps.size() <= 1 ) {
   		return transformStepsToListOfMap(steps);
   	}
   	String concatenatedText = conatenateTexts(steps);
   	if(concatenatedText == null) {
   		//not every step is TEXT type
			return transformStepsToListOfMap(steps);
		}
   	//more than one removal step
      List<Map<String, Object>> result = new ArrayList<Map<String, Object>>(1);
      Map<String, Object> stepMap = transformOneStepToMap(steps.get(0));
      stepMap.put("text", concatenatedText);  //overwrite it with the concatenated text
      result.add(stepMap);
      return result;
   }
   
   //only if the type for each step is TEXT.  Otherwise return null
   private String conatenateTexts(List<Step> steps) {
   	StringBuffer buf = new StringBuffer();
   	for(Step s : steps) {
   		if(!Step.StepType.TEXT.equals(s.getType())) {
   			return null;
   		}else{
   			if(s.getOrder() > 0) {
   				buf.append(EMPTY_SPACES).append(s.getOrder()).append(". ");
   			}
   			buf.append(s.getText());
   		}
   	}
   	return buf.toString();
   }
   
   private Object transformAppsToListOfMap(List<ExternalApplication> externalApplications) {
   	List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
	   if (externalApplications != null) {
		   for(ExternalApplication cur: externalApplications) {
			   result.add(
		               IrisCollections
		                  .<String, Object>map()
		                  .put(AppAttribute.PLATFORM, AttributeTypes.coerceString(cur.getPlatform()))
		                  .put(AppAttribute.APPURL, AttributeTypes.coerceString(cur.getAppUrl()))		                  
		                  .create()
		            );
		   }
	   	}
	    return result;
	}

	private Object transformInputsToListOfMap(List<Input> inputs) {
	   List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
	   if (inputs != null) {
		   for(Input input: inputs) {
			   result.add(
		               IrisCollections
		                  .<String, Object>map()
		                  .put(InputAttribute.NAME, AttributeTypes.coerceString(input.getName()))
		                  .put(InputAttribute.LABEL, AttributeTypes.coerceString(input.getLabel()))
		                  .put(InputAttribute.TYPE, AttributeTypes.coerceString(input.getType()))
		                  .put(InputAttribute.VALUE, AttributeTypes.coerceString(input.getValue()))
		                  .put(InputAttribute.REQUIRED, AttributeTypes.coerceBoolean(input.getRequired()))
		                  .put(InputAttribute.MAXLEN, AttributeTypes.coerceInt(input.getMaxlen()))
		                  .put(InputAttribute.MINLEN, AttributeTypes.coerceInt(input.getMinlen()))
		                  .create()
		            );
		   }
	   	}
	    return result;
   }

   /* (non-Javadoc)
    * @see com.iris.capability.attribute.transform.ReflectiveBeanAttributesTransformer#setValue(java.lang.Object, java.lang.Object, com.iris.device.model.AttributeDefinition)
    */
   @Override
   protected void setValue(ProductCatalogEntry bean, Object value, AttributeDefinition definition) throws Exception {
      if(ProductCapability.ATTR_PAIR.equals(definition.getName())) {
         List<Step> steps = transformMapToSteps(value);
         bean.setPair(steps);
      }
      else if(ProductCapability.ATTR_REMOVAL.equals(definition.getName())) {
          List<Step> steps = transformMapToSteps(value);
          bean.setRemoval(steps);
       }
      else if(ProductCapability.ATTR_RESET.equals(definition.getName())) {
         List<Step> steps = transformMapToSteps(value);
         bean.setReset(steps);
      }
      else if(ProductCapability.ATTR_RECONNECT.equals(definition.getName())) {
          List<Step> steps = transformMapToSteps(value);
          bean.setReconnect(steps);
       }
      else {
         super.setValue(bean, value, definition);
      }
   }
      
   private List<Step> transformMapToSteps(Object value) {
      List<Step> steps = new ArrayList<Step>();
      if(value == null) {
         return steps;
      }
      List<Object> rawSteps = AttributeTypes.coerceList(value);
      for(Object rawStep: rawSteps) {
         steps.add(transformMapToOneStep(rawStep));
      }
      return steps;
   }
   
   private Step transformMapToOneStep(Object rawStep) {
   	Map<String, Object> map = AttributeTypes.coerceMap(rawStep);
   	Step step = new Step();
   	step.setType(coerceStepEnum(map.get(StepAttribute.TYPE)));
      step.setImg(AttributeTypes.coerceString(map.get(StepAttribute.IMG)));
      step.setText(AttributeTypes.coerceString(map.get(StepAttribute.TEXT)));
      step.setSubText(AttributeTypes.coerceString(map.get(StepAttribute.SUBTEXT)));
      step.setMessage(AttributeTypes.coerceString(map.get(StepAttribute.MESSAGE)));
      step.setTarget(AttributeTypes.coerceString(map.get(StepAttribute.TARGET)));
      step.setInputs(transformMapToInputs(map.get(StepAttribute.INPUTS)));
      step.setExternalApplications(transformMapToApps(map.get(StepAttribute.APPS)));         
      step.setOrder(AttributeTypes.coerceInt(map.get(StepAttribute.ORDER)));
      return step;
   }
   
   
   private List<Input> transformMapToInputs(Object value) {
	   List<Input> inputs = new ArrayList<Input>();
	   if (value == null) 
		   return inputs;
	   
	   List<Object> rawInputs = AttributeTypes.coerceList(value);
	   for (Object rawInput: rawInputs) {
		   Map<String,Object> map = AttributeTypes.coerceMap(rawInput);
		   
		   Input input = new Input();
		   input.setName(AttributeTypes.coerceString(map.get(InputAttribute.NAME)));
		   input.setLabel(AttributeTypes.coerceString(map.get(InputAttribute.LABEL)));
		   input.setType(coerceInputEnum(map.get(InputAttribute.TYPE)));
		   input.setValue(AttributeTypes.coerceString(map.get(InputAttribute.VALUE)));
		   input.setRequired(AttributeTypes.coerceBoolean(map.get(InputAttribute.REQUIRED)));
		   input.setMaxlen(AttributeTypes.coerceInt(map.get(InputAttribute.MAXLEN)));
		   input.setMinlen(AttributeTypes.coerceInt(map.get(InputAttribute.MINLEN)));
		   inputs.add(input);
	   }
	   return inputs;
   }
   
   private List<ExternalApplication> transformMapToApps(Object value) {
	   List<ExternalApplication> apps = new ArrayList<ExternalApplication>();
	   if (value == null) 
		   return apps;
	   
	   List<Object> rawApps = AttributeTypes.coerceList(value);
	   for (Object rawApp: rawApps) {
		   Map<String,Object> map = AttributeTypes.coerceMap(rawApp);
		   
		   ExternalApplication app = new ExternalApplication();
		   app.setPlatform(coercePlatformTypeEnum(map.get(AppAttribute.PLATFORM)));
		   app.setAppUrl(AttributeTypes.coerceString(map.get(AppAttribute.APPURL)));
		   apps.add(app);
	   }
	   return apps;
   }

   private InputType coerceInputEnum(Object e) {
	      if(e == null || !(e instanceof String)) {
	          return null;
	       }
	       String s = (String) e;
	       if(InputType.HIDDEN.name().equalsIgnoreCase(s)) {
	          return InputType.HIDDEN;
	       }
	       else if(InputType.TEXT.name().equalsIgnoreCase(s)) {
	          return InputType.TEXT;
	       }
	       return null;	   
   }
   
   
   private StepType coerceStepEnum(Object e) {
      if(e == null || !(e instanceof String)) {
         return null;
      }
      String s = (String) e;
      if(StepType.INPUT.name().equalsIgnoreCase(s)) {
         return StepType.INPUT;
      }
      else if(StepType.TEXT.name().equalsIgnoreCase(s)) {
         return StepType.TEXT;
      }else if(StepType.EXTERNAL_APP.name().equalsIgnoreCase(s)) {
         return StepType.EXTERNAL_APP;
      }
      return null;
   }
   
   private PlatformType coercePlatformTypeEnum(Object e) {
      if(e == null || !(e instanceof String)) {
         return null;
      }
      String s = (String) e;
      if(PlatformType.ANDROID.name().equalsIgnoreCase(s)) {
         return PlatformType.ANDROID;
      }
      else if(PlatformType.IOS.name().equalsIgnoreCase(s)) {
         return PlatformType.IOS;
      }
      return null;
   }

}


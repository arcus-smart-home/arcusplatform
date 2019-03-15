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
package com.iris.oculus.modules.capability.ux;

import java.awt.Component;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import com.google.common.collect.ImmutableMap;
import com.iris.bootstrap.ServiceLocator;
import com.iris.capability.definition.AttributeType;
import com.iris.capability.definition.AttributeTypes;
import com.iris.capability.definition.Definitions;
import com.iris.capability.definition.MethodDefinition;
import com.iris.capability.definition.ParameterDefinition;
import com.iris.capability.definition.AttributeType.RawType;
import com.iris.client.event.ClientFuture;
import com.iris.client.session.SessionInfo;
import com.iris.oculus.modules.session.OculusSession;
import com.iris.oculus.modules.session.SessionController;
import com.iris.oculus.widget.Dialog;
import com.iris.oculus.widget.Fields;
import com.iris.oculus.widget.Fields.FieldBuilder;
import com.iris.oculus.widget.FormView;


/**
 * 
 */
public class CapabilityInputPrompt {

   public static ClientFuture<Map<String, Object>> prompt(MethodDefinition definition) {
      return prompt(definition, ImmutableMap.of());
   }
   
   public static ClientFuture<Map<String, Object>> prompt(
         MethodDefinition definition,
         Map<String, Object> defaults
   ) {
      CommandInputDialog dialog = new CommandInputDialog(definition, defaults);
      return dialog.prompt();
   }
   
   private static class CommandInputDialog extends Dialog<Map<String, Object>> {
      private MethodDefinition method;
      private Map<String, Object> defaults;
      private FormView view = new FormView();
      
      CommandInputDialog(MethodDefinition command, Map<String, Object> defaults) {
         this.method = command;
         this.defaults = defaults;
         
         setResizable(true);
      }
      
      @Override
      protected Map<String, Object> getValue() {
         return view.getValues();
      }

      @Override
      protected Component createContents() {
         for(ParameterDefinition argument: method.getParameters()) {
            AttributeType type = argument.getType();

            FieldBuilder<? extends Component, ?> builder =
                  Fields
                     .attributeTypeBuilder(type)
                     .named(argument.getName())
                     .labelled(argument.getName())
                     ;
            view.addField(builder.build());
            if(isPlace(argument)) {
            	view.setValue(argument.getName(), getPlaceId());
            }
            if(!type.isPrimitive() && !type.isEnum()) {
               view.addComponent(new JLabel("<html><i>JSON encoded " + type + "</i></html>"));
            }
         }

         JButton execute = new JButton(submitAction("Execute"));
         getRootPane().setDefaultButton(execute);

         view.addButton(new JButton(cancelAction()));
         view.addButton(execute);
         for(Map.Entry<String, Object> e: defaults.entrySet()) {
            view.setValue(e.getKey(), e.getValue());
         }
         return view.getComponent();
      }

		private boolean isPlace(ParameterDefinition argument) {
			return
					("placeid".equalsIgnoreCase(argument.getName()) || "place".equalsIgnoreCase(argument.getName())) && 
					argument.getType().getRawType() == RawType.STRING;
		}

		private String getPlaceId() {
			OculusSession session = ServiceLocator.getInstance(SessionController.class).getSessionInfo();
			if(session == null) {
				return null;
			}
			else {
				return session.getPlaceId();
			}
		}
   }
   
   private static enum Test {
      ON, OFF;
   }
   
   private static ParameterDefinition buildParameter(String name, Class<?> type) {
      return
            Definitions
               .parameterBuilder()
               .withName(name)
               .withType(AttributeTypes.fromJavaType(type))
               .build();
   }
   
   public static void main(String [] args) throws InvocationTargetException, InterruptedException {
      MethodDefinition definition =
            Definitions
               .methodBuilder()
               .withName("test:CommandPrompt")
               .addParameter(buildParameter("boolean", Boolean.class))
               .addParameter(buildParameter("byte", Byte.class))
               .addParameter(buildParameter("int", Integer.class))
//               .addParameter(buildAttribute("long", Long.class))
//               .addParameter(buildAttribute("double", Double.class))
               .addParameter(buildParameter("timestamp", Date.class))
               .addParameter(buildParameter("enum", Test.class))
               .build()
               ;
      SwingUtilities.invokeAndWait(() -> {
         CapabilityInputPrompt
            .prompt(definition)
            .onCompletion((result) -> {
               System.out.println("Result: " + result);
               System.exit(0);
            })
            ;
      });
   }
}


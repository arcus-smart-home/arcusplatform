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
package com.iris.platform.rule.catalog.serializer;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Set;

import org.xml.sax.Attributes;

import com.iris.platform.rule.catalog.action.IfActionTemplate;
import com.iris.validators.Validator;

public class IfActionProcessor extends ActionsContainerProcessor
{
   public static final String TAG = "if";
   public static final String ATTR_VAR = "var";
   public static final String ATTR_EQUALS = "equals";
   public static final String ATTR_NOT_EQUALS = "not-equals";

   private final Set<String> parentContextVariables;

   private IfActionTemplate template;

   protected IfActionProcessor(Validator validator, Set<String> parentContextVariables)
   {
      super(validator);

      this.parentContextVariables = parentContextVariables;
   }

   @Override
   public void enterTag(String qName, Attributes attributes)
   {
      if (TAG.equals(qName))
      {
         String equalsValue = attributes.getValue(ATTR_EQUALS);
         String notEqualsValue = attributes.getValue(ATTR_NOT_EQUALS);

         checkArgument(equalsValue != null ^ notEqualsValue != null,
            "Must specify either %s or %s, but not both", ATTR_EQUALS, ATTR_NOT_EQUALS);

         template = new IfActionTemplate(parentContextVariables);

         template.setVarExpression(getTemplatedExpression(ATTR_VAR, attributes));

         if (equalsValue != null)
         {
            template.setEqualsExpression(getTemplatedExpression(ATTR_EQUALS, attributes));
         }
         else
         {
            template.setNotEqualsExpression(getTemplatedExpression(ATTR_NOT_EQUALS, attributes));
         }
      }

      super.enterTag(qName, attributes);
   }

   public IfActionTemplate getIfActionTemplate()
   {
      template.setActions(getActions());

      return template;
   }
}


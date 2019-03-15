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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.xml.sax.Attributes;

import com.iris.platform.rule.catalog.ActionTemplate;
import com.iris.platform.rule.catalog.action.LogTemplate;
import com.iris.platform.rule.catalog.action.NotificationCustomMessageTemplate;
import com.iris.platform.rule.catalog.action.NotificationMessageTemplate;
import com.iris.platform.rule.catalog.action.SendMessageTemplate;
import com.iris.platform.rule.catalog.action.SetAndRestoreTemplate;
import com.iris.platform.rule.catalog.template.TemplatedExpression;
import com.iris.platform.rule.catalog.template.TemplatedValue;
import com.iris.serializer.sax.TagProcessor;
import com.iris.util.IrisCollections;
import com.iris.validators.Validator;

public abstract class ActionsContainerProcessor extends BaseCatalogProcessor {
   public static final String TAG_SET = "set-attribute";
   public static final String TAG_NOTIFY = "notify";
   public static final String TAG_NOTIFY_CUSTOM = "notify-custom";
   public static final String TAG_SEND = "send";
   public static final String TAG_LOG = "log";
   public static final String TAG_ATTRIBUTE = "attribute";
   public static final String TAG_PARAMETER = "parameter";
   
   private List<ActionTemplate> actions = new ArrayList<ActionTemplate>();

   private final Set<String> contextVariables = new HashSet<>();
   
   protected ActionsContainerProcessor(Validator v) {
      super(v);
   }
   
   public List<ActionTemplate> getActions() {
      return actions;
   }
   
   /* (non-Javadoc)
    * @see com.iris.platform.rule.catalog.serializer.BaseTagHandler#getHandler(java.lang.String, org.xml.sax.Attributes)
    */
   @Override
   public TagProcessor getHandler(String qName, Attributes attributes) {
      if(IfActionProcessor.TAG.equals(qName)) {
         return new IfActionProcessor(getValidator(), getContextVariables());
      }
      if(DeviceQueryProcessor.TAG.equals(qName)) {
         return new DeviceQueryProcessor(getValidator(), getContextVariables());
      }
      if(ParametersProcessor.TAG.equals(qName)) {
         return new ParametersProcessor(getValidator());
      }
      return this;
   }
   
   /* (non-Javadoc)
    * @see com.iris.platform.rule.catalog.serializer.BaseTagHandler#enterTag(java.lang.String, org.xml.sax.Attributes)
    */
   @Override
   public void enterTag(String qName, Attributes attributes) {
      if(TAG_SET.equals(qName)) {
         addSetAttributeAction(attributes);
      }

      else if(TAG_NOTIFY.equals(qName)) {
         addNotificationMessage(attributes);
      }
      
      else if(TAG_NOTIFY_CUSTOM.equals(qName)) {
         addNotificationCustomMessage(attributes);
      }

      else if(TAG_SEND.equals(qName)) {
         addSendMessage(attributes);
      }

      else if(TAG_LOG.equals(qName)) {
         addLogMessage(attributes);
      }
      
      // TODO should verify we're in the context of a send message
      else if(TAG_ATTRIBUTE.equals(qName)) {
         addAttributeToSendMessage(attributes);
      }
   }
   
   @Override
   public void exitChildTag(String qName, TagProcessor handler) {
      if (IfActionProcessor.TAG.equals(qName)) {
         actions.add(((IfActionProcessor)handler).getIfActionTemplate());
      }
      else if (DeviceQueryProcessor.TAG.equals(qName)) {
         actions.add(((DeviceQueryProcessor)handler).getForEachModelTemplate());
      }
      else if (ParametersProcessor.TAG.equals(qName)) {
         NotificationMessageTemplate notify = getNotifyContext();
         if (notify != null) {
            notify.setParameters(((ParametersProcessor)handler).getParameters());
            notify.setParameterConfigs(((ParametersProcessor)handler).getParameterConfigs());
         }
      }
   }
   
   @Override
   public void exitTag(String qName) {
      super.exitTag(qName);
   }
   
   protected void addContextVariable(String variableName) {
      contextVariables.add(variableName);
   }
   
   protected Set<String> getContextVariables() {
      return Collections.unmodifiableSet(contextVariables);
   }
   
   private void addNotificationCustomMessage(Attributes attributes) {
      NotificationCustomMessageTemplate notification = new NotificationCustomMessageTemplate(getContextVariables());
      notification.setTo(getTemplatedString("to", attributes));
      notification.setMethod(getTemplatedString("method", "email", attributes));
      notification.setMessage(getTemplatedString("message", attributes));
      actions.add(notification);
   }
   
   private void addNotificationMessage(Attributes attributes) {
      NotificationMessageTemplate notification = new NotificationMessageTemplate(getContextVariables());
      notification.setTo(getTemplatedString("to", attributes));
      notification.setPriority(getTemplatedString("priority", attributes));
      notification.setKey(attributes.getValue("key"));
      actions.add(notification);
   }

   private void addSetAttributeAction(Attributes attributes) {
      SetAndRestoreTemplate action = new SetAndRestoreTemplate(getContextVariables());
      String attributeName = attributes.getValue("name");

      getValidator().assertFalse(TemplatedValue.isTemplated(attributeName), "attribute name may not be templated");

      action.setAddress(getTemplatedExpression("to", attributes));
      action.setAttributeName(attributeName);
      action.setAttributeType(getAttributeType(attributeName));
      action.setAttributeValue(getTemplatedExpression("value", attributes));
      action.setDuration(getTemplatedExpression("duration", new TemplatedExpression("0"), attributes));
      action.setUnit(getEnumValue("unit", TimeUnit.class, TimeUnit.SECONDS, attributes));
      action.setConditionQuery(getTemplatedExpression("condition-query", null, attributes));
      action.setReevaluateCondition(getTemplatedExpression("conditional", new TemplatedExpression("false"), attributes));
      actions.add(action);
   }

   private void addSendMessage(Attributes attributes) {
      TemplatedExpression to = getTemplatedExpression("to", attributes);
      String method = getValue("method", attributes);

      SendMessageTemplate message = new SendMessageTemplate(getContextVariables());
      message.setTo(to);
      message.setType(method);

      actions.add(message);
   }

   private void addAttributeToSendMessage(Attributes attributes) {
      String attributeName = getValue("name", attributes);
      // TODO maintain typing?
      TemplatedExpression value = getTemplatedExpression("value", attributes);

      SendMessageTemplate message = (SendMessageTemplate) actions.get(actions.size() - 1);
      message.addAttribute(attributeName, value);
   }

   private void addLogMessage(Attributes attributes) {
      TemplatedValue<String> message = getTemplatedString("message", attributes);
      LogTemplate log = new LogTemplate();
      log.setMessage(message);

      actions.add(log);
   }
   
   private boolean isInNotifyContext() {
      ActionTemplate action = IrisCollections.last(actions);
      return action != null && action instanceof NotificationMessageTemplate;
   }
   
   private NotificationMessageTemplate getNotifyContext() {
      if (isInNotifyContext()) {
         return (NotificationMessageTemplate)IrisCollections.last(actions);
      } else {
         getValidator().error("Expected current element to be notify but was " + IrisCollections.last(actions));
         return null;
      }
   }
}


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
package com.iris.common.subsystem.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.iris.common.subsystem.SubsystemContext;
import com.iris.common.subsystem.SubsystemUtils;
import com.iris.messages.MessageBody;
import com.iris.messages.address.Address;
import com.iris.messages.capability.NotificationCapability;
import com.iris.messages.capability.NotificationCapability.NotifyRequest;
import com.iris.messages.capability.PersonCapability;
import com.iris.messages.event.ScheduledEvent;
import com.iris.messages.model.Model;
import com.iris.messages.model.subs.SubsystemModel;
import com.iris.messages.type.CallTreeEntry;
import com.iris.util.TypeMarker;

public class CallTree {

   public static final String CALL_TREE_INDEX_KEY = "calltree.index";
   public static final String CALL_TREE_TIMEOUT_KEY = "calltree.timeout";
   public static final String NOTIFICATION_PARAMETERS_KEY = "notification.params";
   private static final String SEQUENTIAL_TIMEOUT_MS_KEY = "calltree.sequential.timeout";
   private static final String NOTIFICATION_KEY_LOOKUP_KEY = "notification.key";
   private final String CALL_TREE_ATTR;
   private static final TypeMarker<List<Map<String, Object>>> CALL_TREE_MARKER =
         new TypeMarker<List<Map<String,Object>>>() {};

   public CallTree(String callTreeContextAttr) {
      this.CALL_TREE_ATTR = callTreeContextAttr;
   }
   
   public void notifySequential(SubsystemContext<?> context, String notificationKey, int delayMs) {
   	notifySequential(context, notificationKey, null, delayMs);
   }

   public void notifySequential(SubsystemContext<?> context, String notificationKey, Map<String, String> params, int delayMs) {
      context.logger().debug("Received an alertSequential for context [{}]", context);
      context.setVariable(CALL_TREE_INDEX_KEY, 0);
      context.setVariable(NOTIFICATION_KEY_LOOKUP_KEY,notificationKey );
      context.setVariable(NOTIFICATION_PARAMETERS_KEY, params);
      context.setVariable(SEQUENTIAL_TIMEOUT_MS_KEY, delayMs);
      notifyNextPerson(context,notificationKey, params);
   }

   public void notifyParallel(SubsystemContext<?> context, String notificationKey, Map<String, String> alertParams) {
      context.logger().debug("Received an alertAll for cause [{}]", notificationKey);
      List<Map<String, Object>> callTree = getCallTree(context);

      for (Map<String, Object> cte : callTree){
         MessageBody message = MessageBody.buildMessage(
               CallTreeEntry.TYPE.getRepresentation(),
               cte);
         if (CallTreeEntry.getEnabled(message)){
            sendNotification(context, notificationKey, alertParams, CallTreeEntry.getPerson(message), NotificationCapability.NotifyRequest.PRIORITY_CRITICAL);
         }
      }
   }

   // pass through from the subsystem
   public void onScheduledEvent(ScheduledEvent event, SubsystemContext<?> context) {
      context.logger().debug("Received an onScheduledEvent for event [{}]", event);
      String notificationKey = context.getVariable(NOTIFICATION_KEY_LOOKUP_KEY).as(String.class);
      Map<String, String> params = context.getVariable(NOTIFICATION_PARAMETERS_KEY).as(TypeMarker.mapOf(String.class, String.class));
      notifyNextPerson(context, notificationKey, params);
   }

   // should be called by disarm / acknowledge
   public void cancel(SubsystemContext<?> context) {
      context.logger().debug("Call tree was cancelled by a user disarming or acknowledging the alert [{}]", context);
      context.setVariable(CALL_TREE_INDEX_KEY, 0);
      SubsystemUtils.clearTimeout(context, CALL_TREE_TIMEOUT_KEY);
   }

   private void notifyNextPerson(SubsystemContext<?> context,String notificationKey, Map<String,String> params) {
      String personToCall = nextInTree(context);
      if (personToCall != null){
         sendNotification(context, notificationKey, params, personToCall, NotificationCapability.NotifyRequest.PRIORITY_CRITICAL);
         if (hasNext(context)){
            int nextTimeout = context.getVariable(SEQUENTIAL_TIMEOUT_MS_KEY).as(Integer.class);
            SubsystemUtils.setTimeout(nextTimeout, context, CALL_TREE_TIMEOUT_KEY);
         }
      }else{
         context.logger().debug("No more call tree entires [{}]", context);
         // Need to create a failed to contact?
      }
   }

   public boolean hasNext(SubsystemContext<?> context) {
      List<Map<String, Object>> calltree = getCallTree(context);
      int currentCallerIndex = getCallTreeIndex(context);
      if (currentCallerIndex > calltree.size() - 1){
         return false;
      }
      for (Map<String, Object> cte : calltree.subList(currentCallerIndex, calltree.size())){
         if (Boolean.TRUE.equals(cte.get(CallTreeEntry.ATTR_ENABLED))){
            return true;
         }
      }
      return false;
   }

   private String nextInTree(SubsystemContext<?> context) {
      List<Map<String, Object>> calltree = getCallTree(context);
      int currentCallerIndex = getCallTreeIndex(context);
      if (calltree.size() <= currentCallerIndex){
         return null;
      }
      else{
         context.setVariable(CALL_TREE_INDEX_KEY, currentCallerIndex + 1);

         MessageBody message = MessageBody.buildMessage(
               CallTreeEntry.TYPE.getRepresentation(),
               calltree.get(currentCallerIndex));

         if (!CallTreeEntry.getEnabled(message)){
            return nextInTree(context);
         }
         return CallTreeEntry.getPerson(message);
      }
   }

   private int getCallTreeIndex(SubsystemContext<?> context) {
      int currentCallerIndex = context.getVariable(CALL_TREE_INDEX_KEY).as(Integer.class);
      return currentCallerIndex;
   }

   private void sendNotification(SubsystemContext<?> context, String key, Map<String, String> params, String personId, String priority) {
      NotifyRequest.Builder builder = NotifyRequest
		      		.builder()
		            .withMsgKey(key)
		            .withPlaceId(context.getPlaceId().toString())
		            .withPersonId(Address.fromString(personId).getId().toString())
		            .withPriority(priority);
      if (params != null && !params.isEmpty()) {
      	builder.withMsgParams(params);
      }
      MessageBody message = builder.build();
      context.send(Address.platformService(NotificationCapability.NAMESPACE), message);
   }

   @SuppressWarnings("unchecked")
   private List<Map<String, Object>> getCallTree(SubsystemContext<?> context) {
      List<Map<String, Object>> callTree = (List<Map<String, Object>>) context.model().getAttribute(CALL_TREE_ATTR);
      return callTree;
   }
   
   public void syncCallTree(SubsystemContext<? extends SubsystemModel> context){
      String accountOwner = NotificationsUtil.getAccountOwnerAddress(context);

      List<Map<String,Object>>currentCallTree=context.model().getAttribute(CALL_TREE_MARKER,CALL_TREE_ATTR).get();

      // maintain sorted order
      Map<String, Boolean> callTree = CallTree.callTreeToMap(currentCallTree);

      Set<String> personAddresses = new HashSet<>();
      for (Model model : context.models().getModelsByType(PersonCapability.NAMESPACE)){

         String personAddress = model.getAddress().getRepresentation();
         personAddresses.add(personAddress);

         // if they're already added to the call tree, don't overwrite the
         // current settings
         if (callTree.containsKey(personAddress)){
            continue;
         }

         // else add them, but don't enable it
         boolean isAccountOwner = accountOwner.equals(personAddress);
         callTree.put(personAddress, isAccountOwner);
      }

      // remove stale entries
      callTree.keySet().retainAll(personAddresses);

      List<Map<String, Object>> callTreeEntries = CallTree.callTreeToList(callTree);
      context.model().setAttribute(CALL_TREE_ATTR, callTreeEntries);
   }
   
   public static LinkedHashMap<String, Boolean> callTreeToMap(List<Map<String, Object>> callTree) {
      LinkedHashMap<String, Boolean> callTreeMap = new LinkedHashMap<>(callTree.size() + 1);
      for(Map<String, Object> entry: callTree) {
         String personAddress = (String) entry.get(CallTreeEntry.ATTR_PERSON);
         if(personAddress == null) {
            continue;
         }
         
         // dynamically fix legacy call tree entries
         if (!personAddress.startsWith("SERV:")){
            personAddress = "SERV:" + PersonCapability.NAMESPACE + ":" + personAddress;
         }
         
         boolean enabled = Boolean.TRUE.equals(entry.get(CallTreeEntry.ATTR_ENABLED));
         callTreeMap.put(personAddress, enabled);
      }
      return callTreeMap;
   }
   
   public static List<Map<String, Object>> callTreeToList(Map<String, Boolean> callTree) {
      List<Map<String, Object>> callTreeEntries = new ArrayList<>(callTree.size() + 1);
      for(Map.Entry<String, Boolean> callTreeEntry: callTree.entrySet()) {
         callTreeEntries.add( createCallTreeEntry(callTreeEntry.getKey(), callTreeEntry.getValue()) );
      }
      return callTreeEntries;
   }
   private static Map<String, Object> createCallTreeEntry(String personAddress, Boolean enabled) {
      return 
         ImmutableMap
            .<String, Object>of(
                  CallTreeEntry.ATTR_PERSON, personAddress,
                  CallTreeEntry.ATTR_ENABLED, Boolean.TRUE.equals(enabled) // nulls become false
            );
   }   
}


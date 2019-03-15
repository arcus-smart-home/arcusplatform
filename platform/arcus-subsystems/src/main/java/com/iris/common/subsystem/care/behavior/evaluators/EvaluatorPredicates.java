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
package com.iris.common.subsystem.care.behavior.evaluators;

import com.google.common.base.Predicate;
import com.iris.messages.capability.ContactCapability;
import com.iris.messages.model.Model;
import com.iris.messages.model.dev.ContactModel;

public class EvaluatorPredicates {

   public static Predicate<Model> OPEN_CONTACT = openContactPredicate();
   
   private static Predicate<Model>openContactPredicate(){
      return new Predicate<Model>() {
         @Override
         public boolean apply(Model model) {
            return ContactCapability.CONTACT_OPENED.equals(ContactModel.getContact(model));
         }
      };
   } 
}


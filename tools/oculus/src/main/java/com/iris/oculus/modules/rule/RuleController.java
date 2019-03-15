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
package com.iris.oculus.modules.rule;

import java.util.Collection;
import java.util.Map;

import javax.inject.Singleton;

import com.iris.client.IrisClientFactory;
import com.iris.client.event.ClientFuture;
import com.iris.client.model.RuleModel;
import com.iris.client.model.RuleTemplateModel;
import com.iris.client.model.Store;
import com.iris.client.service.RuleService;
import com.iris.client.service.RuleService.ListRulesResponse;
import com.iris.oculus.Oculus;
import com.iris.oculus.modules.BaseController;
import com.iris.oculus.modules.rule.ux.RuleEditorWizard;
import com.iris.oculus.modules.session.SessionAwareController;

/**
 * 
 */
@Singleton
public class RuleController extends BaseController<RuleModel> {
   private Store<RuleTemplateModel> templates;

   /**
    * 
    */
   public RuleController() {
      super(RuleModel.class);
      templates = IrisClientFactory.getStore(RuleTemplateModel.class);
   }

   public Store<RuleTemplateModel> getTemplates() {
      return templates;
   }

   public void createRuleFromTemplate() {
      reloadTemplates();
      RuleEditorWizard.create(getPlaceId().toString());
   }
   
   public ClientFuture<?> reloadTemplates() {
      return
         IrisClientFactory
            .getService(RuleService.class)
            .listRuleTemplates(getPlaceId().toString())
            .onFailure((error) -> Oculus.error("Unable to load rule templates", error));
   }
   
   public void reloadRules() {
      IrisClientFactory
         .getService(RuleService.class)
         .listRules(getPlaceId().toString())
         .onFailure((error) -> Oculus.error("Unable to load rules", error));
   }

   public void enable(RuleModel model) {
      Oculus.showProgress(
            model.enable(),
            "Enabling rule..."
      );
   }
   
   public void disable(RuleModel model) {
      Oculus.showProgress(
            model.disable(),
            "Disabling rule..."
      );
   }
   
   public void update(RuleModel model) {
      reloadTemplates()
         .onSuccess((e) -> RuleEditorWizard.edit(getPlaceId().toString(), model))
         ;
   }

   public void delete(RuleModel model) {
      Oculus.showProgress(
            model.delete(),
            "Deleting rule..."
      );
   }



   @Override
   protected ClientFuture<? extends Collection<Map<String, Object>>> doLoad() {
      return 
         IrisClientFactory
            .getService(RuleService.class)
            .listRules(getPlaceId().toString())
            .transform(ListRulesResponse::getRules)
            ;

   }

}


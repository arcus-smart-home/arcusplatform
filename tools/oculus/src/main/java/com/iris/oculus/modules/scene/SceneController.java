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
package com.iris.oculus.modules.scene;

import javax.inject.Singleton;
import javax.swing.Action;

import com.iris.client.IrisClientFactory;
import com.iris.client.event.ClientFuture;
import com.iris.client.model.SceneModel;
import com.iris.client.model.SceneTemplateModel;
import com.iris.client.model.Store;
import com.iris.client.service.SceneService;
import com.iris.oculus.Oculus;
import com.iris.oculus.modules.rule.ux.RuleEditorWizard;
import com.iris.oculus.modules.scene.ux.SceneEditorWizard;
import com.iris.oculus.modules.session.SessionAwareController;
import com.iris.oculus.modules.session.OculusSession;
import com.iris.oculus.util.Actions;
import com.iris.oculus.util.DefaultSelectionModel;
import com.iris.oculus.util.SelectionModel;

/**
 * 
 */
@Singleton
public class SceneController extends SessionAwareController {
   private Store<SceneTemplateModel> templates;
   private Store<SceneModel> scenes;
   private SelectionModel<SceneModel> selector;
   private Action reload = Actions.build("Refresh", this, SceneController::reloadRules);

   /**
    * 
    */
   public SceneController() {
      this.templates = IrisClientFactory.getStore(SceneTemplateModel.class);
      this.scenes = IrisClientFactory.getStore(SceneModel.class);
      this.selector = new DefaultSelectionModel<>();
   }

   @Override
   protected void onSessionInitialized(OculusSession info) {
      reload();
   }
   
   public Store<SceneTemplateModel> getTemplates() {
      return templates;
   }

   public Store<SceneModel> getScenes() {
      return scenes;
   }

   public SelectionModel<SceneModel> getSceneSelector() {
      return selector;
   }
   
   public Action actionReloadRules() {
      return reload;
   }
   
   public void addScene() {
      // TODO don't really need to reload this every time
      ClientFuture<?> result = reloadTemplates();
      Oculus.showProgress(result, "Loading templates...");
      result
         .onSuccess((e) -> SceneEditorWizard.create(getPlaceId()))
         .onFailure((e) -> Oculus.showError("Unable to Load Scene Templates", e))
         ;
   }
   
   public void reload() {
      reloadRules();
   }
   
   public ClientFuture<?> reloadTemplates() {
      return
         IrisClientFactory
            .getService(SceneService.class)
            .listSceneTemplates(getPlaceId())
            .onFailure((error) -> Oculus.error("Unable to load scene templates", error));
   }
   
   public ClientFuture<?> reloadRules() {
      return
         IrisClientFactory
            .getService(SceneService.class)
            .listScenes(getPlaceId().toString())
            .onFailure((error) -> Oculus.error("Unable to load scenes", error));
   }

   public void edit(SceneModel model) {
      SceneEditorWizard.edit(getPlaceId(), model);
   }

}


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
package com.iris.oculus.modules;

import java.awt.Component;

import com.iris.client.model.Model;
import com.iris.oculus.OculusSection;
import com.iris.oculus.modules.capability.ux.CapabilityViewBuilder;
import com.iris.oculus.util.BaseComponentWrapper;
import com.iris.oculus.util.Models;

/**
 * 
 */
public abstract class BaseSection<M extends Model> extends BaseComponentWrapper<Component> implements OculusSection {
   private BaseController<M> controller;
   
   public BaseSection(BaseController<M> controller) {
      this.controller = controller;
   }

   protected BaseToolbar<M> createToolbar() {
      return new BaseToolbar<>();
   }
   
   protected CapabilityViewBuilder<M> viewBuilder() {
      BaseToolbar<M> toolbar = createToolbar();
      controller.addSelectedListener(toolbar);
      return
         CapabilityViewBuilder
            .builder(controller.getStore())
            .withTypeName(getName())
            .withModelSelector(
                  (model) -> renderLabel(model),
                  controller.getSelection(),
                  controller.actionReload()
            )
            .withToolbarComponent(toolbar.getComponent())
            .addShowListener((e) -> controller.reload())
            ;
   }
   
   @Override
   protected Component createComponent() {
      return viewBuilder().build();
   }

   protected String renderLabel(M model) {
      return Models.nameOf(model);
   }

}


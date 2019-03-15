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

import java.util.Optional;

import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.eclipse.jdt.annotation.Nullable;

import com.iris.client.ErrorEvent;
import com.iris.client.capability.Capability;
import com.iris.client.capability.Capability.SetAttributesErrorEvent;
import com.iris.client.event.Listener;
import com.iris.client.model.Model;
import com.iris.oculus.Oculus;
import com.iris.oculus.util.Actions;
import com.iris.oculus.util.BaseComponentWrapper;
import com.iris.oculus.widget.Toolbar;

/**
 * @author tweidlin
 *
 */
public class BaseToolbar<M extends Model>
      extends BaseComponentWrapper<JPanel>
      implements Listener<M>
{
   private Action refresh = Actions.builder().withName("Refresh").withRunnableAction(this::onRefresh).withToolTip("base:GetAttributes").build();
   private Action save = Actions.builder().withName("Save").withRunnableAction(this::onSave).withToolTip("base:SetAttributes").build();
   private M model;

   public BaseToolbar() {
   }
   
   @Nullable
   protected M model() { return model; }

   protected Action refresh() { return refresh; }
   protected Action save() { return save; }
   
   protected void onRefresh() {
      refresh.setEnabled(false);
      this
         .model
         .refresh()
         .onFailure((error) -> onRefreshFailed(error))
         .onCompletion((v) -> refresh.setEnabled(model != null))
         ;
   }

   protected void onSave() {
      save.setEnabled(false);
      this
         .model
         .commit()
         .onFailure((error) -> onSaveFailed(error))
         .onSuccess((event) -> { 
            if(SetAttributesErrorEvent.NAME.equals(event.getType())) {
               onSaveFailed(new SetAttributesErrorEvent(event));
            }
         })
         .onCompletion((v) -> save.setEnabled(model != null))
         ;
   }

   protected void onRefreshFailed(Throwable error) {
      Oculus.showError("Unable to Refresh", error);
   }

   protected void onSaveFailed(Throwable error) {
      Oculus.showError("Unable to Save", error);
   }

   protected void onSaveFailed(Capability.SetAttributesErrorEvent errors) {
      StringBuilder sb = new StringBuilder("<html>Unable to update some attributes:<ul>");
      for(ErrorEvent error: errors.getErrors()) {
         sb.append("<li>" + error.getCode() + ": " + error.getMessage() + "</li>");
      }
      sb.append("</li></html>");
      JOptionPane.showMessageDialog(getComponent(), sb.toString(), "Error", JOptionPane.WARNING_MESSAGE);
   }

   @Override
   public void onEvent(@Nullable M model) {
      if(model != null) {
         setModel(model);
      }
      else {
         clearModel();
      }
   }

   @Override
   protected JPanel createComponent() {
      if(model == null) {
         clearModel();
      }
      return
         Toolbar
            .builder()
            .center().addButton(refresh())
            .center().addButton(save())
            .build();
   }

   protected void setModel(M model) {
      this.model = model;
      this.refresh.setEnabled(true);
      this.save.setEnabled(true);
   }

   protected void clearModel() {
      this.refresh.setEnabled(false);
      this.save.setEnabled(false);
      this.model = null;
   }

}


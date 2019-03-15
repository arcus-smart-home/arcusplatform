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
package com.iris.oculus.modules.scene.ux;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;
import com.iris.bootstrap.Bootstrap;
import com.iris.bootstrap.ServiceLocator;
import com.iris.bootstrap.guice.GuiceServiceLocator;
import com.iris.capability.util.Addresses;
import com.iris.client.IrisClientFactory;
import com.iris.client.bean.ActionTemplate;
import com.iris.client.capability.Capability;
import com.iris.client.capability.SceneTemplate;
import com.iris.client.capability.SceneTemplate.ResolveActionsRequest;
import com.iris.client.capability.SceneTemplate.ResolveActionsResponse;
import com.iris.client.event.ClientFuture;
import com.iris.client.event.SettableClientFuture;
import com.iris.client.model.SceneModel;
import com.iris.client.model.SceneTemplateModel;
import com.iris.core.IrisApplicationModule;
import com.iris.oculus.Oculus;
import com.iris.oculus.OculusModule;
import com.iris.oculus.modules.scene.SceneController;
import com.iris.oculus.modules.session.SessionController;
import com.iris.oculus.util.Actions;
import com.iris.oculus.widget.Dialog;
import com.iris.oculus.widget.table.Table;
import com.iris.oculus.widget.table.TableModel;
import com.iris.oculus.widget.table.TableModelBuilder;

/**
 * 
 */
public class SceneEditorWizard {
   private static final Logger logger = LoggerFactory.getLogger(SceneEditorWizard.class);
   
   public static ClientFuture<Boolean> create(String placeId) {
      return new SceneWizardRunner(placeId).create();
   }
   
   public static ClientFuture<Boolean> edit(String placeId, SceneModel model) {
      return new SceneWizardRunner(placeId).edit(model);
   }

   private static class SceneWizardRunner {
      private String placeId;
      private SceneModel scene;
      private SceneTemplateModel template;
      private SettableClientFuture<Boolean> result;
      
      public SceneWizardRunner(String placeId) {
         this.placeId = placeId;
         this.result = new SettableClientFuture<>();
      }
      
      public ClientFuture<Boolean> result() {
         return this.result;
      }
      
      public ClientFuture<Boolean> create() {
         SelectTemplateDialog
            .INSTANCE
            .prompt()
            .onSuccess((template) -> this.create(template))
            .onCompletion((e) -> logger.debug("SelectTemplateDialog#onComplete {}", e));
         return result;
      }
      
      public void create(SceneTemplateModel template) {
         // TODO check if its create or edit
         ClientFuture<?> result =
            template
               .create(placeId, null, null)
               .onSuccess((response) -> load(template.getId()))
               .onFailure((error) -> Oculus.showError("Unable to Create Scene", error));
         Oculus.showProgress(result, "Creating scene...");
      }
      
      public ClientFuture<Boolean> edit(SceneModel scene) {
         this.scene = scene;
         
         ResolveActionsRequest request = 
               new ResolveActionsRequest();
         request.setAddress(Addresses.toObjectAddress(SceneTemplate.NAMESPACE, scene.getTemplate()));
         request.setPlaceId(placeId);
         ClientFuture<?> result = 
            IrisClientFactory.getClient()
               .request(request)
               .onSuccess((response) -> onTemplateResolved(new ResolveActionsResponse(response)))
               .onFailure((error) -> Oculus.showError("Unable to Resolve Actions", error));
         Oculus.showProgress(result, "Resolving actions...");
         return this.result;
      }
      
      public void load(String templateId) {
         SceneModel model =
            Iterables
               .tryFind(IrisClientFactory.getStore(SceneModel.class).values(), (m) -> templateId.equals(m.getTemplate()))
               .orNull();
         if(model == null) {
            Oculus.showDialog("Unable to Load Scene", "Unable to load the scene associated with '" + templateId + "'", JOptionPane.ERROR_MESSAGE);
            result.setError(new RuntimeException());
         }
         else {
            edit(model);
         }
      }
      
      public void onTemplateResolved(ResolveActionsResponse r) {
         showEditScene(r.getActions());
      }
      
      public void showEditScene(List<Map<String, Object>> actions) {
         (new EditSceneDialog(scene, actions))
            .prompt()
            .onSuccess((values) -> this.onUpdate(values))
            ;
      }
      
      public void onUpdate(Map<String, Object> values) {
         Oculus.showProgress(result, "Updating Scene...");
         
         scene
            .request(Capability.CMD_SET_ATTRIBUTES, values)
            .onFailure((e) -> result.setError(e))
            .onSuccess((r) -> result.setValue(true))
            ;
      }

   }
   
   private static class SelectTemplateDialog extends Dialog<SceneTemplateModel> {
      private static final SelectTemplateDialog INSTANCE = new SelectTemplateDialog();
      
      private JButton nextButton;
      
      private SceneTemplateModel value = null;

      SelectTemplateDialog() {
         nextButton = new JButton(submitAction("Next >"));
      }
      
      @Override
      protected SceneTemplateModel getValue() {
         return value;
      }

      @Override
      protected Component createContents() {
         Component templateSelector = createTemplateSelector();
         Component buttons = createButtons();
         
         JPanel contents = new JPanel();
         contents.setLayout(new BoxLayout(contents, BoxLayout.Y_AXIS));
         contents.add(templateSelector);
         contents.add(buttons);
         return contents;
      }

      private Component createTemplateSelector() {
         TableModel<SceneTemplateModel> templates =
               TableModelBuilder
                  .builder(ServiceLocator.getInstance(SceneController.class).getTemplates())
                  .columnBuilder()
                     .withName("ID")
                     .withGetter(SceneTemplateModel::getId)
                     .add()
                  .columnBuilder()
                     .withName("Name")
                     .withGetter(SceneTemplateModel::getName)
                     .add()
                  .columnBuilder()
                     .withName("Available?")
                     .withGetter(SceneTemplateModel::getAvailable)
                     .add()
                  .build();
         
         Table<SceneTemplateModel> table = new Table<>(templates);
         table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
         table.getSelectionModel().addListSelectionListener((e) -> {
            if(e.getValueIsAdjusting()) {
               return;
            }
            this.value = templates.getValue(table.getSelectedRow());
            if(this.value != null) {
               nextButton.setEnabled(true);
            }
         });
         return new JScrollPane(table);
      }
      
      private Component createButtons() {
         nextButton.setEnabled(false);
         
         JPanel panel = new JPanel();
         panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
         panel.add(Box.createGlue());
         panel.add(nextButton);
         return panel;
      }
      
   }

   private static class EditSceneDialog extends Dialog<Map<String, Object>> {
      private List<ActionTemplate> templates;
      private AddActionDialog dialog;
      private SceneEditorForm form;
      private JButton addAction;
      private JButton nextButton;
      
      EditSceneDialog(SceneModel scene, List<Map<String, Object>> templates) {
         this.templates = templates.stream().map(ActionTemplate::new).collect(Collectors.toList());
         this.form = SceneEditorForm.edit(this.templates, scene);
         this.addAction = new JButton(Actions.build("Add Action", this, EditSceneDialog::onAddAction));
         this.nextButton = new JButton(submitAction("Update"));
      }
      
      @Override
      protected Map<String, Object> getValue() {
         return form.getValues();
      }

      @Override
      protected Component createContents() {
         Component buttons = createButtons();
         
         JPanel contents = new JPanel();
         contents.setLayout(new BorderLayout());
         contents.add(new JScrollPane(form.getComponent()), BorderLayout.CENTER);
         contents.add(buttons, BorderLayout.SOUTH);
         return contents;
      }
      
      protected void onAddAction() {
         if(this.dialog == null) {
            this.dialog = new AddActionDialog(templates);
         }
         this.dialog
            .prompt()
            .onSuccess((template) -> addTemplate(template))
            ;
      }

      private void addTemplate(ActionTemplate template) {
         form.addAction(template);
         invalidate();
      }

      private Component createButtons() {
         JPanel panel = new JPanel();
         panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
         panel.add(addAction);
         panel.add(Box.createGlue());
         panel.add(nextButton);
         return panel;
      }
      
   }
   
   private static class AddActionDialog extends Dialog<ActionTemplate> {
      
      private List<ActionTemplate> templates;
      private JButton addButton;
      
      private ActionTemplate value = null;

      AddActionDialog(List<ActionTemplate> templates) {
         this.templates = templates;
         this.addButton = new JButton(submitAction("Add Action"));
         this.addButton.setEnabled(false);
      }
      
      @Override
      protected ActionTemplate getValue() {
         return value;
      }

      @Override
      protected Component createContents() {
         Component templateSelector = createActionSelector();
         Component buttons = createButtons();
         
         JPanel contents = new JPanel();
         contents.setLayout(new BoxLayout(contents, BoxLayout.Y_AXIS));
         contents.add(templateSelector);
         contents.add(buttons);
         return contents;
      }

      private Component createActionSelector() {
         TableModel<ActionTemplate> model =
               TableModelBuilder
                  .builder(templates)
                  // TODO render an icon here
                  .columnBuilder()
                     .withName("Type")
                     .withGetter(ActionTemplate::getTypehint)
                     .add()
                  .columnBuilder()
                     .withName("Name")
                     .withGetter(ActionTemplate::getName)
                     .add()
                  .columnBuilder()
                     .withName("Available?")
                     .withGetter(ActionTemplate::getSatisfiable)
                     .add()
                  // TODO already in use?
                  .build();
         
         Table<ActionTemplate> table = new Table<>(model);
         table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
         table.getSelectionModel().addListSelectionListener((e) -> {
            if(e.getValueIsAdjusting()) {
               return;
            }
            this.value = model.getValue(table.getSelectedRow());
            if(this.value != null) {
               addButton.setEnabled(true);
            }
         });
         return new JScrollPane(table);
      }
      
      private Component createButtons() {
         addButton.setEnabled(false);
         
         JPanel panel = new JPanel();
         panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
         panel.add(Box.createGlue());
         panel.add(addButton);
         return panel;
      }
      
   }

   public static void main(String [] args) throws Exception {
      Bootstrap.Builder builder = Bootstrap
            .builder()
            .withBootstrapModules(new IrisApplicationModule())
            .withModuleClasses(OculusModule.class);
      Bootstrap bootstrap = builder.build();
      ServiceLocator.init(GuiceServiceLocator.create(bootstrap.bootstrap()));
      
      SessionController controller = ServiceLocator.getInstance(SessionController.class);
      controller
         .login()
         .onSuccess((b) -> {
            SwingUtilities.invokeLater(() -> SceneEditorWizard.create(controller.getSessionInfo().getPlaceId().toString()));  
         });
      
   }
}


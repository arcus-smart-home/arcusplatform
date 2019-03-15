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
package com.iris.oculus.modules.rule.ux;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.bootstrap.Bootstrap;
import com.iris.bootstrap.ServiceLocator;
import com.iris.bootstrap.guice.GuiceServiceLocator;
import com.iris.client.capability.Rule.UpdateContextRequest;
import com.iris.client.capability.RuleTemplate.CreateRuleRequest;
import com.iris.client.capability.RuleTemplate.ResolveResponse;
import com.iris.client.event.ClientFuture;
import com.iris.client.event.SettableClientFuture;
import com.iris.client.model.RuleModel;
import com.iris.client.model.RuleTemplateModel;
import com.iris.core.IrisApplicationModule;
import com.iris.oculus.Oculus;
import com.iris.oculus.OculusModule;
import com.iris.oculus.modules.rule.RuleController;
import com.iris.oculus.modules.session.SessionController;
import com.iris.oculus.widget.Dialog;
import com.iris.oculus.widget.FormView;
import com.iris.oculus.widget.table.Table;
import com.iris.oculus.widget.table.TableModel;
import com.iris.oculus.widget.table.TableModelBuilder;

/**
 * 
 */
public class RuleEditorWizard {
   private static final Logger logger = LoggerFactory.getLogger(RuleEditorWizard.class);
   
   public static ClientFuture<Boolean> create(String placeId) {
      return new RuleWizardRunner(placeId).create();
   }
   
   public static ClientFuture<Boolean> edit(String placeId, RuleModel model) {
      return new RuleWizardRunner(placeId).edit(model);
   }

   private static class RuleWizardRunner {
      private String placeId;
      private RuleModel rule;
      private RuleTemplateModel template;
      private SettableClientFuture<Boolean> result;
      
      public RuleWizardRunner(String placeId) {
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
            .onSuccess((template) -> this.onTemplateLoaded(template))
            .onCompletion((e) -> logger.debug("SelectTemplateDialog#onComplete {}", e));
         return result;
      }
      
      public ClientFuture<Boolean> edit(RuleModel rule) {
         this.rule = rule;
         String templateId = rule.getTemplate();
         if(StringUtils.isEmpty(templateId)) {
            Oculus.showDialog("Unable to Edit", "This rule was not created from a template, so it can't be edited", JOptionPane.OK_OPTION);
            result.setError(new IllegalArgumentException("Can't edit a rule which is not associated with a template"));
            return result;
         }
         
         RuleTemplateModel template =
            ServiceLocator
               .getInstance(RuleController.class)
               .getTemplates()
               .get(templateId);
         if(template == null) {
            Oculus.showDialog("Unable to Edit", "This rule was not created from a template, so it can't be edited", JOptionPane.OK_OPTION);
            result.setError(new IllegalArgumentException("Can't edit a rule which is not associated with a template"));
            return result;
         }
         onTemplateLoaded(template);

         return result;
      }
      
      public void onTemplateLoaded(RuleTemplateModel template) {
         this.template = template;
         ClientFuture<ResolveResponse> response = template.resolve(placeId);
         Oculus.showProgress(response, "Resolving template options...");
         response
            .onSuccess((r) -> onTemplateResolved(r))
            .onFailure((e) -> result.setError(e))
            ;
      }
      
      public void onTemplateResolved(ResolveResponse r) {
         if(rule == null) {
            showCreateRule(r.getSelectors());
         }
         else {
            showEditRule(r.getSelectors());
         }
      }
      
      public void showCreateRule(Map<String, Map<String, Object>> selectors) {
         (new CreateRuleDialog(template, selectors))
            .prompt()
            .onSuccess((values) -> this.onCreate(values))
            ;
      }
      
      public void showEditRule(Map<String, Map<String, Object>> selectors) {
         (new EditRuleDialog(template, rule, selectors))
            .prompt()
            .onSuccess((values) -> this.onUpdate(values))
            ;
      }
      
      public void onCreate(Map<String, Object> values) {
         String name = (String) values.get(CreateRuleRequest.ATTR_NAME);
         Map<String, Object> vars = new HashMap<String,Object>();
         for(String key: values.keySet()) {
            if(key.startsWith("var:")) {
               vars.put(key.substring(4), values.get(key));
            }
         }
         
         Oculus.showProgress(
            template
               .createRule(
                     placeId,
                     name,
                     StrSubstitutor.replace(template.getTemplate(), vars),
                     vars
               )
               .onFailure((e) -> result.setError(e))
               .onSuccess((r) -> result.setValue(true)),
            "Creating rule..."
         );
         
      }
   
      public void onUpdate(Map<String, Object> values) {
         Map<String, Object> vars = new HashMap<String,Object>();
         for(String key: values.keySet()) {
            if(key.startsWith("var:")) {
               vars.put(key.substring(4), values.get(key));
            }
         }

         Oculus.showProgress(result, "Updating rule...");
         
         String templateId = (String) values.get(UpdateContextRequest.ATTR_TEMPLATE);
         rule.setName((String) values.get(CreateRuleRequest.ATTR_NAME));
         rule.setDescription(StrSubstitutor.replace(template.getTemplate(), vars));
         rule
            .commit()
            .onFailure((e) -> result.setError(e))
            .onSuccess((v) -> {
               rule
                  .updateContext(vars, templateId)
                  .onFailure((e) -> result.setError(e))
                  .onSuccess((r) -> result.setValue(true))
                  ;
            })
            ;
         
      }

   }
   
   // TODO should re-use the same window instead of doing each one as a dialog
   private static class SelectTemplateDialog extends Dialog<RuleTemplateModel> {
      private static final SelectTemplateDialog INSTANCE = new SelectTemplateDialog();
      
      private JButton nextButton;
      
      private RuleTemplateModel value = null;

      SelectTemplateDialog() {
         nextButton = new JButton(submitAction("Next >"));
      }
      
      @Override
      protected RuleTemplateModel getValue() {
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
         TableModel<RuleTemplateModel> templates =
               TableModelBuilder
                  .builder(ServiceLocator.getInstance(RuleController.class).getTemplates())
                  .columnBuilder()
                     .withName("ID")
                     .withGetter(RuleTemplateModel::getId)
                     .add()
                  .columnBuilder()
                     .withName("Template")
                     .withGetter((model) -> model.getTemplate())
                     .add()
                  .columnBuilder()
                     .withName("Satisfiable")
                     .withGetter((model) -> model.getSatisfiable())
                     .add()
                  
                  .build();
         
         Table<RuleTemplateModel> table = new Table<>(templates);
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

   private static class CreateRuleDialog extends Dialog<Map<String, Object>> {
      private FormView form;
//      private JButton prevButton;
      private JButton nextButton;
      
      CreateRuleDialog(RuleTemplateModel template, Map<String, Map<String, Object>> selectors) {
         this.form = RuleEditorForm.create(template, selectors, false);
//         this.prevButton = new JButton("< Back");
         this.nextButton = new JButton(submitAction("Create"));
      }
      
      @Override
      protected Map<String, Object> getValue() {
         return form.getValues();
      }

      @Override
      protected Component createContents() {
         Component buttons = createButtons();
         
         JPanel contents = new JPanel();
         contents.setLayout(new BoxLayout(contents, BoxLayout.Y_AXIS));
         contents.add(form.getComponent());
         contents.add(buttons);
         return contents;
      }

      private Component createButtons() {
//         nextButton.setEnabled(false);
         
         JPanel panel = new JPanel();
         panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
//         panel.add(prevButton);
         panel.add(Box.createGlue());
         panel.add(nextButton);
         return panel;
      }
      
   }
   
   private static class EditRuleDialog extends Dialog<Map<String, Object>> {
      private FormView form;
//      private JButton prevButton;
      private JButton nextButton;
      
      EditRuleDialog(RuleTemplateModel template, RuleModel rule, Map<String, Map<String, Object>> selectors) {
         this.form = RuleEditorForm.edit(template, rule, selectors);
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

      private Component createButtons() {
//         nextButton.setEnabled(false);
         
         JPanel panel = new JPanel();
         panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
//         panel.add(prevButton);
         panel.add(Box.createGlue());
         panel.add(nextButton);
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
            SwingUtilities.invokeLater(() -> RuleEditorWizard.create(controller.getSessionInfo().getPlaceId().toString()));  
         });
      
   }
}


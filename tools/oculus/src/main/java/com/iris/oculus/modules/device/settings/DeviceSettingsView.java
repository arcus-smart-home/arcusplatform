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
package com.iris.oculus.modules.device.settings;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.UIManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.client.capability.DeviceSettings;
import com.iris.client.capability.DeviceSettings.GetParamRequest;
import com.iris.client.capability.DeviceSettings.GetParamResponse;
import com.iris.client.capability.DeviceSettings.SetParamRequest;
import com.iris.client.capability.DeviceSettings.SetParamResponse;
import com.iris.client.event.ListenerRegistration;
import com.iris.client.model.Model;
import com.iris.oculus.modules.device.settings.DeviceSettingEditors.Editor;
import com.iris.oculus.util.Actions;
import com.iris.oculus.util.BaseComponentWrapper;

/**
 * Lists the settings in a model's devsettings:params attribute, one row each,
 * and follows the attribute while bound to the model. The attribute holds every
 * param, so rows are updated in place to keep what the user is editing in the
 * others.
 */
public class DeviceSettingsView extends BaseComponentWrapper<JPanel> {
   private static final Logger logger = LoggerFactory.getLogger(DeviceSettingsView.class);

   private static final String NO_SETTINGS = "This device has not reported any settings.";

   private static final Color COLOR_OK = new Color(0x1E, 0x7B, 0x34);
   private static final Color COLOR_PENDING = new Color(0x1A, 0x5F, 0xB4);
   private static final Color COLOR_WARNING = new Color(0xB3, 0x6B, 0x00);
   private static final Color COLOR_ERROR = new Color(0xB0, 0x00, 0x20);

   private static final int COLUMN_SETTING = 0;
   private static final int COLUMN_CURRENT = 1;
   private static final int COLUMN_DEFAULT = 2;
   private static final int COLUMN_EDITOR = 3;
   private static final int COLUMN_BUTTONS = 4;
   private static final int COLUMN_COUNT = 5;

   private final Action refresh = Actions.build("Refresh", this::onRefresh);
   private final JLabel message = new JLabel();
   private static final int DESCRIPTION_WIDTH_PX = 330;

   private final JPanel rowsPanel = new JPanel(new GridBagLayout());
   private final Map<String, ParamRow> rows = new LinkedHashMap<>();

   private Model model;
   private ListenerRegistration modelListener;

   public DeviceSettingsView() {
      refresh.putValue(Action.SHORT_DESCRIPTION, "Re-read " + DeviceSettings.ATTR_PARAMS + " with base:GetAttributes");
      refresh.setEnabled(false);
   }

   /** Rereads the attribute, Read on a row is what asks the device. */
   public Action actionRefresh() {
      return refresh;
   }

   public void setModel(Model model) {
      if(model == this.model) {
         return;
      }

      clearModel();
      this.model = model;
      this.modelListener = model.addListener((event) -> onModelChanged(model, event));
      this.refresh.setEnabled(true);

      Object params = model.get(DeviceSettings.ATTR_PARAMS);
      showParams(params);
      if(params == null) {
         // drivers fill in the attribute when the device connects, until then only GetAttributes has it
         onRefresh();
      }
   }

   public void clearModel() {
      if(modelListener != null) {
         modelListener.remove();
         modelListener = null;
      }
      model = null;
      refresh.setEnabled(false);
      showParams(null);
   }

   @Override
   protected JPanel createComponent() {
      JPanel contents = new JPanel(new BorderLayout());
      contents.add(message, BorderLayout.NORTH);
      JScrollPane scroller = new JScrollPane(rowsPanel);
      scroller.getVerticalScrollBar().setUnitIncrement(16);
      contents.add(scroller, BorderLayout.CENTER);
      return contents;
   }

   private void onModelChanged(Model source, PropertyChangeEvent event) {
      if(source == model && DeviceSettings.ATTR_PARAMS.equals(event.getPropertyName())) {
         showParams(event.getNewValue());
      }
   }

   private void onRefresh() {
      Model target = model;
      refresh.setEnabled(false);
      if(rows.isEmpty()) {
         message.setText("Loading settings...");
      }
      target
         .refresh()
         .onSuccess((event) -> {
            if(target == model) {
               refresh.setEnabled(true);
               showParams(target.get(DeviceSettings.ATTR_PARAMS));
            }
         })
         .onFailure((error) -> {
            logger.warn("Unable to refresh device settings", error);
            if(target == model) {
               refresh.setEnabled(true);
               message.setText("Unable to refresh: " + describe(error));
            }
         });
   }

   private void showParams(Object attribute) {
      List<DeviceSettingParam> params = DeviceSettingParam.fromAttribute(attribute);
      if(hasSameDefinitions(params)) {
         params.forEach((param) -> rows.get(param.getParamId()).update(param));
      }
      else {
         rebuildRows(params);
      }
      message.setText(rows.isEmpty() ? NO_SETTINGS : "");
   }

   private boolean hasSameDefinitions(List<DeviceSettingParam> params) {
      if(params.size() != rows.size()) {
         return false;
      }
      Iterator<ParamRow> it = rows.values().iterator();
      for(DeviceSettingParam param: params) {
         if(!it.next().edit.getParam().hasSameDefinition(param)) {
            return false;
         }
      }
      return true;
   }

   private void rebuildRows(List<DeviceSettingParam> params) {
      rows.clear();
      rowsPanel.removeAll();

      if(!params.isEmpty()) {
         addHeaders();
      }
      int y = 1;
      for(DeviceSettingParam param: params) {
         ParamRow row = new ParamRow(model, param);
         rows.put(param.getParamId(), row);
         row.addTo(rowsPanel, y);
         y += 3;
      }

      GridBagConstraints filler = new GridBagConstraints();
      filler.gridy = y;
      filler.weighty = 1.0;
      rowsPanel.add(Box.createVerticalGlue(), filler);

      rowsPanel.revalidate();
      rowsPanel.repaint();
   }

   private void addHeaders() {
      addHeader("Setting", COLUMN_SETTING);
      addHeader("Current", COLUMN_CURRENT);
      addHeader("Default", COLUMN_DEFAULT);
      addHeader("New Value", COLUMN_EDITOR);
   }

   private void addHeader(String text, int column) {
      JLabel header = new JLabel(text);
      header.setFont(header.getFont().deriveFont(Font.BOLD));
      rowsPanel.add(header, constraints(column, 0));
   }

   private static GridBagConstraints constraints(int column, int row) {
      GridBagConstraints gbc = new GridBagConstraints();
      gbc.gridx = column;
      gbc.gridy = row;
      gbc.anchor = GridBagConstraints.NORTHWEST;
      gbc.insets = new Insets(4, 6, 4, 6);
      return gbc;
   }

   private static String describe(Throwable error) {
      return error.getMessage() != null ? error.getMessage() : error.getClass().getSimpleName();
   }

   private static Color colorOf(DeviceSettingEdit.Status status) {
      switch(status) {
      case OK:
         return COLOR_OK;
      case PENDING:
         return COLOR_PENDING;
      case WARNING:
         return COLOR_WARNING;
      case ERROR:
         return COLOR_ERROR;
      default:
         return UIManager.getColor("Label.foreground");
      }
   }

   private static class ParamRow implements DeviceSettingEditors.Listener {
      private final Model target;
      private final DeviceSettingEdit edit;
      private final Editor editor;
      private final JLabel current = new JLabel();
      private final JLabel status = new JLabel(" ");
      private final Action save = Actions.build("Save", this::onSave);
      private final Action reset = Actions.build("Reset", this::onReset);
      private final Action read = Actions.build("Read", this::onRead);

      ParamRow(Model target, DeviceSettingParam param) {
         this.target = target;
         this.edit = new DeviceSettingEdit(param);
         this.editor = DeviceSettingEditors.create(param, this);
         this.save.putValue(Action.SHORT_DESCRIPTION, "Send this value with " + DeviceSettings.CMD_SETPARAM);
         this.reset.putValue(Action.SHORT_DESCRIPTION, "Send the default with " + DeviceSettings.CMD_SETPARAM);
         this.read.putValue(Action.SHORT_DESCRIPTION, "Ask for this value with " + DeviceSettings.CMD_GETPARAM);
         render();
      }

      void addTo(JPanel panel, int y) {
         DeviceSettingParam param = edit.getParam();

         GridBagConstraints setting = constraints(COLUMN_SETTING, y);
         setting.gridheight = 2;
         setting.weightx = 1.0;
         setting.fill = GridBagConstraints.HORIZONTAL;
         panel.add(createSettingCell(param), setting);

         GridBagConstraints currentCell = constraints(COLUMN_CURRENT, y);
         currentCell.gridheight = 2;
         panel.add(current, currentCell);

         GridBagConstraints defaultCell = constraints(COLUMN_DEFAULT, y);
         defaultCell.gridheight = 2;
         panel.add(new JLabel(param.describe(param.getDefault())), defaultCell);

         GridBagConstraints editorCell = constraints(COLUMN_EDITOR, y);
         editorCell.fill = GridBagConstraints.HORIZONTAL;
         panel.add(editor.getComponent(), editorCell);

         panel.add(createButtons(), constraints(COLUMN_BUTTONS, y));

         GridBagConstraints statusCell = constraints(COLUMN_EDITOR, y + 1);
         statusCell.gridwidth = 2;
         panel.add(status, statusCell);

         GridBagConstraints separator = constraints(0, y + 2);
         separator.gridwidth = COLUMN_COUNT;
         separator.fill = GridBagConstraints.HORIZONTAL;
         separator.insets = new Insets(0, 0, 0, 0);
         panel.add(new JSeparator(), separator);
      }

      /** Long descriptions wrap, so the setting column leaves room for the buttons. */
      private String wrapped(String text) {
         String escaped = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
         return "<html><body style='width:" + DESCRIPTION_WIDTH_PX + "px'>" + escaped + "</body></html>";
      }

      private JPanel createSettingCell(DeviceSettingParam param) {
         JLabel name = new JLabel(param.getDisplayName());
         name.setFont(name.getFont().deriveFont(Font.BOLD));

         JLabel description = new JLabel(wrapped(param.getDisplayDescription()));
         description.setFont(description.getFont().deriveFont(Font.PLAIN));

         JLabel detail = new JLabel(param.getDetail());
         detail.setFont(detail.getFont().deriveFont(Font.PLAIN, detail.getFont().getSize2D() - 1f));
         detail.setForeground(UIManager.getColor("Label.disabledForeground"));

         JPanel cell = new JPanel();
         cell.setLayout(new BoxLayout(cell, BoxLayout.PAGE_AXIS));
         cell.add(name);
         cell.add(description);
         cell.add(detail);
         return cell;
      }

      private JPanel createButtons() {
         JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
         buttons.setBorder(BorderFactory.createEmptyBorder());
         buttons.add(new JButton(save));
         buttons.add(new JButton(reset));
         buttons.add(new JButton(read));
         return buttons;
      }

      void update(DeviceSettingParam param) {
         edit.onParamChanged(param);
         render();
      }

      private void render() {
         if(!edit.hasUnsavedEdit()) {
            editor.setValue(edit.getEditorValue());
         }
         renderState();
      }

      /** Leaves the editor alone, for when it holds what the user is typing. */
      private void renderState() {
         DeviceSettingParam param = edit.getParam();
         boolean valid = param.validate(param.getValue()) == null;
         current.setText(param.describe(param.getValue()));
         current.setForeground(valid ? UIManager.getColor("Label.foreground") : COLOR_WARNING);
         current.setToolTipText(valid ? null : param.validate(param.getValue()));

         // an empty label collapses the row
         status.setText(edit.getStatusText().isEmpty() ? " " : edit.getStatusText());
         status.setForeground(colorOf(edit.getStatus()));

         editor.setEnabled(edit.canEdit());
         save.setEnabled(edit.canSave());
         reset.setEnabled(edit.canReset());
         read.setEnabled(edit.canRead());
      }

      @Override
      public void onEdit(String value) {
         edit.onEdit(value);
         renderState();
      }

      @Override
      public void onInvalidEdit(String message) {
         edit.onInvalidEdit(message);
         renderState();
      }

      private void onSave() {
         send(edit.beginSave());
      }

      private void onReset() {
         send(edit.beginReset());
      }

      private void send(String value) {
         render();

         Map<String, Object> attributes = new HashMap<>();
         attributes.put(SetParamRequest.ATTR_PARAMID, edit.getParam().getParamId());
         attributes.put(SetParamRequest.ATTR_VALUE, value);
         target
            .request(DeviceSettings.CMD_SETPARAM, attributes)
            .onSuccess((event) -> {
               edit.onSetResponse(new SetParamResponse(event).getStatus());
               render();
            })
            .onFailure((error) -> {
               logger.warn("Unable to set device setting [{}]", edit.getParam().getParamId(), error);
               edit.onSetFailed(describe(error));
               render();
            });
      }

      private void onRead() {
         edit.beginRead();
         render();

         target
            .request(
                  DeviceSettings.CMD_GETPARAM,
                  Collections.<String, Object>singletonMap(GetParamRequest.ATTR_PARAMID, edit.getParam().getParamId())
            )
            .onSuccess((event) -> {
               GetParamResponse response = new GetParamResponse(event);
               edit.onReadResponse(response.getStatus(), response.getValue());
               render();
            })
            .onFailure((error) -> {
               logger.warn("Unable to read device setting [{}]", edit.getParam().getParamId(), error);
               edit.onReadFailed(describe(error));
               render();
            });
      }
   }
}

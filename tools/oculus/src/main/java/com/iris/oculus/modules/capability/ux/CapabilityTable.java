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
package com.iris.oculus.modules.capability.ux;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.iris.capability.definition.AttributeDefinition;
import com.iris.capability.definition.CapabilityDefinition;
import com.iris.client.capability.DeviceAdvanced;
import com.iris.client.capability.HubZigbee;
import com.iris.client.event.ListenerRegistration;
import com.iris.client.model.Model;
import com.iris.oculus.Oculus;
import com.iris.oculus.util.Actions;
import com.iris.oculus.util.JsonPrettyPrinter;
import com.iris.oculus.util.JsonPrettyPrinter.Format;
import com.iris.oculus.view.SimpleViewModel;
import com.iris.oculus.widget.Toast;
import com.iris.oculus.widget.table.Table;
import com.iris.oculus.widget.table.TableModel;
import com.iris.oculus.widget.table.TableModelBuilder;
import com.iris.protocol.zigbee.ZigbeeProtocol;
import com.iris.protocol.zwave.ZWaveProtocol;

/**
 *
 */
public class CapabilityTable extends JPanel {
   private static final Logger logger = LoggerFactory.getLogger(CapabilityTable.class);

   private Color editedColor = Color.decode("#EEE8AA");
   private Color modifiedColor = Color.decode("#E0EEEE");
   private Color collisionColor = Color.decode("#FF3030");

   private Action copy =
         Actions
            .builder()
            .withName("Copy")
            .withFunctionalAction(this, CapabilityTable::copy)
            .build();
   private ListenerRegistration registration = null;
   private Table<AttributeDefinition> table;
   private CapabilityDefinition definition;
   private Model data;
   private Set<String> modified = new HashSet<>();
   private Set<String> changing = new HashSet<>();
   private SimpleViewModel<AttributeDefinition> viewModel = new SimpleViewModel<>();
   private TableModel<AttributeDefinition> model =
         TableModelBuilder
            .<AttributeDefinition>builder(viewModel)
            .columnBuilder()
               .withName("Attribute")
               .withGetter((definition) -> definition.getName())
               .add()
            .columnBuilder()
               .withName("type")
               .withGetter((definition) -> definition.getType().getRepresentation())
               .add()
            .columnBuilder()
               .withName("writable")
               .withGetter((definition) -> definition.isWritable() ? "Writable" : "Read Only")
               .add()
            .columnBuilder()
               .withName("required")
               .withGetter((definition) -> definition.isOptional() ? "Optional" : "Required")
               .add()
            .columnBuilder()
               .withName("value")
               .withRenderer((c, definition, isSelected, hasFocus) -> {
                  // there is a swing odd-ness where this call can be invoked from the
                  // editor which will pass in a non-AttributeDefinition
                  if(definition != null && definition instanceof AttributeDefinition) {
                     this.renderValue(c, (AttributeDefinition) definition, isSelected);
                  }
               })
               .isEditable((definition) -> definition.isWritable())
               .withGetter((definition) -> definition)
               .withSetter((definition, value) -> setValue(getAttributeName((AttributeDefinition) definition), value))
               .add()
            .columnBuilder()
               .withName("description")
               .withGetter((definition) -> definition.getDescription())
               .add()
            .build();

   public CapabilityTable() {
      init();
   }

   public CapabilityTable(CapabilityDefinition definition) {
      init();
      setCapabilityDefinition(definition);
   }

   private void init() {
      table = new Table<AttributeDefinition>(model);
      table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      table.getTableHeader().setVisible(true);
      table.getColumnModel().getColumn(4).setCellEditor(new CapabilityTableCellEditor() {

         @Override
         protected Object renderValue(Object value) {
            AttributeDefinition ad = (AttributeDefinition) value;
            String name = getAttributeName(ad);
            return getValue(name);
         }

      });
      table
         .getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
         .put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK), "copy");
      table.getActionMap().put("copy", copy);
      table.getActionMap().put("logit", Actions.builder().withAction((event) -> logger.warn("LOGIT {}", event)).build());
      table.addMouseListener(new RightClickMouseAdapter());

      setLayout(new BorderLayout());
      JPanel tablePane = new JPanel();
      tablePane.setLayout(new BorderLayout());
      tablePane.add(table.getTableHeader(), BorderLayout.NORTH);
      tablePane.add(table, BorderLayout.CENTER);

      add(tablePane, BorderLayout.CENTER);
      setCapabilityDefinition(definition);
   }

   public CapabilityDefinition getCapabilityDefinition() {
      return definition;
   }

   public void setCapabilityDefinition(CapabilityDefinition definition) {
      this.definition = definition;
      this.viewModel.removeAll();
      if(definition != null) {
         this.viewModel.addAll(definition.getAttributes());
      }
      this.validate();
   }

   public void setAttributes(Model data) {
      if(this.registration != null) {
         this.registration.remove();
      }
      this.data = data;
      this.model.fireTableDataChanged();
      if(data != null) {
         this.registration = data.addListener((event) -> {
            String name = event.getPropertyName();

            boolean asyncChange = false;
            if(!this.changing.remove(name)) {
               asyncChange = true;
               this.modified.add(name);
            }

            // TODO this doesn't fully take into account multi instance things
            AttributeDefinition ad = viewModel.findBy((definition) -> definition.getName().equals(name));
            if(ad == null) {
               return;
            }
            this.model.fireTableCellUpdated("value", ad);
            if(this.data != null && this.data.isDirty(name) && asyncChange) {
               Oculus.warn("Attribute [" + name + "] has been modified since it was edited.", new DiscardChangesAction(data, ad));
            }
         });
      }
      this.validate();
   }

   public ListSelectionModel getSelectionModel() {
      return table.getSelectionModel();
   }

   protected String getAttributeName(AttributeDefinition definition) {
      return definition.getName();
   }

   protected Object getValue(String name) {
      try {
         if(data == null) {
            return null;
         }
         return data.get(name);
      }
      catch(Exception e) {
         Oculus.warn("Invalid value for attribute: " + name, e);
         return null;
      }
   }

   protected void setValue(String name, Object value) {
//      AttributeValue<?> av = definition.coerceToValue(value);
      // TODO enforce typing...
      this.changing.add(name);
      this.data.set(name, value);
   }

   protected void renderValue(Component c, AttributeDefinition definition, boolean isSelected) {
      JLabel label = (JLabel) c;
      String name = getAttributeName(definition);
      Object value = getValue(name);
      if(value == null) {
         label.setText("Not Set");
         label.setFont(getFont().deriveFont(Font.ITALIC));
      }
      else {
         String text = getValueText(definition, name, value);
         if(text == null) {
            label.setText("Invalid");
            label.setFont(getFont().deriveFont(Font.ITALIC));
         }
         else {
            label.setText(text);
            label.setToolTipText(getValueTooltipText(definition, name, value));
         }
      }
      if(definition.isWritable()) {
         label.setForeground(UIManager.getColor("Label.foreground"));
      }
      else {
         label.setForeground(UIManager.getColor("Label.disabledForeground"));
      }
      boolean isModified = this.modified.contains(name);
      boolean isDirty = this.data.isDirty(name);

      if(isModified && isDirty) {
         label.setBackground(collisionColor);
      }
      else if(isDirty) {
         label.setBackground(editedColor);
      }
      else if(isModified) {
         label.setBackground(modifiedColor);
      }
      else if(isSelected) {
         label.setBackground(UIManager.getColor("Table.selectionBackground"));
      }
      else {
         label.setBackground(table.getBackground());
      }
   }

   protected String getValueText(AttributeDefinition definition, String name, Object value) {
      try {
         String decoded = getDecodedText(definition, name, value);
         if (decoded != null) {
            return decoded;
         }
   
         return String.valueOf(definition.getType().coerce(value));
      }
      catch(Exception e) {
         Oculus.warn("Invalid value for attribute: " + name, e);
         return null;
      }
   }

   protected String getValueTooltipText(AttributeDefinition definition, String name, Object value) {
      return String.valueOf(definition.getType().coerce(value));
   }

   protected String getDecodedText(AttributeDefinition definition, String name, Object value) {
      if (name == null || name.trim().isEmpty()) {
         return null;
      }

      try {
         switch (name) {
         case DeviceAdvanced.ATTR_PROTOCOLID:
            return getProtocolIdText(definition, value);

         case HubZigbee.ATTR_PANID:
         case HubZigbee.ATTR_MANUFACTURER:
            return toHex(((Number)value).shortValue());

         case HubZigbee.ATTR_EXTID:
         case HubZigbee.ATTR_EUI64:
         case HubZigbee.ATTR_TCEUI64:
            return toHex(((Number)value).longValue());

         default:
            return null;
         }
      } catch (Throwable th) {
         return null;
      }
   }

   protected String getProtocolIdText(AttributeDefinition definition, Object value) {
      Object protocol = data.get(DeviceAdvanced.ATTR_PROTOCOL);
      if (!(protocol instanceof CharSequence)) {
         return null;
      }

      switch (String.valueOf(protocol)) {
      case ZWaveProtocol.NAMESPACE: {
         byte[] data = Base64.getDecoder().decode(String.valueOf(value));
         ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);

         int idx = buffer.remaining() - 1;
         while (idx >= 5) {
            if ((buffer.get(idx) & 0xFF) != 0) {
               break;
            }

            idx--;
         }

         if (idx < 5) {
            return null;
         }

         byte nodeId = buffer.get(idx);
         int homeId = buffer.getInt(idx - 4);
         return toHex(homeId) + ":" + toZWave(nodeId);
      }

      case ZigbeeProtocol.NAMESPACE: {
         byte[] data = Base64.getDecoder().decode(String.valueOf(value));
         ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
         if (buffer.remaining() < 8) {
            return null;
         }

         return toHex(buffer.getLong(0));
      }

      default:
         return null;
      }
   }

   protected static String toZWave(byte value) {
      String result = String.valueOf(value & 0xFF);
      while (result.length() < 3) {
         result = "0" + result;
      }

      return result;
   }

   protected String toHex(byte value) {
      if (value < 0x00) return Integer.toHexString(value & 0xFF).toUpperCase();
      if (value < 0x10) return "0" + Integer.toHexString(value & 0xFF).toUpperCase();
      return Integer.toHexString(value & 0xFF).toUpperCase();
   }

   protected String toHex(short value) {
      if (value < 0x0000) return Integer.toHexString(value & 0xFFFF).toUpperCase();
      if (value < 0x0010) return "000" + Integer.toHexString(value & 0xFFFF).toUpperCase();
      if (value < 0x0100) return "00" + Integer.toHexString(value & 0xFFFF).toUpperCase();
      if (value < 0x1000) return "0" + Integer.toHexString(value & 0xFFFF).toUpperCase();
      return Integer.toHexString(value & 0xFFFF).toUpperCase();
   }

   protected String toHex(int value) {
      if (value < 0x00000000) return Integer.toHexString(value).toUpperCase();
      if (value < 0x00000010) return "0000000" + Integer.toHexString(value).toUpperCase();
      if (value < 0x00000100) return "000000" + Integer.toHexString(value).toUpperCase();
      if (value < 0x00001000) return "00000" + Integer.toHexString(value).toUpperCase();
      if (value < 0x00010000) return "0000" + Integer.toHexString(value).toUpperCase();
      if (value < 0x00100000) return "000" + Integer.toHexString(value).toUpperCase();
      if (value < 0x01000000) return "00" + Integer.toHexString(value).toUpperCase();
      if (value < 0x10000000) return "0" + Integer.toHexString(value).toUpperCase();;
      return Integer.toHexString(value).toUpperCase();
   }

   protected String toHex(long value) {
      if (value < 0x0000000000000000L) return Long.toHexString(value).toUpperCase();
      if (value < 0x0000000000000010L)  return "000000000000000" + Long.toHexString(value).toUpperCase();
      if (value < 0x0000000000000100L)  return "00000000000000" + Long.toHexString(value).toUpperCase();
      if (value < 0x0000000000001000L) return "0000000000000" + Long.toHexString(value).toUpperCase();
      if (value < 0x0000000000010000L) return "000000000000" + Long.toHexString(value).toUpperCase();
      if (value < 0x0000000000100000L) return "00000000000" + Long.toHexString(value).toUpperCase();
      if (value < 0x0000000001000000L) return "0000000000" + Long.toHexString(value).toUpperCase();
      if (value < 0x0000000010000000L) return "000000000" + Long.toHexString(value).toUpperCase();
      if (value < 0x0000000100000000L) return "00000000" + Long.toHexString(value).toUpperCase();
      if (value < 0x0000001000000000L) return "0000000" + Long.toHexString(value).toUpperCase();
      if (value < 0x0000010000000000L) return "000000" + Long.toHexString(value).toUpperCase();
      if (value < 0x0000100000000000L) return "00000" + Long.toHexString(value).toUpperCase();
      if (value < 0x0001000000000000L) return "0000" + Long.toHexString(value).toUpperCase();
      if (value < 0x0010000000000000L) return "000" + Long.toHexString(value).toUpperCase();
      if (value < 0x0100000000000000L) return "00" + Long.toHexString(value).toUpperCase();
      if (value < 0x1000000000000000L) return "0" + Long.toHexString(value).toUpperCase();
      return Long.toHexString(value).toUpperCase();
   }

   protected void copy() {
      int index = getSelectionModel().getLeadSelectionIndex();
      if(index < 0) {
         return;
      }

      Point screen = table.getLocationOnScreen();
      Point point = new Point(screen.x + 100, screen.y - table.getHeight() + (table.getRowHeight() * index));
      copy(index, point);
   }

   protected void copy(int row, Point toastPoint /* mmmmm.... toast points */) {
      AttributeDefinition definition = model.getValue(row);
      String name = getAttributeName(definition);
      Object value = getValue(name);

      StringSelection selection = new StringSelection(getCopyValue(definition, name, value));
      Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
      clipboard.setContents(selection, null);

      Toast.showToast(table, "Copied to Clipboard", toastPoint, 1000);
   }

   protected String getCopyValue(AttributeDefinition definition, String name, Object value) {
      String decoded = getDecodedText(definition, name, value);
      if (decoded != null) {
         return decoded;
      }

      if (value instanceof CharSequence) {
         return String.valueOf(value);
      }

      return JsonPrettyPrinter.prettyPrint(value, Format.PLAIN_TEXT);
   }

   private class RightClickMouseAdapter extends MouseAdapter {
      @Override
      public void mouseReleased(MouseEvent e) {
         super.mouseReleased(e);

         int rowindex = table.rowAtPoint(e.getPoint());

         if (rowindex < 0)
            return;

         if (SwingUtilities.isRightMouseButton(e) && e.getComponent() instanceof JTable) {
            Point point = new Point(e.getXOnScreen(), e.getYOnScreen() - CapabilityTable.this.getHeight());
            copy(rowindex, point);
         }
      }
   }

   private class DiscardChangesAction extends AbstractAction {
      private Model model;
      private AttributeDefinition definition;

      public DiscardChangesAction(Model model, AttributeDefinition definition) {
         super("Discard Local Changes");
         this.model = model;
         this.definition = definition;
      }

      @Override
      public void actionPerformed(ActionEvent e) {
         this.model.clearChange(definition.getName());
      }


   }

}


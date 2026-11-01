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
package com.iris.oculus.util;

import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;

/**
 * Closes the active window on Command-W, or Control-W off macOS. The window
 * gets a normal close, so its own close behavior applies and one that refuses
 * to close, like a progress dialog, stays open. The main window is left alone:
 * closing it quits Oculus.
 */
public final class CloseWindowShortcut {
   private static final int MODIFIERS =
      InputEvent.SHIFT_DOWN_MASK | InputEvent.ALT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK | InputEvent.META_DOWN_MASK;

   private CloseWindowShortcut() {
   }

   public static void install(Window mainWindow) {
      int shortcut = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
      KeyboardFocusManager focus = KeyboardFocusManager.getCurrentKeyboardFocusManager();
      focus.addKeyEventDispatcher((event) -> {
         if (!isCloseShortcut(event.getID(), event.getKeyCode(), event.getModifiersEx(), shortcut)) {
            return false;
         }
         Window active = focus.getActiveWindow();
         if (active == null || active == mainWindow) {
            return false;
         }
         active.dispatchEvent(new WindowEvent(active, WindowEvent.WINDOW_CLOSING));
         return true;
      });
   }

   static boolean isCloseShortcut(int eventId, int keyCode, int modifiersEx, int shortcut) {
      return eventId == KeyEvent.KEY_PRESSED
         && keyCode == KeyEvent.VK_W
         && (modifiersEx & MODIFIERS) == shortcut;
   }
}

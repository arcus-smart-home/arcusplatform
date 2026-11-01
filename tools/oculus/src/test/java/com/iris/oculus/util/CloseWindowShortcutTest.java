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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import org.junit.Test;

public class CloseWindowShortcutTest {
   private static final int COMMAND = InputEvent.META_DOWN_MASK;

   @Test
   public void commandWClosesTheWindow() {
      assertTrue(CloseWindowShortcut.isCloseShortcut(KeyEvent.KEY_PRESSED, KeyEvent.VK_W, COMMAND, COMMAND));
   }

   @Test
   public void aPlainWIsTyping() {
      assertFalse(CloseWindowShortcut.isCloseShortcut(KeyEvent.KEY_PRESSED, KeyEvent.VK_W, 0, COMMAND));
   }

   @Test
   public void anotherModifierIsADifferentShortcut() {
      assertFalse(CloseWindowShortcut.isCloseShortcut(KeyEvent.KEY_PRESSED, KeyEvent.VK_W, COMMAND | InputEvent.SHIFT_DOWN_MASK, COMMAND));
   }

   @Test
   public void onlyTheKeyPressCounts() {
      assertFalse(CloseWindowShortcut.isCloseShortcut(KeyEvent.KEY_RELEASED, KeyEvent.VK_W, COMMAND, COMMAND));
   }
}

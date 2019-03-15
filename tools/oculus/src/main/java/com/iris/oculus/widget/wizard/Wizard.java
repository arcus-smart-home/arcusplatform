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
package com.iris.oculus.widget.wizard;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import com.iris.client.event.ClientFuture;
import com.iris.client.event.Futures;
import com.iris.client.event.SettableClientFuture;
import com.iris.oculus.modules.status.ShowExceptionAction;
import com.iris.oculus.util.Actions;
import com.iris.oculus.widget.Dialog;

public class Wizard<I, O> extends Dialog<O> {
   
   public static <I, O> Builder<I, O> builder(Transition<I, O> transition) {
      return new Builder<I, O>(transition);
   }
   
   private JButton back = new JButton(Actions.build("Cancel", this, Wizard::prev));
   private JButton next = new JButton(Actions.build("Next >", this, Wizard::next));
   private JPanel contents = new JPanel(new GridBagLayout());
   
   private WizardStep<I, ?> first;
   private WizardStep<?, ?> current;
   private WizardStep<?, O> last;

   protected Wizard(WizardStep<I, ?> first, WizardStep<?, O> last) {
      this.first = first;
      this.current = first;
      this.last = last;

      GridBagConstraints gbc = new GridBagConstraints();
      
      gbc.gridx = 0;
      gbc.gridy = 0;
      gbc.fill = GridBagConstraints.BOTH;
      gbc.weightx = 1.0;
      gbc.weighty = 1.0;
      contents.add(new JLabel(), gbc.clone());
      
      gbc.fill = GridBagConstraints.HORIZONTAL;
      gbc.weightx = 1.0;
      gbc.weighty = 0.0;
      gbc.gridy++;
      contents.add(new JSeparator(JSeparator.HORIZONTAL), gbc.clone());
      
      JPanel buttons = new JPanel();
      buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
      buttons.add(back);
      buttons.add(Box.createHorizontalGlue());
      buttons.add(next);
      
      gbc.gridy++;
      contents.add(buttons, gbc.clone());
   }
   
   public ClientFuture<O> prompt(I input) {
      setContent(this.first.show(this, input));
      this.current = this.first;
      this.update();
      return this.prompt();
   }

   public void prev() {
      if(this.current.isFirst()) {
         dispose();
      }
      else {
         this.current = this.current.prev(this);
         update();
      }
   }
   
   public void next() {
   	setEnabled(false);
   	ClientFuture<WizardStep<?, ?>> transition =
   			this.current
   				.commit(this)
   				.onFailure((e) -> setErrorMessage(e.getMessage(), new ShowExceptionAction(e)))
   				.onCompletion((e) -> setEnabled(true));
   	if(current.isLast()) {
   		transition.onSuccess((step) -> {
   			submit();
   			dispose();
   		});
   	}
   	else {
   		transition.onSuccess((step) -> {
   			current = step;
   			update();
   		});
   	}
   }
   
   private void update() {
      this.setContent(this.current.restore(this));
      if(current.isFirst()) {
         back.setText("Cancel");
      }
      else {
         back.setText("< Back");
      }
      if(current.isLast()) {
         next.setText("Done");
      }
      else {
         next.setText("Next >");
      }
   }

   public boolean isNextEnabled() {
      return next.isEnabled();
   }
   
   public void setNextEnabled(boolean b) {
      next.setEnabled(b);
   }
   
   @Override
   protected O getValue() {
      return last.getValue();
   }

   @Override
   protected Component createContents() {
      return contents;
   }
   
   protected void setContent(Component component) {
      GridBagConstraints gbc = new GridBagConstraints();
      
      gbc.gridx = 0;
      gbc.gridy = 0;
      gbc.fill = GridBagConstraints.BOTH;
      gbc.weightx = 1.0;
      gbc.weighty = 1.0;

      contents.remove(0);
      contents.add(component, gbc, 0);
      
      this.invalidate();
      if(this.isVisible()) {
         this.pack();
      }
   }
   
   private static class WizardStep<I, O> {
      private WizardStep<?, I> prev;
      private WizardStep<O, ?> next;
      private Transition<I, O> transition;
      
      private WizardStep(WizardStep<?, I> prev, Transition<I, O> transition) {
         this.prev = prev;
         this.transition = transition;
      }
      
      public boolean isFirst() {
         return prev == null;
      }
      
      public boolean isLast() {
         return next == null;
      }
      
      public void setNext(WizardStep<O, ?> next) {
         this.next = next;
      }
      
      public WizardStep<?, I> prev(Wizard<?, ?> wiz) {
         return prev;
      }
      
      public WizardStep<O, ?> next(Wizard<?, ?> wiz) {
      	if(this.next != null) {
      		next.show(wiz, getValue());
      	}
         return next;
      }
      
      public ClientFuture<WizardStep<?, ?>> commit(Wizard<?, ?> wiz) {
      	SettableClientFuture<WizardStep<?, ?>> result = new SettableClientFuture<>();
      	this.transition
      		.commit()
      		.onSuccess((o) -> result.setValue(next(wiz)))
      		.onFailure((e) -> result.setError(e));
      	return result;
      }
      
      public Component show(Wizard<?, ?> wiz, I value) {
         transition.update(wiz, value);
         return transition.show(wiz);
      }
      
      public Component restore(Wizard<?, ?> wiz) {
         return transition.show(wiz);
      }
      
      public O getValue() {
         return transition.getValue();
      }
      
   }
   
   public static interface Transition<I, O> {
      
      void update(Wizard<?, ?> dialog, I input);

      Component show(Wizard<?, ?> dialog);
      
      O getValue();
      
      default ClientFuture<O> commit() { return Futures.succeededFuture(getValue()); }
   }
   
   public static class Builder<I, O> {
      private WizardStep<I, ?> first;
      private WizardStep<?, O> next;
      
      private Builder(Transition<I, O> first) {
         WizardStep<I, O> step = new WizardStep<I, O>(null, first);
         this.first = step;
         this.next = step;
      }
      
      private Builder(WizardStep<I, ?> first, WizardStep<?, O> next) {
         this.first = first;
         this.next = next;
      }
      
      public <T> Builder<I, T> addStep(Transition<O, T> transition) {
         WizardStep<O, T> last = new WizardStep<O, T>(this.next, transition);
         this.next.setNext(last);
         return new Builder<I, T>(first, last);
      }
      
      public Wizard<I, O> build() {
         return new Wizard<I, O>(first, next);
      }
   }

}


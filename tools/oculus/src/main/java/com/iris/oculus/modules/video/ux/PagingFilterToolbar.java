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
package com.iris.oculus.modules.video.ux;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.iris.client.service.VideoService.PageRecordingsRequest;
import com.iris.oculus.Oculus;
import com.iris.oculus.modules.video.VideoController;
import com.iris.oculus.modules.video.VideoFilter;
import com.iris.oculus.modules.video.dialog.VideoFilterDialog;
import com.iris.oculus.util.Actions;

public class PagingFilterToolbar extends JPanel {

   private Action prevAction = Actions.build("< Prev", this::onPrev);
   private Action nextAction = Actions.build("Next >", this::onNext);
   private Action filterAction = Actions.build("Filter", this::onFilter);

   private VideoController controller;
   private JLabel filterDescription;
   private List<String> tokens = new ArrayList<>();
   private VideoFilter model;

   public PagingFilterToolbar(VideoController controller) {
      init();
      this.controller = controller;
      this.model = new VideoFilter();
      this.prevAction.setEnabled(false);
      this.nextAction.setEnabled(false);
   }

   private void init() {
      filterDescription = new JLabel("Filtering...");

      setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
      add(new JButton(filterAction));
      add(filterDescription);
      add(Box.createHorizontalGlue());
      add(new JButton(prevAction));
      add(new JButton(nextAction));
      
      addComponentListener(new ComponentAdapter() {
         @Override
         public void componentShown(ComponentEvent e) {
            reloadCurrentPage();
         }
      });
   }
   
   public void reloadCurrentPage() {
      loadPage(currentToken(), this::updateToken);
   }
   
   private void loadPage(String token, Consumer<String> nextTokenConsumer) {
      filterDescription.setText("Loading...");
      controller
         .loadPage(model, token)
         .onSuccess((response) -> {
            nextTokenConsumer.accept(response.getNextToken());
            updateFilterBar();
         })
         .onFailure((e) -> {
            Oculus.error("Unable to load page", e);
            filterDescription.setText("Error loading recordings");
         });
   }

   private int currentPage() {
      return tokens.isEmpty() ? 1 : tokens.size();
   }
   
   private void updateFilterBar() {
      StringBuilder description = 
         new StringBuilder()
            .append("Page: ")
            .append(currentPage())
            ;
      if(model.isIncludeDeleted()) {
         description.append(" | Include Deleted");
      }
      if(model.isIncludeInProgress()) {
         description.append(" | Include In Progress");
      }
      if(PageRecordingsRequest.TYPE_ANY.equals(model.getType())) {
         description.append(" | STREAMS & RECORDINGS");
      }
      else {
         description.append(" | Only ").append(model.getType()).append("S");
      }
      if(model.getCameras().isEmpty()) {
         description.append(" | Any Camera");
      }
      else {
         description
            .append(" | ")
            .append(model.getCameras().size())
            .append(" Camera(s)");
      }
      if(model.getTags().contains("FAVORITE")) {
         description.append(" | Favorites Only");
      }
      if(model.getNewest().isPresent() && model.getOldest().isPresent()) {
         description
            .append(" | Recorded from ")
            .append(model.getNewest().get())
            .append(" to ")
            .append(model.getOldest().get());
      }
      else if(model.getNewest().isPresent()) {
         description.append(" | Recorded before ").append(model.getNewest().get());
      }
      else if(model.getOldest().isPresent()) {
         description.append(" | Recorded after ").append(model.getNewest().get());
      }
      filterDescription.setText(description.toString());
      nextAction.setEnabled(nextToken() != null);
      prevAction.setEnabled(currentToken() != null);
   }

   private String prevToken() {
      return getToken(3);
   }

   private String currentToken() {
      return getToken(2);
   }

   private String nextToken() {
      return getToken(1);
   }
   
   private String getToken(int back) {
      return tokens.size() < back ? null : tokens.get(tokens.size() - back);
   }
   
   private void rewindToken(String next) {
      if(tokens.isEmpty()) {
         // weird
         tokens.add(next);
      }
      else {
         tokens.remove(tokens.size() - 1);
         if(tokens.isEmpty()) {
            // weird
            tokens.add(next);
         }
         else {
            tokens.set(tokens.size() - 1, next);
         }
      }
   }
   
   private void updateToken(String next) {
      if(tokens.isEmpty()) {
         // weird
         tokens.add(next);
      }
      else {
         tokens.set(tokens.size() - 1, next);
      }
   }
   
   private void advanceToken(String next) {
      tokens.add(next);
   }

   protected void onFilter() {
      VideoFilterDialog
         .prompt(model)
         .onSuccess((model) -> {
            this.model = model;
            this.tokens.clear();
            reloadCurrentPage();
         });
   }
   
   protected void onPrev() {
      loadPage(prevToken(), this::rewindToken);
   }
   
   protected void onNext() {
      loadPage(nextToken(), this::advanceToken);
   }

}


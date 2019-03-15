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

import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JList;
import javax.swing.JScrollPane;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.iris.client.IrisClientFactory;
import com.iris.client.capability.Camera;
import com.iris.client.model.DeviceModel;
import com.iris.client.service.VideoService.PageRecordingsRequest;
import com.iris.oculus.modules.video.VideoFilter;
import com.iris.oculus.view.FilteredViewModel;
import com.iris.oculus.view.StoreViewModel;
import com.iris.oculus.view.ViewListModel;
import com.iris.oculus.widget.FieldWrapper;
import com.iris.oculus.widget.Fields;
import com.iris.oculus.widget.FormView;
import com.iris.oculus.widget.ListBoxBuilder;

public class VideoFilterForm extends FormView {
	private FieldWrapper<?, Date> startDate;
	private FieldWrapper<?, Date> endDate;
	private FieldWrapper<?, Boolean> includeDeleted;
	private FieldWrapper<?, Boolean> includeInProgress;
	private FieldWrapper<?, Boolean> favoritesOnly;
	private FieldWrapper<?, String> type;
	private FieldWrapper<?, Set<String>> cameras;

	public VideoFilterForm() {
		init();
	}
	
	private void init() {
		startDate =
				Fields
					.timestampBuilder()
					.labelled("Start Date")
					.build();
		endDate =
			Fields
				.timestampBuilder()
				.labelled("End Date")
				.build();
		includeDeleted =
			Fields
				.checkBoxBuilder()
				.labelled("Include Deleted")
				.build();
		includeInProgress =
				Fields
					.checkBoxBuilder()
					.labelled("Include In Progress")
					.build();
		favoritesOnly =
			Fields
				.checkBoxBuilder()
				.labelled("Only Favorites (Pinned)")
				.build();
		type =
			Fields
				.<String>comboBoxBuilder()
				.withValues(ImmutableList.of(PageRecordingsRequest.TYPE_ANY, PageRecordingsRequest.TYPE_RECORDING, PageRecordingsRequest.TYPE_STREAM))
				.noteditable()
				.labelled("Video Type")
				.build();
		JList<DeviceModel> cameraList = cameraList();
		cameras =
			Fields
				.<JList<DeviceModel>, Set<String>>builder(cameraList)
				.labelled("Cameras")
				.withGetter(
					(list) ->
						cameraList
							.getSelectedValuesList()
							.stream()
							.map(DeviceModel::getAddress)
							.collect(Collectors.toSet())
				)
//				.withSetter(
//					(addresses) ->
//						cameraList.
//						filter
//							.getCameras()
//							.stream()
//							.map((address) -> (DeviceModel) IrisClientFactory.getModelCache().get(address))
//							.filter(Objects::notNull)
//							.collect(Coll)
//				)
				.build();
		addField(startDate);
		addField(endDate);
		addField(includeDeleted);
		addField(includeInProgress);
		addField(favoritesOnly);
		addField(type);
		addField(cameras.getLabel(), new JScrollPane(cameras.getComponent()), LabelLocation.TOP);
	}

	private JList<DeviceModel> cameraList() {
		FilteredViewModel<DeviceModel> models = new FilteredViewModel<>(new StoreViewModel<>(IrisClientFactory.getStore(DeviceModel.class)));
		models.filterBy((model) -> model.getCaps() != null && model.getCaps().contains(Camera.NAMESPACE));
		ViewListModel<DeviceModel> viewListModel = new ViewListModel<>();
		viewListModel.bind(models);
		return 
			new ListBoxBuilder<DeviceModel>()
				.withModel(viewListModel)
				.multipleIntervalSelectionMode()
				.withRenderer(DeviceModel::getName)
				.create();
	}

	public void setValues(VideoFilter filter) {
		startDate.setValue(filter.getNewest().orElse(null));
		endDate.setValue(filter.getOldest().orElse(null));
		includeDeleted.setValue(filter.isIncludeDeleted());
		includeInProgress.setValue(filter.isIncludeInProgress());
		favoritesOnly.setValue(filter.getTags().contains("FAVORITE"));
		type.setValue(filter.getType());
	}
	
	public VideoFilter toFilter() {
		VideoFilter filter = new VideoFilter();
		filter.setNewest(startDate.getValue());
		filter.setOldest(endDate.getValue());
		filter.setIncludeDeleted(includeDeleted.getValue());
		filter.setIncludeInProgress(includeInProgress.getValue());
		filter.setTags(favoritesOnly.getValue() == Boolean.TRUE ? ImmutableSet.of("FAVORITE") : ImmutableSet.of());
		filter.setType(type.getValue());
		filter.setCameras(cameras.getValue());
		return filter;
	}
}


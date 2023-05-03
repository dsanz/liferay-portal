/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.frontend.data.set.views.web.internal.dataset.provider;

import com.liferay.frontend.data.set.views.web.internal.dataset.provider.api.ViewProvider;
import com.liferay.object.rest.dto.v1_0.ObjectEntry;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

import java.util.Collection;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Daniel Sanz
 */
@Component(service = ViewProvider.class)
public class ViewProviderImpl implements ViewProvider {

	@Override
	public JSONArray getViewsJSONArray(ObjectEntry fdsView) {
		Collection<ObjectEntry> fdsFields =
			_fdsEntryProviderHelper.getFDSFields(fdsView);

		if ((fdsFields == null) || fdsFields.isEmpty()) {
			return _getDefaultJSONArray();
		}

		return _getViewsJSONArray(fdsFields);
	}

	private JSONArray _getDefaultJSONArray() {
		return JSONUtil.putAll(
			JSONUtil.put(
				"contentRenderer", "table"
			).put(
				"default", false
			).put(
				"label", "Table"
			).put(
				"name", "table"
			).put(
				"quickActionsEnabled", false
			).put(
				"schema",
				JSONUtil.put(
					"fields",
					JSONUtil.putAll(
						JSONUtil.put(
							"contentRenderer", "default"
						).put(
							"expand", false
						).put(
							"fieldName", "id"
						).put(
							"label", "ID"
						).put(
							"localizeLabel", false
						).put(
							"sortable", false
						)))
			).put(
				"thumbnail", "table"
			));
	}

	private JSONArray _getViewsJSONArray(Collection<ObjectEntry> fdsFields) {
		try {
			return JSONUtil.putAll(
				JSONUtil.put(
					"contentRenderer", "table"
				).put(
					"default", false
				).put(
					"label", "Table"
				).put(
					"name", "table"
				).put(
					"quickActionsEnabled", false
				).put(
					"schema",
					JSONUtil.put(
						"fields",
						JSONUtil.toJSONArray(
							fdsFields,
							(ObjectEntry fdsField) -> {
								Map<String, Object> fdsFieldProperties =
									fdsField.getProperties();

								JSONArray jsonArray =
									_fdsEntryProviderHelper.
										getFieldNameJSONArray(fdsField);

								Object fieldName;

								if (jsonArray.length() > 1) {
									fieldName = jsonArray;
								}
								else {
									fieldName = jsonArray.get(0);
								}

								return JSONUtil.put(
									"contentRenderer",
									(String)fdsFieldProperties.get("renderer")
								).put(
									"expand", false
								).put(
									"fieldName", fieldName
								).put(
									"label",
									(String)fdsFieldProperties.get("label")
								).put(
									"localizeLabel", false
								).put(
									"sortable",
									(Boolean)fdsFieldProperties.get("sortable")
								);
							}))
				).put(
					"thumbnail", "table"
				));
		}
		catch (Exception exception) {
			_log.error("Unable to generate FDS view from FDSFields", exception);

			return _getDefaultJSONArray();
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(
		ViewProviderImpl.class);

	@Reference
	private FDSEntryProviderHelper _fdsEntryProviderHelper;

}
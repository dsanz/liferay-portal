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

import com.liferay.object.rest.dto.v1_0.ObjectEntry;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONUtil;

/**
 * @author Daniel Sanz
 */
public class ViewProviderImpl implements ViewProvider {

	@Override
	public JSONArray getViewsJSONArray(ObjectEntry fdsView) {
		return _getSampleViewsJSONArray();
	}

	private JSONArray _getSampleViewsJSONArray() {
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
							"contentRenderer", "image"
						).put(
							"expand", false
						).put(
							"fieldName", "thumbnail"
						).put(
							"label", ""
						).put(
							"localizeLabel", true
						).put(
							"sortable", false
						),
						JSONUtil.put(
							"contentRenderer", "actionLink"
						).put(
							"expand", false
						).put(
							"fieldName", JSONUtil.putAll("name", "LANG")
						).put(
							"label", "Name"
						).put(
							"localizeLabel", true
						).put(
							"sortable", true
						),
						JSONUtil.put(
							"contentRenderer", "actionLink"
						).put(
							"expand", false
						).put(
							"fieldName", JSONUtil.putAll("catalog", "name")
						).put(
							"label", "Catalog"
						).put(
							"localizeLabel", true
						).put(
							"sortable", false
						),
						JSONUtil.put(
							"expand", false
						).put(
							"fieldName", "productTypeI18n"
						).put(
							"label", "Type"
						).put(
							"localizeLabel", true
						).put(
							"sortable", false
						),
						JSONUtil.put(
							"contentRenderer", "status"
						).put(
							"expand", false
						).put(
							"fieldName", "workflowStatusInfo"
						).put(
							"label", "Status"
						).put(
							"localizeLabel", true
						).put(
							"sortable", false
						),
						JSONUtil.put(
							"contentRenderer", "dateTime"
						).put(
							"expand", false
						).put(
							"fieldName", "modifiedDate"
						).put(
							"label", "Modified Date"
						).put(
							"localizeLabel", true
						).put(
							"sortable", true
						)))
			).put(
				"thumbnail", "table"
			));
	}

}
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

import com.liferay.frontend.data.set.views.web.internal.dataset.provider.api.FilterProvider;
import com.liferay.object.rest.dto.v1_0.ObjectEntry;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONUtil;

/**
 * @author Daniel Sanz
 */
public class FilterProviderImpl implements FilterProvider {

	@Override
	public JSONArray getFiltersJSONArray(ObjectEntry fdsView) {
		return _getSampleFiltersJSONArray();
	}

	private JSONArray _getSampleFiltersJSONArray() {
		return JSONUtil.putAll(
			JSONUtil.put(
				"autocompleteEnabled", false
			).put(
				"id", "productType"
			).put(
				"items",
				JSONUtil.putAll(
					JSONUtil.put(
						"label", "Simple"
					).put(
						"value", "simple"
					),
					JSONUtil.put(
						"label", "Grouped"
					).put(
						"value", "grouped"
					),
					JSONUtil.put(
						"label", "Virtual"
					).put(
						"value", "virtual"
					))
			).put(
				"label", "Product Type"
			).put(
				"multiple", false
			).put(
				"type", "selection"
			));
	}

}
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

import com.liferay.frontend.data.set.views.web.internal.dataset.provider.api.PaginationProvider;
import com.liferay.object.rest.dto.v1_0.ObjectEntry;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import org.osgi.service.component.annotations.Component;

/**
 * @author Daniel Sanz
 */
@Component(
	service = PaginationProvider.class
)
public class PaginationProviderImpl implements PaginationProvider {

	@Override
	public JSONObject getPaginationJSONObject(ObjectEntry fdsView) {
		return _getSamplePaginationJSONObject();
	}

	private JSONObject _getSamplePaginationJSONObject() {
		return JSONUtil.put(
			"deltas",
			JSONUtil.putAll(
				JSONUtil.put("label", 4), JSONUtil.put("label", 10),
				JSONUtil.put("label", 20))
		).put(
			"initialDelta", 10
		).put(
			"initialPageNumber", 0
		);
	}

}
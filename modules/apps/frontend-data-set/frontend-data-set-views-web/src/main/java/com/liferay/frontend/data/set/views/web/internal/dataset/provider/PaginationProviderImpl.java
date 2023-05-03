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
import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringUtil;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.util.PropsValues;

import java.util.Arrays;
import java.util.Map;

import org.osgi.service.component.annotations.Component;

/**
 * @author Daniel Sanz
 */
@Component(service = PaginationProvider.class)
public class PaginationProviderImpl implements PaginationProvider {

	@Override
	public JSONObject getPaginationJSONObject(ObjectEntry fdsView) {
		Map<String, Object> fdsViewProperties = fdsView.getProperties();

		JSONObject paginationJSONObject;

		String itemsPerPageList = (String)fdsViewProperties.get(
			"listOfItemsPerPage");

		try {
			paginationJSONObject = JSONUtil.put(
				"deltas",
				JSONUtil.toJSONArray(
					StringUtil.split(itemsPerPageList, CharPool.COMMA),
					(String itemPerPageElement) -> JSONUtil.put(
						"label", Integer.parseInt(itemPerPageElement.trim())))
			).put(
				"initialDelta", fdsViewProperties.get("defaultItemsPerPage")
			).put(
				"initialPageNumber", 0
			);
		}
		catch (Exception exception) {
			if (_log.isWarnEnabled()) {
				_log.warn(
					StringBundler.concat(
						"Unable to create pagination JSONArray from '",
						itemsPerPageList, "'"),
					exception);
			}

			paginationJSONObject = _getDefaultPaginationJSONObject();
		}

		return paginationJSONObject;
	}

	private JSONObject _getDefaultPaginationJSONObject() {
		JSONArray itemsPerPageListJSONArray;

		int defaultItemsPerPage;

		try {
			itemsPerPageListJSONArray = JSONUtil.toJSONArray(
				Arrays.asList(PropsValues.SEARCH_CONTAINER_PAGE_DELTA_VALUES),
				itemPerPageElement -> JSONUtil.put(
					"label", itemPerPageElement));

			defaultItemsPerPage =
				PropsValues.SEARCH_CONTAINER_PAGE_DEFAULT_DELTA;
		}
		catch (Exception exception) {
			if (_log.isWarnEnabled()) {
				_log.warn(
					StringBundler.concat(
						"Please check ",
						PropsKeys.SEARCH_CONTAINER_PAGE_DELTA_VALUES,
						" property"),
					exception);
			}

			itemsPerPageListJSONArray = JSONUtil.putAll(
				JSONUtil.put("label", 4), JSONUtil.put("label", 10),
				JSONUtil.put("label", 20));

			defaultItemsPerPage = 10;
		}

		return JSONUtil.put(
			"deltas", itemsPerPageListJSONArray
		).put(
			"initialDelta", defaultItemsPerPage
		).put(
			"initialPageNumber", 0
		);
	}

	private static final Log _log = LogFactoryUtil.getLog(
		PaginationProviderImpl.class);

}
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

import com.liferay.frontend.data.set.constants.FDSEntityFieldTypes;
import com.liferay.frontend.data.set.views.web.internal.dataset.provider.helper.FDSObjectEntryProviderHelper;
import com.liferay.object.rest.dto.v1_0.ObjectEntry;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONObject;
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
@Component(service = FiltersProvider.class)
public class FiltersProvider {

	public JSONArray getFiltersJSONArray(ObjectEntry fdsView) {
		Collection<ObjectEntry> fdsFilters =
			_fdsObjectEntryProviderHelper.getFDSFilters(fdsView);

		if ((fdsFilters == null) || fdsFilters.isEmpty()) {
			return _jsonFactory.createJSONArray();
		}

		return _getFiltersJSONArray(fdsFilters);
	}

	private JSONObject _getFDSDateFilterJSONObject(ObjectEntry fdsDateFilter) {
		Map<String, Object> fdsDateFilterProperties =
			fdsDateFilter.getProperties();

		return JSONUtil.put(
			"entityFieldType", FDSEntityFieldTypes.DATE
		).put(
			"id", fdsDateFilterProperties.get("fieldName")
		).put(
			"label", (String)fdsDateFilterProperties.get("label")
		).put(
			"type", "dateRange"
		);
	}

	private JSONArray _getFiltersJSONArray(Collection<ObjectEntry> fdsFilters) {
		try {
			return JSONUtil.toJSONArray(
				fdsFilters,
				(ObjectEntry fdsFilter) -> {
					if (_fdsObjectEntryProviderHelper.isFDSDateFilter(
							fdsFilter)) {

						return _getFDSDateFilterJSONObject(fdsFilter);
					}

					return null;
				});
		}
		catch (Exception exception) {
			if (_log.isWarnEnabled()) {
				_log.warn(
					"Unable to generate FDS filters from FDSView", exception);
			}

			return _jsonFactory.createJSONArray();
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(
		FiltersProvider.class);

	@Reference
	private FDSObjectEntryProviderHelper _fdsObjectEntryProviderHelper;

	@Reference
	private JSONFactory _jsonFactory;

}
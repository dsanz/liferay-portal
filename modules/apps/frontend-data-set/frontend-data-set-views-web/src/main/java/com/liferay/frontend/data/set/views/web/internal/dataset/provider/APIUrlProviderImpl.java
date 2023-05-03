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

import com.liferay.frontend.data.set.views.web.internal.dataset.provider.api.APIUrlProvider;
import com.liferay.object.rest.dto.v1_0.ObjectEntry;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.util.StringUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Map;

/**
 * @author Daniel Sanz
 */
@Component(service = APIUrlProvider.class)
public class APIUrlProviderImpl implements APIUrlProvider {
	/* main things to do here:
	    - interpolate URL parameters with context values (siteId, userId)
	    - add required nested fields depending on field mappings
	    - add sorting options

	  things we don't need to worry about (FDS manages them)
	    - Add page numbers and items per page
	    - Add odata query in case filters are pre-applied
	 */
	public String getApiUrl(
		ObjectEntry fdsView, HttpServletRequest httpServletRequest) {

		ObjectEntry fdsEntry = _fdsEntryProviderUtil.getFDSEntry(fdsView);

		String apiUrl = _getAPIUrlBasePath(fdsEntry);

		apiUrl = _getNestedFields(apiUrl, fdsView);

		return apiUrl;
	}

	private String _getNestedFields(String apiUrl, ObjectEntry fdsView) {
		Collection<ObjectEntry> fdsFields = _fdsEntryProviderUtil.getFDSFields(fdsView);

		if (fdsFields == null || fdsFields.size() == 0) {
			return apiUrl;
		}

		String nestedFields = StringPool.BLANK;

		for (ObjectEntry fdsField : fdsFields) {
			JSONArray jsonArray = _fdsEntryProviderUtil.getFieldName(fdsField);

			if (jsonArray.length() > 1) {
				nestedFields =
					StringUtil.add(nestedFields, jsonArray.getString(0));
			}
		}

		if (nestedFields.equals(StringPool.BLANK)) {
			return apiUrl;
		}

		StringBundler sb = new StringBundler(3);

		sb.append(apiUrl);
		sb.append("?nestedFields=");
		sb.append(StringUtil.replaceLast(nestedFields, StringPool.COMMA, StringPool.BLANK));

		return sb.toString();
	}


	private String _getAPIUrlBasePath(ObjectEntry fdsEntry) {
		Map<String, Object> fdsEntryProperties = fdsEntry.getProperties();

		String restEndpoint = (String) fdsEntryProperties.get("restEndpoint");

		String restApplication = (String) fdsEntryProperties.get("restApplication");

		StringBundler sb = new StringBundler(3);

		sb.append("/o");
		// temporary hack while restApp contains the version
		sb.append(StringUtil.replaceLast(restApplication, "/v1.0", StringPool.BLANK));
		sb.append(restEndpoint);

		return sb.toString();
	}

	@Reference
	private FDSEntryProviderUtil _fdsEntryProviderUtil;

	@Reference
	private JSONFactory _jsonFactory;

}
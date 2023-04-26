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

import org.osgi.service.component.annotations.Component;

import javax.servlet.http.HttpServletRequest;

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
	public String getApiUrl(ObjectEntry fdsView, HttpServletRequest httpServletRequest) {

		return "/o/headless-commerce-admin-catalog/v1.0/products" +
			"?nestedFields=skus,catalog";
	}

}
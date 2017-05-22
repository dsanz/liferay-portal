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

package com.liferay.dynamic.data.mapping.data.provider.internal;

import com.liferay.dynamic.data.mapping.data.provider.DDMDataProvider;
import com.liferay.dynamic.data.mapping.data.provider.DDMDataProviderContext;
import com.liferay.dynamic.data.mapping.data.provider.DDMDataProviderContextContributor;
import com.liferay.dynamic.data.mapping.data.provider.DDMDataProviderException;
import com.liferay.dynamic.data.mapping.data.provider.DDMDataProviderInvoker;
import com.liferay.dynamic.data.mapping.data.provider.DDMDataProviderRequest;
import com.liferay.dynamic.data.mapping.data.provider.DDMDataProviderResponse;
import com.liferay.dynamic.data.mapping.data.provider.DDMDataProviderTracker;

import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Marcellus Tavares
 */
@Component(immediate = true)
public class DDMDataProviderInvokerImpl implements DDMDataProviderInvoker {

	public DDMDataProviderResponse invoke(
			DDMDataProviderRequest ddmDataProviderRequest)
		throws DDMDataProviderException {

		addDDMDataProviderContextParameters(ddmDataProviderRequest);

		DDMDataProvider ddmDataProvider = getDDMDataProvider(
			ddmDataProviderRequest.getDDMDataProviderContext());

		return ddmDataProvider.getData(ddmDataProviderRequest);
	}

	protected void addDDMDataProviderContextParameters(
		DDMDataProviderRequest ddmDataProviderRequest) {

		DDMDataProviderContext ddmDataProviderContext =
			ddmDataProviderRequest.getDDMDataProviderContext();

		if (ddmDataProviderContext.getType() == null) {
			return;
		}

		// Backwards compatibility

		ddmDataProviderRequest.queryString(
			ddmDataProviderContext.getParameters());

		// Context Contributors

		List<DDMDataProviderContextContributor>
			ddmDataProviderContextContributors =
				ddmDataProviderTracker.getDDMDataProviderContextContributors(
					ddmDataProviderContext.getType());

		for (DDMDataProviderContextContributor
				ddmDataProviderContextContributor :
					ddmDataProviderContextContributors) {

			Map<String, String> parameters =
				ddmDataProviderContextContributor.getParameters(
					ddmDataProviderRequest.getHttpServletRequest());

			if (parameters == null) {
				continue;
			}

			ddmDataProviderRequest.queryString(parameters);
		}
	}

	protected DDMDataProvider getDDMDataProvider(
		DDMDataProviderContext ddmDataProviderContext) {

		String type = ddmDataProviderContext.getType();

		if (type == null) {
			return ddmDataProviderTracker.getDDMDataProviderByInstanceId(
				ddmDataProviderContext.getDDMDataProviderInstanceId());
		}

		return ddmDataProviderTracker.getDDMDataProvider(type);
	}

	@Reference
	protected DDMDataProviderTracker ddmDataProviderTracker;

}
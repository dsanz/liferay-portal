/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.frontend.data.set.internal.url.builder;

import com.liferay.frontend.data.set.url.builder.FDSAPIURLBuilder;
import com.liferay.frontend.data.set.url.builder.FDSAPIURLBuilderFactory;
import com.liferay.frontend.data.set.url.builder.FDSAPIURLResolverRegistry;

import javax.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Daniel Sanz
 */
@Component(service = FDSAPIURLBuilderFactory.class)
public class FDSAPIURLBuilderFactoryImpl implements FDSAPIURLBuilderFactory {

	@Override
	public FDSAPIURLBuilder create(
		String restEndpoint, String restApplication, String restSchema,
		HttpServletRequest httpServletRequest) {

		return new FDSAPIURLBuilderImpl(
			restEndpoint, restApplication, restSchema,
			_fdsAPIURLResolverRegistry, httpServletRequest);
	}

	@Reference
	private FDSAPIURLResolverRegistry _fdsAPIURLResolverRegistry;

}
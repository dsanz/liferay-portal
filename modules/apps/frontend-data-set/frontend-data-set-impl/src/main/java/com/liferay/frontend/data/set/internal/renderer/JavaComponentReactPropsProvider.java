/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.frontend.data.set.internal.renderer;

import com.liferay.frontend.data.set.DataSetRegistry;
import com.liferay.frontend.data.set.renderer.ReactPropsProvider;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

import java.io.IOException;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Daniel Sanz
 */
@Component(
	property = "react.props.provider.order:Integer=100",
	service = ReactPropsProvider.class
)
public class JavaComponentReactPropsProvider implements ReactPropsProvider {

	@Override
	public Map<String, Object> getObjectEntryReactProps(
			String fdsName, HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse,
			Map<String, Object> context)
		throws IOException {

		/* TODO: use new APIs to serialize from here. In this PoC we are
		using all the info provided by the BaseDisplayTag. If we want fragment
		to render system datasets from java services, we'll need to
		calculate all props here and not in the tag lib
		*/

		return context;
	}

	@Override
	public boolean isAvailable(
		String fdsName, HttpServletRequest httpServletRequest) {

		return true;
		//return _dataSetRegistry.getDataSet(fdsName) != null;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		JavaComponentReactPropsProvider.class);

	@Reference
	private DataSetRegistry _dataSetRegistry;

}
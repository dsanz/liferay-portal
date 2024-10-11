/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.frontend.data.set.internal.renderer;

import com.liferay.frontend.data.set.renderer.DataSetRenderer;
import com.liferay.frontend.data.set.renderer.ReactPropsProvider;
import com.liferay.frontend.data.set.renderer.ReactPropsProviderRegistry;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.template.react.renderer.ComponentDescriptor;
import com.liferay.portal.template.react.renderer.ReactRenderer;

import java.io.Writer;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Daniel Sanz
 */
@Component(service = DataSetRenderer.class)
public class DataSetRendererImpl implements DataSetRenderer {

	@Override
	public void render(
		boolean inline, String componentId, String propsTransformer,
		String fdsName, HttpServletRequest httpServletRequest,
		HttpServletResponse httpServletResponse, Map<String, Object> context,
		Writer writer) {

		try {
			ReactPropsProvider reactPropsProvider =
				_reactPropsProviderRegistry.getReactPropsProvider(
					fdsName, httpServletRequest);

			ComponentDescriptor componentDescriptor = new ComponentDescriptor(
				"{FrontendDataSet} from frontend-data-set-web", componentId,
				null, inline, propsTransformer);

			_reactRenderer.renderReact(
				componentDescriptor,
				reactPropsProvider.getObjectEntryReactProps(
					fdsName, httpServletRequest, httpServletResponse, context),
				httpServletRequest, writer);
		}
		catch (Exception exception) {
			if (_log.isWarnEnabled()) {
				_log.warn("Unable to render data set " + fdsName, exception);
			}
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(
		DataSetRendererImpl.class);

	@Reference
	private ReactPropsProviderRegistry _reactPropsProviderRegistry;

	@Reference
	private ReactRenderer _reactRenderer;

}
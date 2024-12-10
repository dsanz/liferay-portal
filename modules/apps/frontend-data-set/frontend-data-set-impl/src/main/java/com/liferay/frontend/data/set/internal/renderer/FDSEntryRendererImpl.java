/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.frontend.data.set.internal.renderer;

import com.liferay.frontend.data.set.renderer.FDSEntryPropsProvider;
import com.liferay.frontend.data.set.renderer.FDSEntryPropsProviderRegistry;
import com.liferay.frontend.data.set.renderer.FDSEntryRenderer;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.template.react.renderer.ComponentDescriptor;
import com.liferay.portal.template.react.renderer.ReactRenderer;

import java.io.Writer;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Daniel Sanz
 */
@Component(service = FDSEntryRenderer.class)
public class FDSEntryRendererImpl implements FDSEntryRenderer {

	@Override
	public void render(
		String fdsName, String componentId, String propsTransformer,
		boolean inline, HttpServletRequest httpServletRequest,
		HttpServletResponse httpServletResponse, Map<String, Object> baseProps,
		Writer writer) {

		try {
			FDSEntryPropsProvider fdsEntryPropsProvider =
				_fdsEntryPropsProviderRegistry.getFDSEntryPropsProvider(
					fdsName, httpServletRequest);

			Map<String, Object> props = new HashMap<>();

			if (baseProps != null) {
				props.putAll(baseProps);
			}

			props.putAll(
				fdsEntryPropsProvider.prepareProps(
					fdsName, httpServletRequest));

			_reactRenderer.renderReact(
				new ComponentDescriptor(
					"{FrontendDataSet} from frontend-data-set-web", componentId,
					null, inline, propsTransformer),
				props, httpServletRequest, writer);
		}
		catch (Exception exception) {
			_log.error("Unable to render data set " + fdsName, exception);
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(
		FDSEntryRendererImpl.class);

	@Reference
	private FDSEntryPropsProviderRegistry _fdsEntryPropsProviderRegistry;

	@Reference
	private ReactRenderer _reactRenderer;

}
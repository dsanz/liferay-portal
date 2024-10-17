/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.client.extension.web.internal.frontend.data.set.action;

import com.liferay.client.extension.type.factory.CETFactory;
import com.liferay.client.extension.web.internal.constants.ClientExtensionAdminFDSNames;
import com.liferay.client.extension.web.internal.constants.ClientExtensionAdminPortletKeys;
import com.liferay.client.extension.web.internal.display.context.ClientExtensionAdminDisplayContext;
import com.liferay.frontend.data.set.DataSetEntityImportPolicy;
import com.liferay.frontend.data.set.action.FDSCreationMenu;
import com.liferay.frontend.taglib.clay.servlet.taglib.util.CreationMenu;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Portlet;
import com.liferay.portal.kernel.portlet.InvokerPortlet;
import com.liferay.portal.kernel.portlet.LiferayRenderRequest;
import com.liferay.portal.kernel.portlet.PortletConfigFactoryUtil;
import com.liferay.portal.kernel.portlet.PortletInstanceFactoryUtil;
import com.liferay.portal.kernel.portlet.PortletPreferencesFactoryUtil;
import com.liferay.portal.kernel.service.PortletLocalService;
import com.liferay.portal.kernel.service.PortletPreferencesLocalService;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.JavaConstants;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portlet.RenderRequestFactory;
import com.liferay.portlet.RenderResponseFactory;

import javax.portlet.PortletConfig;
import javax.portlet.PortletContext;
import javax.portlet.PortletMode;
import javax.portlet.PortletPreferences;
import javax.portlet.PortletResponse;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.WindowState;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Daniel Sanz
 */
@Component(
	property = "frontend.data.set.name=" + ClientExtensionAdminFDSNames.CLIENT_EXTENSION_TYPES,
	service = FDSCreationMenu.class
)
public class CETCreationMenu implements FDSCreationMenu {

	@Override
	public CreationMenu getCreationMenu(
			HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse)
		throws PortalException {

		CreationMenu creationMenu = new CreationMenu();

		Portlet portlet = _portletLocalService.getPortletById(
			ClientExtensionAdminPortletKeys.CLIENT_EXTENSION_ADMIN);

		ServletContext servletContext =
			(ServletContext)httpServletRequest.getAttribute(WebKeys.CTX);

		try {
			HttpServletRequest newHttpServletRequest =
				new HttpServletRequestWrapper(httpServletRequest);

			InvokerPortlet invokerPortlet = PortletInstanceFactoryUtil.create(
				portlet, servletContext);

			PortletPreferences portletPreferences =
				_portletPreferencesLocalService.getStrictPreferences(
					PortletPreferencesFactoryUtil.getPortletPreferencesIds(
						newHttpServletRequest, portlet.getPortletId()));

			PortletConfig portletConfig = PortletConfigFactoryUtil.create(
				portlet, servletContext);

			PortletContext portletContext = portletConfig.getPortletContext();

			ThemeDisplay themeDisplay =
				(ThemeDisplay)httpServletRequest.getAttribute(
					WebKeys.THEME_DISPLAY);

			LiferayRenderRequest liferayRenderRequest =
				RenderRequestFactory.create(
					newHttpServletRequest, portlet, invokerPortlet,
					portletContext, WindowState.NORMAL, PortletMode.VIEW,
					portletPreferences, themeDisplay.getPlid());

			liferayRenderRequest.setPortletRequestDispatcherRequest(
				newHttpServletRequest);

			PortletResponse portletResponse = RenderResponseFactory.create(
				httpServletResponse, liferayRenderRequest);

			liferayRenderRequest.defineObjects(portletConfig, portletResponse);

			RenderRequest renderRequest =
				(RenderRequest)httpServletRequest.getAttribute(
					JavaConstants.JAVAX_PORTLET_REQUEST);

			RenderResponse renderResponse =
				(RenderResponse)httpServletRequest.getAttribute(
					JavaConstants.JAVAX_PORTLET_RESPONSE);

			newHttpServletRequest.setAttribute(
				JavaConstants.JAVAX_PORTLET_REQUEST, liferayRenderRequest);

			newHttpServletRequest.setAttribute(
				JavaConstants.JAVAX_PORTLET_RESPONSE, portletResponse);

			ClientExtensionAdminDisplayContext
				clientExtensionAdminDisplayContext =
					new ClientExtensionAdminDisplayContext(
						_cetFactory, liferayRenderRequest, portletResponse);

			creationMenu = clientExtensionAdminDisplayContext.getCreationMenu();

			httpServletRequest.setAttribute(
				JavaConstants.JAVAX_PORTLET_REQUEST, renderRequest);

			httpServletRequest.setAttribute(
				JavaConstants.JAVAX_PORTLET_RESPONSE, renderResponse);
		}
		catch (Exception exception) {
			if (_log.isWarnEnabled()) {
				_log.warn(exception);
			}
		}

		return creationMenu;
	}

	public DataSetEntityImportPolicy getImportPolicy() {
		return DataSetEntityImportPolicy.GROUP_PROXY;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		CETCreationMenu.class);

	@Reference
	private CETFactory _cetFactory;

	@Reference
	private PortletLocalService _portletLocalService;

	@Reference
	private PortletPreferencesLocalService _portletPreferencesLocalService;

}
/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.commerce.product.definitions.web.internal.frontend.data.set.action;

import com.liferay.account.service.AccountGroupRelLocalService;
import com.liferay.commerce.product.constants.CPConstants;
import com.liferay.commerce.product.constants.CPPortletKeys;
import com.liferay.commerce.product.definitions.web.internal.constants.CommerceProductFDSNames;
import com.liferay.commerce.product.definitions.web.internal.display.context.CPDefinitionsDisplayContext;
import com.liferay.commerce.product.portlet.action.ActionHelper;
import com.liferay.commerce.product.service.CPDefinitionService;
import com.liferay.commerce.product.service.CommerceCatalogService;
import com.liferay.commerce.product.service.CommerceChannelRelService;
import com.liferay.commerce.product.url.CPFriendlyURL;
import com.liferay.frontend.data.set.action.FDSItemActionList;
import com.liferay.frontend.data.set.model.FDSActionDropdownItem;
import com.liferay.item.selector.ItemSelector;
import com.liferay.portal.configuration.module.configuration.ConfigurationProvider;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Portlet;
import com.liferay.portal.kernel.portlet.InvokerPortlet;
import com.liferay.portal.kernel.portlet.LiferayRenderRequest;
import com.liferay.portal.kernel.portlet.PortletConfigFactoryUtil;
import com.liferay.portal.kernel.portlet.PortletInstanceFactoryUtil;
import com.liferay.portal.kernel.portlet.PortletPreferencesFactoryUtil;
import com.liferay.portal.kernel.security.permission.resource.PortletResourcePermission;
import com.liferay.portal.kernel.service.PortletLocalService;
import com.liferay.portal.kernel.service.PortletPreferencesLocalService;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.JavaConstants;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portlet.RenderRequestFactory;
import com.liferay.portlet.RenderResponseFactory;

import java.util.Collections;
import java.util.List;

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
	property = "frontend.data.set.name=" + CommerceProductFDSNames.PRODUCT_DEFINITIONS,
	service = FDSItemActionList.class
)
public class CPDefinitionsItemActionList implements FDSItemActionList {

	@Override
	public List<FDSActionDropdownItem> getDropdownItems(
			HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse)
		throws PortalException {

		/* pretend we are in the CPDefinitions Portlet,
		 though we are in the DSM. This is an experiment that attempts to reuse
		 the CPDefinitionsDisplayContext object to calculate action URLs, aimed
		 at testing the import when isProxy() returns false.
		 For this dataset, however, isProxy() must return true as there is
		 context information to calculate the action dropdown items*/

		Portlet portlet = _portletLocalService.getPortletById(
			CPPortletKeys.CP_DEFINITIONS);

		ServletContext servletContext =
			(ServletContext)httpServletRequest.getAttribute(WebKeys.CTX);

		List<FDSActionDropdownItem> fdsActionDropdownItems =
			Collections.emptyList();

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

			CPDefinitionsDisplayContext cpDefinitionsDisplayContext =
				new CPDefinitionsDisplayContext(
					_actionHelper, newHttpServletRequest,
					_accountGroupRelLocalService, _commerceCatalogService,
					_commerceChannelRelService, _configurationProvider,
					_cpDefinitionService, _cpFriendlyURL, _itemSelector,
					_portletResourcePermission);

			fdsActionDropdownItems =
				cpDefinitionsDisplayContext.getFDSActionDropdownItems();

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

		return fdsActionDropdownItems;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		CPDefinitionsItemActionList.class);

	@Reference
	private AccountGroupRelLocalService _accountGroupRelLocalService;

	@Reference
	private ActionHelper _actionHelper;

	@Reference
	private CommerceCatalogService _commerceCatalogService;

	@Reference
	private CommerceChannelRelService _commerceChannelRelService;

	@Reference
	private ConfigurationProvider _configurationProvider;

	@Reference
	private CPDefinitionService _cpDefinitionService;

	@Reference
	private CPFriendlyURL _cpFriendlyURL;

	@Reference
	private ItemSelector _itemSelector;

	@Reference
	private PortletLocalService _portletLocalService;

	@Reference
	private PortletPreferencesLocalService _portletPreferencesLocalService;

	@Reference(
		target = "(resource.name=" + CPConstants.RESOURCE_NAME_PRODUCT + ")"
	)
	private PortletResourcePermission _portletResourcePermission;

}
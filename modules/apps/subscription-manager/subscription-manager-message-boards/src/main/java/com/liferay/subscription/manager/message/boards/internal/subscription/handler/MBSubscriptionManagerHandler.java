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

package com.liferay.subscription.manager.message.boards.internal.subscription.handler;

import com.liferay.message.boards.kernel.model.MBCategory;
import com.liferay.message.boards.kernel.service.MBCategoryService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.portlet.LiferayPortletResponse;
import com.liferay.portal.kernel.portlet.LiferayPortletURL;
import com.liferay.portal.kernel.portlet.PortletProvider;
import com.liferay.portal.kernel.portlet.PortletProviderUtil;
import com.liferay.portal.kernel.util.JavaConstants;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.ResourceBundleLoader;
import com.liferay.portal.kernel.util.ResourceBundleUtil;
import com.liferay.portal.language.LanguageResources;
import com.liferay.subscription.manager.SubscriptionManagerHandler;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.portlet.PortletResponse;
import javax.portlet.PortletURL;

import javax.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Roberto Díaz
 */
@Component(
	property =
		{"model.class.name=com.liferay.message.boards.kernel.model.MBCategory"}
)
public class MBSubscriptionManagerHandler
	implements SubscriptionManagerHandler<MBCategory> {

	@Override
	public String getClassName() {
		return MBCategory.class.getName();
	}

	@Override
	public long getClassPK(Object object) {
		MBCategory mbCategory = (MBCategory)object;

		return mbCategory.getCategoryId();
	}

	@Override
	public String getEmptyResultsMessage() {
		return "no-categories-were-found";
	}

	@Override
	public String getModelName(Locale locale) {
		ResourceBundleLoader resourceBundleLoader = getResourceBundleLoader();

		ResourceBundle resourceBundle = resourceBundleLoader.loadResourceBundle(
			locale);

		return ResourceBundleUtil.getString(
			resourceBundle,
			"model.resource.com.liferay.message.boards.kernel.model." +
				"MBCategory");
	}

	@Override
	public String getName(Object object) {
		MBCategory mbCategory = (MBCategory)object;

		return mbCategory.getName();
	}

	@Override
	public String getNameLabel(Locale locale) {
		return "name";
	}

	@Override
	public List<MBCategory> getResults(
			long groupId, long classPK, int start, int end)
		throws PortalException {

		return _mbCategoryService.getCategories(groupId, classPK, start, end);
	}

	@Override
	public int getTabPosition() {
		return 100;
	}

	@Override
	public int getTotal(long groupId, long classPK) throws PortalException {
		return _mbCategoryService.getCategoriesCount(groupId, classPK);
	}

	@Override
	public PortletURL getViewURL(
			long classPK, HttpServletRequest request, String redirect)
		throws PortalException {

		PortletResponse portletResponse = (PortletResponse)request.getAttribute(
			JavaConstants.JAVAX_PORTLET_RESPONSE);

		LiferayPortletResponse liferayPortletResponse =
			_portal.getLiferayPortletResponse(portletResponse);

		String portletId = PortletProviderUtil.getPortletId(
			MBCategory.class.getName(), PortletProvider.Action.MANAGE);

		LiferayPortletURL viewURL = liferayPortletResponse.createRenderURL(
			portletId);

		viewURL.setParameter("mvcRenderCommandName", "/message_boards/view");
		viewURL.setParameter("mbCategoryId", String.valueOf(classPK));

		return viewURL;
	}

	protected ResourceBundleLoader getResourceBundleLoader() {
		return LanguageResources.RESOURCE_BUNDLE_LOADER;
	}

	@Reference(unbind = "-")
	protected void setMBCategoryService(MBCategoryService mbCategoryService) {
		_mbCategoryService = mbCategoryService;
	}

	@Reference(unbind = "-")
	protected void setPortal(Portal portal) {
		_portal = portal;
	}

	private MBCategoryService _mbCategoryService;
	private Portal _portal;

}
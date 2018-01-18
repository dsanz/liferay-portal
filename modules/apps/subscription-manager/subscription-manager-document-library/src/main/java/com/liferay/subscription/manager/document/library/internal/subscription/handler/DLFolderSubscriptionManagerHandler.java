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

package com.liferay.subscription.manager.document.library.internal.subscription.handler;

import com.liferay.document.library.kernel.model.DLFolder;
import com.liferay.document.library.kernel.model.DLFolderConstants;
import com.liferay.document.library.kernel.service.DLAppService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.portlet.LiferayPortletResponse;
import com.liferay.portal.kernel.portlet.LiferayPortletURL;
import com.liferay.portal.kernel.portlet.PortletProvider;
import com.liferay.portal.kernel.portlet.PortletProviderUtil;
import com.liferay.portal.kernel.repository.model.Folder;
import com.liferay.portal.kernel.util.JavaConstants;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.ResourceBundleLoader;
import com.liferay.portal.kernel.util.ResourceBundleUtil;
import com.liferay.portal.language.LanguageResources;
import com.liferay.subscription.manager.SubscriptionManagerHandler;
import com.liferay.subscription.manager.document.library.internal.breadcrumb.DLFolderSubscriptionManagerBreadcrumbHandler;

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
		{"model.class.name=com.liferay.portal.kernel.repository.model.Folder"}
)
public class DLFolderSubscriptionManagerHandler
	implements SubscriptionManagerHandler<Folder> {

	@Override
	public void addPortletBreadcrumbEntries(
			long classPK, HttpServletRequest request,
			LiferayPortletResponse liferayPortletResponse)
		throws Exception {

		Folder folder = null;

		if (classPK > DLFolderConstants.DEFAULT_PARENT_FOLDER_ID) {
			folder = _dlAppService.getFolder(classPK);
		}

		_dlFolderSubscriptionManagerBreadcrumbHandler.
			addPortletBreadcrumbEntries(
				folder, request, liferayPortletResponse);
	}

	@Override
	public String getClassName() {
		return DLFolder.class.getName();
	}

	@Override
	public long getClassPK(Object object) {
		Folder folder = (Folder)object;

		return folder.getFolderId();
	}

	@Override
	public String getEmptyResultsMessage() {
		return "there-are-no-folders";
	}

	@Override
	public String getModelName(Locale locale) {
		ResourceBundleLoader resourceBundleLoader = getResourceBundleLoader();

		ResourceBundle resourceBundle = resourceBundleLoader.loadResourceBundle(
			locale);

		return ResourceBundleUtil.getString(
			resourceBundle,
			"model.resource.com.liferay.portal.kernel.repository.model.Folder");
	}

	@Override
	public String getName(Object object) {
		Folder folder = (Folder)object;

		return folder.getName();
	}

	@Override
	public String getNameLabel(Locale locale) {
		return "name";
	}

	@Override
	public List<Folder> getResults(
			long groupId, long classPK, int start, int end)
		throws PortalException {

		return _dlAppService.getFolders(groupId, classPK, start, end);
	}

	@Override
	public int getTotal(long groupId, long classPK) throws PortalException {
		return _dlAppService.getFoldersCount(groupId, classPK);
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
			Folder.class.getName(), PortletProvider.Action.MANAGE);

		LiferayPortletURL viewURL = liferayPortletResponse.createRenderURL(
			portletId);

		viewURL.setParameter("mvcRenderCommandName", "/document_library/view");
		viewURL.setParameter("folderId", String.valueOf(classPK));

		return viewURL;
	}

	@Override
	public boolean isModelBrowseable(long classPK) throws PortalException {
		Folder folder = _dlAppService.getFolder(classPK);

		int foldersCount = _dlAppService.getFoldersCount(
			folder.getRepositoryId(), folder.getFolderId());

		if (foldersCount > 0) {
			return true;
		}

		return false;
	}

	@Override
	public boolean isShowPortletBreadcrumb() throws PortalException {
		return true;
	}

	protected ResourceBundleLoader getResourceBundleLoader() {
		return LanguageResources.RESOURCE_BUNDLE_LOADER;
	}

	@Reference(unbind = "-")
	protected void setDLAppService(DLAppService dlAppService) {
		_dlAppService = dlAppService;
	}

	@Reference(unbind = "-")
	protected void setDLFolderSubscriptionManagerBreadcrumbHandler(
		DLFolderSubscriptionManagerBreadcrumbHandler
			dlFolderSubscriptionManagerBreadcrumbHandler) {

		_dlFolderSubscriptionManagerBreadcrumbHandler =
			dlFolderSubscriptionManagerBreadcrumbHandler;
	}

	@Reference(unbind = "-")
	protected void setPortal(Portal portal) {
		_portal = portal;
	}

	private DLAppService _dlAppService;
	private DLFolderSubscriptionManagerBreadcrumbHandler
		_dlFolderSubscriptionManagerBreadcrumbHandler;
	private Portal _portal;

}
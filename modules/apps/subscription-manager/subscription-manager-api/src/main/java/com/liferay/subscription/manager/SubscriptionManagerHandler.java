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

package com.liferay.subscription.manager;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.portlet.LiferayPortletResponse;

import java.util.List;
import java.util.Locale;

import javax.portlet.PortletURL;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Roberto Díaz
 */
public interface SubscriptionManagerHandler<T> {

	public default void addPortletBreadcrumbEntries(
			long classPK, HttpServletRequest request,
			LiferayPortletResponse liferayPortletResponse)
		throws Exception {

		return;
	}

	public String getClassName();

	public long getClassPK(Object object);

	public String getEmptyResultsMessage();

	public String getModelName(Locale locale);

	public String getName(Object object);

	public String getNameLabel(Locale locale);

	public List<T> getResults(long groupId, long classPK, int start, int end)
		throws PortalException;

	public int getTabPosition();

	public int getTotal(long groupId, long classPK) throws PortalException;

	public PortletURL getViewURL(
			long classPK, HttpServletRequest request, String redirect)
		throws PortalException;

	public default boolean isModelBrowseable(long classPK)
		throws PortalException {

		return false;
	}

	public default boolean isShowPortletBreadcrumb() throws PortalException {
		return false;
	}

}
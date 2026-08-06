/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.portal.url.builder;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Creates new Absolute Portal URL Builder instances.
 *
 * @author Iván Zaera Avellón
 */
public interface AbsolutePortalURLBuilderFactory {

	/**
	 * Returns a new Absolute Portal URL Builder instance tied to the given
	 * request.
	 *
	 * <p>
	 * The request may be <code>null</code> for callers that genuinely have
	 * none, such as a content transformer that takes a string and returns a
	 * string. The CDN host is then resolved from the ambient company instead of
	 * from the request, and the secure host is preferred. Builders that consume
	 * the request for anything else must not be used on such an instance.
	 * </p>
	 *
	 * @param  httpServletRequest the servlet request, or <code>null</code>
	 * @return an instance of Absolute Portal URL Builder
	 */
	public AbsolutePortalURLBuilder getAbsolutePortalURLBuilder(
		HttpServletRequest httpServletRequest);

}
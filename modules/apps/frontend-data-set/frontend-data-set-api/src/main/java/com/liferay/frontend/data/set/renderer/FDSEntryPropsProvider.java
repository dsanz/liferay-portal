/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.frontend.data.set.renderer;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Daniel Sanz
 */
public interface FDSEntryPropsProvider {

	public boolean isAvailable(
		String fdsName, HttpServletRequest httpServletRequest);

	public Map<String, Object> prepareProps(
		String fdsName, HttpServletRequest httpServletRequest);

}
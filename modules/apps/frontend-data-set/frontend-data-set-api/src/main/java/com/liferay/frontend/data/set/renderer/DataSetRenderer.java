/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.frontend.data.set.renderer;

import java.io.Writer;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Daniel Sanz
 */
public interface DataSetRenderer {

	public void render(
		boolean inline, String componentId, String propsTransformer,
		String fdsName, HttpServletRequest httpServletRequest,
		HttpServletResponse httpServletResponse, Map<String, Object> context,
		Writer writer);

}
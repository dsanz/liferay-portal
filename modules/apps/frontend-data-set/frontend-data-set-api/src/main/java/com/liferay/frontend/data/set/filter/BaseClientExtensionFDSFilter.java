/**
 * SPDX-FileCopyrightText: (c) 2023 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.frontend.data.set.filter;

import java.util.Map;

/**
 * @author Iván Zaera Avellón
 */
public abstract class BaseClientExtensionFDSFilter implements FDSFilter {

	public abstract String getCETExternalReferenceCode();

	public abstract String getModuleURL();

	public final Map<String, Object> getPreloadedData() {
		return null;
	}

	@Override
	public String getType() {
		return "clientExtension";
	}

}
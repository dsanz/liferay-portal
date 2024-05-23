/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.info.item;

import java.util.Map;

/**
 * @author Daniel Sanz
 */
public class InfoItemRESTEndpointParameterMap {

	public InfoItemRESTEndpointParameterMap(Map<String, Object> parameterMap) {
		_restEndpointParameterMap = parameterMap;
	}

	public Map<String, Object> getRESTEndpointParameterMap() {
		return _restEndpointParameterMap;
	}

	private final Map<String, Object> _restEndpointParameterMap;

}
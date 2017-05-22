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

package com.liferay.dynamic.data.mapping.data.provider;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Leonardo Barros
 */
public class DDMDataProviderResponse {

	public static DDMDataProviderResponse of(
		DDMDataProviderResponseOutput... ddmDataProviderResponseOutputs) {

		return new DDMDataProviderResponse(
			Arrays.asList(ddmDataProviderResponseOutputs));
	}

	public DDMDataProviderResponseOutput get(String name) {
		return _dataMap.get(name);
	}

	public Map<String, DDMDataProviderResponseOutput> getDataMap() {
		return Collections.unmodifiableMap(_dataMap);
	}

	private DDMDataProviderResponse(
		List<DDMDataProviderResponseOutput> ddmDataProviderResponseOutputs) {

		ddmDataProviderResponseOutputs.forEach(
			ddmDataProviderResponseOutput -> _dataMap.put(
				ddmDataProviderResponseOutput.getName(),
				ddmDataProviderResponseOutput));
	}

	private final Map<String, DDMDataProviderResponseOutput> _dataMap =
		new HashMap<>();

}
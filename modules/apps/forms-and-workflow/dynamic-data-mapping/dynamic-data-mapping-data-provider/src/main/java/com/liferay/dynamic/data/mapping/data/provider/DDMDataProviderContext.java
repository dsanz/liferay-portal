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

import com.liferay.dynamic.data.mapping.storage.DDMFormValues;
import com.liferay.dynamic.data.mapping.util.DDMFormInstanceFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Marcellus Tavares
 */
public class DDMDataProviderContext {

	/**
	 * @deprecated As of 2.1.0, with no direct replacement
	 */
	@Deprecated
	public DDMDataProviderContext(DDMFormValues ddmFormValues) {
		this(null, null, ddmFormValues);
	}

	public DDMDataProviderContext(
		String type, String ddmDataProviderInstanceId,
		DDMFormValues ddmFormValues) {

		_type = type;
		_ddmDataProviderInstanceId = ddmDataProviderInstanceId;
		_ddmFormValues = ddmFormValues;
	}

	/**
	 * @deprecated As of 2.1.0, replaced by {@link
	 *             DDMDataProviderRequest#queryString(Map)}
	 */
	@Deprecated
	public void addParameters(Map<String, String> parameters) {
		_parameters.putAll(parameters);
	}

	public String getDDMDataProviderInstanceId() {
		return _ddmDataProviderInstanceId;
	}

	public DDMFormValues getDDMFormValues() {
		return _ddmFormValues;
	}

	/**
	 * @deprecated As of 2.1.0, replaced by {@link
	 *             DDMDataProviderRequest#getParameters()}
	 */
	@Deprecated
	public Map<String, String> getParameters() {
		return _parameters;
	}

	public <T> T getSettingsInstance(Class<T> clazz) {
		return DDMFormInstanceFactory.create(clazz, _ddmFormValues);
	}

	public String getType() {
		return _type;
	}

	private final String _ddmDataProviderInstanceId;
	private final DDMFormValues _ddmFormValues;
	private final Map<String, String> _parameters = new HashMap<>();
	private final String _type;

}
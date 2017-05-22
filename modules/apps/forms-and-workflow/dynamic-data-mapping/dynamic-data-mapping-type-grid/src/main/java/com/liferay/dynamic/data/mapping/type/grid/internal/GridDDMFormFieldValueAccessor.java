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

package com.liferay.dynamic.data.mapping.type.grid.internal;

import com.liferay.dynamic.data.mapping.form.field.type.DDMFormFieldValueAccessor;
import com.liferay.dynamic.data.mapping.model.DDMFormField;
import com.liferay.dynamic.data.mapping.model.DDMFormFieldOptions;
import com.liferay.dynamic.data.mapping.model.Value;
import com.liferay.dynamic.data.mapping.storage.DDMFormFieldValue;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Pedro Queiroz
 */
@Component(
	immediate = true, property = "ddm.form.field.type.name=grid",
	service = {
		DDMFormFieldValueAccessor.class, GridDDMFormFieldValueAccessor.class
	}
)
public class GridDDMFormFieldValueAccessor
	implements DDMFormFieldValueAccessor<JSONObject> {

	@Override
	public JSONObject getValue(
		DDMFormFieldValue ddmFormFieldValue, Locale locale) {

		return getValue(ddmFormFieldValue.getValue(), locale);
	}

	@Override
	public boolean isEmpty(
		DDMFormField ddmFormField, Value value, Locale locale) {

		JSONObject jsonObject = getValue(value, locale);

		Iterator<String> keys = jsonObject.keys();

		Set<String> keyValues = new HashSet<>();

		while (keys.hasNext()) {
			keyValues.add(keys.next());
		}

		DDMFormFieldOptions ddmFormFieldOptions =
			(DDMFormFieldOptions)ddmFormField.getProperty("rows");

		Set<String> optionsValues = ddmFormFieldOptions.getOptionsValues();

		Stream<String> stream = optionsValues.stream();

		return stream.anyMatch(rowValue -> !keyValues.contains(rowValue));
	}

	protected JSONObject createJSONObject(String json) {
		try {
			return jsonFactory.createJSONObject(json);
		}
		catch (JSONException jsone) {
			if (_log.isDebugEnabled()) {
				_log.debug(jsone, jsone);
			}

			return jsonFactory.createJSONObject();
		}
	}

	protected JSONObject getValue(Value value, Locale locale) {
		return createJSONObject(value.getString(locale));
	}

	@Reference
	protected JSONFactory jsonFactory;

	private static final Log _log = LogFactoryUtil.getLog(
		GridDDMFormFieldValueAccessor.class);

}
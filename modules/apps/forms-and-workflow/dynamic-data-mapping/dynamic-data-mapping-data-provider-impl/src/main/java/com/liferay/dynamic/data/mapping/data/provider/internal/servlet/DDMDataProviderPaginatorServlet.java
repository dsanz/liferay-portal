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

package com.liferay.dynamic.data.mapping.data.provider.internal.servlet;

import com.liferay.dynamic.data.mapping.data.provider.DDMDataProvider;
import com.liferay.dynamic.data.mapping.data.provider.DDMDataProviderContext;
import com.liferay.dynamic.data.mapping.data.provider.DDMDataProviderContextContributor;
import com.liferay.dynamic.data.mapping.data.provider.DDMDataProviderOutputParametersSettings;
import com.liferay.dynamic.data.mapping.data.provider.DDMDataProviderParameterSettings;
import com.liferay.dynamic.data.mapping.data.provider.DDMDataProviderRequest;
import com.liferay.dynamic.data.mapping.data.provider.DDMDataProviderResponse;
import com.liferay.dynamic.data.mapping.data.provider.DDMDataProviderTracker;
import com.liferay.dynamic.data.mapping.io.DDMFormValuesJSONDeserializer;
import com.liferay.dynamic.data.mapping.model.DDMDataProviderInstance;
import com.liferay.dynamic.data.mapping.model.DDMForm;
import com.liferay.dynamic.data.mapping.service.DDMDataProviderInstanceService;
import com.liferay.dynamic.data.mapping.storage.DDMFormValues;
import com.liferay.dynamic.data.mapping.util.DDMFormFactory;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONSerializer;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.servlet.ServletResponseUtil;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.CharPool;
import com.liferay.portal.kernel.util.ClassUtil;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Rafael Praxedes
 */
@Component(
	immediate = true,
	property = {
		"osgi.http.whiteboard.context.path=/dynamic-data-mapping-data-provider-paginator",
		"osgi.http.whiteboard.servlet.name=com.liferay.dynamic.data.mapping.data.provider.internal.servlet.DDMDataProviderPaginatorServlet",
		"osgi.http.whiteboard.servlet.pattern=/dynamic-data-mapping-data-provider-paginator/*"
	},
	service = {DDMDataProviderPaginatorServlet.class, Servlet.class}
)
public class DDMDataProviderPaginatorServlet extends HttpServlet {

	protected void addDDMDataProviderContextParameters(
			DDMDataProviderContext ddmDataProviderContext,
			HttpServletRequest request)
		throws Exception {

		addParametersFromRequest(ddmDataProviderContext, request);

		if (ddmDataProviderContext.getType() == null) {
			return;
		}

		addParametersFromContextContributors(ddmDataProviderContext, request);
	}

	protected void addParametersFromContextContributors(
		DDMDataProviderContext ddmDataProviderContext,
		HttpServletRequest request) {

		List<DDMDataProviderContextContributor>
			ddmDataProviderContextContributors =
				_ddmDataProviderTracker.getDDMDataProviderContextContributors(
					ddmDataProviderContext.getType());

		for (DDMDataProviderContextContributor
				ddmDataProviderContextContributor :
					ddmDataProviderContextContributors) {

			Map<String, String> parameters =
				ddmDataProviderContextContributor.getParameters(request);

			if (parameters == null) {
				continue;
			}

			ddmDataProviderContext.addParameters(parameters);
		}
	}

	protected void addParametersFromRequest(
			DDMDataProviderContext ddmDataProviderContext,
			HttpServletRequest request)
		throws Exception {

		JSONObject inputParametersJSONObject = getInputParametersJSONObject(
			request);

		inputParametersJSONObject.keys().forEachRemaining(
			inputParameterName -> {
				ddmDataProviderContext.addParameter(
					inputParameterName,
					inputParametersJSONObject.getString(inputParameterName));
			});
	}

	protected DDMDataProviderContext createDDMDataProviderContext(
			DDMDataProvider ddmDataProvider,
			DDMDataProviderInstance ddmDataProviderInstance)
		throws Exception {

		DDMForm ddmForm = DDMFormFactory.create(ddmDataProvider.getSettings());

		DDMFormValues ddmFormValues =
			_ddmFormValuesJSONDeserializer.deserialize(
				ddmForm, ddmDataProviderInstance.getDefinition());

		DDMDataProviderContext ddmDataProviderContext =
			new DDMDataProviderContext(
				ddmDataProviderInstance.getType(),
				String.valueOf(
					ddmDataProviderInstance.getDataProviderInstanceId()),
				ddmFormValues);

		return ddmDataProviderContext;
	}

	@Override
	protected void doPost(
			HttpServletRequest request, HttpServletResponse response)
		throws IOException, ServletException {

		List<Map<String, String>> dataProviderResult = executeDataProvider(
			request, response);

		if (dataProviderResult == null) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);

			return;
		}

		JSONSerializer jsonSerializer = _jsonFactory.createJSONSerializer();

		response.setContentType(ContentTypes.APPLICATION_JSON);
		response.setStatus(HttpServletResponse.SC_OK);

		ServletResponseUtil.write(
			response, jsonSerializer.serializeDeep(dataProviderResult));
	}

	protected List<Map<String, String>> executeDataProvider(
		HttpServletRequest request, HttpServletResponse response) {

		String dataProviderInstanceUUID = ParamUtil.getString(
			request, "dataProviderInstanceUUID");

		String outputParameterName = ParamUtil.getString(
			request, "outputParameterName");

		int start = ParamUtil.getInteger(request, "start");
		int end = ParamUtil.getInteger(request, "end");

		if (Validator.isNull(dataProviderInstanceUUID) ||
			Validator.isNull(outputParameterName) || (start < 0) ||
			(end < 0) || (end < start) || (start == end)) {

			return null;
		}

		List<Map<String, String>> dataProviderResult = new ArrayList<>();

		try {
			DDMDataProvider ddmDataProvider =
				_ddmDataProviderTracker.getDDMDataProviderByInstanceId(
					dataProviderInstanceUUID);

			DDMDataProviderContext ddmDataProviderContext = null;

			if (ddmDataProvider != null) {
				ddmDataProviderContext = new DDMDataProviderContext(null);
			}
			else {
				DDMDataProviderInstance ddmDataProviderInstance =
					_ddmDataProviderInstanceService.
						getDataProviderInstanceByUuid(dataProviderInstanceUUID);

				ddmDataProvider = _ddmDataProviderTracker.getDDMDataProvider(
					ddmDataProviderInstance.getType());

				ddmDataProviderContext = createDDMDataProviderContext(
					ddmDataProvider, ddmDataProviderInstance);
			}

			addDDMDataProviderContextParameters(
				ddmDataProviderContext, request);

			DDMDataProviderRequest ddmDataProviderRequest =
				new DDMDataProviderRequest(ddmDataProviderContext);

			ddmDataProviderRequest.setPaginationStart(start);
			ddmDataProviderRequest.setPaginationEnd(end);

			DDMDataProviderResponse ddmDataProviderResponse =
				ddmDataProvider.getData(ddmDataProviderRequest);

			String[] keyValue = getKeyValue(
				outputParameterName, ddmDataProvider, ddmDataProviderContext);

			String key = keyValue[0];
			String value = keyValue[1];

			for (Map<Object, Object> map : ddmDataProviderResponse.getData()) {
				Map<String, String> result = new HashMap<>();

				result.put("label", String.valueOf(map.get(value)));
				result.put("value", String.valueOf(map.get(key)));

				dataProviderResult.add(result);
			}
		}
		catch (Exception e) {
			if (_log.isDebugEnabled()) {
				_log.debug(e, e);
			}
		}

		return dataProviderResult;
	}

	protected JSONObject getInputParametersJSONObject(
			HttpServletRequest request)
		throws JSONException {

		String inputParameters = ParamUtil.getString(
			request, "inputParameters");

		if (Validator.isNull(inputParameters)) {
			return _jsonFactory.createJSONObject();
		}

		return _jsonFactory.createJSONObject(inputParameters);
	}

	protected String[] getKeyValue(
		String ddmDataProviderOutput, DDMDataProvider ddmDataProvider,
		DDMDataProviderContext ddmDataProviderContext) {

		String[] keyValue = getKeyValuePathsFromDataProviderOutputParameter(
			ddmDataProviderOutput, ddmDataProvider, ddmDataProviderContext);

		if (keyValue == null) {
			keyValue =
				new String[] {ddmDataProviderOutput, ddmDataProviderOutput};
		}

		return keyValue;
	}

	protected String[] getKeyValuePaths(
		String ddmDataProviderOutput,
		DDMDataProviderParameterSettings ddmDataProviderParemeterSettings) {

		for (DDMDataProviderOutputParametersSettings
				ddmDataProviderOutputParametersSetting :
					ddmDataProviderParemeterSettings.outputParameters()) {

			if (ddmDataProviderOutput.equals(
					ddmDataProviderOutputParametersSetting.
						outputParameterName())) {

				String[] paths = StringUtil.split(
					ddmDataProviderOutputParametersSetting.
						outputParameterPath(),
					CharPool.SEMICOLON);

				if (paths.length == 1) {
					paths = ArrayUtil.append(paths, paths[0]);
				}

				return paths;
			}
		}

		return null;
	}

	protected String[] getKeyValuePathsFromDataProviderOutputParameter(
		String ddmDataProviderOutput, DDMDataProvider ddmDataProvider,
		DDMDataProviderContext ddmDataProviderContext) {

		if ((ddmDataProviderContext.getType() != null) &&
			ClassUtil.isSubclass(
				ddmDataProvider.getSettings(),
				DDMDataProviderParameterSettings.class)) {

			DDMDataProviderParameterSettings ddmDataProviderParemeterSettings =
				(DDMDataProviderParameterSettings)
					ddmDataProviderContext.getSettingsInstance(
						ddmDataProvider.getSettings());

			return getKeyValuePaths(
				ddmDataProviderOutput, ddmDataProviderParemeterSettings);
		}

		return null;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		DDMDataProviderPaginatorServlet.class);

	private static final long serialVersionUID = 1L;

	@Reference
	private DDMDataProviderInstanceService _ddmDataProviderInstanceService;

	@Reference
	private DDMDataProviderTracker _ddmDataProviderTracker;

	@Reference
	private DDMFormValuesJSONDeserializer _ddmFormValuesJSONDeserializer;

	@Reference
	private JSONFactory _jsonFactory;

}
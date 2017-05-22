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

package com.liferay.dynamic.data.mapping.data.provider.internal.rest;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import com.liferay.dynamic.data.mapping.data.provider.DDMDataProvider;
import com.liferay.dynamic.data.mapping.data.provider.DDMDataProviderContext;
import com.liferay.dynamic.data.mapping.data.provider.DDMDataProviderException;
import com.liferay.dynamic.data.mapping.data.provider.DDMDataProviderOutputParametersSettings;
import com.liferay.dynamic.data.mapping.data.provider.DDMDataProviderRequest;
import com.liferay.dynamic.data.mapping.data.provider.DDMDataProviderResponse;
import com.liferay.dynamic.data.mapping.data.provider.DDMDataProviderResponseOutput;
import com.liferay.portal.kernel.cache.MultiVMPool;
import com.liferay.portal.kernel.cache.PortalCache;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.CharPool;
import com.liferay.portal.kernel.util.KeyValuePair;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import jodd.http.HttpRequest;
import jodd.http.HttpResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Marcellus Tavares
 */
@Component(immediate = true, property = "ddm.data.provider.type=rest")
public class DDMRESTDataProvider implements DDMDataProvider {

	@Override
	public List<KeyValuePair> getData(
			DDMDataProviderContext ddmDataProviderContext)
		throws DDMDataProviderException {

		try {
			DDMDataProviderRequest ddmDataProviderRequest =
				new DDMDataProviderRequest(ddmDataProviderContext, null);

			DDMDataProviderResponse ddmDataProviderResponse = doGetData(
				ddmDataProviderRequest);

			DDMDataProviderResponseOutput ddmDataProviderResponseOutput =
				ddmDataProviderResponse.get("Default-Output");

			List<KeyValuePair> results = ddmDataProviderResponseOutput.getValue(
				List.class);

			return results;
		}
		catch (PortalException pe) {
			throw new DDMDataProviderException(pe);
		}
	}

	@Override
	public DDMDataProviderResponse getData(
			DDMDataProviderRequest ddmDataProviderRequest)
		throws DDMDataProviderException {

		try {
			return doGetData(ddmDataProviderRequest);
		}
		catch (PortalException pe) {
			throw new DDMDataProviderException(pe);
		}
	}

	@Override
	public Class<?> getSettings() {
		return DDMRESTDataProviderSettings.class;
	}

	protected DDMDataProviderResponse createDDMDataProviderResponse(
		DocumentContext documentContext,
		DDMDataProviderRequest ddmDataProviderRequest,
		DDMRESTDataProviderSettings ddmRESTDataProviderSettings) {

		DDMDataProviderOutputParametersSettings[] outputParameterSettingsArray =
			ddmRESTDataProviderSettings.outputParameters();

		if ((outputParameterSettingsArray == null) ||
			(outputParameterSettingsArray.length == 0)) {

			return DDMDataProviderResponse.of();
		}

		List<DDMDataProviderResponseOutput> ddmDataProviderResponseOutputs =
			new ArrayList<>();

		for (DDMDataProviderOutputParametersSettings outputParameterSettings :
				outputParameterSettingsArray) {

			String name = outputParameterSettings.outputParameterName();
			String type = outputParameterSettings.outputParameterType();
			String path = outputParameterSettings.outputParameterPath();

			if (Objects.equals(type, "[\"text\"]")) {
				String nomalizedPath = normalizePath(path);

				ddmDataProviderResponseOutputs.add(
					DDMDataProviderResponseOutput.of(
						name, "text", documentContext.read(nomalizedPath)));
			}
			else if (Objects.equals(type, "[\"number\"]")) {
				String nomalizedPath = normalizePath(path);

				ddmDataProviderResponseOutputs.add(
					DDMDataProviderResponseOutput.of(
						name, "number", documentContext.read(nomalizedPath)));
			}
			else if (Objects.equals(type, "[\"list\"]")) {
				String[] paths = StringUtil.split(path, CharPool.SEMICOLON);

				String normalizedValuePath = normalizePath(paths[0]);

				String normalizedKeyPath = normalizedValuePath;

				List<String> values = documentContext.read(normalizedValuePath);

				List<String> keys = new ArrayList<>(values);

				if (paths.length >= 2) {
					normalizedKeyPath = normalizePath(paths[1]);

					keys = documentContext.read(normalizedKeyPath);
				}

				List<KeyValuePair> keyValuePairs = new ArrayList<>();

				for (int i = 0; i < values.size(); i++) {
					keyValuePairs.add(
						new KeyValuePair(keys.get(i), values.get(i)));
				}

				if (ddmRESTDataProviderSettings.pagination()) {
					int start = Integer.valueOf(
						ddmDataProviderRequest.getParameter("paginationStart"));

					int end = Integer.valueOf(
						ddmDataProviderRequest.getParameter("paginationEnd"));

					if (keyValuePairs.size() > (end - start)) {
						keyValuePairs = ListUtil.subList(
							keyValuePairs, start, end);
					}
				}

				ddmDataProviderResponseOutputs.add(
					DDMDataProviderResponseOutput.of(
						name, "list", keyValuePairs));
			}
		}

		int size = ddmDataProviderResponseOutputs.size();

		return DDMDataProviderResponse.of(
			ddmDataProviderResponseOutputs.toArray(
				new DDMDataProviderResponseOutput[size]));
	}

	protected DDMDataProviderResponse doGetData(
			DDMDataProviderRequest ddmDataProviderRequest)
		throws JSONException {

		DDMDataProviderContext ddmDataProviderContext =
			ddmDataProviderRequest.getDDMDataProviderContext();

		DDMRESTDataProviderSettings ddmRESTDataProviderSettings =
			ddmDataProviderContext.getSettingsInstance(
				DDMRESTDataProviderSettings.class);

		HttpRequest httpRequest = HttpRequest.get(
			ddmRESTDataProviderSettings.url());

		if (Validator.isNotNull(ddmRESTDataProviderSettings.username())) {
			httpRequest.basicAuthentication(
				ddmRESTDataProviderSettings.username(),
				ddmRESTDataProviderSettings.password());
		}

		setRequestParameters(
			ddmDataProviderRequest, ddmRESTDataProviderSettings, httpRequest);

		String cacheKey = getCacheKey(httpRequest);

		DDMRESTDataProviderResult ddmRESTDataProviderResult = _portalCache.get(
			cacheKey);

		if ((ddmRESTDataProviderResult != null) &&
			ddmRESTDataProviderSettings.cacheable()) {

			return ddmRESTDataProviderResult.getDDMDataProviderResponse();
		}

		HttpResponse httpResponse = httpRequest.send();

		DocumentContext documentContext = JsonPath.parse(httpResponse.body());

		DDMDataProviderResponse ddmDataProviderResponse =
			createDDMDataProviderResponse(
				documentContext, ddmDataProviderRequest,
				ddmRESTDataProviderSettings);

		if (ddmRESTDataProviderSettings.cacheable()) {
			_portalCache.put(
				cacheKey,
				new DDMRESTDataProviderResult(ddmDataProviderResponse));
		}

		return ddmDataProviderResponse;
	}

	protected String getCacheKey(HttpRequest httpRequest) {
		return httpRequest.url();
	}

	protected Set<String> getOutputParameterPaths(
		DDMDataProviderContext ddmDataProviderContext) {

		DDMRESTDataProviderSettings ddmRESTDataProviderSettings =
			ddmDataProviderContext.getSettingsInstance(
				DDMRESTDataProviderSettings.class);

		Set<String> outputParameterPaths = new HashSet<>();

		for (DDMDataProviderOutputParametersSettings outputParameterSettings :
				ddmRESTDataProviderSettings.outputParameters()) {

			String[] paths = StringUtil.split(
				outputParameterSettings.outputParameterPath(),
				CharPool.SEMICOLON);

			for (String path : paths) {
				outputParameterPaths.add(path);
			}
		}

		return outputParameterPaths;
	}

	protected String normalizePath(String path) {
		if (StringUtil.startsWith(path, StringPool.PERIOD) ||
			StringUtil.startsWith(path, StringPool.DOLLAR)) {

			return path;
		}

		return StringPool.PERIOD.concat(path);
	}

	@Reference(unbind = "-")
	protected void setJSONFactory(JSONFactory jsonFactory) {
		_jsonFactory = jsonFactory;
	}

	@Reference(unbind = "-")
	protected void setMultiVMPool(MultiVMPool multiVMPool) {
		_portalCache =
			(PortalCache<String, DDMRESTDataProviderResult>)
				multiVMPool.getPortalCache(DDMRESTDataProvider.class.getName());
	}

	protected void setRequestParameters(
		DDMDataProviderRequest ddmDataProviderRequest,
		DDMRESTDataProviderSettings ddmRESTDataProviderSettings,
		HttpRequest httpRequest) {

		if (ddmRESTDataProviderSettings.filterable()) {
			httpRequest.query(
				ddmRESTDataProviderSettings.filterParameterName(),
				ddmDataProviderRequest.getParameter("filterParameterValue"));
		}

		if (ddmRESTDataProviderSettings.pagination()) {
			httpRequest.query(
				ddmRESTDataProviderSettings.paginationEndParameterName(),
				ddmDataProviderRequest.getParameter("paginationStart"));
			httpRequest.query(
				ddmRESTDataProviderSettings.paginationEndParameterName(),
				ddmDataProviderRequest.getParameter("paginationEnd"));
		}

		httpRequest.query(ddmDataProviderRequest.getParameters());
	}

	private static final Log _log = LogFactoryUtil.getLog(
		DDMRESTDataProvider.class);

	private JSONFactory _jsonFactory;
	private PortalCache<String, DDMRESTDataProviderResult> _portalCache;

	private static class DDMRESTDataProviderResult implements Serializable {

		public DDMRESTDataProviderResult(
			DDMDataProviderResponse ddmDataProviderResponse) {

			_ddmDataProviderResponse = ddmDataProviderResponse;
		}

		public DDMDataProviderResponse getDDMDataProviderResponse() {
			return _ddmDataProviderResponse;
		}

		private final DDMDataProviderResponse _ddmDataProviderResponse;

	}

}
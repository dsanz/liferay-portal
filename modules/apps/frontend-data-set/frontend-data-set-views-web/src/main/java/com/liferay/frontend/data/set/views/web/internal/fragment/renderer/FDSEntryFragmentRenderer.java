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

package com.liferay.frontend.data.set.views.web.internal.fragment.renderer;

import com.liferay.fragment.model.FragmentEntryLink;
import com.liferay.fragment.renderer.FragmentRenderer;
import com.liferay.fragment.renderer.FragmentRendererContext;
import com.liferay.fragment.util.configuration.FragmentEntryConfigurationParser;
import com.liferay.frontend.data.set.views.web.internal.js.loader.modules.extender.npm.NPMResolverProvider;
import com.liferay.frontend.js.loader.modules.extender.npm.NPMResolver;
import com.liferay.object.model.ObjectDefinition;
import com.liferay.object.service.ObjectDefinitionLocalService;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.template.react.renderer.ComponentDescriptor;
import com.liferay.portal.template.react.renderer.ReactRenderer;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Daniel Sanz
 */
@Component(service = FragmentRenderer.class)
public class FDSEntryFragmentRenderer implements FragmentRenderer {

	public String getApiUrl(JSONObject configurationJSONObject) {
		return "/o/headless-commerce-admin-catalog/v1.0/products" +
			"?nestedFields=skus,catalog";
	}

	@Override
	public String getCollectionKey() {
		return "content-display";
	}

	@Override
	public String getConfiguration(
		FragmentRendererContext fragmentRendererContext) {

		FragmentEntryLink fragmentEntryLink =
			fragmentRendererContext.getFragmentEntryLink();

		ObjectDefinition fdsEntryObjectDefinition =
			_objectDefinitionLocalService.fetchObjectDefinition(
				fragmentEntryLink.getCompanyId(), "C_FDSEntry");

		String className = "";

		if (fdsEntryObjectDefinition != null) {
			className = fdsEntryObjectDefinition.getClassName();
		}

		return JSONUtil.put(
			"fieldSets",
			JSONUtil.putAll(
				JSONUtil.put(
					"fields",
					JSONUtil.putAll(
						JSONUtil.put(
							"label", "Dataset"
						).put(
							"name", "itemSelector"
						).put(
							"type", "itemSelector"
						).put(
							"typeOptions", JSONUtil.put("itemType", className)
						))))
		).toString();
	}

	public JSONArray getFiltersJSONArray(JSONObject configurationJSONObject) {
		return JSONUtil.putAll(
			JSONUtil.put(
				"autocompleteEnabled", false
			).put(
				"id", "productType"
			).put(
				"items",
				JSONUtil.putAll(
					JSONUtil.put(
						"label", "Simple"
					).put(
						"value", "simple"
					),
					JSONUtil.put(
						"label", "Grouped"
					).put(
						"value", "grouped"
					),
					JSONUtil.put(
						"label", "Virtual"
					).put(
						"value", "virtual"
					))
			).put(
				"label", "Product Type"
			).put(
				"multiple", false
			).put(
				"type", "selection"
			));
	}

	@Override
	public String getIcon() {
		return "web-content";  // TODO: find the right icon
	}

	public String getLabel(Locale locale) {
		return "Frontend Dataset";     // TODO: add language keys
	}

	public JSONObject getPaginationJSONObject(
		JSONObject configurationJSONObject) {

		return JSONUtil.put(
			"deltas",
			JSONUtil.putAll(
				JSONUtil.put("label", 4), JSONUtil.put("label", 10),
				JSONUtil.put("label", 20))
		).put(
			"initialDelta", 10
		).put(
			"initialPageNumber", 0
		);
	}

	public JSONArray getViewsJSONArray(JSONObject configurationJSONObject) {
		return JSONUtil.putAll(
			JSONUtil.put(
				"contentRenderer", "table"
			).put(
				"default", false
			).put(
				"label", "Table"
			).put(
				"name", "table"
			).put(
				"quickActionsEnabled", false
			).put(
				"schema",
				JSONUtil.put(
					"fields",
					JSONUtil.putAll(
						JSONUtil.put(
							"contentRenderer", "image"
						).put(
							"expand", false
						).put(
							"fieldName", "thumbnail"
						).put(
							"label", ""
						).put(
							"localizeLabel", true
						).put(
							"sortable", false
						),
						JSONUtil.put(
							"contentRenderer", "actionLink"
						).put(
							"expand", false
						).put(
							"fieldName", JSONUtil.putAll("name", "LANG")
						).put(
							"label", "Name"
						).put(
							"localizeLabel", true
						).put(
							"sortable", true
						),
						JSONUtil.put(
							"contentRenderer", "actionLink"
						).put(
							"expand", false
						).put(
							"fieldName", JSONUtil.putAll("catalog", "name")
						).put(
							"label", "Catalog"
						).put(
							"localizeLabel", true
						).put(
							"sortable", false
						),
						JSONUtil.put(
							"expand", false
						).put(
							"fieldName", "productTypeI18n"
						).put(
							"label", "Type"
						).put(
							"localizeLabel", true
						).put(
							"sortable", false
						),
						JSONUtil.put(
							"contentRenderer", "status"
						).put(
							"expand", false
						).put(
							"fieldName", "workflowStatusInfo"
						).put(
							"label", "Status"
						).put(
							"localizeLabel", true
						).put(
							"sortable", false
						),
						JSONUtil.put(
							"contentRenderer", "dateTime"
						).put(
							"expand", false
						).put(
							"fieldName", "modifiedDate"
						).put(
							"label", "Modified Date"
						).put(
							"localizeLabel", true
						).put(
							"sortable", true
						)))
			).put(
				"thumbnail", "table"
			));
	}

	@Override
	public void render(
			FragmentRendererContext fragmentRendererContext,
			HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse)
		throws IOException {

		try {
			PrintWriter printWriter = httpServletResponse.getWriter();

			FragmentEntryLink fragmentEntryLink =
				fragmentRendererContext.getFragmentEntryLink();

			JSONObject configurationJSONObject =
				_jsonFactory.createJSONObject();

			if (Validator.isNotNull(fragmentEntryLink.getConfiguration())) {
				configurationJSONObject =
					_fragmentEntryConfigurationParser.
						getConfigurationJSONObject(
							fragmentEntryLink.getConfiguration(),
							fragmentEntryLink.getEditableValues(),
							LocaleUtil.getMostRelevantLocale());
			}

			printWriter.write(
				_renderFragmentEntry(
					fragmentRendererContext, configurationJSONObject,
					httpServletRequest));
		}
		catch (PortalException portalException) {
			throw new IOException(portalException);
		}
	}

	private Map<String, Object> _prepareData(
		String fragmentElementId, JSONObject configurationJSONObject) {

		return HashMapBuilder.<String, Object>put(
			"apiURL", getApiUrl(configurationJSONObject)
		).put(
			"filters", getFiltersJSONArray(configurationJSONObject)
		).put(
			"namespace", fragmentElementId
		).put(
			"pagination", getPaginationJSONObject(configurationJSONObject)
		).put(
			"selectedItems", ""
		).put(
			"uniformActionsDisplay", false
		).put(
			"views", getViewsJSONArray(configurationJSONObject)
		).build();
	}

	private String _renderFragmentEntry(
			FragmentRendererContext fragmentRendererContext,
			JSONObject configurationJSONObject,
			HttpServletRequest httpServletRequest)
		throws IOException {

		NPMResolver npmResolver = NPMResolverProvider.getNPMResolver();

		String moduleName = npmResolver.resolveModuleName(
			"@liferay/frontend-data-set-web/FrontendDataSet");

		StringBundler sb = new StringBundler(5);

		sb.append("<div id=\"");
		sb.append(fragmentRendererContext.getFragmentElementId());
		sb.append("\" >");

		Writer writer = new CharArrayWriter();

		ComponentDescriptor componentDescriptor = new ComponentDescriptor(
			moduleName, fragmentRendererContext.getFragmentElementId());

		_reactRenderer.renderReact(
			componentDescriptor,
			_prepareData(
				fragmentRendererContext.getFragmentElementId(),
				configurationJSONObject),
			httpServletRequest, writer);

		sb.append(writer.toString());

		sb.append("</div>");

		return sb.toString();
	}

	@Reference
	private FragmentEntryConfigurationParser _fragmentEntryConfigurationParser;

	@Reference
	private JSONFactory _jsonFactory;

	@Reference
	private ObjectDefinitionLocalService _objectDefinitionLocalService;

	@Reference
	private ReactRenderer _reactRenderer;

}
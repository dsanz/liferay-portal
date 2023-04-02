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
import com.liferay.frontend.data.set.views.web.internal.dataset.provider.APIUrlDatasetProvider;
import com.liferay.frontend.data.set.views.web.internal.js.loader.modules.extender.npm.NPMResolverProvider;
import com.liferay.frontend.js.loader.modules.extender.npm.NPMResolver;
import com.liferay.object.model.ObjectDefinition;

import com.liferay.object.rest.dto.v1_0.ObjectEntry;
import com.liferay.object.rest.manager.v1_0.ObjectEntryManager;
import com.liferay.object.service.ObjectDefinitionLocalService;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.language.Language;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.template.react.renderer.ComponentDescriptor;
import com.liferay.portal.template.react.renderer.ReactRenderer;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.vulcan.dto.converter.DTOConverterContext;
import com.liferay.portal.vulcan.dto.converter.DefaultDTOConverterContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Daniel Sanz
 */
@Component(service = FragmentRenderer.class)
public class FDSEntryFragmentRenderer implements FragmentRenderer {

	@Override
	public String getCollectionKey() {
		return "content-display";
	}

	@Override
	public String getConfiguration(
		FragmentRendererContext fragmentRendererContext) {

		String className = _getFDSViewClassName(fragmentRendererContext);

		return JSONUtil.put(
			"fieldSets",
			JSONUtil.putAll(
				JSONUtil.put(
					"fields",
					JSONUtil.putAll(
						JSONUtil.put(
							"label", "Dataset View"
						).put(
							"name", "itemSelector"
						).put(
							"type", "itemSelector"
						).put(
							"typeOptions", JSONUtil.put("itemType", className)
						))))
		).toString();
	}

	public JSONArray getFiltersJSONArray(ObjectEntry fdsView) {
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
		ObjectEntry fdsView) {

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

	public JSONArray getViewsJSONArray(ObjectEntry fdsView) {
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

			ObjectEntry fdsView = _getFDSView(httpServletRequest,
				fragmentRendererContext);

			if ((fdsView == null) && fragmentRendererContext.isEditMode()) {
				printWriter.write(
					StringBundler.concat(
						"<div class=\"portlet-msg-info\">",
							_language.get(httpServletRequest,
								"select-a-dataset-view"), "</div>"));
			}

			if (fdsView == null) {
				return;
			}

			printWriter.write(
				_renderFragmentEntry(
					fragmentRendererContext, fdsView,
					httpServletRequest));
		}
		catch (Exception e) {
			throw new IOException(e);
		}
	}

	private ObjectEntry _getFDSView(HttpServletRequest httpServletRequest,
		FragmentRendererContext fragmentRendererContext) {

		try {
			FragmentEntryLink fragmentEntryLink =
				fragmentRendererContext.getFragmentEntryLink();

			JSONObject jsonObject =
				(JSONObject)_fragmentEntryConfigurationParser.getFieldValue(
					getConfiguration(fragmentRendererContext),
					fragmentEntryLink.getEditableValues(),
					fragmentRendererContext.getLocale(), "itemSelector");

			if ((jsonObject != null) && jsonObject.has("className") &&
				jsonObject.has("externalReferenceCode") &&
				Objects.equals(jsonObject.get("className"),
					_getFDSViewClassName(fragmentRendererContext))) {

				String externalReferenceCode =
					jsonObject.getString("externalReferenceCode");

				ObjectDefinition fdsViewObjectDefinition =
					_getFDSViewObjectDefinition(fragmentRendererContext);

				DTOConverterContext dtoConverterContext =
					new DefaultDTOConverterContext(false, null, null, null,
						null, LocaleUtil.getSiteDefault(), null, null);

				return  _objectEntryManager.getObjectEntry(
					dtoConverterContext, externalReferenceCode,
					fdsViewObjectDefinition.getCompanyId(),
					fdsViewObjectDefinition, null);
			}

			return null;
		}
		catch (Exception e) {
			_log.error(e);
			return null;
		}
	}

	private ObjectDefinition _getFDSViewObjectDefinition(FragmentRendererContext
			fragmentRendererContext) {

		long companyId = CompanyThreadLocal.getCompanyId();

		FragmentEntryLink fragmentEntryLink =
			fragmentRendererContext.getFragmentEntryLink();

		if (fragmentEntryLink != null) {
			companyId = fragmentEntryLink.getCompanyId();
		}

		return _objectDefinitionLocalService.fetchObjectDefinition(
			companyId, "C_FDSView");
	}


	private String _getFDSViewClassName(FragmentRendererContext
			fragmentRendererContext) {

		ObjectDefinition fdsViewObjectDefinition =
			_getFDSViewObjectDefinition(fragmentRendererContext);

		String className = "";

		if (fdsViewObjectDefinition != null) {
			className = fdsViewObjectDefinition.getClassName();
		}

		return className;
	}

	private Map<String, Object> _prepareData(
		String fragmentElementId, ObjectEntry fdsView) {

		return HashMapBuilder.<String, Object>put(
			"apiURL", _apiUrlDatasetProvider.getApiUrl(fdsView)
		).put(
			"filters", getFiltersJSONArray(fdsView)
		).put(
			"namespace", fragmentElementId
		).put(
			"pagination", getPaginationJSONObject(fdsView)
		).put(
			"selectedItems", ""
		).put(
			"uniformActionsDisplay", false
		).put(
			"views", getViewsJSONArray(fdsView)
		).build();
	}

	private String _renderFragmentEntry(
			FragmentRendererContext fragmentRendererContext,
			ObjectEntry fdsView,
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
				fdsView),
			httpServletRequest, writer);

		sb.append(writer.toString());

		sb.append("</div>");

		return sb.toString();
	}

	private static final Log _log = LogFactoryUtil.getLog(
		FDSEntryFragmentRenderer.class);

	@Reference
	private APIUrlDatasetProvider _apiUrlDatasetProvider;

	@Reference
	private FragmentEntryConfigurationParser _fragmentEntryConfigurationParser;

	@Reference
	private JSONFactory _jsonFactory;

	@Reference
	private Language _language;

	@Reference
	private ObjectDefinitionLocalService _objectDefinitionLocalService;

	@Reference(target = "(object.entry.manager.storage.type=default)")
	private ObjectEntryManager _objectEntryManager;

	@Reference
	private ReactRenderer _reactRenderer;

}
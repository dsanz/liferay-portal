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
import com.liferay.frontend.data.set.views.web.internal.dataset.provider.FDSEntryProviderUtil;
import com.liferay.frontend.data.set.views.web.internal.dataset.provider.api.APIUrlProvider;
import com.liferay.frontend.data.set.views.web.internal.dataset.provider.api.FilterProvider;
import com.liferay.frontend.data.set.views.web.internal.dataset.provider.api.PaginationProvider;
import com.liferay.frontend.data.set.views.web.internal.dataset.provider.api.ViewProvider;
import com.liferay.object.model.ObjectDefinition;
import com.liferay.object.rest.dto.v1_0.ObjectEntry;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.language.Language;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
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

	@Override
	public String getIcon() {
		return "web-content";  // TODO: find the right icon
	}

	public String getLabel(Locale locale) {
		return "Frontend Dataset";     // TODO: add language keys
	}

	@Override
	public void render(
			FragmentRendererContext fragmentRendererContext,
			HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse)
		throws IOException {

		try {
			PrintWriter printWriter = httpServletResponse.getWriter();

			ObjectEntry fdsView = _getFDSView(fragmentRendererContext);

			if ((fdsView == null) && fragmentRendererContext.isEditMode()) {
				printWriter.write(
					StringBundler.concat(
						"<div class=\"portlet-msg-info\">",
						_language.get(
							httpServletRequest, "select-a-dataset-view"),
						"</div>"));
			}

			if (fdsView == null) {
				return;
			}

			printWriter.write(
				_renderFragmentEntry(
					fragmentRendererContext, fdsView, httpServletRequest));
		}
		catch (Exception exception) {
			throw new IOException(exception);
		}
	}

	private ObjectEntry _getFDSView(
		FragmentRendererContext fragmentRendererContext) {

		FragmentEntryLink fragmentEntryLink =
			fragmentRendererContext.getFragmentEntryLink();

		JSONObject jsonObject =
			(JSONObject)_fragmentEntryConfigurationParser.getFieldValue(
				getConfiguration(fragmentRendererContext),
				fragmentEntryLink.getEditableValues(),
				fragmentRendererContext.getLocale(), "itemSelector");

		if ((jsonObject != null) && jsonObject.has("className") &&
			jsonObject.has("externalReferenceCode") &&
			Objects.equals(
				jsonObject.get("className"),
				_getFDSViewClassName(fragmentRendererContext))) {

			String externalReferenceCode = jsonObject.getString(
				"externalReferenceCode");

			return _fdsEntryProviderUtil.getFDSView(
				externalReferenceCode, fragmentRendererContext);
		}

		return null;
	}

	private String _getFDSViewClassName(
		FragmentRendererContext fragmentRendererContext) {

		ObjectDefinition fdsViewObjectDefinition =
			_fdsEntryProviderUtil.getFDSViewObjectDefinition(
				fragmentRendererContext);

		String className = "";

		if (fdsViewObjectDefinition != null) {
			className = fdsViewObjectDefinition.getClassName();
		}

		return className;
	}

	private Map<String, Object> _prepareData(
		String fragmentElementId, ObjectEntry fdsView,
		HttpServletRequest httpServletRequest) {

		return HashMapBuilder.<String, Object>put(
			"apiURL", _apiUrlProvider.getApiUrl(fdsView, httpServletRequest)
		).put(
			"filters", _filterProvider.getFiltersJSONArray(fdsView)
		).put(
			"id", "FDS_" + fragmentElementId
		).put(
			"namespace", fragmentElementId
		).put(
			"pagination", _paginationProvider.getPaginationJSONObject(fdsView)
		).put(
			"selectedItems", ""
		).put(
			"uniformActionsDisplay", false
		).put(
			"views", _viewProvider.getViewsJSONArray(fdsView)
		).build();
	}

	private String _renderFragmentEntry(
			FragmentRendererContext fragmentRendererContext,
			ObjectEntry fdsView, HttpServletRequest httpServletRequest)
		throws IOException {

		StringBundler sb = new StringBundler(8);

		Map<String, Object> fdsViewProperties = fdsView.getProperties();

		sb.append("<span>");
		sb.append(fdsViewProperties.get("label"));
		sb.append("</span>");
		sb.append("<div id=\"");
		sb.append(fragmentRendererContext.getFragmentElementId());
		sb.append("\" >");

		ComponentDescriptor componentDescriptor = new ComponentDescriptor(
			"{FrontendDataSet} from frontend-data-set-web",
			fragmentRendererContext.getFragmentElementId(), null, true);

		Writer writer = new CharArrayWriter();

		_reactRenderer.renderReact(
			componentDescriptor, // TODO: prepare the ID for the FDS component
			_prepareData(
				fragmentRendererContext.getFragmentElementId(), fdsView, httpServletRequest),
			httpServletRequest, writer);

		sb.append(writer.toString());

		sb.append("</div>");

		return sb.toString();
	}

	private static final Log _log = LogFactoryUtil.getLog(
		FDSEntryFragmentRenderer.class);

	@Reference
	private APIUrlProvider _apiUrlProvider;

	@Reference
	private FDSEntryProviderUtil _fdsEntryProviderUtil;

	@Reference
	private FilterProvider _filterProvider;

	@Reference
	private FragmentEntryConfigurationParser _fragmentEntryConfigurationParser;

	@Reference
	private JSONFactory _jsonFactory;

	@Reference
	private Language _language;

	@Reference
	private PaginationProvider _paginationProvider;

	@Reference
	private ReactRenderer _reactRenderer;

	@Reference
	private ViewProvider _viewProvider;

}
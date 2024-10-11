/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.frontend.data.set.admin.web.internal.fragment.renderer;

import com.liferay.client.extension.type.FDSCellRendererCET;
import com.liferay.client.extension.type.FDSFilterCET;
import com.liferay.client.extension.type.manager.CETManager;
import com.liferay.fragment.model.FragmentEntryLink;
import com.liferay.fragment.renderer.FragmentRenderer;
import com.liferay.fragment.renderer.FragmentRendererContext;
import com.liferay.fragment.util.configuration.FragmentEntryConfigurationParser;
import com.liferay.frontend.data.set.DataSetEntityImportPolicy;
import com.liferay.frontend.data.set.action.FDSItemActionList;
import com.liferay.frontend.data.set.action.FDSItemActionListRegistry;
import com.liferay.frontend.data.set.constants.FDSEntityFieldTypes;
import com.liferay.frontend.data.set.model.FDSActionDropdownItem;
import com.liferay.frontend.data.set.renderer.DataSetRenderer;
import com.liferay.frontend.data.set.resolver.FDSAPIURLResolver;
import com.liferay.frontend.data.set.resolver.FDSAPIURLResolverRegistry;
import com.liferay.list.type.model.ListTypeDefinition;
import com.liferay.list.type.model.ListTypeEntry;
import com.liferay.list.type.service.ListTypeDefinitionLocalService;
import com.liferay.list.type.service.ListTypeEntryLocalService;
import com.liferay.object.entry.util.ObjectEntryThreadLocal;
import com.liferay.object.model.ObjectDefinition;
import com.liferay.object.rest.dto.v1_0.ObjectEntry;
import com.liferay.object.rest.manager.v1_0.DefaultObjectEntryManager;
import com.liferay.object.rest.manager.v1_0.DefaultObjectEntryManagerProvider;
import com.liferay.object.rest.manager.v1_0.ObjectEntryManagerRegistry;
import com.liferay.object.service.ObjectDefinitionLocalService;
import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.feature.flag.FeatureFlagManagerUtil;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.language.Language;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.MapUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.template.react.renderer.ComponentDescriptor;
import com.liferay.portal.template.react.renderer.ReactRenderer;
import com.liferay.portal.vulcan.dto.converter.DTOConverterContext;
import com.liferay.portal.vulcan.dto.converter.DefaultDTOConverterContext;
import com.liferay.portal.vulcan.pagination.Page;
import com.liferay.portal.vulcan.pagination.Pagination;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import java.time.Instant;

import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Daniel Sanz
 * @author Marko Cikos
 */
@Component(service = FragmentRenderer.class)
public class FDSAdminFragmentRenderer implements FragmentRenderer {

	@Override
	public String getCollectionKey() {
		return "content-display";
	}

	@Override
	public String getConfiguration(
		FragmentRendererContext fragmentRendererContext) {

		return JSONUtil.put(
			"fieldSets",
			JSONUtil.putAll(
				JSONUtil.put(
					"fields",
					JSONUtil.putAll(
						JSONUtil.put(
							"label", "data-set-view"
						).put(
							"name", "itemSelector"
						).put(
							"type", "itemSelector"
						).put(
							"typeOptions", JSONUtil.put("itemType", "FDSView")
						))))
		).toString();
	}

	@Override
	public String getIcon() {
		return "table";
	}

	@Override
	public String getLabel(Locale locale) {
		return _language.get(locale, "data-set");
	}

	@Override
	public boolean isSelectable(HttpServletRequest httpServletRequest) {
		if (!FeatureFlagManagerUtil.isEnabled("LPS-164563")) {
			return false;
		}

		return true;
	}

	@Override
	public void render(
			FragmentRendererContext fragmentRendererContext,
			HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse)
		throws IOException {

		try {
			ObjectEntryThreadLocal.setSkipObjectEntryResourcePermission(true);

			PrintWriter printWriter = httpServletResponse.getWriter();

			FragmentEntryLink fragmentEntryLink =
				fragmentRendererContext.getFragmentEntryLink();

			JSONObject jsonObject =
				(JSONObject)_fragmentEntryConfigurationParser.getFieldValue(
					getConfiguration(fragmentRendererContext),
					fragmentEntryLink.getEditableValues(),
					fragmentRendererContext.getLocale(), "itemSelector");

			String externalReferenceCode = jsonObject.getString(
				"externalReferenceCode");

			ObjectEntry dataSetObjectEntry = null;

			ObjectDefinition dataSetObjectDefinition =
				_dataSetObjectDefinitionLocalService.fetchObjectDefinition(
					fragmentEntryLink.getCompanyId(), "DataSet");

			if (Validator.isNotNull(externalReferenceCode)) {
				try {
					dataSetObjectEntry = _getObjectEntry(
						fragmentEntryLink.getCompanyId(), externalReferenceCode,
						dataSetObjectDefinition);
				}
				catch (Exception exception) {
					if (_log.isWarnEnabled()) {
						_log.warn(
							"Unable to get frontend data set view with " +
								"external reference code " +
									externalReferenceCode,
							exception);
					}
				}
			}

			if ((dataSetObjectEntry == null) &&
				fragmentRendererContext.isEditMode()) {

				printWriter.write("<div class=\"portlet-msg-info\">");
				printWriter.write("<ul class=\"navbar-nav\">");
				printWriter.write("<li class=\"nav-item\">");
				printWriter.write(
					_language.get(
						httpServletRequest, "select-a-data-set-view"));
				printWriter.write("</li><li class=\"nav-item\"><div id=\"");

				String betaBadgeComponentId =
					fragmentRendererContext.getFragmentElementId() + "Beta";

				printWriter.write(betaBadgeComponentId);

				printWriter.write("\">");

				Writer writer = new CharArrayWriter();

				ComponentDescriptor componentDescriptor =
					new ComponentDescriptor(
						"{FeatureIndicator} from frontend-js-components-web",
						betaBadgeComponentId, null, true);

				_reactRenderer.renderReact(
					componentDescriptor, new HashMap<>(), httpServletRequest,
					writer);

				printWriter.write(writer.toString());

				printWriter.write("</div></li></ul></div>");
			}

			if (dataSetObjectEntry == null) {
				return;
			}

			printWriter.write(
				_buildFragmentHTML(
					dataSetObjectDefinition, dataSetObjectEntry,
					fragmentRendererContext, httpServletRequest,
					httpServletResponse));
		}
		catch (Exception exception) {
			_log.error("Unable to render frontend data set view", exception);

			throw new IOException(exception);
		}
		finally {
			ObjectEntryThreadLocal.setSkipObjectEntryResourcePermission(false);
		}
	}

	private String _buildFragmentHTML(
			ObjectDefinition dataSetObjectDefinition,
			ObjectEntry dataSetObjectEntry,
			FragmentRendererContext fragmentRendererContext,
			HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse)
		throws Exception {

		StringBundler sb = new StringBundler(5);

		sb.append("<div id=\"");
		sb.append(fragmentRendererContext.getFragmentElementId());
		sb.append("\" >");

		Writer writer = new CharArrayWriter();

		FragmentEntryLink fragmentEntryLink =
			fragmentRendererContext.getFragmentEntryLink();

		_dataSetRenderer.render(true,
			fragmentRendererContext.getFragmentElementId(), null,
			dataSetObjectEntry.getExternalReferenceCode(), httpServletRequest,
			httpServletResponse, Collections.emptyMap(), writer);

		sb.append(writer.toString());

		sb.append("</div>");

		return sb.toString();
	}

	private ObjectEntry _getObjectEntry(
			long companyId, String externalReferenceCode,
			ObjectDefinition dataSetObjectDefinition)
		throws Exception {

		DTOConverterContext dtoConverterContext =
			new DefaultDTOConverterContext(
				false, null, null, null, null,
				LocaleUtil.getMostRelevantLocale(), null, null);

		DefaultObjectEntryManager defaultObjectEntryManager =
			DefaultObjectEntryManagerProvider.provide(
				_dataSetObjectEntryManagerRegistry.getObjectEntryManager(
					dataSetObjectDefinition.getStorageType()));

		return defaultObjectEntryManager.getObjectEntry(
			companyId, dtoConverterContext, externalReferenceCode,
			dataSetObjectDefinition, null);
	}

	private static final Log _log = LogFactoryUtil.getLog(
		FDSAdminFragmentRenderer.class);

	@Reference
	private ObjectDefinitionLocalService _dataSetObjectDefinitionLocalService;

	@Reference
	private ObjectEntryManagerRegistry _dataSetObjectEntryManagerRegistry;

	@Reference
	private FragmentEntryConfigurationParser _fragmentEntryConfigurationParser;

	@Reference
	private Language _language;

	@Reference
	private DataSetRenderer _dataSetRenderer;

	@Reference
	private ReactRenderer _reactRenderer;

}
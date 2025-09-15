/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.frontend.data.set.admin.web.internal.fragment.renderer;

import com.liferay.fragment.entry.processor.constants.FragmentEntryProcessorConstants;
import com.liferay.fragment.model.FragmentEntryLink;
import com.liferay.fragment.processor.DefaultFragmentEntryProcessorContext;
import com.liferay.fragment.processor.FragmentEntryProcessorRegistry;
import com.liferay.fragment.renderer.FragmentRenderer;
import com.liferay.fragment.renderer.FragmentRendererContext;
import com.liferay.fragment.service.FragmentEntryLinkLocalService;
import com.liferay.fragment.util.configuration.FragmentEntryConfigurationParser;
import com.liferay.frontend.data.set.renderer.FDSRenderer;
import com.liferay.info.constants.InfoDisplayWebKeys;
import com.liferay.info.item.ClassPKInfoItemIdentifier;
import com.liferay.info.item.InfoItemIdentifier;
import com.liferay.info.item.InfoItemReference;
import com.liferay.object.entry.util.ObjectEntryThreadLocal;
import com.liferay.object.model.ObjectDefinition;
import com.liferay.object.rest.dto.v1_0.ObjectEntry;
import com.liferay.object.rest.manager.v1_0.DefaultObjectEntryManager;
import com.liferay.object.rest.manager.v1_0.DefaultObjectEntryManagerProvider;
import com.liferay.object.rest.manager.v1_0.ObjectEntryManagerRegistry;
import com.liferay.object.service.ObjectDefinitionLocalService;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.feature.flag.FeatureFlagManagerUtil;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.language.Language;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.SetUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.vulcan.dto.converter.DefaultDTOConverterContext;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	public JSONObject getConfigurationJSONObject(
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
						)))));
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
		return FeatureFlagManagerUtil.isEnabled(
			_portal.getCompanyId(httpServletRequest), "LPS-164563");
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
					getConfigurationJSONObject(fragmentRendererContext),
					fragmentEntryLink.getEditableValuesJSONObject(),
					fragmentRendererContext.getLocale(), "itemSelector");

			String externalReferenceCode = jsonObject.getString(
				"externalReferenceCode");

			ObjectEntry dataSetObjectEntry = null;

			if (Validator.isNotNull(externalReferenceCode)) {
				try {
					ObjectDefinition dataSetObjectDefinition =
						_dataSetObjectDefinitionLocalService.
							fetchObjectDefinition(
								fragmentEntryLink.getCompanyId(), "DataSet");

					DefaultObjectEntryManager defaultObjectEntryManager =
						DefaultObjectEntryManagerProvider.provide(
							_dataSetObjectEntryManagerRegistry.
								getObjectEntryManager(
									dataSetObjectDefinition.getStorageType()));

					dataSetObjectEntry =
						defaultObjectEntryManager.getObjectEntry(
							fragmentEntryLink.getCompanyId(),
							new DefaultDTOConverterContext(
								false, null, null, null, null,
								LocaleUtil.getMostRelevantLocale(), null, null),
							externalReferenceCode, dataSetObjectDefinition,
							null);
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

				printWriter.write(
					StringBundler.concat(
						"<div class=\"portlet-msg-info\">",
						_language.get(
							httpServletRequest, "select-a-data-set-view"),
						"</div>"));
			}

			if (dataSetObjectEntry == null) {
				return;
			}

			boolean mappingComplete = _isMappingComplete(
				fragmentEntryLink, httpServletRequest, dataSetObjectEntry);

			/*
			similar to isMapping complete: check fdsAPIURL does not contain {
			 */
			if (fragmentRendererContext.isEditMode()) {
				/* prepare mapping UI */
				StringBundler markupSB = new StringBundler();

				Set<String> placeholders = _getParameterNames(dataSetObjectEntry);

				markupSB.append("<div class='p-2' data-fragment-namespace=");
				markupSB.append("'${fragmentEntryLinkNamespace}'>");

				for (String placeholder : placeholders) {
					markupSB.append("<div><span><strong>");
					markupSB.append(placeholder);
					markupSB.append(": </strong></span>");
					markupSB.append("<span class='navbar-text-truncate'");
					markupSB.append("data-lfr-editable-id=\"");
					markupSB.append(placeholder);
					markupSB.append("\" data-lfr-editable-type=\"text\">\n\t{");
					markupSB.append(placeholder);
					markupSB.append("}\n</span></div>");
				}

				Matcher matcher = _pattern.matcher(
					_getRawFDSAPIURL(dataSetObjectEntry));

				String urlMarkup = matcher.replaceAll(
					match -> {
						String editableName = match.group(1);

						String editableValue = _getEditableValue(
							fragmentEntryLink, httpServletRequest,
							editableName);

						if (Validator.isNull(editableValue)) {
							editableValue = "{" + editableName + "}";
						}

						String editableMarkup =
							"<span><strong>" + editableValue +
								"</strong></span>";

						return Matcher.quoteReplacement(editableMarkup);
					});

				markupSB.append(
					"<span class='workflow-status'><strong class='label ");

				if (mappingComplete) {
					markupSB.append("label-success'>Mapped");
				}
				else {
					markupSB.append("label-info'>Unmapped");
				}

				markupSB.append("</strong></span> Data Set URL: ");
				markupSB.append(urlMarkup);
				markupSB.append("</div>");

				fragmentEntryLink.setHtml(markupSB.toString());

				/* render mapping UI, PoC quality. Final version will be separated into:
					- mapping UI being a react app
					- calculation of the editables with _processFragmentEntryLinkHTML using dummy HTML or internal APIs
				 */
				printWriter.write(
					_processFragmentEntryLinkHTML(
						fragmentRendererContext, httpServletRequest,
						httpServletResponse));
			}

			if (mappingComplete) {
				printWriter.write("<div>");

				_fdsRenderer.render(
					HashMapBuilder.<String, Object>put(
						"namespace",
						fragmentRendererContext.getFragmentElementId()
					).put(
						"resolvedParameters",
						_getParametersJSONObject(
							dataSetObjectEntry, fragmentEntryLink,
							httpServletRequest)
					).put(
						"style", "fluid"
					).build(),
					fragmentRendererContext.getFragmentElementId(),
					externalReferenceCode, httpServletRequest,
					httpServletResponse, true, null, printWriter);

				printWriter.write("</div>");
			}
		}
		catch (Exception exception) {
			_log.error("Unable to render frontend data set view", exception);

			throw new IOException(exception);
		}
		finally {
			ObjectEntryThreadLocal.setSkipObjectEntryResourcePermission(false);
		}
	}

	private String _getEditableValue(
		FragmentEntryLink fragmentEntryLink,
		HttpServletRequest httpServletRequest, String placeholder) {

		JSONObject editableValuesJSONObject =
			fragmentEntryLink.getEditableValuesJSONObject();

		if (editableValuesJSONObject == null) {
			return null;
		}

		JSONObject editablePlaceholdersJSONObject =
			editableValuesJSONObject.getJSONObject(
				FragmentEntryProcessorConstants.
					KEY_EDITABLE_FRAGMENT_ENTRY_PROCESSOR);

		if (editablePlaceholdersJSONObject == null) {
			return null;
		}

		JSONObject editablePlaceholderJSONObject =
			editablePlaceholdersJSONObject.getJSONObject(placeholder);

		if (editablePlaceholderJSONObject == null) {
			return null;
		}

		String value = editablePlaceholderJSONObject.getString(
			LanguageUtil.getLanguageId(httpServletRequest));

		if (Validator.isNull(value)) {
			value = editablePlaceholderJSONObject.getString("classPK");
		}

		if (Validator.isNull(value)) {
			InfoItemReference infoItemReference = (InfoItemReference)
				httpServletRequest.getAttribute(
					InfoDisplayWebKeys.INFO_ITEM_REFERENCE);

			if (infoItemReference == null) {
				return null;
			}

			InfoItemIdentifier infoItemIdentifier =
				infoItemReference.getInfoItemIdentifier();

			if (!(infoItemIdentifier instanceof ClassPKInfoItemIdentifier)) {
				return null;
			}

			ClassPKInfoItemIdentifier classPKInfoItemIdentifier =
				(ClassPKInfoItemIdentifier)
					infoItemReference.getInfoItemIdentifier();

			value = String.valueOf(classPKInfoItemIdentifier.getClassPK());
		}

		return value;
	}

	private String _getRawFDSAPIURL(
		ObjectEntry dataSetObjectEntry) {

		String restEndpoint = (String)dataSetObjectEntry.getPropertyValue(
			"restEndpoint");
		String additionalURLParameters =
			(String)dataSetObjectEntry.getPropertyValue(
				"additionalAPIURLParameters");

		String value = restEndpoint;

		if (Validator.isNotNull(additionalURLParameters)) {
			value += "?" + additionalURLParameters;
		}

		return value;
	}

	private JSONObject _getParametersJSONObject(
		ObjectEntry dataSetObjectEntry, FragmentEntryLink fragmentEntryLink,
		HttpServletRequest httpServletRequest) {

		Set<String> placeholders = _getParameterNames(dataSetObjectEntry);

		JSONObject parametersJSONObject = _jsonFactory.createJSONObject();

		for (String placeholder : placeholders) {
			String value = _getEditableValue(
				fragmentEntryLink, httpServletRequest, placeholder);

			if (Validator.isNotNull(value)) {
				parametersJSONObject.put(placeholder, value);
			}
		}

		return parametersJSONObject;
	}

	private Set<String> _getParameterNames(ObjectEntry dataSetObjectEntry) {
		Set<String> parameterNames = new HashSet<>();

		Matcher matcher = _pattern.matcher(
			_getRawFDSAPIURL(dataSetObjectEntry));

		while (matcher.find()) {
			parameterNames.add(matcher.group(1));
		}

		return parameterNames;
	}

	private boolean _isMappingComplete(
		FragmentEntryLink fragmentEntryLink,
		HttpServletRequest httpServletRequest, ObjectEntry dataSetObjectEntry) {

		Set<String> parameterNames = _getParameterNames(dataSetObjectEntry);

		if (SetUtil.isEmpty(parameterNames)) {
			return true;
		}

		JSONObject editablesJSONObject =
			fragmentEntryLink.getEditableValuesJSONObject();

		JSONObject parametersJSONObject =
			editablesJSONObject.getJSONObject(
				FragmentEntryProcessorConstants.
					KEY_EDITABLE_FRAGMENT_ENTRY_PROCESSOR);

		if ((parametersJSONObject == null) ||
			JSONUtil.isEmpty(parametersJSONObject)) {

			return false;
		}

		for (String parameter : parameterNames) {
			if (Validator.isNull(
					_getEditableValue(
						fragmentEntryLink, httpServletRequest, parameter))) {

				return false;
			}
		}

		return true;
	}

	private String _processFragmentEntryLinkHTML(
			FragmentRendererContext fragmentRendererContext,
			HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse)
		throws PortalException {

		FragmentEntryLink fragmentEntryLink =
			fragmentRendererContext.getFragmentEntryLink();

		DefaultFragmentEntryProcessorContext
			defaultFragmentEntryProcessorContext =
				new DefaultFragmentEntryProcessorContext(
					httpServletRequest, httpServletResponse,
					fragmentRendererContext.getMode(),
					fragmentRendererContext.getLocale());

		defaultFragmentEntryProcessorContext.setAttributes(
			fragmentRendererContext.getAttributes());
		defaultFragmentEntryProcessorContext.setContextInfoItemReference(
			fragmentRendererContext.getContextInfoItemReference());
		defaultFragmentEntryProcessorContext.setFragmentElementId(
			fragmentRendererContext.getFragmentElementId());
		defaultFragmentEntryProcessorContext.setInfoForm(
			fragmentRendererContext.getInfoForm());
		defaultFragmentEntryProcessorContext.setPreviewClassNameId(
			fragmentRendererContext.getPreviewClassNameId());
		defaultFragmentEntryProcessorContext.setPreviewClassPK(
			fragmentRendererContext.getPreviewClassPK());
		defaultFragmentEntryProcessorContext.setPreviewType(
			fragmentRendererContext.getPreviewType());
		defaultFragmentEntryProcessorContext.setPreviewVersion(
			fragmentRendererContext.getPreviewVersion());
		defaultFragmentEntryProcessorContext.setSegmentsEntryIds(
			fragmentRendererContext.getSegmentsEntryIds());

		String html = StringPool.BLANK;

		if (Validator.isNotNull(fragmentEntryLink.getHtml()) ||
			Validator.isNotNull(fragmentEntryLink.getEditableValues())) {

			html = _fragmentEntryProcessorRegistry.processFragmentEntryLinkHTML(
				fragmentEntryLink, defaultFragmentEntryProcessorContext);
		}

		return html;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		FDSAdminFragmentRenderer.class);

	private static final Pattern _pattern = Pattern.compile("\\{(.*?)\\}");

	@Reference
	private ObjectDefinitionLocalService _dataSetObjectDefinitionLocalService;

	@Reference
	private ObjectEntryManagerRegistry _dataSetObjectEntryManagerRegistry;

	@Reference
	private FDSRenderer _fdsRenderer;

	@Reference
	private FragmentEntryConfigurationParser _fragmentEntryConfigurationParser;

	@Reference
	private FragmentEntryLinkLocalService _fragmentEntryLinkLocalService;

	@Reference
	private FragmentEntryProcessorRegistry _fragmentEntryProcessorRegistry;

	@Reference
	private JSONFactory _jsonFactory;

	@Reference
	private Language _language;

	@Reference
	private Portal _portal;

}
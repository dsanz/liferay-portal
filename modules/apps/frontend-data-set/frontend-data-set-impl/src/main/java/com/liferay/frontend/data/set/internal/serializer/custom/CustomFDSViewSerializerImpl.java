/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.frontend.data.set.internal.serializer.custom;

import com.liferay.client.extension.type.FDSCellRendererCET;
import com.liferay.client.extension.type.manager.CETManager;
import com.liferay.frontend.data.set.constants.FDSTypes;
import com.liferay.frontend.data.set.serializer.FDSViewSerializer;
import com.liferay.object.rest.dto.v1_0.ObjectEntry;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.language.Language;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.Portal;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Daniel Sanz
 * @author Marko Cikos
 */
@Component(
	property = "dataset.type=" + FDSTypes.CUSTOM,
	service = FDSViewSerializer.class
)
public class CustomFDSViewSerializerImpl implements FDSViewSerializer {

	@Override
	public JSONArray serialize(
		String fdsName, HttpServletRequest httpServletRequest) {

		Map<String, Object> properties =
			_customFDSSerializerHelper.getDataSetObjectEntryProperties(
				fdsName, httpServletRequest);

		JSONArray jsonArray = _jsonFactory.createJSONArray();

		try {
			jsonArray = _getFDSViewsJSONArray(
				_portal.getCompanyId(httpServletRequest),
				_customFDSSerializerHelper.getDataSetCardSectionObjectEntries(
					fdsName, httpServletRequest),
				String.valueOf(properties.get("defaultVisualizationMode")),
				_customFDSSerializerHelper.getDataSetTableSectionObjectEntries(
					fdsName, httpServletRequest),
				_customFDSSerializerHelper.getDataSetListSectionObjectEntries(
					fdsName, httpServletRequest),
				httpServletRequest);
		}
		catch (Exception exception) {
			_log.error(
				"Unable to serialize FDS View for " + fdsName, exception);
		}

		return jsonArray;
	}

	private JSONObject _getFDSCardsViewJSONObject(
			Collection<ObjectEntry> dataSetCardsSectionObjectEntries,
			String defaultVisualizationMode,
			HttpServletRequest httpServletRequest)
		throws Exception {

		return JSONUtil.put(
			"contentRenderer", "cards"
		).put(
			"default", defaultVisualizationMode.equals("cards")
		).put(
			"label", _language.get(httpServletRequest, "cards")
		).put(
			"name", "cards"
		).put(
			"schema", _getViewSchemaJSONObject(dataSetCardsSectionObjectEntries)
		).put(
			"thumbnail", "cards2"
		);
	}

	private JSONObject _getFDSListViewJSONObject(
			String defaultVisualizationMode,
			Collection<ObjectEntry> dataSetListSectionObjectEntries,
			HttpServletRequest httpServletRequest)
		throws Exception {

		return JSONUtil.put(
			"contentRenderer", "list"
		).put(
			"default", defaultVisualizationMode.equals("list")
		).put(
			"label", _language.get(httpServletRequest, "list")
		).put(
			"name", "list"
		).put(
			"schema", _getViewSchemaJSONObject(dataSetListSectionObjectEntries)
		).put(
			"thumbnail", "list"
		);
	}

	private JSONObject _getFDSTableViewJSONObject(
			long companyId, String defaultVisualizationMode,
			Set<ObjectEntry> dataSetTableSectionObjectEntries,
			HttpServletRequest httpServletRequest)
		throws Exception {

		return JSONUtil.put(
			"contentRenderer", "table"
		).put(
			"default", defaultVisualizationMode.equals("table")
		).put(
			"label", _language.get(httpServletRequest, "table")
		).put(
			"name", "table"
		).put(
			"schema",
			JSONUtil.put(
				"fields",
				_getFieldsJSONArray(
					companyId, dataSetTableSectionObjectEntries))
		).put(
			"thumbnail", "table"
		);
	}

	private JSONArray _getFDSViewsJSONArray(
			long companyId,
			Collection<ObjectEntry> dataSetCardsSectionObjectEntries,
			String defaultVisualizationMode,
			Set<ObjectEntry> dataSetTableSectionObjectEntries,
			Collection<ObjectEntry> dataSetListSectionObjectEntries,
			HttpServletRequest httpServletRequest)
		throws Exception {

		JSONArray viewsJSONArray = _jsonFactory.createJSONArray();

		if (!dataSetCardsSectionObjectEntries.isEmpty()) {
			viewsJSONArray.put(
				_getFDSCardsViewJSONObject(
					dataSetCardsSectionObjectEntries, defaultVisualizationMode,
					httpServletRequest));
		}

		if (!dataSetListSectionObjectEntries.isEmpty()) {
			viewsJSONArray.put(
				_getFDSListViewJSONObject(
					defaultVisualizationMode, dataSetListSectionObjectEntries,
					httpServletRequest));
		}

		if (!dataSetTableSectionObjectEntries.isEmpty()) {
			viewsJSONArray.put(
				_getFDSTableViewJSONObject(
					companyId, defaultVisualizationMode,
					dataSetTableSectionObjectEntries, httpServletRequest));
		}

		return viewsJSONArray;
	}

	private JSONArray _getFieldsJSONArray(
			long companyId, Set<ObjectEntry> dataSetTableSectionObjectEntries)
		throws Exception {

		return JSONUtil.toJSONArray(
			dataSetTableSectionObjectEntries,
			(ObjectEntry objectEntry) -> {
				Map<String, Object> properties = objectEntry.getProperties();

				JSONObject jsonObject = JSONUtil.put(
					"contentRenderer",
					String.valueOf(properties.get("renderer"))
				).put(
					"fieldName", String.valueOf(properties.get("fieldName"))
				).put(
					"label",
					_customFDSSerializerHelper.getLabelValue(
						"label", "fieldName", properties)
				).put(
					"sortable", (boolean)properties.get("sortable")
				);

				String rendererType = String.valueOf(
					properties.get("rendererType"));

				if (!Objects.equals(rendererType, "clientExtension")) {
					return jsonObject;
				}

				FDSCellRendererCET fdsCellRendererCET =
					(FDSCellRendererCET)_cetManager.getCET(
						companyId, String.valueOf(properties.get("renderer")));

				return jsonObject.put(
					"contentRendererClientExtension", true
				).put(
					"contentRendererModuleURL",
					"default from " + fdsCellRendererCET.getURL()
				);
			});
	}

	private JSONObject _getViewSchemaJSONObject(
			Collection<ObjectEntry> fdsViewObjectEntries)
		throws Exception {

		JSONObject jsonObject = _jsonFactory.createJSONObject();

		for (ObjectEntry dataSetObjectEntry : fdsViewObjectEntries) {
			Map<String, Object> properties = dataSetObjectEntry.getProperties();

			jsonObject.put(
				String.valueOf(properties.get("name")),
				String.valueOf(properties.get("fieldName")));
		}

		return jsonObject;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		CustomFDSViewSerializerImpl.class);

	@Reference
	private CETManager _cetManager;

	@Reference
	private CustomFDSSerializerHelper _customFDSSerializerHelper;

	@Reference
	private JSONFactory _jsonFactory;

	@Reference
	private Language _language;

	@Reference
	private Portal _portal;

}
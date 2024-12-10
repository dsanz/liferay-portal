/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.frontend.data.set.internal.renderer;

import com.liferay.frontend.data.set.constants.FDSTypes;
import com.liferay.frontend.data.set.internal.serializer.custom.CustomFDSSerializerHelper;
import com.liferay.frontend.data.set.renderer.FDSEntryPropsProvider;
import com.liferay.frontend.data.set.serializer.FDSAPIURLSerializer;
import com.liferay.frontend.data.set.serializer.FDSBulkActionListSerializer;
import com.liferay.frontend.data.set.serializer.FDSCreationMenuSerializer;
import com.liferay.frontend.data.set.serializer.FDSFilterSerializer;
import com.liferay.frontend.data.set.serializer.FDSItemActionListSerializer;
import com.liferay.frontend.data.set.serializer.FDSSortListSerializer;
import com.liferay.frontend.data.set.serializer.FDSViewSerializer;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.json.JSONUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Daniel Sanz
 */
@Component(
	property = "dataset.type=" + FDSTypes.CUSTOM,
	service = FDSEntryPropsProvider.class
)
public class CustomFDSPropsProviderImpl implements FDSEntryPropsProvider {

	@Override
	public boolean isAvailable(
		String fdsName, HttpServletRequest httpServletRequest) {

		return Validator.isNotNull(
			_customFDSSerializerHelper.getDataSetObjectEntry(
				fdsName, httpServletRequest));
	}

	@Override
	public Map<String, Object> prepareProps(
		String fdsName, HttpServletRequest httpServletRequest) {

		Map<String, Object> properties =
			_customFDSSerializerHelper.getDataSetObjectEntryProperties(
				fdsName, httpServletRequest);

		return HashMapBuilder.<String, Object>put(
			"apiURL",
			() -> _fdsAPIURLSerializer.serialize(fdsName, httpServletRequest)
		).put(
			"bulkActions",
			() -> _fdsBulkActionListSerializer.serialize(
				fdsName, httpServletRequest)
		).put(
			"creationMenu",
			() -> _fdsCreationMenuSerializer.serialize(
				fdsName, httpServletRequest)
		).put(
			"currentURL", _portal.getCurrentURL(httpServletRequest)
		).put(
			"filters",
			() -> _fdsFilterSerializer.serialize(fdsName, httpServletRequest)
		).put(
			"itemsActions",
			() -> _fdsItemActionListSerializer.serialize(
				fdsName, httpServletRequest)
		).put(
			"pagination",
			JSONUtil.put(
				"deltas",
				() -> JSONUtil.toJSONArray(
					StringUtil.split(
						String.valueOf(properties.get("listOfItemsPerPage")),
						StringPool.COMMA_AND_SPACE),
					(String itemPerPage) -> JSONUtil.put(
						"label", GetterUtil.getInteger(itemPerPage)))
			).put(
				"initialDelta",
				String.valueOf(properties.get("defaultItemsPerPage"))
			)
		).put(
			"sorts",
			() -> _fdsSortSerializer.serialize(fdsName, httpServletRequest)
		).put(
			"views",
			() -> _fdsViewSerializer.serialize(fdsName, httpServletRequest)
		).build();
	}

	@Reference
	private CustomFDSSerializerHelper _customFDSSerializerHelper;

	@Reference(target = "(dataset.type=" + FDSTypes.CUSTOM + ")")
	private FDSAPIURLSerializer _fdsAPIURLSerializer;

	@Reference(target = "(dataset.type=" + FDSTypes.CUSTOM + ")")
	private FDSBulkActionListSerializer _fdsBulkActionListSerializer;

	@Reference(target = "(dataset.type=" + FDSTypes.CUSTOM + ")")
	private FDSCreationMenuSerializer _fdsCreationMenuSerializer;

	@Reference(target = "(dataset.type=" + FDSTypes.CUSTOM + ")")
	private FDSFilterSerializer _fdsFilterSerializer;

	@Reference(target = "(dataset.type=" + FDSTypes.CUSTOM + ")")
	private FDSItemActionListSerializer _fdsItemActionListSerializer;

	@Reference(target = "(dataset.type=" + FDSTypes.CUSTOM + ")")
	private FDSSortListSerializer _fdsSortSerializer;

	@Reference(target = "(dataset.type=" + FDSTypes.CUSTOM + ")")
	private FDSViewSerializer _fdsViewSerializer;

	@Reference
	private Portal _portal;

}
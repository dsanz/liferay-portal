/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.frontend.data.set.internal.serializer.custom;

import com.liferay.frontend.data.set.constants.FDSTypes;
import com.liferay.frontend.data.set.model.FDSSortItem;
import com.liferay.frontend.data.set.model.FDSSortItemBuilder;
import com.liferay.frontend.data.set.serializer.FDSSortListSerializer;
import com.liferay.object.rest.dto.v1_0.ObjectEntry;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.Validator;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Daniel Sanz
 * @author Marko Cikos
 */
@Component(
	property = "dataset.type=" + FDSTypes.CUSTOM,
	service = FDSSortListSerializer.class
)
public class CustomFDSSortListSerializerImpl implements FDSSortListSerializer {

	@Override
	public List<FDSSortItem> serialize(
		String fdsName, HttpServletRequest httpServletRequest) {

		return ListUtil.toList(
			ListUtil.fromCollection(
				_customFDSSerializerHelper.getSortObjectEntries(
					fdsName, httpServletRequest)),
			(ObjectEntry objectEntry) -> {
				Map<String, Object> properties = objectEntry.getProperties();

				String label = (String)properties.get("label");

				if (Validator.isNull(label)) {
					Map<String, String> labelI18n =
						(Map<String, String>)properties.get("label_i18n");

					label = labelI18n.get(
						LocaleUtil.toLanguageId(LocaleUtil.getSiteDefault()));
				}

				return FDSSortItemBuilder.setActive(
					Boolean.valueOf(String.valueOf(properties.get("default")))
				).setDirection(
					String.valueOf(properties.get("orderType"))
				).setKey(
					String.valueOf(properties.get("fieldName"))
				).setLabel(
					label
				).build();
			});
	}

	@Reference
	private CustomFDSSerializerHelper _customFDSSerializerHelper;

}
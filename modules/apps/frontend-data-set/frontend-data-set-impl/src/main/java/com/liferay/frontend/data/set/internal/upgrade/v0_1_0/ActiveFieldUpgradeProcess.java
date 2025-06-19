/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */


package com.liferay.frontend.data.set.internal.upgrade.v0_1_0;

import com.liferay.object.model.ObjectDefinition;
import com.liferay.object.model.ObjectEntry;
import com.liferay.object.service.ObjectDefinitionLocalService;
import com.liferay.object.service.ObjectEntryLocalService;
import com.liferay.portal.kernel.service.CompanyLocalService;
import com.liferay.portal.kernel.upgrade.UpgradeProcess;

import java.util.List;
import java.util.Map;

/**
 * @author Daniel Sanz
 */
public class ActiveFieldUpgradeProcess extends UpgradeProcess {
	public ActiveFieldUpgradeProcess(CompanyLocalService companyLocalService,
									 ObjectDefinitionLocalService objectDefinitionLocalService,
									 ObjectEntryLocalService objectEntryLocalService) {

		_companyLocalService = companyLocalService;
		_objectDefinitionLocalService = objectDefinitionLocalService;
		_objectEntryLocalService = objectEntryLocalService;
	}

	private void _updateActiveFlag(ObjectDefinition objectDefinition) {
		List<ObjectEntry> objectEntries = _objectEntryLocalService.getObjectEntries(0, objectDefinition.getObjectDefinitionId(), -1, -1);

		objectEntries.forEach(objectEntry -> {
			Map<String, java.io.Serializable> values = objectEntry.getValues();
			values.put("active", true);
			objectEntry.setValues(values);
			_objectEntryLocalService.updateObjectEntry(objectEntry);
		});
	}

	@Override
	protected void doUpgrade() throws Exception {

		_companyLocalService.forEachCompanyId(
			companyId -> {
				for (String objectDefinitionERC : _OBJECT_DEFINITION_EXTERNAL_REFERENCE_CODES) {
					ObjectDefinition objectDefinition =
						_objectDefinitionLocalService.
							fetchObjectDefinitionByExternalReferenceCode(
								objectDefinitionERC, companyId);

					if (objectDefinition == null) {
						return;
					}

					_updateActiveFlag(objectDefinition);
				}
			});

	}

	private static final String[] _OBJECT_DEFINITION_EXTERNAL_REFERENCE_CODES = {
		"L_DATA_SET", "L_DATA_SET_ACTION"
	};

	private final CompanyLocalService _companyLocalService;
	private final ObjectDefinitionLocalService _objectDefinitionLocalService;
	private final ObjectEntryLocalService _objectEntryLocalService;
}

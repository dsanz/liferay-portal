/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.frontend.data.set.internal.serializer.system;

import com.liferay.frontend.data.set.SystemFDSEntry;
import com.liferay.frontend.data.set.SystemFDSEntryRegistry;
import com.liferay.frontend.data.set.constants.FDSTypes;
import com.liferay.frontend.data.set.serializer.FDSAPIURLSerializer;
import com.liferay.frontend.data.set.url.builder.FDSAPIURLBuilder;
import com.liferay.frontend.data.set.url.builder.FDSAPIURLBuilderFactory;

import javax.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Daniel Sanz
 */
@Component(
	property = "dataset.type=" + FDSTypes.SYSTEM,
	service = FDSAPIURLSerializer.class
)
public class SystemFDSAPIURLSerializerImpl implements FDSAPIURLSerializer {

	@Override
	public String serialize(
		String fdsName, HttpServletRequest httpServletRequest) {

		SystemFDSEntry systemFDSEntry =
			_systemFDSEntryRegistry.getSystemFDSEntry(fdsName);

		if (systemFDSEntry == null) {
			return null;
		}

		FDSAPIURLBuilder fdsAPIURLBuilder = _fdsAPIURLBuilderFactory.create(
			systemFDSEntry.getRESTEndpoint(),
			systemFDSEntry.getRESTApplication(), systemFDSEntry.getRESTSchema(),
			httpServletRequest);

		return fdsAPIURLBuilder.build();
	}

	@Reference
	private FDSAPIURLBuilderFactory _fdsAPIURLBuilderFactory;

	@Reference
	private SystemFDSEntryRegistry _systemFDSEntryRegistry;

}
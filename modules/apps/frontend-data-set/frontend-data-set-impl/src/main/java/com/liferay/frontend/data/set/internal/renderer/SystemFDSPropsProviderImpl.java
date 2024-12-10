/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.frontend.data.set.internal.renderer;

import com.liferay.frontend.data.set.SystemFDSEntry;
import com.liferay.frontend.data.set.SystemFDSEntryRegistry;
import com.liferay.frontend.data.set.constants.FDSTypes;
import com.liferay.frontend.data.set.renderer.FDSEntryPropsProvider;
import com.liferay.frontend.data.set.serializer.FDSAPIURLSerializer;
import com.liferay.frontend.data.set.serializer.FDSBulkActionListSerializer;
import com.liferay.frontend.data.set.serializer.FDSCreationMenuSerializer;
import com.liferay.frontend.data.set.serializer.FDSFilterSerializer;
import com.liferay.frontend.data.set.serializer.FDSItemActionListSerializer;
import com.liferay.frontend.data.set.serializer.FDSSortListSerializer;
import com.liferay.frontend.data.set.serializer.FDSViewSerializer;
import com.liferay.frontend.data.set.view.FDSViewRegistry;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.Portal;
import com.liferay.portal.kernel.util.Validator;

import java.util.Collections;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Daniel Sanz
 */
@Component(
	property = "dataset.type=" + FDSTypes.SYSTEM,
	service = FDSEntryPropsProvider.class
)
public class SystemFDSPropsProviderImpl implements FDSEntryPropsProvider {

	@Override
	public boolean isAvailable(
		String fdsName, HttpServletRequest httpServletRequest) {

		if (Validator.isNotNull(
				_systemFDSEntryRegistry.getSystemFDSEntry(fdsName)) ||
			Validator.isNotNull(_fdsViewRegistry.getFDSViews(fdsName))) {

			return true;
		}

		return false;
	}

	@Override
	public Map<String, Object> prepareProps(
		String fdsName, HttpServletRequest httpServletRequest) {

		SystemFDSEntry systemFDSEntry =
			_systemFDSEntryRegistry.getSystemFDSEntry(fdsName);

		if (systemFDSEntry == null) {
			return Collections.emptyMap();
		}

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
			HashMapBuilder.<String, Object>put(
				"deltas", systemFDSEntry.getListOfItemsPerPage()
			).put(
				"initialDelta", systemFDSEntry.getDefaultItemsPerPage()
			).build()
		).put(
			"sorts",
			() -> _fdsSortSerializer.serialize(fdsName, httpServletRequest)
		).put(
			"views",
			() -> _fdsViewSerializer.serialize(fdsName, httpServletRequest)
		).build();
	}

	@Reference(target = "(dataset.type=" + FDSTypes.SYSTEM + ")")
	private FDSAPIURLSerializer _fdsAPIURLSerializer;

	@Reference(target = "(dataset.type=" + FDSTypes.SYSTEM + ")")
	private FDSBulkActionListSerializer _fdsBulkActionListSerializer;

	@Reference(target = "(dataset.type=" + FDSTypes.SYSTEM + ")")
	private FDSCreationMenuSerializer _fdsCreationMenuSerializer;

	@Reference(target = "(dataset.type=" + FDSTypes.SYSTEM + ")")
	private FDSFilterSerializer _fdsFilterSerializer;

	@Reference(target = "(dataset.type=" + FDSTypes.SYSTEM + ")")
	private FDSItemActionListSerializer _fdsItemActionListSerializer;

	@Reference(target = "(dataset.type=" + FDSTypes.SYSTEM + ")")
	private FDSSortListSerializer _fdsSortSerializer;

	@Reference
	private FDSViewRegistry _fdsViewRegistry;

	@Reference(target = "(dataset.type=" + FDSTypes.SYSTEM + ")")
	private FDSViewSerializer _fdsViewSerializer;

	@Reference
	private Portal _portal;

	@Reference
	private SystemFDSEntryRegistry _systemFDSEntryRegistry;

}
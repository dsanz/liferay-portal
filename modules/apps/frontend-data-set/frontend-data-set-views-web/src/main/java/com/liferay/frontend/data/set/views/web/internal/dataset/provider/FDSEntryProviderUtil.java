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

package com.liferay.frontend.data.set.views.web.internal.dataset.provider;

import com.liferay.fragment.model.FragmentEntryLink;
import com.liferay.fragment.renderer.FragmentRendererContext;
import com.liferay.object.model.ObjectDefinition;
import com.liferay.object.rest.dto.v1_0.ObjectEntry;
import com.liferay.object.rest.manager.v1_0.ObjectEntryManager;
import com.liferay.object.service.ObjectDefinitionLocalService;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.vulcan.dto.converter.DTOConverterContext;
import com.liferay.portal.vulcan.dto.converter.DefaultDTOConverterContext;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Daniel Sanz
 */
@Component(service = FDSEntryProviderUtil.class)
public class FDSEntryProviderUtil {

	public ObjectDefinition _getFDSEntryObjectDefinition() {
		return _getFDSEntryObjectDefinition(CompanyThreadLocal.getCompanyId());
	}

	public ObjectDefinition _getFDSEntryObjectDefinition(long companyId) {
		return _objectDefinitionLocalService.fetchObjectDefinition(
			companyId, "C_FDSEntry");
	}

	public ObjectDefinition _getFDSViewObjectDefinition() {
		return _getFDSViewObjectDefinition(CompanyThreadLocal.getCompanyId());
	}

	public ObjectDefinition _getFDSViewObjectDefinition(long companyId) {
		return _objectDefinitionLocalService.fetchObjectDefinition(
			companyId, "C_FDSView");
	}

	public ObjectEntry getFDSView(
		String externalReferenceCode,
		FragmentRendererContext fragmentRendererContext) {

		try {
			ObjectDefinition fdsViewObjectDefinition =
				getFDSViewObjectDefinition(fragmentRendererContext);

			DTOConverterContext dtoConverterContext =
				new DefaultDTOConverterContext(
					false, null, null, null, null, LocaleUtil.getSiteDefault(),
					null, null);

			return _objectEntryManager.getObjectEntry(
				dtoConverterContext, externalReferenceCode,
				fdsViewObjectDefinition.getCompanyId(), fdsViewObjectDefinition,
				null);
		}
		catch (Exception exception) {
			_log.error(exception);

			return null;
		}
	}

	public ObjectDefinition getFDSViewObjectDefinition(
		FragmentRendererContext fragmentRendererContext) {

		long companyId = CompanyThreadLocal.getCompanyId();

		FragmentEntryLink fragmentEntryLink =
			fragmentRendererContext.getFragmentEntryLink();

		if (fragmentEntryLink != null) {
			companyId = fragmentEntryLink.getCompanyId();
		}

		return _getFDSViewObjectDefinition(companyId);
	}

	private static final Log _log = LogFactoryUtil.getLog(
		FDSEntryProviderUtil.class);

	@Reference
	private ObjectDefinitionLocalService _objectDefinitionLocalService;

	@Reference(target = "(object.entry.manager.storage.type=default)")
	private ObjectEntryManager _objectEntryManager;

}
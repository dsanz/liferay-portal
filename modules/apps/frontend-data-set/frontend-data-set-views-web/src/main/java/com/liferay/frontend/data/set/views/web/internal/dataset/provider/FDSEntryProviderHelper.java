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
import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.security.auth.CompanyThreadLocal;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.vulcan.dto.converter.DTOConverterContext;
import com.liferay.portal.vulcan.dto.converter.DefaultDTOConverterContext;
import com.liferay.portal.vulcan.pagination.Page;
import com.liferay.portal.vulcan.pagination.Pagination;

import java.util.Collection;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Daniel Sanz
 */
@Component(service = FDSEntryProviderHelper.class)
public class FDSEntryProviderHelper {

	public ObjectEntry getFDSEntry(ObjectEntry fdsView) {
		Map<String, Object> fdsViewProperties = fdsView.getProperties();

		Long fdsEntryId = (Long)fdsViewProperties.get(
			"r_fdsEntryFDSViewRelationship_c_fdsEntryId");

		ObjectDefinition fdsEntryObjectDefinition =
			getFDSEntryObjectDefinition();

		DTOConverterContext dtoConverterContext =
			new DefaultDTOConverterContext(
				false, null, null, null, null, LocaleUtil.getSiteDefault(),
				null, null);

		try {
			return _objectEntryManager.getObjectEntry(
				dtoConverterContext, fdsEntryObjectDefinition, fdsEntryId);
		}
		catch (Exception exception) {
			_log.error(exception);

			return null;
		}
	}

	public ObjectDefinition getFDSEntryObjectDefinition() {
		return getFDSEntryObjectDefinition(CompanyThreadLocal.getCompanyId());
	}

	public ObjectDefinition getFDSEntryObjectDefinition(long companyId) {
		return _objectDefinitionLocalService.fetchObjectDefinition(
			companyId, "C_FDSEntry");
	}

	public ObjectDefinition getFDSFieldObjectDefinition() {
		return getFDSFieldObjectDefinition(CompanyThreadLocal.getCompanyId());
	}

	public ObjectDefinition getFDSFieldObjectDefinition(long companyId) {
		return _objectDefinitionLocalService.fetchObjectDefinition(
			companyId, "C_FDSField");
	}

	public Collection<ObjectEntry> getFDSFields(ObjectEntry fdsView) {
		ObjectDefinition fdsViewObjectDefinition = getFDSViewObjectDefinition();

		DTOConverterContext dtoConverterContext =
			new DefaultDTOConverterContext(
				false, null, null, null, null, LocaleUtil.getSiteDefault(),
				null, null);

		try {
			Page<ObjectEntry> fieldsPage =
				_objectEntryManager.getObjectEntryRelatedObjectEntries(
					dtoConverterContext, fdsViewObjectDefinition,
					fdsView.getId(), "fdsViewFDSFieldRelationship",
					Pagination.of(1, 500));

			return fieldsPage.getItems();
		}
		catch (Exception exception) {
			_log.error(exception);

			return null;
		}
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

	public ObjectDefinition getFDSViewObjectDefinition() {
		return getFDSViewObjectDefinition(CompanyThreadLocal.getCompanyId());
	}

	public ObjectDefinition getFDSViewObjectDefinition(
		FragmentRendererContext fragmentRendererContext) {

		long companyId = CompanyThreadLocal.getCompanyId();

		FragmentEntryLink fragmentEntryLink =
			fragmentRendererContext.getFragmentEntryLink();

		if (fragmentEntryLink != null) {
			companyId = fragmentEntryLink.getCompanyId();
		}

		return getFDSViewObjectDefinition(companyId);
	}

	public ObjectDefinition getFDSViewObjectDefinition(long companyId) {
		return _objectDefinitionLocalService.fetchObjectDefinition(
			companyId, "C_FDSView");
	}

	public JSONArray getFieldNameJSONArray(ObjectEntry fdsField) {
		Map<String, Object> fdsFieldProperties = fdsField.getProperties();

		String fieldName = (String)fdsFieldProperties.get("name");

		JSONArray jsonArray = null;

		try {
			jsonArray = _jsonFactory.createJSONArray(
				StringUtil.split(fieldName, CharPool.PERIOD));
		}
		catch (Exception exception) {
			if (_log.isInfoEnabled()) {
				_log.info(
					StringBundler.concat(
						"Unable to build JSONArray from '", fieldName, "'"),
					exception);
			}
		}

		return jsonArray;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		FDSEntryProviderHelper.class);

	@Reference
	private JSONFactory _jsonFactory;

	@Reference
	private ObjectDefinitionLocalService _objectDefinitionLocalService;

	@Reference(target = "(object.entry.manager.storage.type=default)")
	private ObjectEntryManager _objectEntryManager;

}
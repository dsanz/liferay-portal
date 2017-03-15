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

package com.liferay.portal.workflow.kaleo.service.impl;

import aQute.bnd.annotation.ProviderType;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.OrderByComparator;
import com.liferay.portal.workflow.kaleo.exception.NoSuchDefinitionVersionException;
import com.liferay.portal.workflow.kaleo.model.KaleoDefinitionVersion;
import com.liferay.portal.workflow.kaleo.service.base.KaleoDefinitionVersionLocalServiceBaseImpl;
import com.liferay.portal.workflow.kaleo.util.comparator.KaleoDefinitionVersionVersionComparator;

import java.util.Collections;
import java.util.List;

/**
 * @author Inácio Nery
 */
@ProviderType
public class KaleoDefinitionVersionLocalServiceImpl
	extends KaleoDefinitionVersionLocalServiceBaseImpl {

	@Override
	public KaleoDefinitionVersion getKaleoDefinitionVersion(
			long kaleoDefinitionId, String version)
		throws PortalException {

		return kaleoDefinitionVersionPersistence.findByD_V(
			kaleoDefinitionId, version);
	}

	@Override
	public KaleoDefinitionVersion getKaleoDefinitionVersion(
			long companyId, String name, String version)
		throws PortalException {

		return kaleoDefinitionVersionPersistence.findByC_N_V(
			companyId, name, version);
	}

	@Override
	public List<KaleoDefinitionVersion> getKaleoDefinitionVersions(
		long companyId, boolean active, int start, int end,
		OrderByComparator<KaleoDefinitionVersion> orderByComparator) {

		return kaleoDefinitionVersionPersistence.findByC_A(
			companyId, active, start, end, orderByComparator);
	}

	@Override
	public List<KaleoDefinitionVersion> getKaleoDefinitionVersions(
		long companyId, int start, int end,
		OrderByComparator<KaleoDefinitionVersion> orderByComparator) {

		return kaleoDefinitionVersionPersistence.findByCompanyId(
			companyId, start, end, orderByComparator);
	}

	@Override
	public List<KaleoDefinitionVersion> getKaleoDefinitionVersions(
			long companyId, String name)
		throws PortalException {

		return kaleoDefinitionVersionPersistence.findByC_N(companyId, name);
	}

	@Override
	public List<KaleoDefinitionVersion> getKaleoDefinitionVersions(
		long companyId, String name, boolean active, int start, int end,
		OrderByComparator<KaleoDefinitionVersion> orderByComparator) {

		return kaleoDefinitionVersionPersistence.findByC_N_A(
			companyId, name, active, start, end, orderByComparator);
	}

	@Override
	public List<KaleoDefinitionVersion> getKaleoDefinitionVersions(
		long companyId, String name, int start, int end,
		OrderByComparator<KaleoDefinitionVersion> orderByComparator) {

		return kaleoDefinitionVersionPersistence.findByC_N(
			companyId, name, start, end, orderByComparator);
	}

	@Override
	public int getKaleoDefinitionVersionsCount(long companyId) {
		return kaleoDefinitionVersionPersistence.countByCompanyId(companyId);
	}

	@Override
	public int getKaleoDefinitionVersionsCount(long companyId, boolean active) {
		return kaleoDefinitionVersionPersistence.countByC_A(companyId, active);
	}

	@Override
	public int getKaleoDefinitionVersionsCount(long companyId, String name) {
		return kaleoDefinitionVersionPersistence.countByC_N(companyId, name);
	}

	@Override
	public int getKaleoDefinitionVersionsCount(
		long companyId, String name, boolean active) {

		return kaleoDefinitionVersionPersistence.countByC_N_A(
			companyId, name, active);
	}

	@Override
	public KaleoDefinitionVersion getLatestKaleoDefinitionVersion(
			long kaleoDefinitionId)
		throws PortalException {

		List<KaleoDefinitionVersion> kaleoDefinitionVersions =
			kaleoDefinitionVersionPersistence.findByKaleoDefinitionId(
				kaleoDefinitionId);

		if (kaleoDefinitionVersions.isEmpty()) {
			throw new NoSuchDefinitionVersionException();
		}

		kaleoDefinitionVersions = ListUtil.copy(kaleoDefinitionVersions);

		Collections.sort(
			kaleoDefinitionVersions,
			new KaleoDefinitionVersionVersionComparator());

		return kaleoDefinitionVersions.get(0);
	}

}
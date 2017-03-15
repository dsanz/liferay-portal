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

package com.liferay.portal.workflow.kaleo.runtime.integration.impl.internal;

import com.liferay.portal.kernel.io.unsync.UnsyncByteArrayInputStream;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.util.OrderByComparator;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.workflow.WorkflowDefinition;
import com.liferay.portal.kernel.workflow.WorkflowDefinitionManager;
import com.liferay.portal.kernel.workflow.WorkflowException;
import com.liferay.portal.kernel.workflow.comparator.WorkflowComparatorFactory;
import com.liferay.portal.workflow.kaleo.KaleoWorkflowModelConverter;
import com.liferay.portal.workflow.kaleo.model.KaleoDefinition;
import com.liferay.portal.workflow.kaleo.model.KaleoDefinitionVersion;
import com.liferay.portal.workflow.kaleo.runtime.WorkflowEngine;
import com.liferay.portal.workflow.kaleo.runtime.util.comparator.KaleoDefinitionVersionOrderByComparator;
import com.liferay.portal.workflow.kaleo.service.KaleoDefinitionLocalService;
import com.liferay.portal.workflow.kaleo.service.KaleoDefinitionVersionLocalService;

import java.util.ArrayList;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

/**
 * @author Michael C. Han
 * @author Eduardo Lundgren
 */
@Component(
	immediate = true, property = {"proxy.bean=false"},
	service = WorkflowDefinitionManager.class
)
public class WorkflowDefinitionManagerImpl
	implements WorkflowDefinitionManager {

	@Override
	public WorkflowDefinition deployWorkflowDefinition(
			long companyId, long userId, String title, byte[] bytes)
		throws WorkflowException {

		ServiceContext serviceContext = new ServiceContext();

		serviceContext.setCompanyId(companyId);
		serviceContext.setUserId(userId);

		return _workflowEngine.deployWorkflowDefinition(
			title, new UnsyncByteArrayInputStream(bytes), serviceContext);
	}

	@Override
	public int getActiveWorkflowDefinitionCount(long companyId)
		throws WorkflowException {

		try {
			return _kaleoDefinitionVersionLocalService.
				getKaleoDefinitionVersionsCount(companyId, true);
		}
		catch (Exception e) {
			throw new WorkflowException(e);
		}
	}

	@Override
	public int getActiveWorkflowDefinitionCount(long companyId, String name)
		throws WorkflowException {

		try {
			return _kaleoDefinitionVersionLocalService.
				getKaleoDefinitionVersionsCount(companyId, name, true);
		}
		catch (Exception e) {
			throw new WorkflowException(e);
		}
	}

	@Override
	public List<WorkflowDefinition> getActiveWorkflowDefinitions(
			long companyId, int start, int end,
			OrderByComparator<WorkflowDefinition> orderByComparator)
		throws WorkflowException {

		try {
			if (orderByComparator == null) {
				orderByComparator =
					_workflowComparatorFactory.getDefinitionNameComparator(
						true);
			}

			ServiceContext serviceContext = new ServiceContext();

			serviceContext.setCompanyId(companyId);

			List<KaleoDefinitionVersion> kaleoDefinitionVersions =
				_kaleoDefinitionVersionLocalService.getKaleoDefinitionVersions(
					companyId, true, start, end,
					KaleoDefinitionVersionOrderByComparator.
						getOrderByComparator(
							orderByComparator, _kaleoWorkflowModelConverter));

			return toWorkflowDefinitions(kaleoDefinitionVersions);
		}
		catch (Exception e) {
			throw new WorkflowException(e);
		}
	}

	@Override
	public List<WorkflowDefinition> getActiveWorkflowDefinitions(
			long companyId, String name, int start, int end,
			OrderByComparator<WorkflowDefinition> orderByComparator)
		throws WorkflowException {

		try {
			ServiceContext serviceContext = new ServiceContext();

			serviceContext.setCompanyId(companyId);

			List<KaleoDefinitionVersion> kaleoDefinitionVersions =
				_kaleoDefinitionVersionLocalService.getKaleoDefinitionVersions(
					companyId, name, true, start, end,
					KaleoDefinitionVersionOrderByComparator.
						getOrderByComparator(
							orderByComparator, _kaleoWorkflowModelConverter));

			return toWorkflowDefinitions(kaleoDefinitionVersions);
		}
		catch (Exception e) {
			throw new WorkflowException(e);
		}
	}

	@Override
	public WorkflowDefinition getLatestKaleoDefinition(
			long companyId, String name)
		throws WorkflowException {

		try {
			ServiceContext serviceContext = new ServiceContext();

			serviceContext.setCompanyId(companyId);

			KaleoDefinition kaleoDefinition =
				_kaleoDefinitionLocalService.getLatestKaleoDefinition(
					name, serviceContext);

			return _kaleoWorkflowModelConverter.toWorkflowDefinition(
				kaleoDefinition);
		}
		catch (Exception e) {
			throw new WorkflowException(e);
		}
	}

	@Override
	public WorkflowDefinition getWorkflowDefinition(
			long companyId, String name, int version)
		throws WorkflowException {

		try {
			KaleoDefinitionVersion kaleoDefinitionVersion =
				_kaleoDefinitionVersionLocalService.getKaleoDefinitionVersion(
					companyId, name, getVersion(version));

			return _kaleoWorkflowModelConverter.toWorkflowDefinition(
				kaleoDefinitionVersion);
		}
		catch (Exception e) {
			throw new WorkflowException(e);
		}
	}

	@Override
	public int getWorkflowDefinitionCount(long companyId)
		throws WorkflowException {

		try {
			return _kaleoDefinitionVersionLocalService.
				getKaleoDefinitionVersionsCount(companyId);
		}
		catch (Exception e) {
			throw new WorkflowException(e);
		}
	}

	@Override
	public int getWorkflowDefinitionCount(long companyId, String name)
		throws WorkflowException {

		try {
			return _kaleoDefinitionVersionLocalService.
				getKaleoDefinitionVersionsCount(companyId, name);
		}
		catch (Exception e) {
			throw new WorkflowException(e);
		}
	}

	@Override
	public List<WorkflowDefinition> getWorkflowDefinitions(
			long companyId, int start, int end,
			OrderByComparator<WorkflowDefinition> orderByComparator)
		throws WorkflowException {

		try {
			List<KaleoDefinitionVersion> kaleoDefinitionVersions =
				_kaleoDefinitionVersionLocalService.getKaleoDefinitionVersions(
					companyId, start, end,
					KaleoDefinitionVersionOrderByComparator.
						getOrderByComparator(
							orderByComparator, _kaleoWorkflowModelConverter));

			return toWorkflowDefinitions(kaleoDefinitionVersions);
		}
		catch (Exception e) {
			throw new WorkflowException(e);
		}
	}

	@Override
	public List<WorkflowDefinition> getWorkflowDefinitions(
			long companyId, String name, int start, int end,
			OrderByComparator<WorkflowDefinition> orderByComparator)
		throws WorkflowException {

		try {
			List<KaleoDefinitionVersion> kaleoDefinitionVersions =
				_kaleoDefinitionVersionLocalService.getKaleoDefinitionVersions(
					companyId, name, start, end,
					KaleoDefinitionVersionOrderByComparator.
						getOrderByComparator(
							orderByComparator, _kaleoWorkflowModelConverter));

			return toWorkflowDefinitions(kaleoDefinitionVersions);
		}
		catch (Exception e) {
			throw new WorkflowException(e);
		}
	}

	@Override
	public void undeployWorkflowDefinition(
			long companyId, long userId, String name, int version)
		throws WorkflowException {

		try {
			ServiceContext serviceContext = new ServiceContext();

			serviceContext.setCompanyId(companyId);
			serviceContext.setUserId(userId);

			_workflowEngine.deleteWorkflowDefinition(
				name, version, serviceContext);
		}
		catch (Exception e) {
			throw new WorkflowException(e);
		}
	}

	@Override
	public WorkflowDefinition updateActive(
			long companyId, long userId, String name, int version,
			boolean active)
		throws WorkflowException {

		try {
			ServiceContext serviceContext = new ServiceContext();

			serviceContext.setCompanyId(companyId);
			serviceContext.setUserId(userId);

			if (active) {
				_kaleoDefinitionLocalService.activateKaleoDefinition(
					name, version, serviceContext);
			}
			else {
				_kaleoDefinitionLocalService.deactivateKaleoDefinition(
					name, version, serviceContext);
			}

			return getWorkflowDefinition(companyId, name, version);
		}
		catch (Exception e) {
			throw new WorkflowException(e);
		}
	}

	@Override
	public WorkflowDefinition updateTitle(
			long companyId, long userId, String name, int version, String title)
		throws WorkflowException {

		try {
			ServiceContext serviceContext = new ServiceContext();

			serviceContext.setCompanyId(companyId);
			serviceContext.setUserId(userId);

			KaleoDefinition kaleoDefinition =
				_kaleoDefinitionLocalService.updateTitle(
					name, version, title, serviceContext);

			return _kaleoWorkflowModelConverter.toWorkflowDefinition(
				kaleoDefinition);
		}
		catch (Exception e) {
			throw new WorkflowException(e);
		}
	}

	@Override
	public void validateWorkflowDefinition(byte[] bytes)
		throws WorkflowException {

		_workflowEngine.validateWorkflowDefinition(
			new UnsyncByteArrayInputStream(bytes));
	}

	protected String getVersion(int version) {
		return version + StringPool.PERIOD + 0;
	}

	protected int getVersion(String version) {
		int[] versionParts = StringUtil.split(version, StringPool.PERIOD, 0);

		return versionParts[0];
	}

	protected List<WorkflowDefinition> toWorkflowDefinitions(
		List<KaleoDefinitionVersion> kaleoDefinitionVersions) {

		List<WorkflowDefinition> workflowDefinitions = new ArrayList<>(
			kaleoDefinitionVersions.size());

		for (KaleoDefinitionVersion kaleoDefinitionVersion :
				kaleoDefinitionVersions) {

			WorkflowDefinition workflowDefinition =
				_kaleoWorkflowModelConverter.toWorkflowDefinition(
					kaleoDefinitionVersion);

			workflowDefinitions.add(workflowDefinition);
		}

		return workflowDefinitions;
	}

	@Reference
	private KaleoDefinitionLocalService _kaleoDefinitionLocalService;

	@Reference
	private KaleoDefinitionVersionLocalService
		_kaleoDefinitionVersionLocalService;

	@Reference
	private KaleoWorkflowModelConverter _kaleoWorkflowModelConverter;

	@Reference
	private WorkflowComparatorFactory _workflowComparatorFactory;

	@Reference(
		cardinality = ReferenceCardinality.OPTIONAL,
		policy = ReferencePolicy.DYNAMIC,
		policyOption = ReferencePolicyOption.GREEDY
	)
	private volatile WorkflowEngine _workflowEngine;

}
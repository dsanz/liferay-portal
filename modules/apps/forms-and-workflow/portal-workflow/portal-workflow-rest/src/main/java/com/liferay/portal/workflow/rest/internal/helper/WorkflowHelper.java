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

package com.liferay.portal.workflow.rest.internal.helper;

import com.liferay.asset.kernel.model.AssetEntry;
import com.liferay.asset.kernel.model.AssetRendererFactory;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.service.WorkflowInstanceLinkLocalService;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.workflow.WorkflowConstants;
import com.liferay.portal.kernel.workflow.WorkflowHandler;
import com.liferay.portal.kernel.workflow.WorkflowHandlerRegistryUtil;
import com.liferay.portal.kernel.workflow.WorkflowInstance;
import com.liferay.portal.kernel.workflow.WorkflowInstanceManager;
import com.liferay.portal.kernel.workflow.WorkflowInstanceManagerUtil;
import com.liferay.portal.kernel.workflow.WorkflowTask;
import com.liferay.portal.kernel.workflow.WorkflowTaskManager;
import com.liferay.portal.workflow.rest.internal.model.WorkflowAssetModel;
import com.liferay.portal.workflow.rest.internal.model.WorkflowListedTaskModel;
import com.liferay.portal.workflow.rest.internal.model.WorkflowTaskModel;
import com.liferay.portal.workflow.rest.internal.model.WorkflowTaskTransitionOperationModel;
import com.liferay.portal.workflow.rest.internal.model.WorkflowUserModel;

import java.io.Serializable;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Adam Brandizzi
 */
@Component(immediate = true, service = WorkflowHelper.class)
public class WorkflowHelper {

	public WorkflowTask completeWorkflowTask(
			long companyId, long userId, long workflowTaskId,
			WorkflowTaskTransitionOperationModel
				workflowTaskTransitionOperationModel)
		throws PortalException {

		Map<String, Serializable> workflowContext = getWorkflowContext(
			companyId, workflowTaskId);

		return _workflowTaskManager.completeWorkflowTask(
			companyId, userId, workflowTaskId,
			workflowTaskTransitionOperationModel.getTransition(),
			workflowTaskTransitionOperationModel.getComment(), workflowContext);
	}

	public WorkflowListedTaskModel getWorkflowListedTaskModel(
			long companyId, WorkflowTask workflowTask, Locale locale)
		throws PortalException {

		WorkflowUserModel workflowUserModel = getWorkflowUserModel(
			workflowTask);
		String state = getState(
			companyId, workflowTask.getWorkflowTaskId(), locale);

		return new WorkflowListedTaskModel(
			workflowTask, workflowUserModel, state);
	}

	public WorkflowTaskModel getWorkflowTaskModel(
			long companyId, long userId, long workflowTaskId, Locale locale)
		throws PortalException {

		WorkflowTask workflowTask = _workflowTaskManager.getWorkflowTask(
			companyId, workflowTaskId);

		WorkflowUserModel workflowUserModel = getWorkflowUserModel(
			workflowTask);

		WorkflowAssetModel workflowAssetModel = getWorkflowAssetModel(
			companyId, workflowTaskId, locale);
		String state = getState(companyId, workflowTaskId, locale);
		List<String> transitions = _workflowTaskManager.getNextTransitionNames(
			companyId, userId, workflowTaskId);

		return new WorkflowTaskModel(
			workflowTask, workflowUserModel, workflowAssetModel, state,
			transitions);
	}

	protected String getState(
			long companyId, long workflowTaskId, Locale locale)
		throws PortalException {

		Map<String, Serializable> workflowContext = getWorkflowContext(
			companyId, workflowTaskId);

		long groupId = GetterUtil.getLong(
			workflowContext.get(WorkflowConstants.CONTEXT_GROUP_ID));
		String className = GetterUtil.getString(
			workflowContext.get(WorkflowConstants.CONTEXT_ENTRY_CLASS_NAME));
		long classPK = GetterUtil.getLong(
			workflowContext.get(WorkflowConstants.CONTEXT_ENTRY_CLASS_PK));

		String state = _workflowInstanceLinkLocalService.getState(
			companyId, groupId, className, classPK);

		return LanguageUtil.get(locale, state);
	}

	protected WorkflowAssetModel getWorkflowAssetModel(
			long companyId, long workflowTaskId, Locale locale)
		throws PortalException {

		Map<String, Serializable> workflowContext = getWorkflowContext(
			companyId, workflowTaskId);

		String className = GetterUtil.getString(
			workflowContext.get(WorkflowConstants.CONTEXT_ENTRY_CLASS_NAME));

		WorkflowHandler<?> workflowHandler =
			WorkflowHandlerRegistryUtil.getWorkflowHandler(className);

		AssetRendererFactory<?> assetRendererFactory =
			workflowHandler.getAssetRendererFactory();

		long classPK = GetterUtil.getLong(
			workflowContext.get(WorkflowConstants.CONTEXT_ENTRY_CLASS_PK));

		AssetEntry assetEntry = assetRendererFactory.getAssetEntry(
			className, classPK);

		return new WorkflowAssetModel(assetEntry, locale);
	}

	protected Map<String, Serializable> getWorkflowContext(
			long companyId, long workflowTaskId)
		throws PortalException {

		WorkflowInstance workflowInstance = getWorkflowInstance(
			companyId, workflowTaskId);

		return workflowInstance.getWorkflowContext();
	}

	protected WorkflowInstance getWorkflowInstance(
			long companyId, long workflowTaskId)
		throws PortalException {

		WorkflowTask workflowTask = _workflowTaskManager.getWorkflowTask(
			companyId, workflowTaskId);

		return WorkflowInstanceManagerUtil.getWorkflowInstance(
			companyId, workflowTask.getWorkflowInstanceId());
	}

	protected WorkflowUserModel getWorkflowUserModel(WorkflowTask workflowTask)
		throws PortalException {

		User user = _userLocalService.fetchUser(
			workflowTask.getAssigneeUserId());

		if (user != null) {
			return new WorkflowUserModel(user);
		}

		return null;
	}

	@Reference
	private UserLocalService _userLocalService;

	@Reference
	private WorkflowInstanceLinkLocalService _workflowInstanceLinkLocalService;

	@Reference
	private WorkflowInstanceManager _workflowInstanceManager;

	@Reference
	private WorkflowTaskManager _workflowTaskManager;

}
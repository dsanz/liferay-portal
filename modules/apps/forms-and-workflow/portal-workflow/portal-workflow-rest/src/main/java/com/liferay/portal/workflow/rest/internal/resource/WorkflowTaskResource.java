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

package com.liferay.portal.workflow.rest.internal.resource;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.workflow.WorkflowException;
import com.liferay.portal.workflow.rest.internal.helper.WorkflowHelper;
import com.liferay.portal.workflow.rest.internal.model.WorkflowOperationResultModel;
import com.liferay.portal.workflow.rest.internal.model.WorkflowTaskModel;
import com.liferay.portal.workflow.rest.internal.model.WorkflowTaskTransitionOperationModel;

import java.util.Locale;

import javax.servlet.http.HttpServletResponse;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Adam Brandizzi
 */
@Component(immediate = true, service = WorkflowTaskResource.class)
@Path("/task")
public class WorkflowTaskResource {

	@GET
	@Path("/{workflowTaskId}")
	@Produces("application/json")
	public WorkflowTaskModel getWorkflowTask(
			@Context Company company, @Context User user,
			@Context Locale locale,
			@PathParam("workflowTaskId") long workflowTaskId)
		throws PortalException {

		return _workflowHelper.getWorkflowTaskModel(
			company.getCompanyId(), user.getUserId(), workflowTaskId, locale);
	}

	@Consumes("application/json")
	@Path("/{workflowTaskId}")
	@POST
	@Produces("application/json")
	public WorkflowOperationResultModel updateStatus(
		@Context Company company, @Context User user,
		@Context HttpServletResponse response,
		@PathParam("workflowTaskId") long workflowTaskId,
		WorkflowTaskTransitionOperationModel
			workflowTaskTransitionOperationModel) {

		try {
			_workflowHelper.completeWorkflowTask(
				company.getCompanyId(), user.getUserId(), workflowTaskId,
				workflowTaskTransitionOperationModel);

			return getSuccessWorkflowOperationResultModel();
		}
		catch (PortalException pe) {
			return getFailureWorkflowOperationResultModel(response, pe);
		}
	}

	protected WorkflowOperationResultModel
		getFailureWorkflowOperationResultModel(
			HttpServletResponse response, PortalException pe) {

		if (pe instanceof WorkflowException) {
			response.setStatus(HttpServletResponse.SC_CONFLICT);
		}
		else {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}

		return new WorkflowOperationResultModel(
			WorkflowOperationResultModel.STATUS_ERROR, pe.getMessage());
	}

	protected WorkflowOperationResultModel
		getSuccessWorkflowOperationResultModel() {

		return new WorkflowOperationResultModel(
			WorkflowOperationResultModel.STATUS_SUCCESS);
	}

	@Reference
	private WorkflowHelper _workflowHelper;

}
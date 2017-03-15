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

package com.liferay.portal.workflow.rest.internal.model;

import com.liferay.portal.kernel.workflow.WorkflowTask;

import java.util.Date;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Adam Brandizzi
 */
@XmlRootElement
public class WorkflowListedTaskModel {

	public WorkflowListedTaskModel() {
		_description = null;
		_dueDate = null;
		_name = null;
		_state = null;
		_workflowTaskId = 0;
		_workflowUserModel = null;
	}

	public WorkflowListedTaskModel(
		WorkflowTask workflowTask, WorkflowUserModel workflowUserModel,
		String state) {

		_workflowUserModel = workflowUserModel;

		_description = workflowTask.getDescription();
		_dueDate = workflowTask.getDueDate();
		_name = workflowTask.getName();
		_state = state;
		_workflowTaskId = workflowTask.getWorkflowTaskId();
	}

	@XmlElement
	public String getDescription() {
		return _description;
	}

	@XmlElement
	public Date getDueDate() {
		return _dueDate;
	}

	@XmlElement
	public String getName() {
		return _name;
	}

	@XmlElement
	public String getState() {
		return _state;
	}

	@XmlElement
	public long getWorkflowTaskId() {
		return _workflowTaskId;
	}

	@XmlElement
	public WorkflowUserModel getWorkflowUserModel() {
		return _workflowUserModel;
	}

	private final String _description;
	private final Date _dueDate;
	private final String _name;
	private final String _state;
	private final long _workflowTaskId;
	private final WorkflowUserModel _workflowUserModel;

}
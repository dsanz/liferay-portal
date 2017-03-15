<%--
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
--%>

<%@ include file="/init.jsp" %>

<%
WorkflowDefinition workflowDefinition = (WorkflowDefinition)request.getAttribute(WebKeys.WORKFLOW_DEFINITION);

List<WorkflowDefinition> workflowDefinitionVersions = workflowDefinitionDisplayContext.getWorkflowDefinitions(workflowDefinition.getName());

for (WorkflowDefinition workflowDefinitionVersion : workflowDefinitionVersions) {
	request.setAttribute("WORKFLOW_DEFINITION_VERSION", workflowDefinitionVersion);
%>

	<div class="sidebar-header">
		<ul class="sidebar-header-actions">
			<li>
				<liferay-util:include page="/workflow_definition_version_action.jsp" servletContext="<%= application %>" />
			</li>
		</ul>

		<h4><liferay-ui:message arguments="<%= workflowDefinitionVersion.getVersion() %>" key="version-x" /></h4>
	</div>

	<%} %>
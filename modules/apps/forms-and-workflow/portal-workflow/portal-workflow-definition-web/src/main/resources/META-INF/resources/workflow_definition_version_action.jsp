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
String redirect = ParamUtil.getString(request, "redirect");

WorkflowDefinition workflowDefinition = (WorkflowDefinition)request.getAttribute(WebKeys.WORKFLOW_DEFINITION);

WorkflowDefinition workflowDefinitionVersion = (WorkflowDefinition)request.getAttribute("WORKFLOW_DEFINITION_VERSION");
%>

<liferay-ui:icon-menu direction="left-side" icon="<%= StringPool.BLANK %>" markupView="lexicon" message="<%= StringPool.BLANK %>" showWhenSingleIcon="<%= true %>">
	<c:if test="<%= workflowDefinition.getVersion() != workflowDefinitionVersion.getVersion() %>">
		<portlet:renderURL var="revertURL">
			<portlet:param name="mvcPath" value="/edit_workflow_definition.jsp" />
			<portlet:param name="redirect" value="<%= redirect %>" />
			<portlet:param name="name" value="<%= workflowDefinitionVersion.getName() %>" />
			<portlet:param name="version" value="<%= String.valueOf(workflowDefinitionVersion.getVersion()) %>" />
		</portlet:renderURL>

		<liferay-ui:icon
			message="revert"
			url="<%= revertURL %>"
		/>
	</c:if>
</liferay-ui:icon-menu>
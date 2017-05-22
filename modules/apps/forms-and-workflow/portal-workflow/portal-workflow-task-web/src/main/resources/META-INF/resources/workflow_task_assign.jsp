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
WorkflowTask workflowTask = workflowTaskDisplayContext.getWorkflowTask();

String closeRedirect = ParamUtil.getString(request, "closeRedirect");

boolean hasOtherAssignees = workflowTaskDisplayContext.hasOtherAssignees(workflowTask);
%>

<liferay-portlet:actionURL name="assignWorkflowTask" portletName="<%= PortletKeys.MY_WORKFLOW_TASK %>" var="assignURL" />

<div class="container-fluid-1280">
	<aui:form action="<%= assignURL %>" method="post" name="assignFm">
		<aui:input name="closeRedirect" type="hidden" value="<%= closeRedirect %>" />
		<aui:input name="workflowTaskId" type="hidden" value="<%= String.valueOf(workflowTask.getWorkflowTaskId()) %>" />

		<aui:select disabled="<%= !hasOtherAssignees %>" label="assign-to" name="assigneeUserId">

			<%
			for (long pooledActorId : workflowTaskDisplayContext.getActorsIds(workflowTask)) {
			%>

				<aui:option label="<%= workflowTaskDisplayContext.getActorName(pooledActorId) %>" selected="<%= workflowTask.getAssigneeUserId() == pooledActorId %>" value="<%= String.valueOf(pooledActorId) %>" />

			<%
			}
			%>

		</aui:select>

		<aui:input cols="55" disabled="<%= !hasOtherAssignees %>" name="comment" placeholder="comment" rows="1" type="textarea" />

		<aui:button-row>
			<aui:button cssClass="btn-lg" disabled="<%= !hasOtherAssignees %>" name="done" primary="<%= true %>" value="done" />

			<aui:button cssClass="btn-lg" name="close" type="cancel" />
		</aui:button-row>
	</aui:form>
</div>

<aui:script use="aui-base,aui-io-request">
	A.one('#<portlet:namespace/>done').on('click', function(event) {
		A.io.request(
			'<%= assignURL.toString() %>',
			{
				method: 'POST',
				form: {id: '<portlet:namespace/>assignFm'},
				on: {
					success: function() {
						Liferay.Util.getWindow('<portlet:namespace />assignToDialog').destroy();
					}
				}
			}
		);
	});
</aui:script>
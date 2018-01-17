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

<%@ include file="/subscription_manager/init.jsp" %>

<%
ResultRow row = (ResultRow)request.getAttribute(WebKeys.SEARCH_CONTAINER_RESULT_ROW);

SubscriptionManagerHandler subscriptionManagerHandler = (SubscriptionManagerHandler)request.getAttribute("view.jsp-subscriptionManagerHandler");

Object object = row.getObject();

long classPK = subscriptionManagerHandler.getClassPK(object);
%>

<liferay-ui:icon-menu direction="left-side" icon="<%= StringPool.BLANK %>" markupView="lexicon" message="<%= StringPool.BLANK %>" showWhenSingleIcon="<%= true %>">

	<%
	PortletURL viewURL = subscriptionManagerHandler.getViewURL(classPK, request, currentURL);
	%>

	<liferay-ui:icon
		message="view"
		url="<%= viewURL.toString() %>"
	/>

	<liferay-portlet:renderURL var="editSubscriptionsURL">
		<portlet:param name="mvcRenderCommandName" value="/subscription_manager/edit_subscriptions" />
		<portlet:param name="redirect" value="<%= currentURL %>" />
		<portlet:param name="className" value="<%= subscriptionManagerHandler.getClassName() %>" />
		<portlet:param name="classPK" value="<%= String.valueOf(classPK) %>" />
		<portlet:param name="title" value="<%= subscriptionManagerHandler.getName(object) %>" />
	</liferay-portlet:renderURL>

	<liferay-ui:icon
		message="manage-subscriptions"
		url="<%= editSubscriptionsURL %>"
	/>
</liferay-ui:icon-menu>
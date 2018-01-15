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

<%@ include file="/message_boards_subscription_manager/init.jsp" %>

<%
long mbCategoryId = ParamUtil.getLong(request, "mbCategoryId");

ResultRow row = (ResultRow)request.getAttribute(WebKeys.SEARCH_CONTAINER_RESULT_ROW);

User user2 = (User)row.getObject();

boolean subscribed = SubscriptionLocalServiceUtil.isSubscribed(user2.getCompanyId(), user2.getUserId(), MBCategory.class.getName(), mbCategoryId);
%>

<portlet:actionURL name="/message_boards_subscription_manager/edit_subscription" var="editSubscriptionURL">
	<portlet:param name="<%= Constants.CMD %>" value="<%= subscribed ? Constants.UNSUBSCRIBE : Constants.SUBSCRIBE %>" />
	<portlet:param name="redirect" value="<%= currentURL %>" />
	<portlet:param name="mbCategoryId" value="<%= String.valueOf(mbCategoryId) %>" />
	<portlet:param name="userIds" value="<%= String.valueOf(user2.getUserId()) %>" />
</portlet:actionURL>

<liferay-ui:icon-menu direction="left-side" icon="<%= StringPool.BLANK %>" markupView="lexicon" message="<%= StringPool.BLANK %>" showWhenSingleIcon="<%= true %>">
	<c:choose>
		<c:when test="<%= subscribed %>">
			<liferay-ui:icon
				message="unsubscribe"
				url="<%= editSubscriptionURL %>"
			/>
		</c:when>
		<c:otherwise>
			<liferay-ui:icon
				message="subscribe"
				url="<%= editSubscriptionURL %>"
			/>
		</c:otherwise>
	</c:choose>
</liferay-ui:icon-menu>
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
String redirect = ParamUtil.getString(request, "redirect");

long mbCategoryId = ParamUtil.getLong(request, "mbCategoryId");

portletURL.setParameter("mvcRenderCommandName", "/message_boards_subscription_manager/edit_subscriptions");
portletURL.setParameter("redirect", redirect);
portletURL.setParameter("mbCategoryId", String.valueOf(mbCategoryId));

request.setAttribute("edit_subscriptions.jsp-portletURL", portletURL);

MBCategory mbCategory = MBCategoryLocalServiceUtil.getMBCategory(mbCategoryId);
%>

<liferay-ui:header
	title="<%= mbCategory.getName() %>"
/>

<form action="<%= portletURL.toString() %>" method="post" name="<portlet:namespace />fm" onSubmit="submitForm(this); return false;">
	<aui:input name="mbCategoryId" type="hidden" value="<%= mbCategoryId %>" />
	<aui:input name="userIds" type="hidden" />

	<h3 class="lfr-panel-title"><span><liferay-ui:message key="users" /></span></h3>

	<%
	SearchContainer userSearchContainer = new UserSearch(renderRequest, portletURL);

	UserSearchTerms searchTerms = (UserSearchTerms)userSearchContainer.getSearchTerms();

	List<User> users = null;
	int usersCount = 0;

	if (searchTerms.isAdvancedSearch()) {
		usersCount = UserLocalServiceUtil.searchCount(company.getCompanyId(), searchTerms.getFirstName(), searchTerms.getMiddleName(), searchTerms.getLastName(), searchTerms.getScreenName(), searchTerms.getEmailAddress(), searchTerms.getStatus(), new LinkedHashMap(), searchTerms.isAndOperator());

		userSearchContainer.setTotal(usersCount);

		users = UserLocalServiceUtil.search(company.getCompanyId(), searchTerms.getFirstName(), searchTerms.getMiddleName(), searchTerms.getLastName(), searchTerms.getScreenName(), searchTerms.getEmailAddress(), searchTerms.getStatus(), new LinkedHashMap(), searchTerms.isAndOperator(), userSearchContainer.getStart(), userSearchContainer.getEnd(), userSearchContainer.getOrderByComparator());
	}
	else {
		usersCount = UserLocalServiceUtil.searchCount(company.getCompanyId(), searchTerms.getKeywords(), searchTerms.getStatus(), new LinkedHashMap());

		userSearchContainer.setTotal(usersCount);

		users = UserLocalServiceUtil.search(company.getCompanyId(), searchTerms.getKeywords(), searchTerms.getStatus(), new LinkedHashMap(), userSearchContainer.getStart(), userSearchContainer.getEnd(), userSearchContainer.getOrderByComparator());
	}

	userSearchContainer.setResults(users);

	userSearchContainer.setRowChecker(new RowChecker(renderResponse));
	%>

	<liferay-ui:search-container
		headerNames="name,screen-name"
		searchContainer="<%= userSearchContainer %>"
		total="<%= userSearchContainer.getTotal() %>"
	>
		<liferay-ui:search-container-results
			results="<%= userSearchContainer.getResults() %>"
		/>

		<c:if test="<%= !users.isEmpty() %>">
			<aui:button-row>
				<aui:button onClick='<%= renderResponse.getNamespace() + "subscribeUsers();" %>' value="subscribe" />

				<aui:button onClick='<%= renderResponse.getNamespace() + "unsubscribeUsers();" %>' value="unsubscribe" />
			</aui:button-row>
		</c:if>

		<liferay-ui:search-container-row
			className="com.liferay.portal.kernel.model.User"
			escapedModel="<%= true %>"
			keyProperty="userId"
			modelVar="user2"
			rowIdProperty="screenName"
		>
			<liferay-ui:search-container-column-text
				name="first-name"
				orderable="<%= true %>"
				property="firstName"
			/>

			<liferay-ui:search-container-column-text
				name="last-name"
				orderable="<%= true %>"
				property="lastName"
			/>

			<liferay-ui:search-container-column-text
				name="screen-name"
				orderable="<%= true %>"
				property="screenName"
			/>

			<liferay-ui:search-container-column-text
				name="job-title"
				orderable="<%= true %>"
				value="<%= user2.getJobTitle() %>"
			/>

			<liferay-ui:search-container-column-text
				name="organizations"
			>
				<liferay-ui:write bean="<%= user2 %>" property="organizations" />
			</liferay-ui:search-container-column-text>

			<liferay-ui:search-container-column-text
				name="user-groups"
			>
				<liferay-ui:write bean="<%= user2 %>" property="user-groups" />
			</liferay-ui:search-container-column-text>

			<liferay-ui:search-container-column-jsp
				align="right"
				path="/message_boards_subscription_manager/subscription_action.jsp"
			/>
		</liferay-ui:search-container-row>

		<liferay-ui:search-iterator />
	</liferay-ui:search-container>
</form>

<aui:script>
	Liferay.provide(
		window,
		'<portlet:namespace />subscribeUsers',
		function() {
			var userIds = Liferay.Util.listCheckedExcept(document.<portlet:namespace />fm, '<portlet:namespace />allRowIds');

			if (userIds) {
				document.<portlet:namespace />fm.<portlet:namespace />userIds.value = userIds;

				submitForm(document.<portlet:namespace />fm, '<portlet:actionURL name="subscribeUsers"><portlet:param name="redirect" value="<%= portletURL.toString() %>" /></portlet:actionURL>');
			}
		},
		['liferay-util-list-fields']
	);

	Liferay.provide(
		window,
		'<portlet:namespace />unsubscribeUsers',
		function() {
			var userIds = Liferay.Util.listCheckedExcept(document.<portlet:namespace />fm, '<portlet:namespace />allRowIds');

			if (userIds) {
				document.<portlet:namespace />fm.<portlet:namespace />userIds.value = userIds;

				submitForm(document.<portlet:namespace />fm, '<portlet:actionURL name="unsubscribeUsers"><portlet:param name="redirect" value="<%= portletURL.toString() %>" /></portlet:actionURL>');
			}
		},
		['liferay-util-list-fields']
	);
</aui:script>
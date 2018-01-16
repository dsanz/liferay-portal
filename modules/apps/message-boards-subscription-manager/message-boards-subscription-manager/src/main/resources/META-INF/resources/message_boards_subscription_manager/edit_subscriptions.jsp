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

PortletURL iteratorURL = PortletURLUtil.clone(currentURLObj, liferayPortletResponse);

iteratorURL.setParameter("redirect", redirect);

MBCategory mbCategory = MBCategoryLocalServiceUtil.getMBCategory(mbCategoryId);

portletDisplay.setShowBackIcon(true);
portletDisplay.setURLBack(redirect);

renderResponse.setTitle(mbCategory.getName());

SearchContainer userSearchContainer = new UserSearch(renderRequest, iteratorURL);

UserSearchTerms searchTerms = (UserSearchTerms)userSearchContainer.getSearchTerms();

List<User> users = null;
int usersCount = 0;

String navigation = ParamUtil.getString(request, "navigation", "active");

searchTerms.setStatus(WorkflowConstants.STATUS_APPROVED);

if (navigation.equals("inactive")) {
	searchTerms.setStatus(WorkflowConstants.STATUS_INACTIVE);
}

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

RowChecker rowChecker = new EmptyOnClickRowChecker(renderResponse);

rowChecker.setRowIds("userIds");

userSearchContainer.setRowChecker(rowChecker);
%>

<aui:nav-bar cssClass="collapse-basic-search" markupView="lexicon">
	<aui:nav cssClass="navbar-nav">
		<portlet:renderURL var="viewEntriesURL" />

		<aui:nav-item
			href="<%= portletURL %>"
			label="users"
			selected="<%= true %>"
		/>
	</aui:nav>
</aui:nav-bar>

<liferay-frontend:management-bar
	includeCheckBox="<%= true %>"
	searchContainerId="users"
>
	<liferay-frontend:management-bar-filters>
		<liferay-frontend:management-bar-navigation
			navigationKeys='<%= new String[] {"active", "inactive"} %>'
			portletURL="<%= PortletURLUtil.clone(currentURLObj, renderResponse) %>"
		/>

		<liferay-frontend:management-bar-sort
			orderByCol="<%= userSearchContainer.getOrderByCol() %>"
			orderByType="<%= userSearchContainer.getOrderByType() %>"
			orderColumns='<%= new String[] {"first-name", "last-name", "screen-name"} %>'
			portletURL="<%= PortletURLUtil.clone(currentURLObj, renderResponse) %>"
		/>
	</liferay-frontend:management-bar-filters>

	<liferay-frontend:management-bar-action-buttons>
		<liferay-frontend:management-bar-button href='<%= "javascript:" + renderResponse.getNamespace() + "subscribeUsers();" %>' icon="star" label="subscribe" />
		<liferay-frontend:management-bar-button href='<%= "javascript:" + renderResponse.getNamespace() + "unsubscribeUsers();" %>' icon="star-o" label="unsubscribe" />
	</liferay-frontend:management-bar-action-buttons>
</liferay-frontend:management-bar>

<div class="container-fluid-1280 main-content-body">
	<liferay-portlet:actionURL name="/message_boards_subscription_manager/edit_subscription" var="editSubscriptionURL" />

	<aui:form action="<%= editSubscriptionURL.toString() %>" method="post" name="fm">
		<aui:input name="mbCategoryId" type="hidden" value="<%= mbCategoryId %>" />
		<aui:input name="<%= Constants.CMD %>" type="hidden" />
		<aui:input name="redirect" type="hidden" value="<%= currentURL %>" />

		<liferay-ui:search-container
			headerNames="name,screen-name"
			id="users"
			searchContainer="<%= userSearchContainer %>"
			total="<%= userSearchContainer.getTotal() %>"
		>
			<liferay-ui:search-container-results
				results="<%= userSearchContainer.getResults() %>"
			/>

			<liferay-ui:search-container-row
				className="com.liferay.portal.kernel.model.User"
				escapedModel="<%= true %>"
				keyProperty="userId"
				modelVar="user2"
				rowIdProperty="screenName"
			>

				<%
				boolean subscribed = SubscriptionLocalServiceUtil.isSubscribed(user2.getCompanyId(), user2.getUserId(), MBCategory.class.getName(), mbCategoryId);

				request.setAttribute("edit_subscriptions.jsp-subscribed", subscribed);
				%>

				<liferay-ui:search-container-column-text>
					<liferay-ui:icon alt='<%= subscribed ? "subscribed" : "unsubscribed" %>' icon='<%= subscribed ? "star" : "star-o" %>' markupView="lexicon" message='<%= subscribed ? "subscribed" : "unsubscribed" %>' />
				</liferay-ui:search-container-column-text>

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

			<liferay-ui:search-iterator markupView="lexicon" />
		</liferay-ui:search-container>
	</aui:form>
</div>

<aui:script>
	function <portlet:namespace />subscribeUsers() {
		var form = AUI.$(document.<portlet:namespace />fm);

		form.fm('<%= Constants.CMD %>').val('<%= Constants.SUBSCRIBE %>')

		submitForm(form);
	}

	function <portlet:namespace />unsubscribeUsers() {
		var form = AUI.$(document.<portlet:namespace />fm);

		form.fm('<%= Constants.CMD %>').val('<%= Constants.UNSUBSCRIBE %>')

		submitForm(form);
	}
</aui:script>
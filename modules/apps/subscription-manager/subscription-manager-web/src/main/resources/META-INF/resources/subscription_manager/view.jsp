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
String navigation = ParamUtil.getString(request, "navigation");

Map<String, SubscriptionManagerHandler> subscriptionManagerHandlerMap = subscriptionManagerDisplayContext.getSubscriptionManagerHandlerMap();

SubscriptionManagerHandler subscriptionManagerHandler = null;

if (Validator.isNotNull(navigation) && !subscriptionManagerHandlerMap.isEmpty()) {
	subscriptionManagerHandler = subscriptionManagerHandlerMap.get(navigation);
}
%>

<c:choose>
	<c:when test="<%= subscriptionManagerHandlerMap.isEmpty() %>">
		<liferay-ui:message key="no-portlet-has-been-configured-to-manage-subscription-from-here" />
	</c:when>
	<c:otherwise>

		<%
		Set<String> keys = subscriptionManagerHandlerMap.keySet();

		if (subscriptionManagerHandler == null) {
			for (String key : keys) {
				subscriptionManagerHandler = subscriptionManagerHandlerMap.get(key);

				navigation = subscriptionManagerHandler.getClassName();

				break;
			}
		}
		%>

		<aui:nav-bar cssClass="collapse-basic-search" markupView="lexicon">
			<aui:nav cssClass="navbar-nav">

				<%
				keys = subscriptionManagerHandlerMap.keySet();

				for (String key : keys) {
					SubscriptionManagerHandler curSubscriptionManagerHandler = subscriptionManagerHandlerMap.get(key);

					String curSubscriptionManagerHandlerClassName = curSubscriptionManagerHandler.getClassName();

					PortletURL viewURL = PortletURLUtil.clone(currentURLObj, liferayPortletResponse);

					viewURL.setParameter("navigation", curSubscriptionManagerHandlerClassName);
				%>

					<aui:nav-item href="<%= viewURL.toString() %>" label="<%= curSubscriptionManagerHandler.getModelName(locale) %>" selected="<%= curSubscriptionManagerHandlerClassName.equals(navigation) %>" />

				<%
				}
				%>

			</aui:nav>
		</aui:nav-bar>

		<%
		SearchContainer modelSearchContainer = new SearchContainer(renderRequest, null, null, SearchContainer.DEFAULT_CUR_PARAM, SearchContainer.DEFAULT_DELTA, portletURL, null, subscriptionManagerHandler.getEmptyResultsMessage());

		modelSearchContainer.setTotal(subscriptionManagerHandler.getTotal(scopeGroupId));
		modelSearchContainer.setResults(subscriptionManagerHandler.getResults(scopeGroupId, modelSearchContainer.getStart(), modelSearchContainer.getEnd()));
		%>

		<div class="container-fluid-1280 main-content-body">
			<liferay-ui:search-container
				searchContainer="<%= modelSearchContainer %>"
			>
				<liferay-ui:search-container-row
					className="Object"
					escapedModel="<%= true %>"
					modelVar="object"
				>
					<liferay-portlet:renderURL var="editSubscriptionsURL">
						<portlet:param name="mvcRenderCommandName" value="/subscription_manager/edit_subscriptions" />
						<portlet:param name="redirect" value="<%= currentURL %>" />
						<portlet:param name="className" value="<%= subscriptionManagerHandler.getClassName() %>" />
						<portlet:param name="classPK" value="<%= String.valueOf(subscriptionManagerHandler.getClassPK(object)) %>" />
						<portlet:param name="title" value="<%= subscriptionManagerHandler.getName(object) %>" />
					</liferay-portlet:renderURL>

					<liferay-ui:search-container-column-text
						href="<%= editSubscriptionsURL %>"
						name="<%= subscriptionManagerHandler.getNameLabel(locale) %>"
						value="<%= subscriptionManagerHandler.getName(object) %>"
					/>

					<%
					request.setAttribute("view.jsp-subscriptionManagerHandler", subscriptionManagerHandler);
					%>

					<liferay-ui:search-container-column-jsp
						align="right"
						path="/subscription_manager/model_action.jsp"
					/>
				</liferay-ui:search-container-row>

				<liferay-ui:search-iterator markupView="lexicon" />
			</liferay-ui:search-container>
		</div>
	</c:otherwise>
</c:choose>
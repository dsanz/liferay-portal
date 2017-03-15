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
String eventName = ParamUtil.getString(request, "eventName", liferayPortletResponse.getNamespace() + "selectDisplayPage");
String selectedTab = ParamUtil.getString(renderRequest, "selectedTab", "public-pages");

PortletURL portletURL = renderResponse.createRenderURL();

portletURL.setParameter("mvcPath", "/select_page.jsp");
portletURL.setParameter("eventName", eventName);

JSONObject layoutsJSONObject = journalDisplayContext.getLayoutsJSON();

JSONObject publicLayoutsJSONObject = layoutsJSONObject.getJSONObject("public");
JSONObject privateLayoutsJSONObject = layoutsJSONObject.getJSONObject("private");

JSONArray publicPages = publicLayoutsJSONObject.getJSONArray("children");
JSONArray privatePages = privateLayoutsJSONObject.getJSONArray("children");
%>

<c:choose>
	<c:when test="<%= (publicPages.length() == 0) && (privatePages.length() == 0) %>">
		<p class="text-muted">
			<liferay-ui:message key="default-display-page-help" />
		</p>
	</c:when>
	<c:otherwise>
		<aui:nav-bar markupView="lexicon">
			<aui:nav cssClass="navbar-nav">
				<c:if test="<%= publicPages.length() > 0 %>">

					<%
					portletURL.setParameter("selectedTab", "public-pages");
					%>

					<aui:nav-item
						href="<%= portletURL.toString() %>"
						label="public-pages"
						selected='<%= selectedTab.equals("public-pages") %>'
					/>
				</c:if>

				<c:if test="<%= privatePages.length() > 0 %>">

					<%
					portletURL.setParameter("selectedTab", "private-pages");
					%>

					<aui:nav-item
						href="<%= portletURL.toString() %>"
						label="private-pages"
						selected='<%= selectedTab.equals("private-pages") %>'
					/>
				</c:if>
			</aui:nav>
		</aui:nav-bar>

		<aui:form cssClass="container-fluid-1280" name="selectDisplayPageFm">
			<aui:fieldset-group markupView="lexicon">
				<aui:fieldset>
					<div class="portlet-journal-tree" id="<portlet:namespace />displayPageContainer">
					</div>
				</aui:fieldset>
			</aui:fieldset-group>
		</aui:form>

		<aui:script require="journal-web/js/CardsTreeView.es,metal-dom/src/dom">
			var CardsTreeView = journalWebJsCardsTreeViewEs.default;
			var dom = metalDomSrcDom.default;

			new CardsTreeView(
				{
					events: {
						selectedNodesChanged: function(event) {
							var node = event.newVal[0];

							var data = {
								id: node.id,
								name: node.value
							};

							Liferay.Util.getOpener().Liferay.fire(
								'<%= HtmlUtil.escapeJS(eventName) %>',
								{
									data: data
								}
							);
						}
					},

					<c:choose>
						<c:when test='<%= selectedTab.equals("public-pages") %>'>
							nodes: [<%= publicLayoutsJSONObject.toString() %>],
						</c:when>
						<c:otherwise>
							nodes: [<%= privateLayoutsJSONObject.toString() %>],
						</c:otherwise>
					</c:choose>

					pathThemeImages: '<%= themeDisplay.getPathThemeImages() %>'
				},
				'#<portlet:namespace />displayPageContainer'
			);
		</aui:script>
	</c:otherwise>
</c:choose>
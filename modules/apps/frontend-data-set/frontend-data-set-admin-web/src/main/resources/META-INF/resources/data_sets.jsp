<%--
/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */
--%>

<%@ include file="/init.jsp" %>

<%
portletDisplay.setBeta(true);
%>

<portlet:actionURL name="/frontend_data_set_admin/customize_system_data_set" var="customizeURL">
	<portlet:param name="fdsName" value="{0}" />
	<portlet:param name="redirect" value="<%= currentURL %>" />
</portlet:actionURL>

<a href="<%= customizeURL %>">Customize</a>
<div>
	<react:component
		module="{DataSets} from frontend-data-set-admin-web"
		props='<%=
			HashMapBuilder.<String, Object>put(
				"customizableDataSets", fdsAdminDisplayContext.getCustomizableDataSets()
			).put(
				"customizeDataSetURL", <%= customizeURL %>
			).put(
				"editDataSetURL", fdsAdminDisplayContext.getEditDataSetURL()
			).put(
				"namespace", liferayPortletResponse.getNamespace()
			).put(
				"permissionsURL", fdsAdminDisplayContext.getDataSetPermissionsURL()
			).put(
				"resolvedRESTSchemas", fdsAdminDisplayContext.getRESTApplicationResolvedSchemasJSONArray()
			).put(
				"restApplications", fdsAdminDisplayContext.getRESTApplicationsJSONArray()
			).build()
		%>'
	/>
</div>
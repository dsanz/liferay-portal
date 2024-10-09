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
	<portlet:param name="fdsName" value="com_liferay_commerce_product_definitions_web_internal_portlet_CPDefinitionsPortlet-productDefinitions" />
</portlet:actionURL>

<a href="<%= customizeURL %>">Customize</a>
<div>
<react:component
	module="{DataSets} from frontend-data-set-admin-web"
	props='<%=
		HashMapBuilder.<String, Object>put(
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
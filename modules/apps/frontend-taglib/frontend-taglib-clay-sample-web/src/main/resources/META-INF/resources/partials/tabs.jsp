<%@ page import="com.liferay.portal.kernel.util.HashMapBuilder" %><%--
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

<h3>TABS hey</h3>

<clay:link
  additionalProps='<%=
  		HashMapBuilder.<String, Object>put(
  			"accountEntryName", "abc"
  		).build()
  	%>'

	href="http://www.google.com"
	icon="times-circle"
/>


<clay:tabs
	tabsItems="<%= tabsDisplayContext.getDefaultTabsItems() %>"
>
	<clay:tabs-panel>
    <clay:button label="One" propsTransformer='js/ClaySampleButtonPropsTransformer' />
  </clay:tabs-panel>
	<clay:tabs-panel>
    Tab Content 2
    <div>
      <clay:button label="Two" propsTransformer='js/ClaySampleButtonPropsTransformer' />
    </div>
  </clay:tabs-panel>
	<clay:tabs-panel>Tab Content 3</clay:tabs-panel>
	<clay:tabs-panel>Tab Content 4</clay:tabs-panel>
</clay:tabs>

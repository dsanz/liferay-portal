/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.client.extension.web.internal.frontend.data.set;

import com.liferay.client.extension.web.internal.constants.ClientExtensionAdminFDSNames;
import com.liferay.frontend.data.set.DataSet;
import com.liferay.petra.string.StringBundler;

import org.osgi.service.component.annotations.Component;

/**
 * @author Daniel Sanz
 */
@Component(
	property = "frontend.data.set.name=" + ClientExtensionAdminFDSNames.CLIENT_EXTENSION_TYPES,
	service = DataSet.class
)
public class CETDataSet implements DataSet {

	@Override
	public String getAdditionalAPIURLParameters() {
		return "";
	}

	@Override
	public String getName() {
		return "Client Extensions";
	}

	@Override
	public String getRESTApplication() {
		return StringBundler.concat(
			"/frontend-data-set-taglib/app/data-set",
			"/com_liferay_client_extension_web_internal_portlet_",
			"ClientExtensionAdminPortlet-clientExtensionTypes");
	}

	@Override
	public String getRESTEndpoint() {
		return StringBundler.concat(
			"/com_liferay_client_extension_web_internal_portlet_",
			"ClientExtensionAdminPortlet-clientExtensionTypes?groupId={siteId}",
			"&plid=1&portletId=com_liferay_client_extension_web_internal_",
			"portlet_ClientExtensionAdminPortlet");
	}

	@Override
	public String getRESTSchema() {
		return "DummyCETSChema";
	}

}
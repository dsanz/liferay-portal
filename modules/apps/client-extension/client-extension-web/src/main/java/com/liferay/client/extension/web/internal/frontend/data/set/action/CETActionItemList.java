/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.client.extension.web.internal.frontend.data.set.action;

import com.liferay.client.extension.web.internal.constants.ClientExtensionAdminFDSNames;
import com.liferay.frontend.data.set.DataSetEntityImportPolicy;
import com.liferay.frontend.data.set.action.FDSItemActionList;
import com.liferay.frontend.data.set.model.FDSActionDropdownItem;
import com.liferay.portal.kernel.exception.PortalException;

import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;

/**
 * @author Daniel Sanz
 */
@Component(
	property = "frontend.data.set.name=" + ClientExtensionAdminFDSNames.CLIENT_EXTENSION_TYPES,
	service = FDSItemActionList.class
)
public class CETActionItemList implements FDSItemActionList {

	@Override
	public List<FDSActionDropdownItem> getDropdownItems(
			HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse)
		throws PortalException {

		// as this is <classic-display>, actions are not required

		return Arrays.asList(
			new FDSActionDropdownItem(
				"PROXY", "view", "view", "Dummy for proxy actions", "get", null,
				null));
	}

	public DataSetEntityImportPolicy getImportPolicy() {
		return DataSetEntityImportPolicy.GROUP_PROXY;
	}

}
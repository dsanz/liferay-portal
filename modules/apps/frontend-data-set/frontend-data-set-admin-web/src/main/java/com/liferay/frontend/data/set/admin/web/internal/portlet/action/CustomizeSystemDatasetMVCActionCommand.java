/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */


package com.liferay.frontend.data.set.admin.web.internal.portlet.action;

import com.liferay.frontend.data.set.admin.web.internal.constants.FDSAdminPortletKeys;
import com.liferay.frontend.data.set.bridge.FDSObjectEntryProxyFactory;
import com.liferay.portal.kernel.portlet.bridges.mvc.BaseTransactionalMVCActionCommand;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCActionCommand;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.PortalUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;

/**
 * @author Daniel Sanz
 */
@Component(
	property = {
		"javax.portlet.name=" + FDSAdminPortletKeys.FDS_ADMIN,
		"mvc.command.name=/frontend_data_set_admin/customize_system_data_set"
	},
	service = MVCActionCommand.class
)
public class CustomizeSystemDatasetMVCActionCommand extends
	BaseTransactionalMVCActionCommand {

	@Override
	protected void doTransactionalCommand(
		ActionRequest actionRequest, ActionResponse actionResponse)
		throws Exception {

		String fdsName = ParamUtil.getString(actionRequest, "fdsName");

			_fdsObjectEntryProxyFactory.create(fdsName,
				PortalUtil.getHttpServletRequest(actionRequest));
	}

	@Reference
	private FDSObjectEntryProxyFactory _fdsObjectEntryProxyFactory;

}

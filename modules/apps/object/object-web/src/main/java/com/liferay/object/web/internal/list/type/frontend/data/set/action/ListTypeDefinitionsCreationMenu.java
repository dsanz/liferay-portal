/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.object.web.internal.list.type.frontend.data.set.action;

import com.liferay.frontend.data.set.DataSetEntityImportPolicy;
import com.liferay.frontend.data.set.action.FDSCreationMenu;
import com.liferay.frontend.taglib.clay.servlet.taglib.util.CreationMenu;
import com.liferay.list.type.model.ListTypeDefinition;
import com.liferay.object.web.internal.list.type.constants.ListTypeFDSNames;
import com.liferay.object.web.internal.list.type.display.context.ViewListTypeDefinitionsDisplayContext;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.security.permission.resource.ModelResourcePermission;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Daniel Sanz
 */
@Component(
	property = "frontend.data.set.name=" + ListTypeFDSNames.LIST_TYPE_DEFINITIONS,
	service = FDSCreationMenu.class
)
public class ListTypeDefinitionsCreationMenu implements FDSCreationMenu {

	@Override
	public CreationMenu getCreationMenu(
			HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse)
		throws PortalException {

		ViewListTypeDefinitionsDisplayContext
			viewListTypeDefinitionsDisplayContext =
				new ViewListTypeDefinitionsDisplayContext(
					httpServletRequest,
					_listTypeDefinitionModelResourcePermission);

		try {
			return viewListTypeDefinitionsDisplayContext.getCreationMenu();
		}
		catch (Exception exception) {
			if (_log.isWarnEnabled()) {
				_log.warn("Unable to get creation menu", exception);
			}
		}

		return new CreationMenu();
	}

	@Override
	public DataSetEntityImportPolicy getImportPolicy() {
		return DataSetEntityImportPolicy.GROUP_PROXY;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		ListTypeDefinitionsCreationMenu.class);

	@Reference(
		target = "(model.class.name=com.liferay.list.type.model.ListTypeDefinition)"
	)
	private ModelResourcePermission<ListTypeDefinition>
		_listTypeDefinitionModelResourcePermission;

}
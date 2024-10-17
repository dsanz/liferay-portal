/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.object.web.internal.list.type.frontend.data.set;

import com.liferay.frontend.data.set.DataSet;
import com.liferay.object.web.internal.list.type.constants.ListTypeFDSNames;

import org.osgi.service.component.annotations.Component;

/**
 * @author Daniel Sanz
 */
@Component(
	property = "frontend.data.set.name=" + ListTypeFDSNames.LIST_TYPE_DEFINITIONS,
	service = DataSet.class
)
public class ListTypeDefinitionsDataSet implements DataSet {

	@Override
	public String getAdditionalAPIURLParameters() {
		return "";
	}

	@Override
	public String getName() {
		return "Picklists";
	}

	@Override
	public String getRESTApplication() {
		return "/headless-admin-list-type/v1.0\n";
	}

	@Override
	public String getRESTEndpoint() {
		return "/v1.0/list-type-definitions";
	}

	@Override
	public String getRESTSchema() {
		return "ListTypeDefinition";
	}

}
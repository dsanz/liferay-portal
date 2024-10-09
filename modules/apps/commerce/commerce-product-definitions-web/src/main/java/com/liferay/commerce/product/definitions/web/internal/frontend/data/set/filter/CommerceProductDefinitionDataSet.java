/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */


package com.liferay.commerce.product.definitions.web.internal.frontend.data.set.filter;

import com.liferay.commerce.product.definitions.web.internal.constants.CommerceProductFDSNames;
import com.liferay.frontend.data.set.DataSet;
import org.osgi.service.component.annotations.Component;

/**
 * @author Daniel Sanz
 */

@Component(
	property = "frontend.data.set.name=" + CommerceProductFDSNames.PRODUCT_DEFINITIONS,
	service = DataSet.class
)
public class CommerceProductDefinitionDataSet implements DataSet {
	@Override
	public String getRESTApplication() {
		return "/headless-commerce-admin-catalog/v1.0";
	}

	@Override
	public String getRESTSchema() {
		return "Product";
	}

	@Override
	public String getRESTEndpoint() {
		return "/v1.0/products";
	}

	@Override
	public String getAdditionalAPIURLParameters() {
		return "nestedFields=skus,catalog";
	}

	@Override
	public String getName() {
		return "Product Definitions";
	}
}

/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.frontend.data.set;

/**
 * @author Daniel Sanz
 */
public interface DataSet {

	public String getAdditionalAPIURLParameters();

	public String getName();

	public String getRESTApplication();

	public String getRESTEndpoint();

	public String getRESTSchema();

}
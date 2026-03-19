/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */


package com.liferay.organizations.item.selector.util;

import com.liferay.petra.function.transform.TransformUtil;
import com.liferay.petra.string.StringUtil;
import java.util.List;

/**
 * @author Daniel Sanz
 */
public class OrganizationsItemSelectorCriterionUtil {
	public static String toString(long[] selectedOrganizationIds) {
		return StringUtil.merge(selectedOrganizationIds, "-");
	}

	public static long[] toLongArray(String selectedOrganizationIds) {
		List<String> ids = StringUtil.split(selectedOrganizationIds, '-');

		return TransformUtil.transformToLongArray(ids, Long::parseLong);
	}
}

/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.frontend.data.set.internal.sort;

import com.liferay.frontend.data.set.model.FDSSortItem;
import com.liferay.frontend.data.set.serializer.FDSSerializer;
import com.liferay.frontend.data.set.sort.FDSSortList;
import com.liferay.frontend.data.set.sort.FDSSortListRegistry;
import com.liferay.frontend.data.set.sort.FDSSortListSerializer;

import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Daniel Sanz
 */
@Component(
	property = "frontend.data.set.serializer.type=" + FDSSerializer.SYSTEM,
	service = FDSSortListSerializer.class
)
public class SystemFDSSortListSerializerImpl implements FDSSortListSerializer {

	@Override
	public List<FDSSortItem> serialize(
		String fdsName, HttpServletRequest httpServletRequest) {

		FDSSortList fdsSortList = _fdsSortListRegistry.getFDSSortList(fdsName);

		if (fdsSortList == null) {
			return Collections.emptyList();
		}

		return fdsSortList.getFDSSortItemList(httpServletRequest);
	}

	@Reference
	private FDSSortListRegistry _fdsSortListRegistry;

}
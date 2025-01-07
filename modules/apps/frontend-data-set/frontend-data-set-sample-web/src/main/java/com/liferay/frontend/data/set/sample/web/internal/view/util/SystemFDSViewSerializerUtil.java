/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.frontend.data.set.sample.web.internal.view.util;

import com.liferay.frontend.data.set.view.SystemFDSViewSerializer;
import com.liferay.portal.kernel.module.service.Snapshot;

/**
 * @author Marko Cikos
 */
public class SystemFDSViewSerializerUtil {

	public static SystemFDSViewSerializer getSystemFDSViewSerializer() {
		return _fdsViewSerializerSnapshot.get();
	}

	private static final Snapshot<SystemFDSViewSerializer>
		_fdsViewSerializerSnapshot = new Snapshot<>(
			SystemFDSViewSerializerUtil.class, SystemFDSViewSerializer.class);

}
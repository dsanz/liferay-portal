/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.frontend.data.set;

import java.util.Map;

/**
 * @author Daniel Sanz
 */
public interface SystemFDSEntryRegistry {

	public Map<String, SystemFDSEntry> getSystemFDSEntries();

	public SystemFDSEntry getSystemFDSEntry(String fdsName);

}
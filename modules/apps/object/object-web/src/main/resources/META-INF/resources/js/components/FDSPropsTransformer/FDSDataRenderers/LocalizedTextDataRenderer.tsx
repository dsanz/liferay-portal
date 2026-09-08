/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import getLocalizedFieldValue from './getLocalizedFieldValue';

interface LocalizedTextDataRendererProps {
	itemData: {[key: string]: any};
	options: {fieldName: string};
	value?: any;
}

export default function LocalizedTextDataRenderer({
	itemData,
	options,
	value,
}: LocalizedTextDataRendererProps) {
	const localizedFieldValue = getLocalizedFieldValue(
		itemData,
		options?.fieldName
	);

	return (
		(localizedFieldValue === undefined ? value : localizedFieldValue) ?? ''
	);
}

/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import type {FDSTableCellHTMLElementBuilder} from '@liferay/js-api/data-set';
import ClayIcon from '@clayui/icon';
import ClayTooltipProvider from '@clayui/tooltip';
import React from 'react';
import ReactDOM from 'react-dom';
import clm from 'country-locale-map';

const fdsCellRenderer: FDSTableCellHTMLElementBuilder = ({value}) => {
	const element = document.createElement('div');

	let locale = clm.getLocaleByName(value as string);

	console.log("country: " + value as string + ", locale: " + locale)

	locale = locale ? locale.replace("_", "-").toLowerCase() : "es-es";

	ReactDOM.render(
		<>
			<ClayIcon
				// @ts-ignore
				spritemap={((Liferay as any).Icons || {}).spritemap}
				symbol={`${locale}`}
			/>
			<ClayTooltipProvider>
				<span className="inline-item-after" data-tooltip-align="top" title={`${value}`}			>
					<ClayIcon
						// @ts-ignore
						spritemap={((Liferay as any).Icons || {}).spritemap}
						symbol={`${locale}`}
					/>
				</span>
			</ClayTooltipProvider>
		</>
,
element);

	return element;
};

export default fdsCellRenderer;

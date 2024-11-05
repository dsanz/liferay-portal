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

	let imperial = false;

	// @ts-ignore
	if (Liferay.ThemeDisplay.getBCP47LanguageId() === 'en-US') {
		imperial = true;
	}

	const unit = imperial ? 'mi' : 'km'

	let amount = imperial ? (Number(value) * 0.621371).toFixed(2) : Number(value);

	// strip decimals if they're 0
	if (!isNaN(amount as number)) {
		const truncated = Number(amount as number).toFixed(0);

		amount = Number(amount) === Number(truncated) ? truncated : amount
	}

	console.log("value: " + value + ", amount: " + amount + ", unit: " + unit)

	element.innerHTML = isNaN(amount as number) ? value as string : (amount + " " + unit);

	return element;
};

export default fdsCellRenderer;

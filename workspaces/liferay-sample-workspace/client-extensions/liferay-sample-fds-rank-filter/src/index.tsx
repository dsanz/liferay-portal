/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import type {
	FDSFilter,
	FDSFilterHTMLElementBuilderArgs,
} from '@liferay/js-api/data-set';
import ClaySlider from '@clayui/slider';
import ClayButton from '@clayui/button';
import {ClayToggle} from '@clayui/form';
import React, {useState, useEffect} from 'react';
import ReactDOM from 'react-dom';
import ClayButtonGroup from "@clayui/button/lib/Group";
// Declare the structure of the internal data that describes the filter state (in this case it will
// be the plain odata string the user enters through the filter's UI).

type Range = {
	from: number,
	to: number
}

let theFieldName = ""

function descriptionBuilder(selectedData: Range): string {
	return `from ${selectedData.from} to ${selectedData.to}`;
}

function htmlElementBuilder({
	fieldName,
	filter,
	setFilter,
}: FDSFilterHTMLElementBuilderArgs<Range>): HTMLElement {
	theFieldName = fieldName;
	const div = document.createElement('div');

	ReactDOM.render(<RangeSelector selectedData={filter.selectedData} setFilter={setFilter}/>, div);

	return div;
}

function RangeSelector({selectedData, setFilter}) {
	const [from, setFrom] = useState(selectedData?.from || 0);
	const [to, setTo] = useState(selectedData?.to || 0);
	const [lock, setLock] = useState(false);

	useEffect(() => {
		if (from > to) {
			setFrom(to)
		}

		if (lock) {
			setFilter({
				selectedData: {
					from,
					to
				}})
		}
	}, [to]);

	useEffect(() => {
		let newTo = to;

		if (to < from) {
			newTo = from;
			setTo(newTo)
		}

		if (lock) {
			setFilter({
				selectedData: {
					from,
					to: newTo
				}});
		}
	}, [from])

	return (
		<div className="form-group">
			<label htmlFor="slider-from">{"Minimum rank"}</label>
			<ClaySlider onChange={(value) => setFrom(value)} min={0} max={1000} defaultValue={from} value={from} id="slider-from"/>

			<label htmlFor="slider-to">{"Maximum rank"}</label>
			<ClaySlider onChange={(value) => setTo(value)}  min={0} max={1000} defaultValue={to} value={to} id="slider-to"/>

			<ClayButton disabled={lock}
				onClick={() => setFilter({
							selectedData: {
								from,
								to
							}
						})}
			>
				Apply
			</ClayButton>

			<ClayToggle label="Lock" onToggle={setLock} toggled={lock} />
		</div>
	);
}

function oDataQueryBuilder(selectedData: Range): string {
	return `${theFieldName} ge ${selectedData.from} and ${theFieldName} le ${selectedData.to}`;
}

const fdsFilter: FDSFilter<Range> = {
	descriptionBuilder,
	htmlElementBuilder,
	oDataQueryBuilder,
};

export default fdsFilter;

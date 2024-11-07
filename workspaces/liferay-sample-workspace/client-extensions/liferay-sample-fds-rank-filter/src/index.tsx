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
import ClayForm, {ClayToggle} from '@clayui/form';
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
let translations = {};

function debounce(func, wait, immediate=false) {
let timeout;

return () => {
	const context = this;
	const args = arguments;
	function later() {
		timeout = null;
		if (!immediate) {
			func.apply(context, args);
		}
	}
	const callNow = immediate && !timeout;

	clearTimeout(timeout);
	timeout = setTimeout(later, wait);
	if (callNow) {
		func.apply(context, args);
	}
};
}

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

function Text({label, defaultValue} : {label: string, defaultValue: string}) {
	// @ts-ignore
	const locale = Liferay.ThemeDisplay.getBCP47LanguageId();
	const [value, setValue] = useState((translations[locale] && translations[locale][label]) || defaultValue);

	useEffect(() => {
			if ((translations[locale] && translations[locale][label])) {
				return;
			}

			// @ts-ignore
			Liferay.Util.fetch(
				`/o/language/v1.0/messages?key=${label}&languageId=${locale}`
			).then((response) => response.json())
			.then(({value}) => {
				translations[locale] || (translations[locale] = {})

				translations[locale][label]= value
				setValue(value);
			});
		}
	);

	return (<>{value}</>);
}

function RangeSelector({selectedData, setFilter}) {
	const [from, setFrom] = useState(selectedData?.from || 0);
	const [to, setTo] = useState(selectedData?.to || 0);
	const [lock, setLock] = useState(false);

	useEffect(() => {
		let newFrom = from;

		if (from > to) {
			newFrom = to;
			setFrom(newFrom);
		}

		if (lock) {
			debounce(setFilter({
				selectedData: {
					from: newFrom,
					to
				}}), 250);
		}
	}, [to]);

	useEffect(() => {
		let newTo = to;

		if (to < from) {
			newTo = from;
			setTo(newTo);
		}

		if (lock) {
			debounce(setFilter({
				selectedData: {
					from,
					to: newTo
				}}), 250);
		}
	}, [from])

	return (
		<ClayForm style={{padding: '0.75rem'}}>
			<ClayForm.Group>
				<label htmlFor="slider-from">{<Text label={'minimum'} defaultValue={'Minimum'}/>}</label>
				<ClaySlider
					id="slider-from"
					defaultValue={from}
					min={0}
					max={1000}
					onChange={(value) => setFrom(value)}
					value={from}
				/>
			</ClayForm.Group>

			<ClayForm.Group>
				<label htmlFor="slider-to">{"Maximum rank"}</label>
				<ClaySlider
					id="slider-to"
					defaultValue={to}
					min={0}
					max={1000}
					onChange={(value) => setTo(value)}
					value={to}
				/>
			</ClayForm.Group>

			<ClayForm.Group>
				<ClayButton
					disabled={lock}
					onClick={() =>
						setFilter({
							selectedData: {
								from,
								to
							}
						})}
				>
					<Text label={'apply'} defaultValue={'Apply'}/>
				</ClayButton>

				<ClayToggle
					label={<Text label={'lock'} defaultValue={'Lock'}/>}
					onToggle={setLock}
					toggled={lock}
				/>
			</ClayForm.Group>
		</ClayForm>
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

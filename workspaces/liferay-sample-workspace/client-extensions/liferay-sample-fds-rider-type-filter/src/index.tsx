/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import type {
	FDSFilter,
	FDSFilterHTMLElementBuilderArgs,
} from '@liferay/js-api/data-set';
import ClayLayout from '@clayui/layout';
import ClayLabel from '@clayui/label';
import ClayButton from '@clayui/button';
import {React, useState} from 'react';
import ReactDOM from 'react-dom';

// Declare the structure of the internal data that describes the filter state (in this case it will
// be the plain odata string the user enters through the filter's UI).

type FilterData = string;

enum RiderType {
	CLimber = "Climber",
	Sprinter = "Sprinter",
	TimeTrialist = "Time Trialist"
}

function descriptionBuilder(selectedData: FilterData): string {
	return selectedData;
}

function htmlElementBuilder({
	fieldName,
	filter,
	setFilter,
}: FDSFilterHTMLElementBuilderArgs<FilterData>): HTMLElement {
	const div = document.createElement('div');

	ReactDOM.render(<Filter fieldName={fieldName} setFilter={setFilter}/>, div);

	return div;
}



function Filter({fieldName, setFilter}) {
	const [selection, setSelection] = useState();

	useEffect(() => {
		let expression = '';

		
			selection

		setFilter({
			selectedData: `${fieldName} lt 5`,
		})
	}, []);


	return (
		<ClayLayout.Row>
			for (riderType: RiderType) {
				<ClayButton displayType="secondary"
						onClick={() => {

						}}
				>
					<ClayLabel displayType={'success'}>
						r
					</ClayLabel>
				</ClayButton>
			}
		</ClayLayout.Row>
	);
}

function htmlElementBuilderOld({
	fieldName,
	filter,
	setFilter,
}: FDSFilterHTMLElementBuilderArgs<FilterData>): HTMLElement {
	const input = document.createElement('input');

	if (filter.selectedData) {
		input.value = filter.selectedData;
	}
	else {
		input.value = `${fieldName} eq ...`;
	}

	input.className = 'form-control';
	input.placeholder = 'Search with Odata';

	const button = document.createElement('button');

	button.className = 'btn btn-block btn-secondary btn-sm mt-2';
	button.innerText = 'Submit';
	button.onclick = () =>
		setFilter({
			selectedData: input.value,
		});

	const div = document.createElement('div');

	div.className = 'dropdown-item';

	div.appendChild(input);
	div.appendChild(button);

	return div;
}

function oDataQueryBuilder(selectedData: FilterData): string {
	return selectedData;
}

const fdsFilter: FDSFilter<FilterData> = {
	descriptionBuilder,
	htmlElementBuilder,
	oDataQueryBuilder,
};

export default fdsFilter;

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
import React, {useState, useEffect} from 'react';
import ReactDOM from 'react-dom';

// Declare the structure of the internal data that describes the filter state (in this case it will
// be the plain odata string the user enters through the filter's UI).

enum RiderType {
	Climber = "Climber",
	Sprinter = "Sprinter",
	TimeTrialist = "Time Trialist"
}

let theFieldName = ""

function descriptionBuilder(selectedData: RiderType): string {
	return selectedData;
}

function htmlElementBuilder({
	fieldName,
	filter,
	setFilter,
}: FDSFilterHTMLElementBuilderArgs<RiderType>): HTMLElement {
	theFieldName = fieldName;
	const div = document.createElement('div');

	ReactDOM.render(<Filter selectedData={filter.selectedData} fieldName={fieldName} setFilter={setFilter}/>, div);

	return div;
}

function Filter({selectedData, fieldName, setFilter}) {
	const [selection, setSelection] = useState(selectedData);

	useEffect(() => {
		setFilter({
			selectedData: selection
		})
	}, [selection]);

	return (
		<ClayLayout.Row size={12}>
			Hello
			{ Object.keys(RiderType).map(
				(riderType) => (
					<ClayButton displayType={selection == riderType ? "primary" : "secondary"}
						onClick={() => setSelection(riderType)}
					>
						<ClayLabel displayType={'success'}>
							{riderType}
						</ClayLabel>
					</ClayButton>
				))
			}
		</ClayLayout.Row>
	);
}

function oDataQueryBuilder(selectedData: RiderType): string {
	let expression = "";

	if (selectedData == RiderType.Climber) {
		expression = `${theFieldName} le 5`;
	}
	else if (selectedData == RiderType.Sprinter) {
		expression = `${theFieldName} ge 5 and ${theFieldName} le 100`;
	}
	else if (selectedData == RiderType.TimeTrialist) {
		expression = `${theFieldName} ge 100`;
	}
	return expression;
}

const fdsFilter: FDSFilter<RiderType> = {
	descriptionBuilder,
	htmlElementBuilder,
	oDataQueryBuilder,
};

export default fdsFilter;

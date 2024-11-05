/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import type {
	FDSFilter,
	FDSFilterHTMLElementBuilderArgs,
} from '@liferay/js-api/data-set';
import ClayLabel from '@clayui/label';
import ClayButton from '@clayui/button';
import React, {useState, useEffect} from 'react';
import ReactDOM from 'react-dom';
// @ts-ignore
import AllRounder from './assets/allrounder.png';
// @ts-ignore
import Climber from './assets/climber.png';
// @ts-ignore
import Sprinter from './assets/sprinter.png';

// Declare the structure of the internal data that describes the filter state (in this case it will
// be the plain odata string the user enters through the filter's UI).

enum RiderType {
	AllRounder = "AllRounder",
	Climber = "Climber",
	Sprinter = "Sprinter",
	None = "None"
}

const RiderTypeDescriptions = {
	[`${RiderType.AllRounder}`]: "All Rounders",
	[`${RiderType.Climber}`]: "Climbers",
	[`${RiderType.Sprinter}`]: "Sprinters",
	[`${RiderType.None}`]: "Everyone",

}

const RiderTypeImages = {
	[`${RiderType.AllRounder}`]: AllRounder,
	[`${RiderType.Climber}`]: Climber,
	[`${RiderType.Sprinter}`]: Sprinter,
}

let theFieldName = ""

function descriptionBuilder(selectedData: RiderType): string {
	return RiderTypeDescriptions[selectedData];
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
		});
	}, [selection]);

	return (
		<ClayButton.Group spaced={true} vertical={true}>
			{ Object.keys(RiderType).filter((riderType) => riderType!= RiderType.None).map(
				(riderType) => (
					<ClayButton displayType={selection == riderType ? "primary" : "secondary"}
						onClick={() => {
							if (selection != riderType) {
								setSelection(riderType)
							}
							else {
								setSelection(RiderType.None);
							}
						}
					}
					>
						<ClayLabel displayType={'success'}>
							{RiderTypeDescriptions[riderType]}
						</ClayLabel>
						<img src={RiderTypeImages[riderType]} width='25%'/>
					</ClayButton>
				))
			}

			<ClayButton borderless
				onClick={() => setSelection(RiderType.None)}
			>
				Clear
			</ClayButton>

		</ClayButton.Group>
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
	else if (selectedData == RiderType.AllRounder) {
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

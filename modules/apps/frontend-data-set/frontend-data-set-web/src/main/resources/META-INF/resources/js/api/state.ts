/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

// What the data set adds to the state on top of the published contract. It
// stays here, rather than in "@liferay/js-api", because only the data set
// deals with it: a consumer influences the data set through the FDSConnection
// API alone, and the package publishes the shared contracts only.

import type {
	FDSFilterEntityFieldType,
	FDSState,
} from '@liferay/js-api/data-set';

/**
 * The whole shape of the data set atom, as the connection reads it: the
 * published state, plus the filters the data set declares in its
 * configuration.
 *
 * Those filters are absent from `FDSState`, which is what the atom is bound
 * to, and that is what makes writing them a compile error rather than a
 * convention.
 *
 * `filters` is readonly, like the rest of the state once it is read: the
 * connection reads this shape through `Liferay.State.read()`, which hands
 * back a deep frozen value.
 */
export interface FDSAtomState extends FDSState {
	readonly filters?: ReadonlyArray<FDSAtomStateFilter>;
}

/**
 * A declared filter as it sits in the state, which is how the data set
 * serializes its configuration and then tracks what the user picked. The
 * connection resolves this into the `FDSFilterInfo` a consumer reads: the
 * members below are unset or zeroed depending on the filter type and on
 * whether the filter is applied, and only some of them mean anything to a
 * consumer.
 */
interface FDSAtomStateFilter {
	readonly active?: boolean;
	readonly apiURL?: string;
	readonly autocompleteEnabled?: boolean;
	readonly entityFieldType: FDSFilterEntityFieldType;
	readonly id: string;
	readonly inputPlaceholder?: string;
	readonly itemKey?: string;
	readonly itemLabel?: string;
	readonly items?: ReadonlyArray<FDSAtomStateFilterItem>;
	readonly label: string;
	readonly max?: FDSAtomStateFilterDate | 'now';
	readonly min?: FDSAtomStateFilterDate | 'now';
	readonly multiple?: boolean;
	readonly odataFilterString?: string;

	/**
	 * What the configuration picks on the data set's behalf. The data set
	 * copies it into `selectedData` while preloading the filters, so a filter
	 * with preloaded data starts out applied.
	 */
	readonly preloadedData?: FDSAtomStateFilterData | null;

	/**
	 * What is picked now, which the data set clears whenever the filter is
	 * deactivated. Shaped after the filter type: selected items for a
	 * selection filter, a range for a date one.
	 */
	readonly selectedData?: FDSAtomStateFilterData | null;
	readonly type: FDSAtomStateFilterType;
}

/**
 * The filter types the data set holds in its state. A `clientExtension`
 * filter never reaches a consumer through the connection: it reaches its own
 * extension through the `FDSFilter` contract, which describes both what it
 * matches and how it draws it.
 */
type FDSAtomStateFilterType =
	| 'clientExtension'
	| 'dateRange'
	| 'dateTimeRange'
	| 'selection';

interface FDSAtomStateFilterData {
	readonly exclude?: boolean;
	readonly from?: FDSAtomStateFilterDate | null;
	readonly selectedItems?: ReadonlyArray<FDSAtomStateFilterItem>;
	readonly to?: FDSAtomStateFilterDate | null;
}

interface FDSAtomStateFilterDate {
	readonly day?: number;
	readonly hour?: number;
	readonly minute?: number;
	readonly month?: number;
	readonly offset?: string;
	readonly year?: number;
}

interface FDSAtomStateFilterItem {
	readonly label?: string;
	readonly value: string;
}

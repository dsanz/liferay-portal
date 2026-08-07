/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {getFDSAtom, getOrCreateSelector} from './getFDSAtom';

import type {
	FDSConnectionFilter,
	FDSConnectionInfo,
	FDSConnectionOptions,
	FDSConnectionStatus,
	FDSFilterDate,
	FDSFilterDateBound,
	FDSFilterDateSelection,
	FDSFilterInfo,
	FDSSelectionFilterItem,
	FDSSelectionFilterSelection,
	FDSState,
	FDSStateChangeCallback,
} from '@liferay/js-api/data-set';

import type {FDSAtomState} from './state';
import Atom = Liferay.State.Atom;

const DEFAULT_TIMEOUT = 10000;

interface Subscriptions {
	search: {dispose: () => void};
}

interface Selectors {
	search: Liferay.State.Selector<string>;
}

type FDSAtomStateFilter = NonNullable<FDSAtomState['filters']>[number];

type FDSAtomStateFilterData = FDSAtomStateFilter['selectedData'];

type FDSAtomStateFilterDate = NonNullable<FDSAtomStateFilterData>['from'];

/**
 * A date the data set would ignore reads as absent: it zeroes the parts of a
 * bound it does not mean to apply.
 */
function toFilterDate(
	date: FDSAtomStateFilterDate | undefined
): FDSFilterDate | null {
	if (!date?.year) {
		return null;
	}

	const {day, hour, minute, month, offset, year} = date;

	return {
		day: day ?? 0,
		month: month ?? 0,
		year,
		...(hour === undefined ? {} : {hour}),
		...(minute === undefined ? {} : {minute}),
		...(offset === undefined ? {} : {offset}),
	};
}

function toFilterDateBound(
	bound: FDSAtomStateFilter['max']
): FDSFilterDateBound | null {

	// The data set serializes a bound that follows the clock as "now".

	if (typeof bound === 'string') {
		return 'now';
	}

	return toFilterDate(bound);
}

function toFilterDateSelection(
	data: FDSAtomStateFilterData
): FDSFilterDateSelection | null {
	const from = toFilterDate(data?.from);
	const to = toFilterDate(data?.to);

	if (!from && !to) {
		return null;
	}

	return {from, to};
}

/**
 * Selected items reach the state without a label when the data set restores
 * them from the URL, so they are named after the values the filter offers.
 */
function toSelectionFilterSelection(
	data: FDSAtomStateFilterData,
	items: Array<FDSSelectionFilterItem>
): FDSSelectionFilterSelection | null {
	if (!data?.selectedItems?.length) {
		return null;
	}

	return {
		exclude: Boolean(data.exclude),
		items: data.selectedItems.map(({label, value}) => ({
			label:
				label ??
				items.find((item) => item.value === value)?.label ??
				value,
			value,
		})),
	};
}

/**
 * Resolves a filter the data set declares into the shape a consumer reads,
 * which describes what the filter matches rather than how the data set tracks
 * it. Every member is copied out of the state, so the result owns its data:
 * the state it came from is deep frozen, and it goes on changing while this
 * snapshot must not.
 */
function toFilterInfo(
	fdsAtomStateFilter: FDSAtomStateFilter
): FDSFilterInfo | null {
	const {
		active,
		entityFieldType,
		id,
		label,
		odataFilterString,
		preloadedData,
		selectedData,
		type,
	} = fdsAtomStateFilter;

	const filterInfo = {
		active: Boolean(active),
		entityFieldType,
		id,
		label,
		odataFilterString: odataFilterString ?? '',
	};

	if (type === 'dateRange' || type === 'dateTimeRange') {
		return {
			...filterInfo,
			max: toFilterDateBound(fdsAtomStateFilter.max),
			min: toFilterDateBound(fdsAtomStateFilter.min),
			preselection: toFilterDateSelection(preloadedData),
			selection: toFilterDateSelection(selectedData),
			type,
		};
	}

	if (type === 'selection') {
		const {
			apiURL,
			autocompleteEnabled,
			inputPlaceholder,
			itemKey,
			itemLabel,
		} = fdsAtomStateFilter;

		const items = (fdsAtomStateFilter.items ?? []).map(
			({label, value}) => ({
				label: label ?? value,
				value,
			})
		);

		return {
			...filterInfo,
			autocomplete:
				autocompleteEnabled && apiURL
					? {
							apiURL,
							itemKey: itemKey ?? '',
							itemLabel: itemLabel ?? '',
							placeholder: inputPlaceholder ?? '',
						}
					: null,
			items,
			multiple: Boolean(fdsAtomStateFilter.multiple),
			preselection: toSelectionFilterSelection(preloadedData, items),
			selection: toSelectionFilterSelection(selectedData, items),
			type,
		};
	}

	// Whatever is left is rendered by a client extension, which reaches its
	// own extension through the FDSFilter contract instead.

	return null;
}

function toFilterInfos(
	fdsAtomState: Liferay.State.Immutable<FDSAtomState>
): Array<FDSFilterInfo> {
	return (fdsAtomState.filters ?? [])
		.map(toFilterInfo)
		.filter(
			(filterInfo): filterInfo is FDSFilterInfo => filterInfo !== null
		);
}

export class FDSConnection {
	private static instanceCount = 0;

	private atom!: Atom<FDSState>;
	private clearFiltersWhenDisconnect = false;
	private disconnected = false;
	private fdsName: string;
	private filters: Array<FDSFilterInfo> | null = null;
	private instanceId: number = ++FDSConnection.instanceCount;
	private isReady = false;
	private navigationHandle: {detach: () => void};
	private onFDSConnectionInfoChange: (
		fdsConnectionInfo: FDSConnectionInfo
	) => void;
	private selectors!: Selectors;
	private subscriptions!: Subscriptions;

	constructor(
		fdsName: string,
		fdsStateChangeCallback: FDSStateChangeCallback,
		onFDSConnectionInfoChange: (
			fdsConnectionInfo: FDSConnectionInfo
		) => void,
		options: FDSConnectionOptions = {}
	) {
		this.fdsName = fdsName;
		this.onFDSConnectionInfoChange = onFDSConnectionInfoChange;
		this.notifyStatus('connecting');

		getFDSAtom(fdsName, {timeout: options.timeout ?? DEFAULT_TIMEOUT})
			.then((atom: Atom<FDSState>) => {
				if (this.disconnected) {
					return;
				}

				this.atom = atom;

				this.selectors = {
					search: getOrCreateSelector(
						`${atom.key}_searchQuery`,
						(get) => get(atom).search.query
					),
				};

				// the filters the data set declares are fixed: it shows no
				// filter UI while a connection drives the filtering, so one
				// snapshot covers everything a consumer needs to know

				const fdsAtomState: Liferay.State.Immutable<FDSAtomState> =
					Liferay.State.read(atom);

				this.filters = toFilterInfos(fdsAtomState);

				// mark connection as ready, so getters/setters are unblocked and available to callbacks

				this.isReady = true;

				this.subscriptions = {
					search: Liferay.State.subscribe(
						this.selectors.search,
						fdsStateChangeCallback.search
					),
				};

				// initialize consumer's state

				fdsStateChangeCallback.search(this.getSearch() || '');

				// then inform consumer everything is settled

				this.notifyStatus('ready');
			})
			.catch((error: Error) => {
				if (this.disconnected) {
					return;
				}

				this.warn(
					'Connection timed out for ' + fdsName + ': ' + error.message
				);

				this.notifyStatus('timeout');
			});

		// ensure consumers don't need to dispose the subscriptions on SPA navigations

		this.navigationHandle = Liferay.on('beforeNavigate', () => {
			this.disconnect();
		});
	}

	getSearch = (): string | null => {
		if (!this.isReady) {
			return null;
		}

		return Liferay.State.read(this.selectors.search);
	};

	setSearch = (query: string): void => {
		if (!this.isReady) {
			return;
		}

		const current = Liferay.State.read(this.atom);

		Liferay.State.write(this.atom, {
			...current,
			search: {...current.search, query},
		});
	};

	/**
	 * The filters the data set declares in its configuration, as they stood
	 * when the connection became ready. Filtering belongs either to the data
	 * set or to the consumer, never to both, so these never change behind the
	 * consumer's back: they are here to be obeyed, or ignored, by whoever
	 * takes the filtering over through `setFilters()`.
	 *
	 * Every call hands over its own copy, so that a consumer working on what
	 * it got back cannot reach the snapshot the next call returns.
	 */
	getFilters = (): Array<FDSFilterInfo> | null => {
		if (!this.isReady || !this.filters) {
			return null;
		}

		return this.filters.map((filter) => ({...filter}));
	};

	/**
	 * Takes the filtering over with the given expressions, replacing whatever
	 * a previous call passed. From the first call on, the filters the data set
	 * declares no longer reach the request: the consumer owns the whole filter
	 * expression, and obeys the declared filters by including the ones it
	 * wants in the set it passes here.
	 */
	setFilters = (filters: Array<FDSConnectionFilter>): void => {
		if (!this.isReady) {
			return;
		}

		const current = Liferay.State.read(this.atom);

		this.clearFiltersWhenDisconnect = true;

		Liferay.State.write(this.atom, {
			...current,
			connectionFilters: filters.map(({id, odataFilterString}) => ({
				id,
				odataFilterString,
			})),
		});
	};

	/**
	 * Drops the filters this connection applies, so that the data set filters
	 * nothing: a shortcut for `setFilters([])`, and what `disconnect()` does
	 * on the way out. The filtering stays taken over, so the filters the data
	 * set declares do not come back.
	 */
	clearFilters = (): void => {
		this.setFilters([]);
	};

	disconnect = (): void => {
		if (this.disconnected) {
			return;
		}

		// Leave nothing of this connection applied: a consumer that never
		// filtered must not suppress the filters the data set declares on its
		// way out, so only a connection that did take the filtering over
		// clears it.

		if (this.clearFiltersWhenDisconnect) {
			this.clearFilters();
		}

		this.subscriptions?.search?.dispose();
		this.disconnected = true;
		this.isReady = false;
		this.filters = null;
		this.navigationHandle.detach();
		this.notifyStatus('disconnected');
	};

	private warn(msg: string): void {
		console.warn('[FDSConnection', this.instanceId, ']', msg);
	}

	private notifyStatus(status: FDSConnectionStatus): void {
		this.onFDSConnectionInfoChange({
			fdsName: this.fdsName,
			instanceId: this.instanceId,
			status,
		});
	}
}

/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

// The "@liferay/frontend-data-set-web/api" import-map module is resolved at
// runtime by the portal. At build time, tsconfig "paths" redirects it to the
// types provided by "@liferay/js-api", so the value and its types come from a
// single import.

// Pair this element with a data set whose "showFilters" prop is false, set
// through a props transformer: filtering belongs either to the data set or to
// this element, never to both. From the first setFilters() call on, the
// filters the data set declares no longer reach the request, so its dropdown
// would no longer tell the truth.

import {
	FDSConnection,
	FDSConnectionFilter,
	FDSConnectionInfo,
	FDSConnectionStatus,
	FDSFilterDate,
	FDSFilterDateBound,
	FDSFilterInfo,
} from '@liferay/frontend-data-set-web/api';
import React, {useEffect, useRef, useState} from 'react';

interface AppProps {
	fdsName: string;
}

const PLACEHOLDERS: Record<FDSConnectionStatus, string> = {
	connecting: 'waiting',
	disconnected: 'Search is not available',
	ready: 'Type search query...',
	timeout: 'Search is not available',
};

const pad = (value: number) => String(value).padStart(2, '0');

function formatDate(date: FDSFilterDate | null): string {
	if (!date) {
		return 'any';
	}

	const {day, hour, minute, month, year} = date;

	const time = hour === undefined ? '' : ` ${pad(hour)}:${pad(minute ?? 0)}`;

	return `${year}-${pad(month)}-${pad(day)}${time}`;
}

function formatDateBound(bound: FDSFilterDateBound | null): string {
	return bound === 'now' ? 'now' : formatDate(bound);
}

/**
 * What a filter matches, read off the description the data set hands over.
 * Narrowing on the type is what gives access to it.
 */
function describeFilter(filter: FDSFilterInfo): string {
	if (filter.type === 'selection') {
		if (filter.autocomplete) {
			return `values from ${filter.autocomplete.apiURL}`;
		}

		return `values: ${filter.items.map(({label}) => label).join(', ')}`;
	}

	return `between ${formatDateBound(filter.min)} and ${formatDateBound(
		filter.max
	)}`;
}

/**
 * What the data set would have filtered by on its own, which is where a
 * consumer starts from to behave the way it would have.
 */
function describePreselection(filter: FDSFilterInfo): string | null {
	if (filter.type === 'selection' && filter.preselection) {
		const {exclude, items} = filter.preselection;

		return `${exclude ? 'all but ' : ''}${items
			.map(({label}) => label)
			.join(', ')}`;
	}

	if (
		(filter.type === 'dateRange' || filter.type === 'dateTimeRange') &&
		filter.preselection
	) {
		const {from, to} = filter.preselection;

		return `${formatDate(from)} to ${formatDate(to)}`;
	}

	return null;
}

function App({fdsName}: AppProps) {
	const [customExpression, setCustomExpression] = useState('');
	const [disabled, setDisabled] = useState<boolean>(true);
	const [declaredFilters, setDeclaredFilters] = useState<
		Array<FDSFilterInfo>
	>([]);
	const [obeyedIds, setObeyedIds] = useState<Array<string> | null>(null);
	const [placeholder, setPlaceholder] = useState<string>(
		PLACEHOLDERS.connecting
	);
	const [query, setQuery] = useState('');
	const fdsConnectionRef = useRef<FDSConnection | null>(null);

	useEffect(() => {
		fdsConnectionRef.current = new FDSConnection(
			fdsName,
			{
				search: (query: string) => {
					setQuery(query);
				},
			},
			(fdsConnectionInfo: FDSConnectionInfo) => {
				setPlaceholder(PLACEHOLDERS[fdsConnectionInfo.status]);
				setDisabled(fdsConnectionInfo.status !== 'ready');

				// The filters the data set declares never change while this
				// element drives the filtering, so reading them once the
				// connection is ready is all it takes.

				if (fdsConnectionInfo.status === 'ready') {
					setDeclaredFilters(
						fdsConnectionRef.current?.getFilters() ?? []
					);
				}
			}
		);

		return () => {
			if (fdsConnectionRef?.current) {
				fdsConnectionRef?.current.disconnect();
				fdsConnectionRef.current = null;
			}
		};
	}, [fdsName]);

	// Until anything is checked or unchecked, the data set's own selection is
	// what this sample would apply.

	const isObeyed = ({active, id}: FDSFilterInfo) =>
		obeyedIds ? obeyedIds.includes(id) : active;

	const handleSearch = () => {
		fdsConnectionRef.current?.setSearch(query);
	};

	const handleApplyFilters = () => {
		const connectionFilters: Array<FDSConnectionFilter> = declaredFilters
			.filter((filter) => !!filter.odataFilterString && isObeyed(filter))
			.map(({id, odataFilterString}) => ({id, odataFilterString}));

		if (customExpression.trim()) {
			connectionFilters.push({
				id: 'custom',
				odataFilterString: customExpression.trim(),
			});
		}

		fdsConnectionRef.current?.setFilters(connectionFilters);
	};

	const handleClearFilters = () => {
		setCustomExpression('');
		setObeyedIds([]);

		fdsConnectionRef.current?.clearFilters();
	};

	const toggleFilter = (filter: FDSFilterInfo) => {
		const ids = declaredFilters.filter(isObeyed).map(({id}) => id);

		setObeyedIds(
			isObeyed(filter)
				? ids.filter((id) => id !== filter.id)
				: [...ids, filter.id]
		);
	};

	return (
		<div style={{display: 'grid', gap: '1rem', padding: '1rem'}}>
			<div style={{display: 'flex', gap: '0.5rem'}}>
				<input
					className="form-control"
					disabled={disabled}
					onChange={(event) => setQuery(event.target.value)}
					onKeyDown={(event) => {
						if (event.key === 'Enter') {
							handleSearch();
						}
					}}
					placeholder={placeholder}
					style={{flex: 1}}
					type="text"
					value={query}
				/>

				<button
					className="btn btn-primary"
					disabled={disabled}
					onClick={handleSearch}
					type="button"
				>
					Search
				</button>
			</div>

			<div>
				<strong>Filters declared in the data set</strong>

				{declaredFilters.length ? (
					declaredFilters.map((filter) => (
						<div className="form-check" key={filter.id}>
							<label>
								<input
									checked={isObeyed(filter)}
									className="form-check-input"
									disabled={
										disabled || !filter.odataFilterString
									}
									onChange={() => toggleFilter(filter)}
									type="checkbox"
								/>

								{filter.label}

								<code style={{marginLeft: '0.5rem'}}>
									{filter.odataFilterString ||
										'(not applied by the data set)'}
								</code>
							</label>

							<small style={{display: 'block'}}>
								{describeFilter(filter)}

								{describePreselection(filter)
									? `, preselected: ${describePreselection(filter)}`
									: ''}
							</small>
						</div>
					))
				) : (
					<p>This data set declares no filter.</p>
				)}
			</div>

			<div style={{display: 'flex', gap: '0.5rem'}}>
				<input
					className="form-control"
					disabled={disabled}
					onChange={(event) =>
						setCustomExpression(event.target.value)
					}
					placeholder="Filter with OData, such as name eq 'Liferay'"
					style={{flex: 1}}
					type="text"
					value={customExpression}
				/>

				<button
					className="btn btn-primary"
					disabled={disabled}
					onClick={handleApplyFilters}
					type="button"
				>
					Apply filters
				</button>

				<button
					className="btn btn-secondary"
					disabled={disabled}
					onClick={handleClearFilters}
					type="button"
				>
					Clear filters
				</button>
			</div>
		</div>
	);
}

export default App;

/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {IStateInURL} from './types';

function getStateParamName(id: string): string {
	return `fds_state_${id}`;
}

export function getStateFromURL(id: string): Partial<IStateInURL> | null {
	const params = new URLSearchParams(window.location.search);

	const stateParam = params.get(getStateParamName(id));

	if (!stateParam) {
		return null;
	}

	let state = {};

	try {
		state = JSON.parse(stateParam);
	}
	catch (error) {
		return null;
	}

	return state;
}

export function writeStateInURL(id: string, state: Partial<IStateInURL>) {
	if (!state) {
		return;
	}

	const params = new URLSearchParams(window.location.search);

	const currentState = getStateFromURL(id);

	params.set(
		getStateParamName(id),
		JSON.stringify({...(currentState || {}), ...state})
	);

	if (!currentState) {
		window.history.replaceState({}, '', `?${params.toString()}`);
	}
	else {
		window.history.pushState({}, '', `?${params.toString()}`);
	}
}

export function getDeltaFromURL(
	stateFromURL: Partial<IStateInURL> | null
): number | null {
	const delta = stateFromURL?.delta;

	if (!stateFromURL || !delta || isNaN(delta) || delta < 1) {
		return null;
	}

	return delta;
}

export function getFiltersFromURL(
	stateFromURL: Partial<IStateInURL> | null
): Array<any> | null {
	const filters = stateFromURL?.filters;

	if (!stateFromURL || !filters) {
		return null;
	}

	return filters;
}

export function getViewNameFromURL(
	stateFromURL: Partial<IStateInURL> | null
): string | null {
	if (!stateFromURL?.view) {
		return null;
	}

	return stateFromURL?.view;
}

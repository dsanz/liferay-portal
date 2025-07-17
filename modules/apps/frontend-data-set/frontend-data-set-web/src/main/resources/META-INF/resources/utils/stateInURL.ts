/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {IStateInURL} from './types';

export function getStateFromURL(): Partial<IStateInURL> | null {
	const params = new URLSearchParams(window.location.search);

	const stateParam = params.get('state');

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

export function writeStateInURL(state: Partial<IStateInURL>) {
	if (!state) {
		return;
	}

	const params = new URLSearchParams(window.location.search);

	const currentState = getStateFromURL();

	params.set('state', JSON.stringify({...(currentState || {}), ...state}));

	if (!currentState) {
		window.history.replaceState({}, '', `?${params.toString()}`);
	}
	else {
		window.history.pushState({}, '', `?${params.toString()}`);
	}
}

export function getDeltaFromURL(): number | null {
	const state = getStateFromURL();

	const delta = state?.delta;

	if (!state || !delta || isNaN(delta) || delta < 1) {
		return null;
	}

	return delta;
}

export function getFiltersFromURL(): Array<any> | null {
	const state = getStateFromURL();

	const filters = state?.filters;

	if (!state || !filters) {
		return null;
	}

	return filters;
}

export function getViewNameFromURL(): string | null {
	const state = getStateFromURL();

	if (!state?.view) {
		return null;
	}

	return state?.view;
}

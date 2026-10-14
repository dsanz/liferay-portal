/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import loadGoogleMaps from '../../src/main/resources/META-INF/resources/js/loadGoogleMaps';

describe('loadGoogleMaps', () => {
	const SCRIPT_SELECTOR = 'script[src*="maps.googleapis.com/maps/api/js"]';

	const getScripts = () => document.querySelectorAll(SCRIPT_SELECTOR);

	afterEach(() => {
		getScripts().forEach((script) => script.remove());
	});

	beforeEach(() => {
		delete window.google;

		window.Liferay = {
			Maps: {},
			detach: jest.fn(),
			fire: jest.fn(),
			namespace: jest.fn(() => window.Liferay.Maps),
			once: jest.fn(),
		};
	});

	it('calls back right away when the API is already loaded', () => {
		window.google = {maps: {}};
		window.Liferay.Maps.gmapsReady = true;

		const callback = jest.fn();

		loadGoogleMaps(undefined, callback);

		expect(callback).toHaveBeenCalledTimes(1);
		expect(getScripts()).toHaveLength(0);
		expect(window.Liferay.once).not.toHaveBeenCalled();
	});

	it('clears the loading flag and fires the ready event once the API is ready', () => {
		loadGoogleMaps(undefined, jest.fn());

		expect(window.Liferay.Maps.gmapsLoading).toBe(true);

		window.Liferay.Maps.onGMapsReady();

		expect(window.Liferay.Maps.gmapsLoading).toBe(false);
		expect(window.Liferay.Maps.gmapsReady).toBe(true);
		expect(window.Liferay.fire).toHaveBeenCalledWith('gmapsReady');
	});

	it('detaches the callback with the returned function', () => {
		const callback = jest.fn();

		const detach = loadGoogleMaps(undefined, callback);

		detach();

		expect(window.Liferay.detach).toHaveBeenCalledWith(
			'gmapsReady',
			callback
		);
	});

	it('includes the API key in the script URL', () => {
		loadGoogleMaps('abc123', jest.fn());

		const [script] = getScripts();

		expect(script.getAttribute('src')).toContain('&key=abc123');
	});

	it('injects the script only once for two callers', () => {
		loadGoogleMaps(undefined, jest.fn());
		loadGoogleMaps(undefined, jest.fn());

		expect(getScripts()).toHaveLength(1);
		expect(window.Liferay.once).toHaveBeenCalledTimes(2);
	});

	it('reinjects the script after a failed load', () => {
		loadGoogleMaps(undefined, jest.fn());

		const [script] = getScripts();

		script.dispatchEvent(new Event('error'));

		expect(window.Liferay.Maps.gmapsLoading).toBe(false);
		expect(getScripts()).toHaveLength(0);

		loadGoogleMaps(undefined, jest.fn());

		expect(getScripts()).toHaveLength(1);
	});

	it('skips injection when a loader script is already in the document', () => {
		const script = document.createElement('script');

		script.setAttribute(
			'src',
			'https://maps.googleapis.com/maps/api/js?v=weekly&libraries=places&callback=Liferay.Maps.onGMapsReady'
		);

		document.head.appendChild(script);

		const callback = jest.fn();

		loadGoogleMaps(undefined, callback);

		expect(getScripts()).toHaveLength(1);
		expect(window.Liferay.Maps.gmapsLoading).toBeUndefined();
		expect(window.Liferay.once).toHaveBeenCalledWith(
			'gmapsReady',
			callback
		);
	});
});

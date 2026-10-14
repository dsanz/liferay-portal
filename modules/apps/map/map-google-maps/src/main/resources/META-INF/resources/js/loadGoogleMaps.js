/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

const GOOGLE_MAPS_READY_EVENT = 'gmapsReady';

const GOOGLE_MAPS_SCRIPT_SELECTOR =
	'script[src*="maps.googleapis.com/maps/api/js"][src*="callback=Liferay.Maps.onGMapsReady"]';

/**
 * Loads the Google Maps API script once per page and runs the callback when
 * the API is ready. Later callers wait for the same script, and a failed
 * load is retried on the next call.
 * @param {string} googleMapsAPIKey API key added to the script URL, if any
 * @param {Function} callback Runs once the API is ready
 * @return {Function} Detaches the callback
 */
function loadGoogleMaps(googleMapsAPIKey, callback) {
	const liferayMaps = Liferay.namespace('Maps');

	// Check if Google Maps is already loaded.

	if (window.google?.maps && liferayMaps.gmapsReady) {
		callback();

		return () => {};
	}

	// Mark Google Maps as ready when the script finishes loading.

	liferayMaps.onGMapsReady = () => {
		liferayMaps.gmapsLoading = false;
		liferayMaps.gmapsReady = true;

		Liferay.fire(GOOGLE_MAPS_READY_EVENT);
	};

	// Run the callback once Google Maps is ready.

	Liferay.once(GOOGLE_MAPS_READY_EVENT, callback);

	const detachReadyListener = () =>
		Liferay.detach(GOOGLE_MAPS_READY_EVENT, callback);

	// Check if the script is already loading.

	if (
		liferayMaps.gmapsLoading ||
		document.querySelector(GOOGLE_MAPS_SCRIPT_SELECTOR)
	) {
		return detachReadyListener;
	}

	liferayMaps.gmapsLoading = true;

	// Build the script URL.

	let apiURL = `${location.protocol}//maps.googleapis.com/maps/api/js?v=weekly&libraries=places&callback=Liferay.Maps.onGMapsReady`;

	if (googleMapsAPIKey) {
		apiURL += `&key=${encodeURIComponent(googleMapsAPIKey)}`;
	}

	// Add the script, and remove it if it fails so the next call can retry.

	const script = document.createElement('script');

	script.addEventListener('error', () => {
		liferayMaps.gmapsLoading = false;

		script.remove();
	});

	script.setAttribute('src', apiURL);

	document.head.appendChild(script);

	return detachReadyListener;
}

export default loadGoogleMaps;

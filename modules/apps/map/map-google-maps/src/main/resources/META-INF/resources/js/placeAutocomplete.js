/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

/**
 * Creates an autocomplete session for one address input. Its suggestion
 * requests share a session token until a place is fetched, which is how
 * Google groups them into one billed autocomplete session, so each input
 * needs its own session.
 * @return {Object} Session with fetchPlace and fetchPlaceSuggestions
 */
function createPlaceAutocompleteSession() {
	let sessionToken = null;

	/**
	 * Fetches the address and coordinates of the suggestion the user picks,
	 * so the result can be passed to the map to move its marker there. The
	 * predictions from fetchPlaceSuggestions have no coordinates, so this is
	 * the request that gets them. It also ends the autocomplete session, so
	 * the next search starts a new one.
	 *
	 * For example:
	 * placePrediction
	 * {
	 * 	placeId: 'ChIJ5x7_mX4oQg0RqWnQ3kE8b2M',
	 * 	text: 'P.º de la Castellana, 280',
	 * 	toPlace(),
	 * }
	 *
	 * returns
	 * {
	 * 	address: 'P.º de la Castellana, 280, 28046 Madrid, Spain',
	 * 	location: {lat: 40.4764291, lng: -3.6858155},
	 * 	viewport: LatLngBounds,
	 * }
	 * @param {Object} placePrediction Prediction from fetchPlaceSuggestions
	 * @return {Promise<Object>} Position with address, location, and viewport
	 */
	async function fetchPlace(placePrediction) {
		const place = placePrediction.toPlace();

		try {
			await place.fetchFields({
				fields: ['formattedAddress', 'location', 'viewport'],
			});
		}
		finally {
			sessionToken = null;
		}

		const address = place.formattedAddress || String(placePrediction.text);

		return {
			address,
			location: {
				lat: place.location.lat(),
				lng: place.location.lng(),
			},
			viewport: place.viewport || null,
		};
	}

	/**
	 * Fetches address suggestions for the typed text with the Places API.
	 * The legacy Autocomplete widget is not enabled for new Google Cloud
	 * projects, so suggestions go through this API and the caller renders
	 * them itself.
	 *
	 * Returns an empty list for empty input or when Google Maps was loaded
	 * without the places library.
	 *
	 * Callers must handle the rejection: the promise rejects when the request
	 * fails, such as when Places API (New) is not enabled for the API key.
	 * @param {string} inputValue Text typed into the address input
	 * @return {Promise<Array<Object>>} Place predictions for the typed text
	 */
	async function fetchPlaceSuggestions(inputValue) {
		if (
			!inputValue ||
			!window.google?.maps?.places?.AutocompleteSuggestion
		) {
			return [];
		}

		if (!sessionToken) {
			sessionToken = new google.maps.places.AutocompleteSessionToken();
		}

		const {suggestions} =
			await google.maps.places.AutocompleteSuggestion.fetchAutocompleteSuggestions(
				{input: inputValue, sessionToken}
			);

		return suggestions
			.map((suggestion) => suggestion.placePrediction)
			.filter(Boolean);
	}

	return {fetchPlace, fetchPlaceSuggestions};
}

export {createPlaceAutocompleteSession};

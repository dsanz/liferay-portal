/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {createPlaceAutocompleteSession} from '../../src/main/resources/META-INF/resources/js/placeAutocomplete';

describe('placeAutocomplete', () => {
	let fetchPlace;
	let fetchPlaceSuggestions;
	let mockFetchAutocompleteSuggestions;
	let mockSessionTokens;

	const getPlacePrediction = (place) => ({
		text: 'P.º de la Castellana, 280',
		toPlace: () => ({
			fetchFields: jest.fn().mockResolvedValue(undefined),
			formattedAddress: 'P.º de la Castellana, 280, 28046 Madrid, Spain',
			location: {
				lat: () => 40.4764291,
				lng: () => -3.6858155,
			},
			viewport: null,
			...place,
		}),
	});

	beforeEach(() => {
		({fetchPlace, fetchPlaceSuggestions} =
			createPlaceAutocompleteSession());

		mockFetchAutocompleteSuggestions = jest.fn();
		mockSessionTokens = [];

		window.google = {
			maps: {
				places: {
					AutocompleteSessionToken: class {
						constructor() {
							mockSessionTokens.push(this);
						}
					},
					AutocompleteSuggestion: {
						fetchAutocompleteSuggestions:
							mockFetchAutocompleteSuggestions,
					},
				},
			},
		};
	});

	it('ends the autocomplete session when a place is fetched', async () => {
		mockFetchAutocompleteSuggestions.mockResolvedValue({suggestions: []});

		await fetchPlaceSuggestions('castellana');
		await fetchPlace(getPlacePrediction());
		await fetchPlaceSuggestions('castellana 280');

		expect(mockSessionTokens).toHaveLength(2);
	});

	it('falls back to the prediction text when the place has no address', async () => {
		const position = await fetchPlace(
			getPlacePrediction({formattedAddress: undefined})
		);

		expect(position.address).toBe('P.º de la Castellana, 280');
	});

	it('keeps a separate session token for each session', async () => {
		mockFetchAutocompleteSuggestions.mockResolvedValue({suggestions: []});

		const otherSession = createPlaceAutocompleteSession();

		await fetchPlaceSuggestions('castellana');
		await otherSession.fetchPlaceSuggestions('montefino');

		expect(mockSessionTokens).toHaveLength(2);
	});

	it('returns an empty list when the Places API is unavailable', async () => {
		delete window.google;

		await expect(fetchPlaceSuggestions('castellana')).resolves.toEqual([]);
		expect(mockFetchAutocompleteSuggestions).not.toHaveBeenCalled();
	});

	it('returns an empty list without a request for empty input', async () => {
		await expect(fetchPlaceSuggestions('')).resolves.toEqual([]);
		expect(mockFetchAutocompleteSuggestions).not.toHaveBeenCalled();
	});

	it('returns the place position in the map search shape', async () => {
		const position = await fetchPlace(getPlacePrediction());

		expect(position).toEqual({
			address: 'P.º de la Castellana, 280, 28046 Madrid, Spain',
			location: {lat: 40.4764291, lng: -3.6858155},
			viewport: null,
		});
	});

	it('returns the predictions of the fetched suggestions', async () => {
		const placePrediction = getPlacePrediction();

		mockFetchAutocompleteSuggestions.mockResolvedValue({
			suggestions: [{placePrediction}, {placePrediction: null}],
		});

		const placePredictions = await fetchPlaceSuggestions('castellana');

		expect(mockFetchAutocompleteSuggestions).toHaveBeenCalledWith({
			input: 'castellana',
			sessionToken: mockSessionTokens[0],
		});
		expect(placePredictions).toEqual([placePrediction]);
	});

	it('shares one session token across suggestion requests', async () => {
		mockFetchAutocompleteSuggestions.mockResolvedValue({suggestions: []});

		await fetchPlaceSuggestions('castellana');
		await fetchPlaceSuggestions('castellana 280');

		expect(mockSessionTokens).toHaveLength(1);
	});
});

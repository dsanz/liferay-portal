/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

// @ts-ignore - Check possibility to install package in ts format

import fetchMock from 'fetch-mock';

import ClientAdapter from '../src/clientAdapter';
import {Analytics} from '../src/types';

const getMockMessageItem = (id = 0, data = {}) => {
	return {
		foo: 'bar',
		id: `${id}`,
		...data,
	};
};

const ADAPTER_CONFIG = {
	endpointUrl: 'http://ac-server',
	projectId: '1234',
};

describe('Client', () => {
	let client: ClientAdapter;

	afterEach(() => {
		fetchMock.hardReset();

		jest.restoreAllMocks();
	});

	beforeEach(() => {
		client = new ClientAdapter(ADAPTER_CONFIG);
	});

	it('default parameters', () => {
		expect(client._getRequestParameters()).toMatchInlineSnapshot(`
		Object {
		  "cache": "default",
		  "credentials": "same-origin",
		  "headers": Object {
		    "Content-Type": "application/json",
		    "OSB-Asah-Project-ID": "1234",
		  },
		  "method": "POST",
		  "mode": "cors",
		}
	`);
	});

	it('send', () => {
		fetchMock.mockGlobal();

		const sentItems: string[] = [];

		fetchMock.route(/ac-server/i, ({options: {body}}) => {
			sentItems.push(JSON.parse(body as string));

			return Promise.resolve(200);
		});

		const payload = getMockMessageItem(1);

		client.send(payload as unknown as Analytics.Event);

		expect(sentItems[0]).toEqual(payload);
	});
});

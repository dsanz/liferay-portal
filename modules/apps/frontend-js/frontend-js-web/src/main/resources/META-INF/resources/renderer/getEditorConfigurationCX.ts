/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import getRenderer from './getRenderer';
import {IInternalRenderer, TRenderer} from './types';

const getEditorConfigurationCX = async (config: Object) => {
	const urls: string[] = (config as any)?.CETConfigurationURLs;

	delete (config as any)?.CETConfigurationURLs;

	if (!urls || !urls.length) {
		return config;
	}

	return Promise.allSettled(
		urls.map((url) => getRenderer({type: 'internal', url}))
	)
		.then((results: PromiseSettledResult<TRenderer>[]) => {
			return results.reduce((cfg, result) => {
				if (result.status === 'fulfilled') {

					// @ts-ignore

					return (result.value as IInternalRenderer).component(cfg);
				}
				else {
					console.error(
						`Unable to load editor configuration client extension: `,
						result.reason
					);

					return cfg;
				}
			}, config);
		})
		.catch((error: string) => {
			console.error(
				`Unable to load editor configuration client extensions: `,
				error
			);

			return config;
		});
};

export default getEditorConfigurationCX;
